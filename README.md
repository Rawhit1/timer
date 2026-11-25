# IntervalPulse — Android Workout Interval Timer

A Kotlin + Jetpack Compose Android app for alternating **Workout** and **Rest** interval loops with **Picture-in-Picture (PiP)** mode, **customizable mini-window width**, **foreground service notification**, and **separate sound selection** for workout and rest cues.

## Key Features
- **Alternating Interval Loop**: Configure Workout and Rest durations independently (default `60s / 40s`), plus continuous loop (`∞`) or fixed round count.
- **Background-Safe Foreground Service**: `TimerForegroundService` runs a drift-free `elapsedRealtime()` ticker with a persistent notification (Pause/Resume, Skip, Stop).
- **Picture-in-Picture (PiP) Mode**: Automatically enters PiP when you press Home or switch apps while the timer is running (`onUserLeaveHint` + Android 12+ `setAutoEnterEnabled`), with Pause/Resume and Skip `RemoteAction` buttons.
- **Customizable PiP (Mini) Width**:
  - **System PiP Width**: Defaults to YouTube's `16:9` aspect ratio; includes `4:3`, `16:9`, `21:9`, `2.39:1` presets and a continuous `1.00:1 – 2.39:1` aspect ratio slider.
  - **Floating Overlay Mode (Optional)**: Toggle on in Settings (`SYSTEM_ALERT_WINDOW`) for a draggable mini-timer with an exact `140 dp – 360 dp` width slider.
- **Separate Workout & Rest Sounds**: Choose independently from Boxing Bell, Coach Whistle, Digital Beep, Energetic Chime, Soft Chime, Calm Gong, Descending Tone, or Silent (Vibrate Only), plus optional 3-2-1 countdown ticks.

## How to Build & Run
1. Unzip `IntervalPulse-Android.zip`.
2. Open the `IntervalPulse` folder in **Android Studio** (Ladybug or newer).
3. Let Gradle sync (requires Android SDK 35 & JDK 17).
4. Run on a physical device or emulator running **Android 8.0 (API 26)+**.
