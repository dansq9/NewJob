# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

**Ascend** — Android job-search app (Kotlin, Jetpack Compose/Material 3, MVVM, Hilt, Retrofit/OkHttp, Room, DataStore). minSdk 24, target/compile 35.
Model: ad-arbitrage, **no login** (anonymous `install_id`), hybrid monetization (ads + Play Billing subscriptions), launching **worldwide incl. EEA/UK/Switzerland**.

**Repo scope — run Claude Code from the repo root.**
- The app is in `android/`. All monetization/analytics work happens there.
- **Ignore** `feature-video/` (marketing assets + generators) and `Ascend.dc.html` / `support.js` (the original prototype — reference only, do NOT modify).
- Jobs come from JSearch (RapidAPI); AI features call the Ascend web-platform API via `ASCEND_API_BASE_URL`. Both are read from `android/local.properties` as `BuildConfig` fields.

## Source of truth — read before implementing monetization or analytics

The specs in `/docs` are authoritative. Read the relevant one before writing code in that area:

- `/docs/monetization-spec.md` — ad placements, Remote Config keys, rewarded caps, the non-negotiable rules
- `/docs/event-schema.md` — every analytics event, its params, and the identity model
- `/docs/qa-gate.md` — the acceptance criteria a change must pass

If a request conflicts with these specs, stop and ask. Do not improvise monetization behavior.

## Non-negotiable rules (the constitution)

1. **UMP consent gates everything.** No ad SDK init and no ad request may happen before `requestConsentInfoUpdate()` has run and `canRequestAds() == true`. Ship the UMP SDK globally; the consent form is shown only where required (EEA/UK/CH). This is an ad-initialization ordering rule, not just a legal checkbox.
2. **One `MonetizationManager` owns all ad decisions.** No ad load/show calls scattered across screens. Screens ask the manager; the manager decides.
3. **Full-screen mutex.** Only one of {app-open, interstitial, rewarded, paywall, purchase dialog, permission dialog} on screen at a time.
4. **All ad behavior is Remote-Config controlled.** A missing full-screen key defaults **OFF**. Never leave a blank ad container on no-fill/offline — collapse it.
5. **Rewarded reward fires ONLY on the earned-reward SDK callback, exactly once.** Never on ad close, never on fail/offline. Track `earned` and `granted` separately.
6. **Paid users see zero forced ads anywhere.** No native loading placeholders, no app-open, no interstitials. **Copilot is Pro-only** (paywall, no rewarded unlock).
7. **Revenue values are real, never hardcoded.** `ad_impression` logs ILRD `value`+`currency` on every format. `purchase` logs the **actual local** `value`+`currency` from Play Billing `ProductDetails` — never a hardcoded USD figure (pricing is localized worldwide).
8. **All analytics go through one `AnalyticsTracker` wrapper** using typed event/param constants generated from `event-schema`. No ad-hoc string events scattered in screens. Never log PII, raw search queries, resume text, email, name, or file names — use booleans/bands.
9. **`placement_id` = canonical snake_case** from the Ad Script (e.g. `ad_native_job_list`). The same string is used in code, Remote Config, and analytics.
10. **Format-specific ad dispatch.** Interstitial, App Open, rewarded, native, paywall, purchase dialog, and permission dialog must NOT share a generic show path that defaults to interstitial. Use format-specific load/show paths (`presentInterstitial`, the App Open path, the rewarded path). A placement must never be displayed through the wrong ad format just because it is "full-screen".
11. **Pure eligibility helpers.** An ad eligibility check used by another placement as a suppression helper must be side-effect-free (no caps consumed, no state mutated, no ad marked shown, no load/show started). Never call a placement's full suppression/show pipeline from another placement just to infer eligibility — use a read-only snapshot (e.g. `isAppOpenEligibleSnapshot()`).

State a rule as "prefer X over Y" when guiding implementation; phrase suppressions positively.

## Conventions

- Kotlin + Compose; MVVM; DI via Hilt.
- Verify builds from the app module: `cd android && ./gradlew :app:assembleDebug`.
- One feature branch per task; never work on `main`.
- Secrets live in `android/local.properties` (git-ignored), surfaced as `BuildConfig` fields. Never commit or edit secret values; never hardcode keys.

## DO NOT TOUCH — leave for the human engineer

- Signing config, keystores, `android/local.properties` secret values.
- Play Console setup, Data Safety form, store listing.
- AdMob mediation console (adapter integration, bidding/waterfall config, floors).
- Final release AAB build + signing.
- Do **not** invent Play product IDs or prices — they are `TBD`. If a product ID is needed, use a clearly-marked constant placeholder and flag it.
