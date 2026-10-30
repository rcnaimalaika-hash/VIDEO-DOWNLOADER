# 🎬 Video Downloader - Full Stack Mobile Application

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202026.02-4285F4.svg?logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Material%203-Latest-3DDC84.svg)](https://m3.material.io/)
[![SQLite](https://img.shields.io/badge/Database-SQLite%20OpenHelper-003B57.svg?logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/sqlite)
[![FastAPI](https://img.shields.io/badge/Backend-FastAPI%20REST-009688.svg?logo=fastapi&logoColor=white)](https://fastapi.tiangolo.com/)

An all-in-one Video & Audio Downloader built with a **Jetpack Compose Android Frontend** and a **Python FastAPI Backend Server**, featuring complete **SQLite Database Connectivity**, **Strict Input Validations**, and responsive modern UI.

---

## 📋 First Evaluation Deliverables (40% Completion Demonstration)

| Requirement | Implementation Details | Status |
|---|---|:---:|
| **1. Database Connectivity** | Implemented native **SQLiteOpenHelper** (`AppDatabaseHelper.kt`) with complete CRUD operations for `users` (auth with SHA-256), `bookmarks`, and `download_history`. | ✅ Complete |
| **2. Aesthetically Appealing UI** | Pixel-perfect Jetpack Compose interfaces based on design reference: 3D Clapperboard illustration, smooth pill inputs, liquid wave backgrounds, and dark/light mode. | ✅ Complete |
| **3. Input Validation: Age/Numeric** | Enforces `KeyboardType.Number` and bounds $13 \le \text{age} \le 120$. Only valid numbers are accepted. | ✅ Complete |
| **4. Input Validation: Email Format** | Strict RFC regex validation (`Patterns.EMAIL_ADDRESS`) verifying proper format (`user@domain.com`). | ✅ Complete |
| **5. Input Validation: Strong Password** | Requires minimum 8 characters, at least 1 uppercase, 1 lowercase, 1 number, and 1 special symbol with dynamic animated **Password Strength Meter**. | ✅ Complete |
| **6. Input Validation: Required Fields** | Empty input blocking with real-time inline red error messages and warning icons for all mandatory fields. | ✅ Complete |
| **7. Working Functional Modules** | Splash Screen $\rightarrow$ Login/Sign Up $\rightarrow$ Onboarding $\rightarrow$ Home $\rightarrow$ Bookmarks $\rightarrow$ Downloads Flow. | ✅ Complete |

---

## 📂 Project Repository Structure

```
VIDEO-DOWNLOADER/
├── README.md                           # Main Project & Evaluation Documentation (Rendered on GitHub)
├── PROJECT_DOCUMENTATION.md            # Detailed Technical Architecture Specifications
├── app/                                # Android Studio Project Root
│   └── src/main/java/com/example/videodownloader/
│       ├── LoginScreen.kt              # Onboarding Login & Sign Up Screen (Reference UI)
│       ├── HomeScreen.kt               # Link input, direct bookmarking, download modal
│       ├── BookmarkScreen.kt           # SQLite-driven bookmarks list, search & delete
│       ├── DownloadScreen.kt           # Tabbed download tasks manager
│       ├── OnboardingScreen.kt         # Feature introduction walkthrough
│       ├── SplashScreen.kt             # Animated app launcher
│       ├── MainActivity.kt             # Navigation flow & session state
│       ├── db/
│       │   └── AppDatabaseHelper.kt    # Native SQLite Database (Users, Bookmarks, History)
│       └── util/
│           └── FormValidator.kt        # Strict form input validation engine
│
├── frontend/                           # Dedicated Frontend Source Code Directory
│   ├── README.md                       # Frontend Architecture & Component Guide
│   ├── screens/                        # Modular Composable screens
│   ├── database/                       # AppDatabaseHelper.kt
│   ├── validation/                     # FormValidator.kt
│   ├── api/                            # DownloadApi.kt
│   └── viewmodel/                      # DownloadViewModel.kt, ThemeViewModel.kt
│
└── backend/                            # Python FastAPI Server Directory
    ├── main.py                         # REST API (Process, Platforms, Auth, Bookmarks)
    ├── requirements.txt                # Python dependencies (fastapi, uvicorn, yt-dlp)
    ├── Dockerfile                      # Container deployment with ffmpeg
    └── README.md                       # Backend setup, execution & API docs
```

---

## 🗄️ Database Connectivity (SQLite Schema)

Database file: `video_downloader.db` managed by [`AppDatabaseHelper.kt`](file:///c:/Users/windows%2010/AndroidStudioProjects/VIDEODOWNLOADER/frontend/database/AppDatabaseHelper.kt):

### 1. `users` Table
Stores registered accounts and authenticates user credentials:
```sql
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    full_name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    age INTEGER NOT NULL,
    created_at TEXT NOT NULL
);
CREATE INDEX idx_user_email ON users (email);
```

### 2. `bookmarks` Table
Stores user bookmarked video links with real-time CRUD:
```sql
CREATE TABLE bookmarks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    url TEXT NOT NULL,
    format TEXT NOT NULL,
    duration TEXT NOT NULL,
    date_added TEXT NOT NULL
);
CREATE INDEX idx_bookmark_url ON bookmarks (url);
```

### 3. `download_history` Table
Logs all successfully completed video/audio download jobs:
```sql
CREATE TABLE download_history (
    job_id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    url TEXT NOT NULL,
    file_type TEXT NOT NULL,
    format TEXT NOT NULL,
    total_bytes INTEGER NOT NULL,
    local_path TEXT,
    status TEXT NOT NULL,
    date_added TEXT NOT NULL
);
```

---

## 🛡️ Input Validation Engine ([`FormValidator.kt`](file:///c:/Users/windows%2010/AndroidStudioProjects/VIDEODOWNLOADER/frontend/validation/FormValidator.kt))

Every form field enforces strict evaluation rules:
- **Age Validation**:
  - Enforced keypad: `KeyboardType.Number`.
  - Non-empty, only numbers allowed, range check: $13 \le \text{age} \le 120$.
- **Email Validation**:
  - Strict RFC pattern checking (`user@example.com`).
- **Strong Password Rules**:
  - Minimum 8 characters.
  - At least 1 uppercase letter (`A-Z`).
  - At least 1 lowercase letter (`a-z`).
  - At least 1 numeric digit (`0-9`).
  - At least 1 special symbol (`!@#$%^&*...`).
  - Dynamic visual strength meter: `Weak` (Red), `Moderate` (Orange), `Strong` (Green).
- **Required Fields**:
  - Full Name, Age, Email, Password, Confirm Password, and Terms Checkbox prevent submission when empty.
  - Displays instant inline red feedback with warning icon.
- **URL Validation**:
  - Home screen ensures links start with `http://` or `https://` and have valid web address formats.

---

## 🚀 Navigation Flow

1. **Splash Screen** (`splash`): App entry, dynamic animations, and Ad check.
2. **Login / Sign Up Screen** (`login`): Matches reference image with 3D illustration, Google/Apple social auth, SQLite registration & authentication.
3. **Onboarding Screen** (`onboarding`): Multi-slide feature walkthrough (First launch).
4. **Language Screen** (`language`): Multi-locale selection (10+ languages).
5. **Home Screen** (`home`): Link paste card, direct bookmarking, download modal format sheet, progress indicator, and native ads.

---

## ⚙️ Backend API Server (`backend/`)

Built with **FastAPI** & **yt-dlp**:
- `GET /api/platforms`: Returns supported platforms list.
- `POST /process1` & `POST /process2`: High-speed streaming media downloader.
- `POST /api/auth/register` & `POST /api/auth/login`: Cloud account synchronization.
- `GET /api/bookmarks` & `POST /api/bookmarks/sync`: Cloud bookmark synchronization.

### Running Backend:
```bash
cd backend
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 5693 --reload
```

---

## 🔨 Building the Android App

```bash
# Build debug APK
./gradlew assembleDebug

# Compile and verify Kotlin code
./gradlew compileDebugKotlin
```
