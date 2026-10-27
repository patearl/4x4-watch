# 4×4 Watch

A standalone Wear OS app (built for Pixel Watch 2) that runs the **Norwegian 4×4** interval workout:

| Phase | Duration | Target |
|---|---|---|
| Warm-up | 10 min | easy |
| 4 × hard | 4 min each | 85–95% HRmax |
| 3 × active recovery | 3 min, between hard intervals | easy |
| Cool-down | 5 min, straight after the last hard interval | easy |

40 minutes in total.

## Features

- Shows the current phase, a countdown, the round number, and a ring for progress through the phase.
- Keeps the screen on during the workout.
- Vibrates at every phase change, with patterns you can tell apart without looking:
  - **Go hard**: three long, strong pulses
  - **Recovery**: two gentle pulses
  - **Warm-up/cool-down**: one medium pulse
  - **Finished**: a long, fading triple
  - Plus a short **3-2-1** tick in the last three seconds before each change.
- Live heart rate from the watch sensor, color-coded against your max HR:
  grey < 60%, blue 60–70%, yellow 70–85%, **green 85–95% (the 4×4 target)**, red > 95%.
- Next to the bpm, a heart or an arrow tells you which way to steer for the current phase:

  | Phase | Target (% of max HR) | Heart | Small arrow | Big arrow |
  |---|---|---|---|---|
  | Warm-up | build to 70% | at or above a bar that rises from 60% to 70% over the warm-up | ↑ within 5% below the bar | ↑ further below |
  | Hard | 85–95% | 88–92% | ↑ 85–88%, ↓ 92–95% | ↑ under 85%, ↓ over 95% |
  | Recovery | 65–75% (NTNU recommends ~70%) | 68–72% | ↑ 65–68%, ↓ 72–75% | ↑ under 65%, ↓ over 75% |
  | Cool-down | ease down to 60% | at or under a ceiling that falls from 75% to 60% over the cool-down | ↓ within 5% above the ceiling | ↓ further above |

- Max HR is set in the app (tap +/- or turn the crown). The setting is saved.
- A foreground service keeps the timer and vibrations running with the screen off. An Ongoing Activity
  icon on the watch face takes you back to the app.
- Pause/resume, and stop (with a confirmation).

## Install

Every push builds a debug APK and attaches it to a [GitHub release](../../releases).
The latest build from `main` is always at:

```
https://github.com/patearl/4x4-watch/releases/latest/download/4x4-watch-debug.apk
```

Sideload it onto the watch with ADB over Wi-Fi:

1. On the watch, turn on **Developer options** (Settings → System → About → tap *Build number* 7 times),
   then **ADB debugging** and **Wireless debugging**.
2. In Wireless debugging, tap **Pair new device** and run `adb pair <ip>:<port>` with the code shown.
3. Run `adb connect <ip>:<port>` (the port on the main Wireless debugging screen), then
   `adb install -r 4x4-watch-debug.apk`.

The APK is signed with a debug key that is committed to the repo (`app/debug.keystore`), so each new
build installs over the previous one without uninstalling. **It is a debug key and is public. Don't
reuse it for anything else.**

On first start the app asks for the body sensors permission (for heart rate) and the notifications
permission. The workout still runs without them, but with no heart rate shown.

## Build locally

Requires JDK 17+ and the Android SDK (platform 35).

```
./gradlew assembleDebug testDebugUnitTest
```

The APK ends up in `app/build/outputs/apk/debug/app-debug.apk`.

To render the workout screen on a Pixel Watch 2-sized display into `screenshots/` (useful when
changing the layout):

```
./gradlew testDebugUnitTest -Pscreenshots
```
