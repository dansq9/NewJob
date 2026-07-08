# Remote Config — Handoff Guide

A practical guide for setting up Firebase Remote Config (RC) for Ascend. Written to be simple.
You know RC already; this tells you exactly what to enter and why.

## In one minute

- RC is a set of **switches and numbers you change from the Firebase website**. The app reads them
  to decide how ads and onboarding behave. No app update needed to change them.
- **All keys are defined in one file:** `android/app/.../monetization/config/RemoteConfig.kt`.
- **Safe by default:** if a value is missing, the app keeps ads **OFF**. So a wrong or missing
  key never turns ads on by accident — you turn things on deliberately.
- **Firebase is not connected yet.** The app needs `google-services.json` + the Google Services
  Gradle plugin. Until that is added, the app runs on the safe built-in values (ads mostly off).

## Setup steps

1. Create a Firebase project and add the Android app (package `app.ascend`).
2. Download `google-services.json` into `android/app/`.
3. Add the Google Services plugin (this is the developer's job, not the console's).
4. In the Firebase console → **Remote Config**, add each parameter from the tables below
   (exact key name, correct type, and the "Value to enter").
5. Click **Publish changes**. The app fetches once at startup.

**Types:** `Boolean` = true/false, `Number` = a whole number, `String` = text.

> The values below are the recommended launch values from `docs/monetization-spec.md`
> (the source of truth). If that file and this one ever disagree, the spec wins.

---

## 1. Global switches

| Key | Type | Value to enter | What it does |
|---|---|---|---|
| `ads_global_enabled` | Boolean | `true` | Master switch. Off = no ads anywhere. |
| `ads.suppress_for_paid_users` | Boolean | `true` | Paid users never see forced ads. |
| `ads.fullscreen.mutex_enabled` | Boolean | `true` | Only one full-screen ad at a time. |
| `ad_aggressiveness_tier` | String | `balanced` | Overall ad intensity preset. |
| `ads.reward.daily_cap` | Number | `5` | Max rewarded ads a user can watch per day. |

## 2. How long to wait for an ad (milliseconds)

| Key | Type | Value | What it does |
|---|---|---|---|
| `ads.inter.load_timeout_ms` | Number | `1000` | Give up loading a full-screen ad after 1s. |
| `ads.reward.load_timeout_ms` | Number | `90000` | ⚠️ See note below — keep this **high**, not 4000. |
| `ads.native.load_timeout_ms` | Number | `0` | Small in-page ads never block; 0 = collapse if none. |
| `ads.appopen.resume.load_timeout_ms` | Number | `1200` | Wait limit for the "app reopened" ad. |

> ⚠️ **`ads.reward.load_timeout_ms` gotcha.** In this app that number limits the **whole rewarded
> ad** (loading **and** the user watching it to the end — the reward only counts at the end). If you
> set it to a small value like 4000, a real ad could never finish and the user would never get their
> reward. Keep it around **90000** (90s). The app's built-in fallback is already 90000.

## 3. Full-screen ad pacing

| Key | Type | Value | What it does |
|---|---|---|---|
| `ads.inter.cooldown_seconds` | Number | `120` | Minimum seconds between full-screen ads. |
| `ads.inter.max_per_session` | Number | `2` | Max full-screen ads per app session. |

## 4. In-page ads (native) — small, non-blocking

All default **on**; they simply collapse if no ad is available.

| Key | Type | Value |
|---|---|---|
| `ads.native.language.enabled` | Boolean | `true` |
| `ads.native.onboarding_final.enabled` | Boolean | `true` |
| `ads.native.home.enabled` | Boolean | `true` |
| `ads.native.job_list.enabled` | Boolean | `true` |
| `ads.native.job_list.frequency` | Number | `6` (an ad every 6 jobs) |
| `ads.native.job_detail.enabled` | Boolean | `true` |
| `ads.native.resume_result.enabled` | Boolean | `true` |
| `ads.native.tracker_empty.enabled` | Boolean | `true` |
| `ads.native.games_hub.enabled` | Boolean | `true` |

## 5. Full-screen interruptive ads (interstitial) — **start OFF**

These interrupt the user, so they ship **off**. Turn them on one at a time later and watch retention.

| Key | Type | Value |
|---|---|---|
| `ads.inter.search_batch.enabled` | Boolean | `false` |
| `ads.inter.job_detail_close.enabled` | Boolean | `false` |
| `ads.inter.resume_score.enabled` | Boolean | `false` |
| `ads.inter.mock_report.enabled` | Boolean | `false` |
| `ads.inter.copilot_end.enabled` | Boolean | `false` |
| `ads.inter.game_complete.enabled` | Boolean | `false` |

## 6. Reward-to-unlock ads (rewarded) — user chooses to watch

All default **on** (except Copilot, which is a paid-only feature with no ad option).

| Key | Type | Value |
|---|---|---|
| `ads.reward.resume_optimize.enabled` | Boolean | `true` |
| `ads.reward.resume_download.enabled` | Boolean | `true` |
| `ads.reward.cover_letter.enabled` | Boolean | `true` |
| `ads.reward.mock_start.enabled` | Boolean | `true` |
| `ads.reward.mock_score.enabled` | Boolean | `true` |
| `ads.reward.game_hint.enabled` | Boolean | `true` |
| `ads.reward.copilot_session.enabled` | Boolean | `false` (reserved — Copilot is Pro-only, no ad) |

## 7. "App reopened" ad (app-open)

| Key | Type | Value | What it does |
|---|---|---|---|
| `ads.appopen.resume.enabled` | Boolean | `true` | The ad when the user returns to the app. |
| `ads.appopen.resume.min_session` | Number | `2` | Don't show before the user's 2nd session. |
| `ads.appopen.resume.require_activation_for_session_2` | Boolean | `true` | In session 2, only after the user did something meaningful. |
| `ads.appopen.resume.cooldown_minutes` | Number | `30` | Wait 30 min between these ads. |
| `ads.appopen.resume.max_per_session` | Number | `1` | Max 1 per session. |
| `ads.appopen.resume.max_per_day` | Number | `2` | Max 2 per day. |
| `ads.appopen.resume.min_background_seconds` | Number | `30` | User must have been away 30s+ to count as a real return. |
| `ads.appopen.resume.suppress_after_fullscreen_ad_seconds` | Number | `180` | Stay quiet 3 min after another full-screen ad. |
| `ads.appopen.resume.suppress_after_external_link_seconds` | Number | `300` | Quiet 5 min after the user opened an external link. |
| `ads.appopen.resume.suppress_after_rewarded_seconds` | Number | `300` | Quiet 5 min after a rewarded ad. |
| `ads.appopen.resume.suppress_after_permission_seconds` | Number | `120` | Quiet 2 min after a permission dialog. |
| `ads.appopen.resume.suppress_during_resume_flow` | Boolean | `true` | Never during the resume flow. |
| `ads.appopen.resume.suppress_during_mock_flow` | Boolean | `true` | Never during a mock interview. |
| `ads.appopen.resume.suppress_during_copilot_flow` | Boolean | `true` | Never during live Copilot. |
| `ads.appopen.resume.suppress_during_billing_flow` | Boolean | `true` | Never during a purchase. |

## 8. Start-up full-screen ad (splash) — **start OFF**

| Key | Type | Value |
|---|---|---|
| `ads.inter.after_splash.enabled` | Boolean | `false` |
| `ads.inter.after_splash.min_session` | Number | `2` |
| `ads.inter.after_splash.require_activation_for_session_2` | Boolean | `true` |
| `ads.inter.after_splash.cooldown_seconds` | Number | `180` |
| `ads.inter.after_splash.load_timeout_ms` | Number | `1000` |
| `ads.inter.after_splash.transition_enabled` | Boolean | `true` |
| `ads.inter.after_splash.transition_duration_ms` | Number | `3000` |
| `ads.inter.after_splash.suppress_if_appopen_eligible` | Boolean | `true` |

## 9. After-onboarding full-screen ad — **start OFF**

| Key | Type | Value |
|---|---|---|
| `ads.inter.after_onboarding_complete.enabled` | Boolean | `false` |
| `ads.inter.after_onboarding_complete.max_per_install` | Number | `1` |
| `ads.inter.after_onboarding_complete.load_timeout_ms` | Number | `1200` |
| `ads.inter.after_onboarding_complete.transition_enabled` | Boolean | `true` |
| `ads.inter.after_onboarding_complete.transition_duration_ms` | Number | `900` |
| `ads.inter.after_onboarding_complete.suppress_if_fullscreen_onboarding_ad_shown_seconds` | Number | `60` |
| `ads.inter.after_onboarding_complete.allow_aggressive_stack` | Boolean | `false` |
| `ads.inter.after_onboarding_complete.suppress_next_fullscreen_seconds` | Number | `180` |

## 10. Onboarding tour (intro slides)

| Key | Type | Value | What it does |
|---|---|---|---|
| `onboarding.tour.enabled` | Boolean | `true` | Show the intro tour. |
| `onboarding.tour.variant` | String | `one_card` | Which tour version. |
| `onboarding.tour.max_cards` | Number | `1` | How many slides. |
| `onboarding.tour.force_completion` | Boolean | `false` | Keep **false** so users can skip. |
| `onboarding.tour.show_skip` | Boolean | `true` | Show a Skip button. |
| `onboarding.tour.placement` | String | `after_location` | Where the tour appears. |
| `onboarding.tour.suppress_if_resume_uploaded` | Boolean | `true` | Skip tour if they already added a resume. |
| `onboarding.tour.suppress_if_returning_user` | Boolean | `true` | Don't re-show to returning users. |
| `onboarding.tour.once_per_install` | Boolean | `true` | Show at most once per install. |

## 11. Onboarding animations

| Key | Type | Value |
|---|---|---|
| `onboarding.animations.enabled` | Boolean | `true` |
| `onboarding.animations.variant` | String | `subtle` |
| `onboarding.animations.duration_ms` | Number | `700` |
| `onboarding.animations.reduce_motion_respect_system` | Boolean | `true` |
| `onboarding.animations.splash_brand_duration_ms` | Number | `800` |

---

## Good to know

- **Names are exact and shared.** Each ad spot's id (e.g. `ad_native_job_list`) is used the same
  way in the app code, in Remote Config, and in analytics. Type the keys exactly as shown.
- **You can't break it "open".** Wrong/missing values fall back to safe-off. The risk is ads being
  *too quiet*, never too aggressive.
- **Testing.** Developers have a debug-only switch (`DEBUG_FORCE_ADS`) that shows Google **test**
  ads in a test build without Firebase. It has no effect on the real app users get.
- **Recommended first launch:** enter everything as above (natives + rewarded + app-open on;
  interstitials off), publish, then enable interstitials later one at a time while watching retention.

---

## Subscription / IAP (separate from Remote Config)

This is **Google Play Console**, not Remote Config.

- **One plan only: a weekly subscription** with a **3-day free trial**.
- The app currently uses a **placeholder** product id: `ascend_pro_weekly` (price/trial are
  placeholders too). In Play Console, create the real subscription + base plan, then update the id
  in the app's billing code (`StubBillingManager` / the real `PlayBillingManager`).
- The paywall shows this **single** plan. **Price and currency are read live from Google Play**
  (localized per country) — the app never hardcodes a price, so worldwide pricing "just works".
- Until Play products exist, a stub grants Pro locally so the paywall and "no ads for Pro" can be
  tested. Swap the stub for the real Play Billing implementation when the product is live.
