# LiveSelling GCash Mobile Payment Bridge 📱

Lightweight native Android client utility for capturing inbound GCash push notifications and forwarding them in real-time to the **LiveSelling** order auto-reconciliation engine.

## Features
- **Zero-Touch Automation:** Listens for push notifications from `com.globe.gcash.android`.
- **Regex Information Extraction:**
  - Transfer Amount (`₱59.00`, `₱259.00`, `₱799.00`, etc.)
  - GCash 10-14 digit Reference Number
  - Sender Name
  - Timestamp
- **Offline Resilience:** Buffers captured transactions in local SQLite storage when in an elevator or offline.
- **Auto-Sync Engine:** Retries synchronization automatically with exponential backoff as soon as network returns.
- **Dedicated Management UI:**
  - Real-time listening indicator (🟢 Active & Listening / 🔴 Permission Needed).
  - Daily total calculation.
  - Live transfer feed with `[SYNCED ✅]` / `[QUEUED ⏳]` badges.
  - "Send Test Transfer" simulator button.

## Setup Instructions
1. Download `app-debug.apk` from the **[Releases](../../releases)** tab.
2. Install the APK on your Android phone.
3. Grant **Notification Access Permission** when prompted.
4. Keep the app open or running in the background. As soon as money hits your GCash, it will automatically sync to your LiveSelling platform!
