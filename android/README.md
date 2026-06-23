# Ascend — Android app

Kotlin + Jetpack Compose Android client for Ascend (the job-search app from the
prototype). This is the initial scaffold: design system, navigation, the Jobs /
JSearch flow, the local Tracker, and the AI feature screens wired to the
Ascend platform API.

## Stack
- **Kotlin + Jetpack Compose** (Material 3), single-Activity
- **MVVM** with `ViewModel` + `StateFlow`
- **Hilt** for DI
- **Retrofit / OkHttp + kotlinx.serialization** for networking
- **Room** for the local Job Tracker
- **Coil** for employer logos
- **Navigation-Compose** with a 4-tab bottom bar (Home · Jobs · Tracker · Interviews)
- minSdk 24, target/compile 35

## Configuration (secrets — never committed)
Copy `local.properties.sample` → `local.properties` and fill in:
```
RAPIDAPI_KEY=…                       # JSearch (RapidAPI)
ASCEND_API_BASE_URL=https://…/       # your web-platform API (trailing slash)
```
These are exposed as `BuildConfig.RAPIDAPI_KEY` / `BuildConfig.ASCEND_API_BASE_URL`.

## What's wired
| Area | Status |
|------|--------|
| Jobs search | **Live** via JSearch (`/search`), filters, save |
| Home | greeting, quick actions, top matches (JSearch) |
| Job detail | header, apply (opens link), save / mark applied |
| Tracker | **Room-backed** pipeline (Saved→Applied→Interview→Offer→Closed) |
| Resume optimize | calls `AscendApi.optimizeResume` (platform) |
| Mock interview | calls `AscendApi.startMock` / `scoreMock`; **Speak** uses OS speech recognizer |
| Live Copilot | calls `AscendApi.copilotAnswer`; live transcription → `SpeechRecognizer` (TODO) |
| Brain Games | hub scaffold — drop in the game engines (TODO) |

The Ascend platform endpoints (`AscendApi`) are placeholders matching the
prototype's flows; align the routes/field names with the real backend, and wire
the Bearer token in `NetworkModule` (`@Named("platform")`).

## Build
Open the `android/` folder in **Android Studio** (it provisions the Gradle
wrapper + SDK). Or from CLI once the SDK + wrapper are present: `./gradlew :app:assembleDebug`.

## Layout
```
app/src/main/java/app/ascend/
  data/        model · remote/jsearch · remote/platform · local (Room) · repo
  di/          NetworkModule · DatabaseModule
  ui/theme     AscendColors · Type · Theme   (tokens mirror the prototype)
  ui/components Pill · JobCard · top bar
  ui/navigation Routes · Tab
  ui/screens   home · jobs · jobdetail · tracker · interviews · resume · mock · copilot · games
  ui/util      SpeechInput (OS speech recognition)
```
