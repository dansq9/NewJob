# Claude Code Handoff — Ascend / NewJob

How to hand the monetization + analytics + consent build to Claude Code so a human engineer only does final prep.

---

## The method (read once)

**1. Set up the repo so Claude Code reads the contract natively.**
- Drop `CLAUDE.md` in the repo root. It auto-loads every session and carries the non-negotiable rules.
- Put markdown specs in `/docs`: `monetization-spec.md`, `event-schema.md`, `qa-gate.md`. Claude Code reads markdown far better than xlsx, so export the workbook/schema tabs to these (ask Claude to do it, or I can generate them).
- Commit all of the above so the context travels with the repo.

**2. One feature area per session, in order.** Don't paste all eight tasks at once. Run them in the sequence below; the order encodes the dependencies (consent gate → analytics spine → manager → placements). Run `/clear` between unrelated areas so context stays clean.

**3. Start each task in plan mode.** Let Claude scan the repo (it delegates that to the Explore subagent) and produce a plan. Read the plan. Correct the approach *before* it writes code — fixing a plan is cheaper than fixing a diff.

**4. Review every diff.** Claude is fast; a fast wrong change is still wrong. If it drifts, use `/rewind` and restate, rather than patching a bad path.

**5. Gate each task on the QA criteria.** Each prompt below ends with acceptance criteria pulled from the QA gate. Don't accept the task as done until those pass.

**6. Run `/security-review` before merging the consent and billing code.** It's built for exactly this — exposed credentials, auth, injection.

**7. Branch per task.** Never on `main`.

---

## The ordered task prompts (copy-paste, one session each)

Each is self-contained and references the in-repo specs. Start each by entering plan mode.

### Task 1 — UMP consent gate + ad-init ordering
```
Read /docs/monetization-spec.md (UMP section) and CLAUDE.md rule 1.
Implement Google UMP (User Messaging Platform):
- Call requestConsentInfoUpdate() on every app launch.
- Gate ALL ad SDK initialization and ad requests behind canRequestAds() == true.
- Call loadAndShowConsentFormIfRequired(); it shows the form only where required (EEA/UK/CH).
- Add a persistent "Privacy options" entry in Settings when required.
Acceptance: no ad SDK init or ad request can fire before consent info update resolves; verify the gate with a forced-EEA debug geo.
```

### Task 2 — Analytics spine (AnalyticsTracker + identity)
```
Read /docs/event-schema.md and CLAUDE.md rules 7-9.
- Create one AnalyticsTracker wrapper over Firebase Analytics. No ad-hoc string events anywhere else.
- Generate typed Kotlin constants for every event name and param from the schema.
- Generate + persist install_id (UUID) on first launch; set it as a user property.
- Set user properties: install_id, user_account_status, user_plan, user_ad_segment, ad_aggressiveness_tier, rc_variant, app_version, os_version, country.
- Implement session_number (local) and the user_ad_segment computation.
- Enforce: no PII / raw query / resume text / filenames in any param; use bands/booleans.
- Client implements push_open only. push_sent is server-side — do NOT implement or log it from Android.
Acceptance: events appear in GA4 DebugView with correct params; no raw strings; install_id stable across sessions.
```

### Task 3 — MonetizationManager core
```
Read /docs/monetization-spec.md (rules + RC keys) and CLAUDE.md rules 2-4, 6.
Build a single MonetizationManager:
- Reads all caps/cooldowns/placement toggles from Remote Config; missing full-screen key => OFF.
- Enforces a full-screen ad mutex (one fullscreen at a time).
- Suppresses ALL forced ads for paid users.
- Applies per-format load timeouts (fail open; never block the core loop).
- Exposes shouldShow(placement, context) decisions; screens never call the ad SDK directly.
Acceptance: flipping an RC key off cleanly hides the placement with no blank container; paid user sees nothing; two fullscreen triggers never overlap.
```

### Task 4 — ILRD revenue logging
```
Read CLAUDE.md rule 7 and /docs/event-schema.md (ad_impression).
- Attach the paid/ILRD listener to EVERY ad format (native, interstitial, rewarded, app-open).
- Normalize valueMicros -> value; log ad_impression with value, currency, ad_format, ad_source, ad_unit, placement, precision, session_number.
Acceptance: ad_impression fires per impression with accurate value+currency, verified in GA4 DebugView and Realtime.
```

### Task 5 — Native + interstitial placements
```
Read /docs/monetization-spec.md (Ad Script table).
Wire native and interstitial placements exactly per the table: placement_id, screen, trigger, RC key, frequency/cap, cooldown, first-eligible session, paid behavior, no-fill/offline behavior. Route every decision through MonetizationManager.
Acceptance: each placement matches the table; native collapses on no-fill; no full-screen ad before the first value moment (native allowed only if non-blocking).
```

### Task 5a — Splash interstitial + branded pre-ad transition
```
Read /docs/monetization-spec.md (ad_inter_after_splash) and CLAUDE.md rules 1-4, 6-9.
Add the Apero-style splash/session-start interstitial — SEPARATE from App Open.
- Implement ad_inter_after_splash exactly from the spec; MonetizationManager only,
  no screen-level ad SDK calls.
- Never on session 1. Session 2 requires a session-1 core_action_done; session 3+
  per Remote Config (min_session + require_activation_for_session_2).
- Show the 3-second branded transition ONLY when eligible, transition enabled, and
  an ad is ready — it is a branded transition, NOT an ad-load wait. If no ad is
  ready within load_timeout_ms, fail open and continue (no 3s hold on failure).
- Suppress if App Open is eligible/active in the same foreground cycle; suppress for
  paid users; honor the UMP consent gate and the full-screen mutex.
- Log ad_show_attempt, ad_suppressed, ad_show_failed, ad_dismissed, ad_impression
  through the existing analytics wrapper; placement_id = ad_inter_after_splash.
Acceptance: no full-screen ad in session 1; session-2 gated on activation; 3s branded
transition before an eligible ad; fail open on not-ready; paid users see nothing;
splash + App Open never both fire in one foreground cycle; QA IA08–IA15 pass.
```

> **Durable note — do not regress (architecture):**
> - `ad_inter_after_splash` is a **live, intentional** placement. Do NOT remove it unless Product explicitly asks.
> - Keep splash interstitial and App Open **separate** (`ad_inter_after_splash` ≠ `ad_appopen_resume`); they never both show in one foreground cycle.
> - Use **format-specific** presenters: `presentInterstitial`/`requestInterstitial` (interstitial), App Open path, rewarded path. No generic `presentFullScreen()` that defaults to `showInterstitial()`. Native/paywall/permission/purchase dialogs never use the interstitial presenter.
> - Eligibility helper calls used for cross-placement suppression must be **side-effect-free** (`isAppOpenEligibleSnapshot()` — no caps consumed, no state mutated, no load/show started). Never call another placement's full suppression/show pipeline just to infer eligibility.

### Task 5b — Interstitial after onboarding complete
```
Implement ad_inter_after_onboarding_complete exactly from /docs/monetization-spec.md.
MonetizationManager only; no screen-level ad SDK calls.
- Trigger AFTER onboarding_complete is logged + persisted, BEFORE the first main
  destination (Home/Jobs). Never during onboarding steps.
- Default OFF (RC). Cap max once per install (persisted). Suppress for paid users,
  inside onboarding, when a full-screen surface is active, and when another
  full-screen onboarding ad was shown recently (unless allow_aggressive_stack).
- Honor the UMP gate + full-screen mutex. Fail open on no-fill/offline/not-ready/timeout.
- Run the branded transition and ad-readiness CONCURRENTLY; never hold the user past
  load_timeout_ms for a not-ready ad. Honest branded loading only (logo/spinner) — no
  CTA/tap-prompt/fake-reward UI.
- On show, forward-suppress the next forced full-screen surface (App Open + forced
  interstitials) for suppress_next_fullscreen_seconds (reason recent_onboarding_
  interstitial); do NOT suppress user-requested rewarded ads, paywalls, or native.
- Log via AnalyticsTracker only (ad_request/show_attempt/suppressed/show_failed/
  load_failed/dismissed/impression); placement_id = ad_inter_after_onboarding_complete.
Acceptance: QA IA18-IA27 pass; assembleDebug green.
```

### Task 5c — Onboarding tour-guide + animation Remote Config
```
Make onboarding tour-guide slides and onboarding animations Remote Config controlled
(no new build to retune). See /docs/monetization-spec.md "Onboarding Remote Config".
- Typed RC keys + safe defaults + validation/clamping via OnboardingConfigProvider;
  missing/invalid RC fails open and NEVER blocks onboarding.
- Enums: OnboardingTourVariant (none/one_card/three_card/full),
  OnboardingTourPlacement (before_language/after_language/after_location/before_home),
  OnboardingAnimationVariant (none/subtle/standard/rich).
- Product can: turn the tour off, pick variant, move placement, control skip vs
  force-completion, suppress for returning users / resume-uploaded / once-per-install,
  and enable/disable + restyle/retime animations.
- Tour cards use ONLY already-localized strings (no new translation keys). Honest
  branded content; no fake CTA/reward UI. Respect system reduced-motion.
- Tour must not block/overlap/delay ad placements (ad_inter_after_splash, full-screen
  onboarding native, onboarding-complete interstitial); the overlay resolves before
  the onboarding-complete interstitial runs.
- Track onboarding_tour_view/skip/complete + onboarding_animation_variant through
  AnalyticsTracker only; controlled values only (no PII/raw text).
Acceptance: QA ONB01-ONB15 pass; assembleDebug green.
```

### Task 6 — Rewarded unlocks + Copilot gating
```
Read /docs/monetization-spec.md (Rewarded caps) and CLAUDE.md rules 5-6.
- Implement rewarded unlocks for resume download, cover letter, mock start/score, game hint with the specified free/rewarded caps (RC-adjustable).
- Reward granted ONLY on the earned-reward callback, exactly once; log ad_reward_earned and ad_reward_granted separately.
- Copilot is Pro-only: gate behind paywall, NO rewarded path.
Acceptance: reward never granted on close/fail/offline; never double-granted on double-tap; Copilot shows paywall, not a rewarded ad.
```

### Task 7 — App Open + suppression zones
```
Read /docs/monetization-spec.md (App Open RC keys) and CLAUDE.md rules 1-4.
- App Open eligible session 2+ (only if a core action happened in session 1), else session 3; not first launch; paid users excluded.
- Suppress per the RC suppress_* keys: after external apply link, billing, permissions, legal screens, ad clicks, and during active resume/mock/copilot flows; respect the mutex and background-time threshold.
- If not preloaded, continue immediately (fail open).
Acceptance: all suppression zones honored; never shown on first launch or to paid users; never blocks resume.
```

### Task 8 — Purchases, entitlement states, diagnostics, failures
```
Read /docs/monetization-spec.md (IAP + entitlement states), /docs/event-schema.md.
- Log purchase with REAL local value+currency from Play Billing ProductDetails + transaction_id; never hardcode USD.
- Do NOT mark Firebase auto in_app_purchase as a key event or log it as duplicate revenue. Manual purchase is the source of truth; dedupe on transaction_id.
- Handle entitlement states incl. entitlement_unknown (no forced ads until restore resolves).
- Add ad-lifecycle diagnostics (ad_request/loaded/load_failed/show_failed/suppressed/dismissed) and the failure events (resume_optimize_failed, job_search_failed, external_apply_failed, etc.).
Acceptance: purchase reports true local value+currency in DebugView for 2 non-USD test markets; entitlement_unknown shows no forced ads; failure events fire on the broken paths.
```

---

## Leave for the human engineer (final prep)

Tell Claude Code not to touch these; they're the human's job:

- Secrets, signing config, keystores, `local.properties` values.
- Play Console: product IDs + localized base prices, Data Safety form, store listing, internal-testing track.
- AdMob mediation console: integrate + verify the five adapters (AdMob Network waterfall; Pangle/Mintegral/AppLovin/Liftoff bidding), confirm App Open mediated support per network, set floors later.
- Final release **AAB** build + signing; run the full QA gate on the signed AAB, not a debug build.
- Security review of the consent + billing paths.
- Live verification: GA4 DebugView across 2+ non-USD markets, and a real EEA consent-flow test.

---

## Practical tips

- If Claude drifts, prompt: "Read back your understanding of what I asked." It surfaces the misread fast.
- Add durable corrections to CLAUDE.md with the `#` prefix as you go, so later sessions inherit them.
- Keep CLAUDE.md tight (a page or two). If it grows past ~300 lines, split specifics into `/docs`.
- The QA workbook's test-case tabs are your definition of done — point Claude at `/docs/qa-gate.md` per task.
