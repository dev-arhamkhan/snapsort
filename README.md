# SnapSort

SnapSort is a fast, 100% offline, privacy-focused Android application designed to automatically organize, classify, and search your screenshots using on-device ML Kit OCR and smart rule-based detection.

---

## Download & Installation

[![Download APK](https://img.shields.io/badge/Download-SnapSort_V1.0.0_APK-2ea44f?style=for-the-badge&logo=android)](https://github.com/dev-arhamkhan/snapsort/releases/latest/download/SnapSort-v1.0.0.apk)

Direct link: [SnapSort-v1.0.0.apk](https://github.com/dev-arhamkhan/snapsort/releases/latest/download/SnapSort-v1.0.0.apk)

### Installation Instructions
1. Download **`SnapSort-v1.0.0.apk`** from GitHub Releases onto your Android device (or transfer it from your PC).
2. Open the downloaded APK file.
3. If prompted by Android, enable **Allow from this source** (or *Install unknown apps*) in your browser / file manager settings.
4. Tap **Install** and launch **SnapSort**.
5. Grant storage permission to allow SnapSort to index your screenshots locally.

---

## SnapSort V1 Features

- 🔒 **100% Offline & Private:** Zero internet permission required (`android.permission.INTERNET` is not even declared). All indexing, OCR, and classifications stay strictly on your device.
- ⚡ **On-Device Text Recognition:** High-performance OCR powered by Google ML Kit runs entirely on your phone.
- 📱 **Intelligent App & Sender Detection:** Automatically identifies source applications (WhatsApp, Instagram, Telegram, SMS, Ride-Sharing apps, etc.) and sender/chat names from headers.
- 🔍 **Instant Full-Text Search & Filtering:** Search across all screenshot text with real-time keyword snippets and combine search queries with category and contact filter chips.
- 🖼️ **Full-Screen Media Viewer:** Complete gesture support including pinch-to-zoom, pan while zoomed, double-tap zoom toggle, swipe left/right between shots, and upward swipe-to-share.
- 🔄 **Resumable Background Indexer:** Android WorkManager-driven scanning service with real-time foreground progress notifications that safely resumes and never re-scans unchanged images.
- ⚙️ **Custom Scan Folders & Settings:** Configure custom screenshot folder paths, review indexing status, and easily re-classify screenshots at any time.

---

## Technical Specifications (V1.0.0)

| Specification | Value |
|---|---|
| **Package Name** | `com.arham.snapsort` |
| **Version Name** | `1.0.0` |
| **Version Code** | `1` |
| **Minimum SDK** | Android 8.0 (API 26) |
| **Target SDK** | Android 15 (API 35) |
| **Architecture** | Jetpack Compose, Room SQLite, Kotlin Coroutines, WorkManager, ML Kit |
| **Security** | Signed with v2 signature scheme, non-debuggable release build |
