# Firebase integration — setup & operations guide

This app (`com.appssouls.video.downloader.app.hd.save`, Firebase project **video-downloader-bb92b**) is wired for:

| Product | What it does here | Code |
|---|---|---|
| **Analytics** | Marketing / Google Ads events, screen views, user properties | `firebase/AnalyticsManager.kt` |
| **Remote Config** | Ads on/off (global + per placement), AdMob ad-unit IDs, timings, feature flags, push topics — **real-time**, no app update needed | `firebase/RemoteConfigManager.kt`, `RemoteConfigKeys.kt`, `AppConfig.kt`, template `firebase/remoteconfig.template.json` |
| **Cloud Messaging** | Push notifications with deep links, big pictures, channels, topics | `firebase/AppFirebaseMessagingService.kt`, `NotificationHelper.kt`, `PushMessaging.kt`, `PushIntents.kt` |
| **Crashlytics** | Crash + non-fatal reporting (release builds) | `firebase/CrashReporter.kt` |
| Bootstrap | Initialises everything at process start | `VideoDownloaderApp.kt` (registered in the manifest) |

---

## 1. One-time console setup

1. **google-services.json** — already in `app/` for this project/package. If you add a second flavour or package name, download a new one from *Project settings → Your apps*.
2. **Analytics** — *Project settings → Integrations*:
   * Link **Google Ads** (so GA4 events become importable conversions) — then in Google Ads: *Goals → Conversions → Import → Google Analytics 4 (Firebase)*.
   * Link **AdMob** (ad revenue flows into GA4 as `ad_impression`, which the app also logs itself).
   * Analytics → *Admin → Data settings* → turn on **Google signals** for cross-device audiences.
   * Analytics → *Admin → Custom definitions* → register user properties `build_type`, `app_language`, `theme`, `is_pro`, `ads_enabled` (needed before you can use them in Remote Config conditions / audiences).
3. **Key events (conversions)** — Analytics → *Admin → Events* → toggle **Mark as key event** for:
   `first_open` (auto), `tutorial_complete`, `download_complete`, `begin_checkout`, `purchase`, `share`, `rate_submitted`, `ad_impression`.
   Import the same list in Google Ads for App-campaign bidding (tCPA on `download_complete`, tROAS on `purchase` / `ad_impression`).
4. **Crashlytics** — open *Crashlytics* in the console once so it's enabled; the first crash from a release build activates the dashboard.
5. **Remote Config** — publish the template (section 3).
6. **Cloud Messaging** — nothing to enable; send a test (section 5).

---

## 2. AdMob IDs — where each one lives

| ID | Where to change | Notes |
|---|---|---|
| **App ID** `ca-app-pub-…~…` | `local.properties` (git-ignored) or `gradle.properties`: `ADMOB_APP_ID=ca-app-pub-XXXX~YYYY` | Manifest-level; a new build is required. Falls back to Google's sample ID. |
| **Ad-unit IDs** `ca-app-pub-…/…` (8 placements) | Firebase Remote Config → group *Ads - AdMob ad unit IDs* | Live-updatable. Debug builds always use test units; set `use_test_ads=true` to force test units in release. |

Placement map:

| # | Placement | Type | Enable key | Ad-unit key |
|---|---|---|---|---|
| 1 | Splash bottom | native | `ad_splash_native_enabled` | `admob_native_splash_id` |
| 2 | After splash | interstitial | `ad_app_launch_interstitial_enabled` | `admob_interstitial_app_launch_id` |
| 3 | Onboarding sponsored page | native | `ad_onboarding_native_enabled` | `admob_native_onboarding_id` |
| 4 | After "Get Started" | interstitial | `ad_get_started_interstitial_enabled` | `admob_interstitial_get_started_id` |
| 5 | Language list | native | `ad_language_native_enabled` | `admob_native_language_id` |
| 6 | Home | native | `ad_home_native_enabled` | `admob_native_home_id` |
| 7 | Downloads list | native | `ad_downloads_native_enabled` | `admob_native_downloads_id` |
| 8 | Before download / retry | interstitial | `ad_on_download_interstitial_enabled` | `admob_interstitial_on_download_id` |

Master switches: `ads_enabled` → `interstitial_ads_enabled` / `native_ads_enabled` → per-placement keys. Any `false` up the chain wins.

---

## 3. Remote Config — publishing the template

**Option A – Firebase CLI (recommended, keeps the template in git):**

```bash
npm i -g firebase-tools
firebase login
firebase deploy --only remoteconfig        # uses firebase.json → firebase/remoteconfig.template.json
```

> `deploy --only remoteconfig` **replaces the whole template** on the server. Always edit `firebase/remoteconfig.template.json` and re-deploy rather than mixing console edits and CLI deploys. To pull what's currently live: `firebase remoteconfig:get -o firebase/remoteconfig.template.json`.

**Option B – console:** *Engage → Remote Config → Add parameter* and copy the keys/defaults from the template (30 keys, descriptions included).

How the app consumes it:

* Defaults are compiled in (`RemoteConfigKeys.DEFAULTS`) → app works offline / before first publish.
* At startup: activate cached values → `fetchAndActivate` (debug: every launch, release: ≤ 1 h).
* **Real-time**: `addOnConfigUpdateListener` — publishing in the console reaches running apps in seconds. Ads switched off disappear on the next screen; interstitial caches are dropped immediately.
* The splash / native loaders wait ≤ 2.5 s for the first fetch so a remote "off" is honoured even on a fresh install.
* Silent push `{"data":{"action":"refresh_config"}}` forces a fetch on all devices.

Full key reference:

| Key | Type | Default | Purpose |
|---|---|---|---|
| `ads_enabled` | bool | true | master kill switch |
| `interstitial_ads_enabled` | bool | true | all interstitials |
| `native_ads_enabled` | bool | true | all native cards |
| `use_test_ads` | bool | false (true for `debug_builds` condition) | force sample ad units |
| `ad_*_enabled` (8) | bool | true | per placement, see table above |
| `admob_*_id` (8) | string | Google sample units | per placement ad unit |
| `interstitial_min_interval_sec` | number | 30 | frequency cap between interstitials (0 = off) |
| `interstitial_load_timeout_ms` | number | 4500 | max "Loading ad…" wait |
| `splash_ad_display_ms` | number | 4500 | splash duration when the ad loaded |
| `splash_no_ad_delay_ms` | number | 2000 | splash duration when no ad |
| `splash_timeout_ms` | number | 7000 | hard cap |
| `onboarding_ad_countdown_sec` | number | 5 | lock time on the sponsored onboarding page |
| `pro_screen_enabled` | bool | true | "Go Premium" opens the Pro screen |
| `analytics_enabled` | bool | true | GA collection |
| `crashlytics_enabled` | bool | true | Crashlytics collection (release) |
| `push_topics` | string | `all_users,android` | comma-separated FCM topics |

Adding a new key: add the constant + default to `RemoteConfigKeys`, a field to `AppConfig` (both `build()` and the data class), and the parameter to the JSON template.

---

## 4. Analytics events (marketing / Google Ads)

Names follow GA4 *recommended events* wherever one exists so Google Ads can model them out of the box.

| Event | Fired when | Key params | Suggested key event |
|---|---|---|---|
| `screen_view` | every in-app screen change | `screen_name` | – |
| `tutorial_begin` / `tutorial_complete` | onboarding shown / "Get Started" | – | ✅ complete |
| `language_selected` | language chosen | `language` | – |
| `download_start` | format confirmed | `platform` (youtube/instagram/…), `file_type`, `format` | – |
| `download_complete` | file saved | + `duration_ms`, `file_size_bytes` | ✅ (primary in-app action) |
| `download_failed` / `download_retry` / `download_cancel` | … | `platform`, `file_type`, `reason` | – |
| `media_opened` | downloaded file played | `file_type` | – |
| `share` | app invite (`content_type=app_invite`, `method`) or media share | | ✅ |
| `rate_prompt_shown` / `rate_submitted` / `rate_later` | rate screen | `rating` | ✅ submitted |
| `pro_screen_view` + `view_item` | Pro screen shown | `source`, `items[]` | – |
| `pro_plan_selected` + `select_item` | plan tapped | `plan`, `items[]` | – |
| `begin_checkout` | "Continue with <plan>" | `value`, `currency`, `items[]` | ✅ |
| `purchase` | call `AnalyticsManager.logPurchase(...)` from Play Billing when integrated | `value`, `currency`, `transaction_id` | ✅ revenue |
| `ad_impression` | AdMob paid event (interstitial + native) | `ad_platform`, `ad_source`, `ad_format`, `ad_unit_name`, `value`, `currency`, `placement` | ✅ ROAS |
| `ad_shown` / `ad_clicked` / `ad_load_failed` / `ad_skipped` | ad diagnostics | `ad_format`, `placement`, `error_code` / `reason` | – |
| `push_received` / `push_opened` | FCM | `campaign`, `message_id`, `screen` | – |
| `notification_permission` | Android 13 prompt answered | `granted` | – |
| `theme_changed` | dark/light toggle | `theme` | – |

User properties: `build_type`, `app_language`, `theme`, `is_pro`, `ads_enabled`.

The full URL is **never** sent — only a platform label derived from the host.

**Debugging events live:**

```bash
adb shell setprop debug.firebase.analytics.app com.appssouls.video.downloader.app.hd.save
# → Firebase console → Analytics → DebugView
adb shell setprop debug.firebase.analytics.app .none.   # turn off
adb logcat -s Analytics RemoteConfig AdManager PushMessaging FCMService
```

---

## 5. Push notifications (FCM)

* Channels: `general_updates` (default, high importance) and `offers_promotions`.
* Android 13+ permission is requested once, the first time the user reaches Home (result → `notification_permission` event).
* Deep links & payload contract: **`firebase/fcm_payload_examples.md`** (`screen`, `url`, `campaign`, `channel`, `image`, `action`).
* Topics: everything in `push_topics` + `lang_<code>` for the chosen language → target segments straight from the console without a backend.
* Device token: printed in logcat as `PushMessaging: FCM token …`; also `PushMessaging.getToken()`. Hook a backend upload in `PushMessaging.onNewToken` if you need per-user targeting.
* Registration uses the **FCM 25.1+ installation-ID API** (`firebase_messaging_installation_id_enabled=true` in the manifest → `FirebaseMessaging.register()`, token via `onRegistered()`). Google deprecated `getToken()`/`onNewToken()` in June 2026 and will remove them; the code still supports that legacy path — just delete the manifest meta-data to switch back.
* Test: console → *Messaging → New campaign → Send test message* → paste the token.

---

## 6. Crashlytics

* Disabled in debug builds (flip `CrashReporter.REPORT_IN_DEBUG` to verify locally) and controlled by `crashlytics_enabled` in release.
* Breadcrumbs: `CrashReporter.log(...)`; non-fatals: `CrashReporter.recordException(t)`; current screen is attached automatically as a custom key.
* Release `optimization.enable` is currently `false` (no R8) so stack traces are already readable; if you enable R8 later the Gradle plugin uploads mapping files automatically.

---

## 7. Recommended next steps (not part of this change)

1. **UMP consent (EEA/UK)** — Google requires a consent form before ads in those regions. `play-services-ads` already bundles UMP; call `AnalyticsManager.setAdConsent(...)` with the result.
2. **Play Billing** — on purchase acknowledged call `AnalyticsManager.logPurchase(plan, price, currency, orderId)` and set `is_pro`; gate ads with `is_pro` in `AdManager`.
3. **Force-update flag** — add `min_supported_version_code` to Remote Config and show a dialog in `MainActivity` when `BuildConfig.VERSION_CODE` is lower.
4. `google-services.json` contains only public, package-restricted keys; keeping it in git is normal, but add it to `.gitignore` if your repo is public and you prefer CI injection.
