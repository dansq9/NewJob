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
| Onboarding | **Live** — name · target role · location · resume (picked or skipped) |
| Profile | **DataStore** (`ProfileRepository`) — hydrates Home/Jobs on launch; `anonymous_install_id` UUID |
| Jobs search | **Live** via JSearch — debounce, pagination/load-more, filter sheet (date/type/remote), dedup, rate-limit state, best apply-URL, save/apply prompt |
| Brain Games | **Playable** — vendored Compose engine (9 boards) behind our hub via `GameHost` |
| Analytics | `Analytics` facade + event taxonomy (`Ev`) + no-op impl; drop in Firebase + Sentry |
| Home | greeting, quick actions, top matches (JSearch) |
| Job detail | header, apply (opens link), save / mark applied |
| Tracker | **Room-backed** pipeline (Saved→Applied→Interview→Offer→Closed) — search, sort, per-job notes, interview/follow-up date, closed-reason, delete confirm; edit stage from job detail |
| Monetization | Entitlement (DataStore) + **paywall** (prototype design) + Pro gates. Ads (interstitial after splash, native every 5 jobs, rewarded for AI features) and Play Billing are behind `AdsManager`/`BillingManager` interfaces with no-op/stub impls — swap for AdMob + Play Billing once unit/product IDs exist. Pro = no ads + Interview Navigator. |
| Resume | **Room-backed library** — add (PDF/DOC/DOCX, ≤10MB validated), select active, remove; `optimize` calls `AscendApi.optimizeResume` (rewarded-gated for free), records ATS score, download/share result |
| Mock interview | calls `AscendApi.startMock` / `scoreMock`; **Speak** uses OS speech recognizer |
| Live Copilot | calls `AscendApi.copilotAnswer` (Pro-gated); live transcription → `SpeechRecognizer` (TODO) |

The Ascend platform endpoints (`AscendApi`) are placeholders matching the
prototype's flows; align the routes/field names with the real backend, and wire
the Bearer token in `NetworkModule` (`@Named("platform")`).

## Permissions & Play Data Safety
- **RECORD_AUDIO** — only the Live Interview Copilot. Requested at runtime on
  the first mic tap, after an in-app rationale; never on launch. Audio is sent
  to the on-device/native `SpeechRecognizer` for transcription **only** — Ascend
  does not record or store audio. The Mock "Speak" mode uses the system
  `RecognizerIntent` (handles its own permission UI).
- **Data Safety form (Play Console)** must declare: microphone/audio used for
  app functionality (live transcription), not shared, not stored; resume files +
  profile collected and stored locally (and in Android Auto Backup, except the
  anonymous install id which is excluded). Update this when wiring analytics
  (Firebase) + crash reporting (Sentry) so collected data types are declared.

## Localization (i18n)
- User-facing strings live in `res/values/strings.xml` (English, default) with
  per-locale overrides in `res/values-<locale>/strings.xml` (e.g. `values-es`).
  Untranslated keys fall back to English automatically.
- In-app language picker: **Settings → Language** (`LocaleManager` +
  `MainActivity.attachBaseContext`), works on all API levels and is surfaced in
  Android 13+ system per-app language settings via `res/xml/locales_config.xml`.
- Supported locales are declared in 3 places — keep them in sync: `locales_config.xml`,
  `LocaleManager.supported`, and `build.gradle.kts` `resourceConfigurations`.
- Debug builds enable **pseudolocales** (`en-rXA` accented/long, `ar-rXB` RTL) for
  layout testing. RTL is enabled (`supportsRtl`); `setLayoutDirection` is applied.
- Extraction is in progress — done: common, nav, Home, Interviews, Onboarding,
  Settings. Remaining screens (Jobs, JobDetail, Tracker, Resume, Mock, Copilot,
  Paywall, Games) still carry inline strings; extract them into `strings.xml` the
  same way. Example data (role/city suggestion chips) is intentionally not localized.

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
