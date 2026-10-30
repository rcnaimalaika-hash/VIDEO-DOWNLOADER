# 📱 Video Downloader - Android Native Frontend

Modern Android application built with **Kotlin 2.2**, **Jetpack Compose**, **Material Design 3**, and native **SQLite Database Connectivity**.

---

## 🏗️ Architecture & Structure

The frontend follows the **MVVM (Model-View-ViewModel)** architectural pattern with unidirectional data flow and reactive `StateFlow` state holders.

```
frontend/
├── MainActivity.kt               # Single-activity container & Navigation host
├── screens/                      # Composable UI screens
│   ├── LoginScreen.kt            # 3D illustration login/signup with strict validation
│   ├── HomeScreen.kt             # Link paste, download sheets, and bookmarking
│   ├── BookmarkScreen.kt         # SQLite-backed bookmarks CRUD
│   ├── DownloadScreen.kt         # Active and finished downloads manager
│   ├── OnboardingScreen.kt       # Interactive multi-page intro walkthrough
│   ├── SplashScreen.kt           # Animated entrance & branding
│   ├── SettingsScreen.kt         # Storage and app configurations
│   ├── SidebarDrawer.kt          # Profile, dark mode toggle, and logout
│   └── ProScreen.kt              # Premium subscription packages
├── database/                     # Local data persistence
│   └── AppDatabaseHelper.kt      # Native SQLiteOpenHelper (users, bookmarks, downloads)
├── validation/                   # Input validation logic
│   └── FormValidator.kt          # Age, Email, Strong Password, and URL rules
├── api/                          # HTTP client and network contracts
│   └── DownloadApi.kt            # OkHttp3 streaming client and server endpoints
└── viewmodel/                    # State management
    ├── DownloadViewModel.kt      # Download operations and jobs flow
    └── ThemeViewModel.kt         # Dynamic Dark/Light mode theme state
```

---

## 🛡️ Evaluation Validation Features

- **Numeric / Age Validation**: Numeric keypad only (`KeyboardType.Number`), bounds $13 \le \text{age} \le 120$.
- **Email Validation**: Strict RFC regex pattern matching.
- **Strong Password Enforcement**: At least 8 characters, uppercase, lowercase, number, and special character with dynamic **Password Strength Meter** (`Weak`, `Moderate`, `Strong`).
- **Required Fields**: Empty submissions prevented with real-time inline red error messages and icons.
- **Database Connectivity**: Full SQLite CRUD for registered accounts, bookmarked videos, and download records.
