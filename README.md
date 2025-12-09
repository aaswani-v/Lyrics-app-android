# LyricsNotificationApp

An Android app that displays real-time song lyrics directly inside the notification shade.  
Designed for a clean, distraction-free music experience while multitasking.

https://github.com/user-attachments/assets/b2811fb2-2b3c-4dd4-aa14-c6b99b4f2c4b

---

## 🚀 Features

- **Live Lyrics Display** – Shows currently playing lyrics in the notification panel.
- **Custom Notification UI** – Blurred background and adaptive design.
- **Background Service** – Runs silently while music is playing.
- **Smart Idle State** – Switches to “Waiting for music…” when idle.
- **Music Player Support**
    - YouTube Music/Youtube ✔️ (working)
    - Spotify ⏳ (working)
    - Apple music (tested it's working)

---

## 🛠 Tech Stack

- **Language:** Kotlin
- **Architecture:** MVVM (Model-View-ViewModel)
- **Core Components:**
    - `LyricsNotificationManager` — Creates and updates notification UI
    - `LyricsRepository` — Handles lyrics data retrieval
    - Android Services for continuous background processing

---

## 📦 Installation / Setup

```bash
git clone https://github.com/aaswani-v/Lyrics-app-android.git
