# Monetization Spec — Ascend / NewJob

Authoritative source for ad placements, Remote Config, rewarded unlocks, IAP, and the consent gate. Generated from the QA workbook; keep in sync with it.

## Non-negotiable rules

1. **UMP consent gates everything.** No ad SDK init and no ad request before `requestConsentInfoUpdate()` runs and `canRequestAds() == true`. UMP SDK ships globally; the form shows only where required (EEA + UK + Switzerland). This is an ad-init ordering rule.
2. **One `MonetizationManager` owns all ad decisions.** Screens ask it; it decides. No ad calls scattered in screens.
3. **Full-screen mutex.** Only one of {app-open, interstitial, rewarded, paywall, purchase dialog, permission dialog} on screen at once.
4. **All ad behavior is Remote-Config controlled.** Missing full-screen key defaults **OFF**. Never leave a blank ad container — collapse on no-fill/offline.
5. **Rewarded reward fires ONLY on the earned-reward callback, exactly once.** Never on close/fail/offline. Track `earned` and `granted` separately.
6. **Paid users see zero forced ads.** No native placeholders, no app-open, no interstitials. **Copilot is Pro-only** (paywall, no rewarded).
7. **Revenue values are real.** `ad_impression` logs ILRD value+currency on every format; `purchase` logs the **actual local** value+currency from Play Billing — never hardcoded USD.
8. **`placement_id` = canonical snake_case** (same string in code, Remote Config, analytics).


## Mediation

AdMob Mediation is the **sole** owner. **AdMob Network = waterfall** backstop; **Pangle, Mintegral, AppLovin, Liftoff Monetize = bidding**. Verify each adapter + App Open mediated support in the AdMob UI (human task).


## Consent gate (UMP)

- Scope of the Google-certified CMP / IAB TCF mandate: **EEA + UK + Switzerland**. Brazil (LGPD) / South Africa (POPIA) = privacy-policy review, NOT this gate.
- On every launch: `requestConsentInfoUpdate()`; `canRequestAds()` stays **false** until it runs.
- `loadAndShowConsentFormIfRequired()` shows the form only where required; elsewhere it no-ops.
- Add a persistent **Privacy options** entry in Settings where required.
- Gate ad SDK init / first request behind `canRequestAds() == true`.


## Ad placements

| placement_id | format | screen | trigger | remote_config_key | cap | cooldown | first eligible | paid | no-fill | blocking |
|---|---|---|---|---|---|---|---|---|---|---|
| `ad_native_language` | Native | Onboarding lang | After language list | `ads.native.language.enabled` | 1 per screen | — | session 1 | hidden | collapse | non-blocking collapse |
| `ad_native_onboarding_final` | Native | End onboarding | After role+location, before Home | `ads.native.onboarding_final.enabled` | 1 | — | session 1 | hidden | collapse | non-blocking collapse |
| `ad_native_home_mid` | Native | Home | After Quick Actions | `ads.native.home.enabled` | 1 per home view | — | session 1 | hidden | collapse | non-blocking collapse |
| `ad_native_job_list` | Native | Jobs results | After first 4 organic, then every N | `ads.native.job_list.enabled` | every 6 (RC: frequency) | — | session 1 | hidden | collapse | non-blocking collapse |
| `ad_inter_after_search_batch` | Interstitial | Jobs results | After 2–3 searches / Load more | `ads.inter.search_batch.enabled` | max 1–2 / session | 90–180s | session 2 | suppress | — | fail open |
| `ad_inter_after_job_detail_close` | Interstitial | Job detail | Close detail after viewing ≥2 | `ads.inter.job_detail_close.enabled` | max 1–2 / session | 90–180s | session 2 | suppress | — | fail open |
| `ad_native_job_detail_bottom` | Native | Job detail | Below description | `ads.native.job_detail.enabled` | 1 | — | session 1 | hidden | collapse | non-blocking collapse |
| `ad_rewarded_resume_optimize` | Rewarded | Resume optimizer | Tap Apply AI fixes (after free score) | `ads.reward.resume_optimize.enabled` | — | — | any | unlimited / no ad | retry/upgrade | gated; clear retry/upgrade |
| `ad_rewarded_resume_download` | Rewarded | Resume result | Tap Download optimized resume | `ads.reward.resume_download.enabled` | — | — | any | unlimited / no ad | retry/upgrade | gated; clear retry/upgrade |
| `ad_rewarded_cover_letter` | Rewarded | Resume tools | Generate cover letter | `ads.reward.cover_letter.enabled` | — | — | any | unlimited / no ad | retry/upgrade | gated; clear retry/upgrade |
| `ad_native_resume_result` | Native | Resume result | Below score, before suggestions | `ads.native.resume_result.enabled` | 1 | — | session 1 | hidden | collapse | non-blocking collapse |
| `ad_inter_after_resume_score` | Interstitial | Resume result | Close result / finish optimize | `ads.inter.resume_score.enabled` | max 1 / session | 180s | session 2 | suppress | — | fail open |
| `ad_rewarded_mock_start` | Rewarded | Mock setup | Start mock after free allowance | `ads.reward.mock_start.enabled` | — | — | any | unlimited / no ad | retry/upgrade | gated; clear retry/upgrade |
| `ad_rewarded_mock_score` | Rewarded | Mock report | Unlock detailed AI scoring | `ads.reward.mock_score.enabled` | — | — | any | unlimited / no ad | retry/upgrade | gated; clear retry/upgrade |
| `ad_inter_after_mock_report` | Interstitial | Mock report | Exit report (no rewarded <180s) | `ads.inter.mock_report.enabled` | max 1 / session | 180s | session 2 | suppress | — | fail open |
| `ad_rewarded_copilot_session` | N/A — reserved | Copilot setup | Pro only — Copilot is fully gated behind paywall (NO rewarded unlock) | `ads.reward.copilot_session.enabled` | — | — | any | N/A — Pro feature (no ads) | N/A | N/A |
| `ad_inter_after_copilot_end` | Interstitial | Copilot ended | Tap Done (no rewarded just played) | `ads.inter.copilot_end.enabled` | max 1 / session | 180s | session 2 | suppress | — | fail open |
| `ad_native_tracker_empty` | Native | Tracker (empty) | No tracked jobs | `ads.native.tracker_empty.enabled` | 1 | — | session 1 | hidden | collapse | non-blocking collapse |
| `ad_native_games_hub` | Native | Games hub | After first game row | `ads.native.games_hub.enabled` | 1 | — | session 1 | hidden | collapse | non-blocking collapse |
| `ad_inter_after_game_complete` | Interstitial | Game complete | After completion | `ads.inter.game_complete.enabled` | 1 per 2 games | 90s | session 2 | suppress | — | fail open |
| `ad_rewarded_game_hint` | Rewarded | Game | Hint / redo / extra puzzle | `ads.reward.game_hint.enabled` | — | — | any | no ad (free for Pro) | retry | gated; clear retry/upgrade |
| `ad_appopen_resume` | App-open | App resume | Return from background | `ads.appopen.resume.enabled` | 1/sess · 2/day | 30 min | s2 if activated, else s3 | suppress | — | fail open |
| `ad_inter_after_splash` | Interstitial | App splash / session start | After splash/profile load + UMP consent resolves, before Home/Onboarding continuation | `ads.inter.after_splash.enabled` | max 1 / session | 180s | session 2 if activated in session 1, else session 3 | suppress | skip | fail open after timeout |
| `ad_inter_after_onboarding_complete` | Interstitial | Onboarding completion / pre-home | After `onboarding_complete`, before Home / Jobs / first main app destination | `ads.inter.after_onboarding_complete.enabled` | max 1 per install | n/a; forward-suppress next fullscreen by RC window | session 1 after onboarding_complete only | suppress | skip | fail open |

**Splash vs App Open.** The splash/session-start interstitial (`ad_inter_after_splash`) is **separate** from App Open (`ad_appopen_resume`). Splash interstitial fires on **cold/session start** after splash/profile load; App Open fires on **background→foreground resume**. They must **never both show in the same foreground cycle** (`ads.inter.after_splash.suppress_if_appopen_eligible`). The 3-second branded transition is a *branded transition duration*, **not** an ad-load wait — if no ad is ready within `load_timeout_ms`, continue (fail open); never hold the user 3s for a failed ad. Never on session 1.

**Implementation notes (architecture).**
- `ad_inter_after_splash` is a deliberate Apero-style post-splash interstitial. Keep it; do not remove unless Product asks.
- It must **not** call App Open's show/suppression pipeline directly. To decide whether to yield to App Open, splash uses a **side-effect-free** eligibility snapshot (`isAppOpenEligibleSnapshot()`) that never marks App Open shown, consumes caps, mutates foreground/cooldown state, or starts a load/show.
- Full-screen presentation is **format-specific**. There is no generic presenter that defaults to `showInterstitial()`: interstitial → `presentInterstitial`/`requestInterstitial` (interstitial show path), App Open → its own App Open path, rewarded → rewarded path. Native, paywall, purchase dialog, and permission dialog never share an interstitial show path. A placement may never be displayed through the wrong ad format just because it is "full-screen".

**Onboarding-complete interstitial (`ad_inter_after_onboarding_complete`).** The only approved onboarding-completion interstitial. It runs **after** onboarding is complete (after `onboarding_complete` is logged + persisted), **not** during onboarding — never between language/role/location/resume steps. Defaults **OFF**. Caps at **once per install**. Suppressed for paid users; honors the UMP gate and the full-screen mutex; fails open to Home/Jobs on no-fill/offline/not-ready/timeout. It must not stack with another full-screen surface, and must not stack right after another full-screen onboarding ad within `suppress_if_fullscreen_onboarding_ad_shown_seconds` unless `allow_aggressive_stack=true`.
- The branded transition is honest branded loading only (logo/spinner / "Preparing your jobs…") — **no** "Tap to continue", fake reward language, fake buttons, or ad-like UI that could manufacture accidental clicks.
- `transition_duration_ms` and `load_timeout_ms` run **concurrently**: do not show the transition first and only then start waiting for the ad. If the ad is ready before the short transition minimum elapses, finish the minimum then show; if not ready by `load_timeout_ms`, fail open to Home/Jobs immediately. Never hold the user past the timeout for decorative animation.
- **Forward suppression:** if it shows, suppress the next forced full-screen surface for `suppress_next_fullscreen_seconds` (reason `recent_onboarding_interstitial`): `ad_appopen_resume` and every forced interstitial (`ad_inter_after_search_batch`, `…_job_detail_close`, `…_resume_score`, `…_mock_report`, `…_copilot_end`, `…_game_complete`). Do **not** suppress user-requested rewarded ads, paywalls, or native/non-blocking placements.

## Remote Config keys

Missing full-screen key => OFF. No blank containers.

| key | default | allowed | paid behavior | offline | missing-key |
|---|---|---|---|---|---|
| `ad_aggressiveness_tier` | balanced | retention_protect|balanced|arbitrage_high|paid_or_high_intent | paid_or_high_intent | last cached | balanced |
| `ads_global_enabled` | true | true|false | n/a | respect cache | false |
| `ads.native.job_list.enabled` | true | true|false | hidden | hide | false |
| `ads.native.job_list.frequency` | 6 | 4|6|8 | n/a | n/a | 8 |
| `ads.native.home.enabled` | true | true|false | hidden | hide | false |
| `ads.native.resume_result.enabled` | true | true|false | hidden | hide | false |
| `ads.inter.search_batch.enabled` | false | true|false | suppress | skip | false |
| `ads.inter.job_detail_close.enabled` | false | true|false | suppress | skip | false |
| `ads.inter.resume_score.enabled` | false | true|false | suppress | skip | false |
| `ads.inter.mock_report.enabled` | false | true|false | suppress | skip | false |
| `ads.inter.cooldown_seconds` | 120 | 60–300 | n/a | n/a | 180 |
| `ads.inter.max_per_session` | 2 | 1|2|3 | 0 | n/a | 1 |
| `ads.reward.resume_optimize.enabled` | true | true|false | opt-in only | retry/upgrade | false |
| `ads.reward.mock_score.enabled` | true | true|false | opt-in only | retry/upgrade | false |
| `ads.reward.daily_cap` | 5 | 3–8 | n/a | n/a | 3 |
| `ads.appopen.resume.enabled` | true | true|false | OFF | skip | false |
| `ads.appopen.resume.min_session` | 2 | 2|3|4 | n/a | n/a | 3 |
| `ads.appopen.resume.require_activation_for_session_2` | true | true|false | n/a | n/a | true |
| `ads.appopen.resume.cooldown_minutes` | 30 | 20–60 | n/a | n/a | 45 |
| `ads.appopen.resume.max_per_session` | 1 | 1 | 0 | n/a | 1 |
| `ads.appopen.resume.max_per_day` | 2 | 1|2|3 | 0 | n/a | 2 |
| `ads.appopen.resume.suppress_after_fullscreen_ad_seconds` | 180 | 120–300 | n/a | n/a | 300 |
| `ads.appopen.resume.load_timeout_ms` | 1200 | 800–1500 | n/a | n/a | 1200 |
| `ads.appopen.resume.ad_expiration_hours` | 4 | 4 | n/a | n/a | 4 |
| `ads.inter.after_splash.enabled` | false | true|false | suppress | skip | false |
| `ads.inter.after_splash.min_session` | 2 | 2|3|4 | n/a | n/a | 3 |
| `ads.inter.after_splash.require_activation_for_session_2` | true | true|false | n/a | n/a | true |
| `ads.inter.after_splash.cooldown_seconds` | 180 | 120–600 | n/a | n/a | 300 |
| `ads.inter.after_splash.load_timeout_ms` | 1000 | 800–1200 | n/a | skip | 1000 |
| `ads.inter.after_splash.transition_enabled` | true | true|false | n/a | n/a | true |
| `ads.inter.after_splash.transition_duration_ms` | 3000 | 1500–3000 | n/a | n/a | 1500 |
| `ads.inter.after_splash.suppress_if_appopen_eligible` | true | true|false | n/a | n/a | true |
| `ads.inter.after_onboarding_complete.enabled` | false | true|false | suppress | skip | false |
| `ads.inter.after_onboarding_complete.max_per_install` | 1 | 0|1 | 0 | n/a | 0 |
| `ads.inter.after_onboarding_complete.load_timeout_ms` | 1200 | 800-1500 | n/a | skip | 1200 |
| `ads.inter.after_onboarding_complete.transition_enabled` | true | true|false | n/a | n/a | true |
| `ads.inter.after_onboarding_complete.transition_duration_ms` | 900 | 300-1200 | n/a | n/a | 700 |
| `ads.inter.after_onboarding_complete.suppress_if_fullscreen_onboarding_ad_shown_seconds` | 60 | 0-300 | n/a | n/a | 60 |
| `ads.inter.after_onboarding_complete.allow_aggressive_stack` | false | true|false | false | n/a | false |
| `ads.inter.after_onboarding_complete.suppress_next_fullscreen_seconds` | 180 | 60-600 | n/a | n/a | 180 |
| `paywall.variant` | control | control|discount|trial|lifetime | n/a | last cached | control |
| `paywall.suppress_if_rewarded_engaged` | true | true|false | n/a | n/a | true |
| `push.jobs_fresh.daily_cap` | 1 | 0–2 | n/a | n/a | 1 |
| `push.winback.enabled` | true | true|false | n/a | n/a | false |
| `ads.appopen.resume.min_background_seconds` | 30 | 15-60 | n/a | n/a | 30 |
| `ads.appopen.resume.suppress_after_external_link_seconds` | 300 | 120-600 | n/a | n/a | 300 |
| `ads.appopen.resume.suppress_after_rewarded_seconds` | 300 | 120-600 | n/a | n/a | 300 |
| `ads.appopen.resume.suppress_after_permission_seconds` | 120 | 60-300 | n/a | n/a | 120 |
| `ads.appopen.resume.suppress_during_resume_flow` | true | true|false | n/a | n/a | true |
| `ads.appopen.resume.suppress_during_mock_flow` | true | true|false | n/a | n/a | true |
| `ads.appopen.resume.suppress_during_copilot_flow` | true | true|false | n/a | n/a | true |
| `ads.appopen.resume.suppress_during_billing_flow` | true | true|false | n/a | n/a | true |
| `ads.fullscreen.mutex_enabled` | true | true|false | n/a | n/a | true |
| `ads.suppress_for_paid_users` | true | true|false | n/a | n/a | true |
| `ads.inter.load_timeout_ms` | 1000 | 800-1200 | n/a | skip | 1000 |
| `ads.reward.load_timeout_ms` | 4000 | 3000-5000 | n/a | retry | 4000 |
| `ads.native.load_timeout_ms` | 0 | non-blocking | n/a | collapse | 0 |
| `ads.reward.copilot_session.enabled` | false | true|false | Pro only | n/a | false |
| `ads.native.language.enabled` | true | true|false | hidden | hide | false |
| `ads.native.onboarding_final.enabled` | true | true|false | hidden | hide | false |
| `ads.native.job_detail.enabled` | true | true|false | hidden | hide | false |
| `ads.native.tracker_empty.enabled` | true | true|false | hidden | hide | false |
| `ads.native.games_hub.enabled` | true | true|false | hidden | hide | false |
| `ads.inter.copilot_end.enabled` | false | true|false | suppress | skip | false |
| `ads.inter.game_complete.enabled` | false | true|false | suppress | skip | false |
| `ads.reward.resume_download.enabled` | true | true|false | opt-in only | retry/upgrade | false |
| `ads.reward.cover_letter.enabled` | true | true|false | opt-in only | retry/upgrade | false |
| `ads.reward.mock_start.enabled` | true | true|false | opt-in only | retry/upgrade | false |
| `ads.reward.game_hint.enabled` | true | true|false | opt-in only | retry | false |

## Onboarding Remote Config

Purpose: control onboarding tour-guide slides and onboarding animations without shipping a new app build. These keys affect onboarding UI only. They must not block onboarding if missing or invalid.

| key | default | allowed | paid behavior | offline | missing-key |
|---|---|---|---|---|---|
| `onboarding.tour.enabled` | true | true|false | n/a | last cached | false |
| `onboarding.tour.variant` | one_card | none|one_card|three_card|full | n/a | last cached | none |
| `onboarding.tour.max_cards` | 1 | 0|1|3|5 | n/a | last cached | 0 |
| `onboarding.tour.force_completion` | false | true|false | n/a | false | false |
| `onboarding.tour.show_skip` | true | true|false | n/a | true | true |
| `onboarding.tour.placement` | after_location | before_language|after_language|after_location|before_home | n/a | after_location | after_location |
| `onboarding.tour.suppress_if_resume_uploaded` | true | true|false | n/a | true | true |
| `onboarding.tour.suppress_if_returning_user` | true | true|false | n/a | true | true |
| `onboarding.tour.once_per_install` | true | true|false | n/a | true | true |
| `onboarding.animations.enabled` | true | true|false | n/a | true | true |
| `onboarding.animations.variant` | subtle | none|subtle|standard|rich | n/a | subtle | none |
| `onboarding.animations.duration_ms` | 700 | 300-1500 | n/a | 700 | 500 |
| `onboarding.animations.reduce_motion_respect_system` | true | true|false | n/a | true | true |
| `onboarding.animations.splash_brand_duration_ms` | 1200 | 500-3000 | n/a | 1200 | 800 |

Rules:
- `onboarding.tour.enabled=false` or `variant=none` → skip all tour slides immediately.
- Missing/invalid Remote Config → use safe defaults and continue onboarding (never block).
- Tour is not forced unless `force_completion=true`; if `show_skip=true` and `force_completion=false`, show Skip.
- Do not place the tour before language by default; default placement is `after_location`.
- Tour must not interfere with ad placements (`ad_inter_after_splash`, full-screen onboarding native, onboarding-complete interstitial). If a tour placement conflicts with a full-screen ad placement, the monetization placement wins unless Product changes RC; the tour overlay resolves before the onboarding-complete interstitial runs.
- Respect Android reduced-motion / animator-scale when `reduce_motion_respect_system=true`; animations never delay ad fail-open or block navigation.
- No raw role/location/query/resume text/filename/name/email/PII may be logged — only controlled `variant`/`card_index`/`cards_seen`/`placement` values.

## Ad-intensity tiers (`ad_aggressiveness_tier`)

App Open first-eligibility is the canonical `s2 if activated, else s3` from the placement table; tiers modulate **cooldown** and whether it runs at all (they do NOT change first-eligibility).
- `retention_protect` — native every 8; interstitial s3+, ≤1/sess; **app-open OFF**. High-IAP geos.
- `balanced` — native every 6; interstitial s2+, ≤2/sess; app-open s2 if activated else s3, **45m** cooldown. **Launch default, worldwide.**
- `arbitrage_high` — native every 4; interstitial s2+, ≤3/sess; app-open s2 if activated else s3, **30m** cooldown. Cheap geos after ~1 wk stable.
- `paid_or_high_intent` — native reduced/hidden; interstitial OFF/limited; app-open OFF; paywall prominent.


## Rewarded unlocks

Reward granted ONLY on earned-reward callback, once. Caps are RC-adjustable starting points.

| placement_id | reward | free/day | rewarded/day | notes |
|---|---|---|---|---|
| `ad_rewarded_resume_optimize` | Optimize this resume once | 0 | 3 | show "Ad unavailable — try again or upgrade" · score free; apply/download via rewarded |
| `ad_rewarded_resume_download` | One export/download | 0 | 3 | paywall first, rewarded after dismiss |
| `ad_rewarded_cover_letter` | One cover letter for this job | 0 | 5 | retry / upgrade |
| `ad_rewarded_mock_start` | One mock interview session | 1 | 3 | retry / upgrade |
| `ad_rewarded_mock_score` | Full score + improvement tips | 0 | 3 | retry / upgrade |
| `ad_rewarded_copilot_session` | RESERVED PLACEHOLDER — do NOT implement. Copilot is Pro-only (paywall). | PRO ONLY | n/a | Copilot = Pro feature only; see IAP/paywall |
| `ad_rewarded_game_hint` | One hint or extra game | 1 | 5 | retry |

**Copilot is Pro-only** — `ad_rewarded_copilot_session` is a reserved placeholder; do not implement a rewarded path.


## IAP / entitlement

Products (IDs + base prices **TBD**, do not invent): `ascend.weekly`, `ascend.yearly`, `ascend.lifetime`. `lifetime` = one-time product (does NOT renew).

**Localized pricing:** set base price + Play pricing templates; the `purchase` event sends the REAL local value+currency from `ProductDetails`. Never hardcode USD; never pre-convert (Google handles FX).

**Entitlement states to handle:** Free, Paid active (zero ads), Expired, Purchase pending (no reward), Purchase canceled, Cache cleared, Offline-was-paid, Billing unavailable, Purchase failed, Restore failed, Refunded/revoked, Grace period, Account hold, Product not found, Duplicate attempt, and `entitlement_unknown` (no forced ads until restore resolves).
