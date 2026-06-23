# Ascend

**Land your next role, faster.** Ascend is an AI-assisted job-search app: find
jobs, track every application, generate/optimize your resume, prep with mock
interviews, get a live interview copilot, and stay sharp with brain games.

This repository contains three things:

| Path | What it is |
|------|------------|
| [`android/`](android/) | The **Kotlin + Jetpack Compose** Android app (Play Store target). |
| [`feature-video/`](feature-video/) | **Marketing/design assets** — the animated Play Store promo video + store screenshots, and the generators that produce them. |
| `Ascend.dc.html` + `support.js` | The original interactive **prototype** the app is built from (a DC-runtime React artifact). |

## Android app
Kotlin · Jetpack Compose (Material 3) · MVVM · Hilt · Retrofit/OkHttp +
kotlinx.serialization · Room · DataStore · Coil · Navigation-Compose. minSdk 24,
target/compile 35.

- **Jobs** are live via **JSearch (RapidAPI)**.
- The **AI features** (resume optimize/generate, mock interview, live copilot)
  talk to the **Ascend web-platform API** behind a configurable base URL.
- See [`android/README.md`](android/README.md) for architecture and module layout.

### Build
Open the `android/` folder in **Android Studio**, or from the CLI:
```bash
cd android
cp local.properties.sample local.properties   # then fill in secrets (below)
./gradlew :app:assembleDebug
```
CI builds every push/PR via [`.github/workflows/android.yml`](.github/workflows/android.yml)
(GitHub runners provide the Android SDK + Google Maven).

## Configuration & secrets
The app has **no `.env`** — Android reads secrets from **`local.properties`**
(git-ignored), surfaced to code as `BuildConfig` fields by `app/build.gradle.kts`.

Copy `android/local.properties.sample` → `android/local.properties`:
```properties
RAPIDAPI_KEY=your_rapidapi_jsearch_key      # https://rapidapi.com/.../jsearch
ASCEND_API_BASE_URL=https://api.ascend.app/ # your web-platform API (trailing slash)
```
- Both are optional for a **debug build to compile** — blank values fall back to
  safe defaults (`secret()` in `app/build.gradle.kts`); JSearch just won't return
  results without a real key.
- In **CI**, set repository secrets `RAPIDAPI_KEY` and `ASCEND_API_BASE_URL`;
  the workflow writes them into `local.properties`. Values can also come from
  environment variables of the same name.
- `android/local.properties` is **never committed** (see `android/.gitignore`).

## Marketing / store assets
The Play Store **video and screenshots live in [`feature-video/`](feature-video/)**.
**Decision:** they **stay in the repo** as the single source of truth, since the
generators (`anim.py`, `make_store.py`) and the rendered deliverables are part of
this project. Large binaries (the ~20 MB `.mp4`, the 1080×1920 PNGs) are marked in
`.gitattributes`; if repo size becomes a concern, migrate those binaries to
**GitHub Releases** or **Git LFS** (the generators stay in git either way).

## License
Music in the promo video is "Business Moves Vol. 1" by ende.app — verify its
license terms before publishing the video to the Play Store (see
[`feature-video/README.md`](feature-video/README.md)).
