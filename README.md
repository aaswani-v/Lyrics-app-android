

https://github.com/user-attachments/assets/b2811fb2-2b3c-4dd4-aa14-c6b99b4f2c4b

# LyricsNotificationApp

An Android application that displays synchronized lyrics directly in your notification shade.

## Features

- **Real-time Lyrics**: Displays lyrics that sync with your currently playing music.
- **Notification Integration**: Uses a custom notification layout to show lyrics unobtrusively.
- **Background Service**: Runs in the background to detect music playback and fetch lyrics.
- **Smart Idle State**: Shows a "Waiting for music..." state when no music is detected.
- **Music Player**: YoutubeMusic (works), Spotify (yet to be tested)

## Tech Stack

- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel)
- **Components**:
  - `LyricsNotificationManager`: Handles the creation and updating of notifications.
  - `LyricsRepository`: Manages data fetching and storage.
  - Android Services for background execution.

## Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/aaswani-v/Lyrics-app-android.git
   ```
2. Open the project in Android Studio.
3. Sync Gradle files.
4. Run the app on an emulator or physical device.

## License

