# Video Downloader Backend API Server

High-performance REST API backend built with **FastAPI** & **yt-dlp** designed to power the Android Video Downloader application.

---

## 🚀 Features

- **Platform Metadata API (`GET /api/platforms`)**: Dynamically returns list of supported social & video platforms (TikTok, Instagram, Facebook, X, Reddit, Snapchat, YouTube, Spotify, etc.).
- **Media Extraction & Streaming (`POST /process1`, `POST /process2`)**:
  - Handles streaming downloads for video (`MP4`, `WEBM`) and audio (`MP3`).
  - Implements failover processing between primary and secondary endpoints.
- **User Authentication Sync (`POST /api/auth/register`, `POST /api/auth/login`)**:
  - Validates full name, email format, strong passwords (SHA-256 hashed), and age.
  - Matches the client-side SQLite database structure.
- **Bookmarks Sync (`GET /api/bookmarks`, `POST /api/bookmarks/sync`)**:
  - Allows syncing bookmarked links across devices.

---

## 🛠️ Setup & Running Locally

### 1. Prerequisites
- Python 3.9+
- `ffmpeg` installed on your system (for audio/video conversions)

### 2. Install Dependencies
```bash
cd backend
pip install -r requirements.txt
```

### 3. Run Development Server
```bash
uvicorn main:app --host 0.0.0.0 --port 5693 --reload
```

The API will be accessible at:
- **Base URL**: `http://localhost:5693`
- **Interactive Swagger Docs**: `http://localhost:5693/docs`
- **Alternative Redoc**: `http://localhost:5693/redoc`

---

## 🐳 Docker Deployment

To build and run using Docker:
```bash
docker build -t video-downloader-backend .
docker run -d -p 5693:5693 --name video-downloader-api video-downloader-backend
```

---

## 📡 API Endpoint Overview

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/` | Service health status |
| `GET` | `/api/platforms` | Returns supported platforms JSON |
| `POST` | `/process1` | Primary video/audio download streaming |
| `POST` | `/process2` | Fallback video/audio streaming |
| `POST` | `/api/auth/register` | Register new account (Name, Email, Password, Age) |
| `POST` | `/api/auth/login` | Authenticate user credentials |
| `GET` | `/api/bookmarks` | Fetch saved bookmarks |
| `POST` | `/api/bookmarks/sync` | Save/Sync new bookmark |
