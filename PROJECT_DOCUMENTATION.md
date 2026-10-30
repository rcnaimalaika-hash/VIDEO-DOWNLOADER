# Video Downloader - Full Stack Mobile Architecture

Comprehensive documentation for the **Video Downloader** project covering Frontend (Android Jetpack Compose + SQLite), Backend (Python FastAPI REST API), Input Validations, and Evaluation Deliverables.

---

## 📱 1. Frontend Architecture (Android Native)

- **Language / Framework**: Kotlin 2.2 + Jetpack Compose + Material 3.
- **Pattern**: MVVM (Model-View-ViewModel) + Single Source of Truth (SSOT) with `StateFlow`.
- **Database Connectivity**: Native Android SQLite via `AppDatabaseHelper` (`SQLiteOpenHelper`).
- **Network Engine**: OkHttp3 streaming client with chunked I/O buffer for high-speed downloads.

### Screen Flow:
1. **Splash Screen** (`SplashScreen.kt`): Branding, dynamic pulsing loader, and launch interstitial ad check.
2. **Login / Sign Up Screen** (`LoginScreen.kt`):
   - 3D clapperboard illustration matching UI reference.
   - Smooth toggle between Login and Sign Up mode.
   - Social auth buttons (Google & Apple).
   - Dynamic animated Password Strength Meter.
   - Skip / Guest option.
3. **Onboarding Screen** (`OnboardingScreen.kt`): Feature overview pager with animated indicators.
4. **Language Selection** (`LanguageScreen.kt`): Multi-language configuration supporting 10+ locales (English, Hindi, Urdu, Arabic, etc.).
5. **Home Screen** (`HomeScreen.kt`): Paste link card, quick actions, format selection bottom sheet, real-time download progress sheet, and direct bookmarking button.
6. **Bookmarks Screen** (`BookmarkScreen.kt`): SQLite-backed bookmarks with instant search and delete functionality.
7. **Downloads Screen** (`DownloadScreen.kt`): Tabbed management of finished and active download tasks.
8. **Navigation Drawer** (`SidebarDrawer.kt`): Displays logged-in user profile, theme toggle (Dark/Light mode), and logout.

---

## 🗄️ 2. Database Schema (SQLite Database Connectivity)

The application utilizes a local SQLite database (`video_downloader.db`) managed by [`AppDatabaseHelper.kt`](file:///c:/Users/windows%2010/AndroidStudioProjects/VIDEODOWNLOADER/app/src/main/java/com/example/videodownloader/db/AppDatabaseHelper.kt):

### `users` Table
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique user ID |
| `full_name` | TEXT | NOT NULL | User's full name |
| `email` | TEXT | UNIQUE NOT NULL | User's email (indexed) |
| `password_hash`| TEXT | NOT NULL | SHA-256 encrypted password |
| `age` | INTEGER | NOT NULL | User's age (numeric validation) |
| `created_at` | TEXT | NOT NULL | Registration timestamp |

### `bookmarks` Table
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique bookmark ID |
| `title` | TEXT | NOT NULL | Media title |
| `url` | TEXT | NOT NULL (indexed) | Source video/audio URL |
| `format` | TEXT | NOT NULL | Format (MP4, MP3, etc.) |
| `duration` | TEXT | NOT NULL | Duration formatted string |
| `date_added` | TEXT | NOT NULL | Date bookmarked |

### `download_history` Table
| Column | Type | Constraints | Description |
|---|---|---|---|
| `job_id` | TEXT | PRIMARY KEY | Unique UUID string |
| `title` | TEXT | NOT NULL | Media file name |
| `url` | TEXT | NOT NULL | Download source link |
| `file_type` | TEXT | NOT NULL | 'video' or 'audio' |
| `format` | TEXT | NOT NULL | Format / resolution |
| `total_bytes`| INTEGER | NOT NULL | File size in bytes |
| `local_path` | TEXT | NULLABLE | Storage file path |
| `status` | TEXT | NOT NULL | Status enum string |
| `date_added` | TEXT | NOT NULL | Completion timestamp |

---

## 🛡️ 3. Input Validation Rules ([`FormValidator.kt`](file:///c:/Users/windows%2010/AndroidStudioProjects/VIDEODOWNLOADER/app/src/main/java/com/example/videodownloader/util/FormValidator.kt))

Every field adheres to strict validation rules:
- **Age Field (Numeric Rule)**:
  - Enforced via `KeyboardType.Number`.
  - Non-empty, numbers only, range: $13 \le \text{age} \le 120$.
- **Email Field**:
  - RFC standard regex matching (`Patterns.EMAIL_ADDRESS`).
- **Strong Password Rule**:
  - Minimum 8 characters.
  - At least 1 uppercase letter (`A-Z`).
  - At least 1 lowercase letter (`a-z`).
  - At least 1 numeric digit (`0-9`).
  - At least 1 special character (`!@#$%^&*()_+-=[]{}|;':,./<>?`).
- **Required Fields**:
  - Non-empty constraints on Full Name, Age, Email, Password, Confirm Password, and Terms Agreement.
  - Real-time inline red error messages and icons.
- **URL Field**:
  - Verifies `http://` or `https://` prefix with valid domain structure.

---

## ⚙️ 4. Backend Architecture (Python FastAPI)

Located in `backend/`:
- **`main.py`**:
  - `GET /api/platforms`: Returns supported media platforms.
  - `POST /process1` & `POST /process2`: Streaming media downloader with failover.
  - `POST /api/auth/register` & `POST /api/auth/login`: Account authentication.
  - `GET /api/bookmarks` & `POST /api/bookmarks/sync`: Cloud bookmark synchronization.
- **`requirements.txt`**: Python dependencies (`fastapi`, `uvicorn`, `yt-dlp`, `requests`, `pydantic`).
- **`Dockerfile`**: Docker container setup with `ffmpeg` integration.
