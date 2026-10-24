# FCM payload examples

All examples use the **HTTP v1 API** body shape (`POST https://fcm.googleapis.com/v1/projects/video-downloader-bb92b/messages:send`).
From the Firebase console (*Engage → Messaging → New campaign*) the same `data` keys go under **Additional options → Custom data**.

Data keys understood by the app (see `firebase/PushIntents.kt`):

| key        | values                                                                          | effect |
|------------|---------------------------------------------------------------------------------|--------|
| `screen`   | `home` `downloads` `settings` `pro` `rate` `share` `help` `privacy` `about` `language` `bookmark` | opens that screen after the launch flow |
| `url`      | any `https://…`                                                                  | opens in the browser (Play listing, promo page…) |
| `campaign` | free text                                                                        | appears as `campaign` param on `push_received` / `push_opened` |
| `channel`  | `general_updates` (default) or `offers_promotions`                               | notification channel (foreground rendering) |
| `image`    | `https://…png/jpg`                                                               | big-picture notification (foreground rendering) |
| `title` / `body` | text                                                                       | used for **data-only** messages |
| `action`   | `refresh_config`                                                                 | silent: re-fetch Remote Config, no notification |

Topics every device is subscribed to: `all_users`, `android` (from Remote Config `push_topics`) and `lang_<code>` (`lang_en`, `lang_hi`, `lang_ur`, `lang_ar`, `lang_bn`, `lang_pa`, `lang_ta`, `lang_te`, `lang_ml`, `lang_ja`).

---

## 1. Marketing push → opens the Pro screen (notification + data)

```json
{
  "message": {
    "topic": "all_users",
    "notification": {
      "title": "🎉 50% off Premium this week",
      "body": "Remove ads and unlock unlimited downloads.",
      "image": "https://example.com/promo/premium-50.png"
    },
    "data": {
      "screen": "pro",
      "campaign": "premium_50_aug26",
      "channel": "offers_promotions"
    },
    "android": {
      "priority": "high",
      "notification": { "channel_id": "offers_promotions" }
    }
  }
}
```

## 2. Localised push to Hindi users → opens Downloads

```json
{
  "message": {
    "topic": "lang_hi",
    "notification": {
      "title": "आपके डाउनलोड तैयार हैं",
      "body": "नई वीडियो सेव करने के लिए टैप करें"
    },
    "data": { "screen": "downloads", "campaign": "hi_reengage_1" }
  }
}
```

## 3. Data-only message (full control, rendered by the app even in background)

```json
{
  "message": {
    "token": "<device FCM token from logcat: PushMessaging: FCM token>",
    "data": {
      "title": "Rate us ⭐",
      "body": "Enjoying Video Downloader? Leave a quick review.",
      "screen": "rate",
      "campaign": "rate_prompt_v1",
      "image": "https://example.com/img/rate.png"
    },
    "android": { "priority": "high" }
  }
}
```

## 4. Open an external link (e.g. Play Store listing)

```json
{
  "message": {
    "topic": "android",
    "notification": { "title": "Update available", "body": "Get the latest version now." },
    "data": {
      "url": "https://play.google.com/store/apps/details?id=com.appssouls.video.downloader.app.hd.save",
      "campaign": "update_1_1"
    }
  }
}
```

## 5. Silent Remote Config refresh (no notification)

```json
{
  "message": {
    "topic": "all_users",
    "data": { "action": "refresh_config" },
    "android": { "priority": "high" }
  }
}
```
