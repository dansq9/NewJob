# QA Gate — Ascend / NewJob

Definition of done. No build ships to PPC scale until the 9-point gate passes on a production-signed AAB.

## The 9-point Monetization QA Gate

1. **Ad-script compliance** — every live placement exists in the Ad Script with owner, RC key, cap.
2. **Remote Config compliance** — every placement RC-controlled; missing full-screen key => OFF; no blank containers.
3. **Full-screen mutex** — only one full-screen surface at a time.
4. **Paid-user suppression** — paid users: no forced ads, no native placeholders, no app-open; opt-in rewarded only.
5. **Rewarded correctness** — reward only on earned-reward callback, once; none on fail/offline.
6. **Lifecycle / no-internet** — ad failure never blocks the core loop; offline + reconnect handled per format.
7. **ILRD + IAP logging** — `ad_impression` (value+currency) and `purchase` (real local value+currency) verified in GA4 DebugView.
8. **Release-build validation** — verified on a production-signed AAB via Play Internal Testing, not debug.
9. **Consent / policy** — UMP (EEA/UK/CH), Data Safety, Advertising-ID + ads declarations, app-ads.txt.


## Ad QA test cases

Dual sign-off (partner check, then QA confirm). Evidence (video/screenshot) required for Critical.

| TC | format | test | expected | priority |
|---|---|---|---|---|
| IA01 | Interstitial | Loading ad shown before full ad | Loading state, then interstitial | Medium |
| IA02 | Interstitial | Click ad | Redirects to landing page | Medium |
| IA03 | Interstitial | Close ad | Next screen shown immediately | High |
| IA04 | Interstitial | Two full-screen ads conflict | MUTEX: only one full-screen ad at a time | Critical |
| IA05 | Interstitial | Show after splash loads | No ad before splash; loading then ad | High |
| IA06 | Interstitial | No internet | Flow continues normally; no block | High |
| IA07 | Interstitial | Ad does not overlay app content/audio | No popup/music overlap during ad | Medium |
| IA08 | Interstitial | Interstitial after splash/session start | `ad_inter_after_splash` only after splash/profile load and UMP consent resolution; never before splash | High |
| IA09 | Interstitial | No splash interstitial on first session | No `ad_inter_after_splash` in session 1 | Critical |
| IA10 | Interstitial | Session-2 activation gate | Session 2 only eligible if `core_action_done` happened in session 1; otherwise first eligible is session 3 | High |
| IA11 | Interstitial | Splash transition animation | 3-second branded loading/transition surface before the ad; no tap prompt, no fake reward language, no ad-like UI | Medium |
| IA12 | Interstitial | Splash interstitial fail-open | If the ad is not ready within `ads.inter.after_splash.load_timeout_ms`, continue to the target screen (no 3s hold for a failed ad) | Critical |
| IA13 | Interstitial | Splash interstitial vs App Open mutex | `ad_inter_after_splash` and `ad_appopen_resume` never both show in the same foreground cycle | Critical |
| IA14 | Interstitial | Splash interstitial paid-user suppression | Paid users never see the splash interstitial or its transition | Critical |
| IA15 | Interstitial | Splash interstitial UMP gate | No splash interstitial request/show before `canRequestAds()==true` | Critical |
| IA16 | All | Format-specific full-screen dispatch | Interstitial placements use the interstitial API, App Open uses the App Open API, rewarded uses the rewarded API; native placements are never passed through the interstitial presenter; no generic presenter defaults to interstitial | Critical |
| IA17 | All | Pure eligibility helpers | Splash suppression can check App Open eligibility without consuming caps, mutating state, marking App Open shown, or starting App Open load/show | High |
| IA18 | Interstitial | Interstitial after onboarding complete | After `onboarding_complete` only, before the first main app screen; never during language/location/role/resume setup | High |
| IA19 | Interstitial | Onboarding interstitial default OFF | With `ads.inter.after_onboarding_complete.enabled=false` or missing, no ad appears and the app continues normally | Critical |
| IA20 | Interstitial | Onboarding interstitial fail-open | If ad is not ready/no-fill/offline/timeout, app goes to Home/Jobs immediately with no blank screen | Critical |
| IA21 | Interstitial | Onboarding interstitial paid suppression | Paid users never see this placement or its transition | Critical |
| IA22 | Interstitial | Onboarding interstitial mutex | Does not show if paywall, purchase dialog, app-open, rewarded, permission dialog, or another interstitial is active | Critical |
| IA23 | Interstitial | Onboarding interstitial recent-ad suppression | If another full-screen onboarding ad was shown recently, suppress unless `allow_aggressive_stack=true` | High |
| IA24 | Interstitial | Onboarding interstitial UMP gate | No request/show before UMP consent resolves and `canRequestAds()==true` | Critical |
| IA25 | Interstitial | Forward suppression after onboarding interstitial | After it shows, App Open + regular interstitials suppressed for `suppress_next_fullscreen_seconds`; user-requested rewarded ads are NOT suppressed | High |
| IA26 | Interstitial | Transition and timeout alignment | Transition and ad readiness run concurrently; user not delayed beyond `load_timeout_ms` if the ad is not ready | Critical |
| IA27 | Interstitial | Honest branded transition | Transition contains no fake CTA, tap prompt, fake reward copy, or ad-like UI before the interstitial | High |
| OA01 | App-open | Background fill under resume ad | Branded/neutral surface, no half-rendered content | Medium |
| OA02 | App-open | Click resume ad | Redirects to landing page | Medium |
| OA03 | App-open | Close resume ad | Returns to the resume screen | High |
| OA04 | App-open | Return from permission/settings | Do NOT show resume ad | High |
| OA05 | App-open | On IAP/Sub, Terms, Privacy screens | Do NOT show resume ad | High |
| OA06 | App-open | Return from Rate-Us / share sheet | Do NOT show resume ad | Medium |
| OA07 | App-open | No internet on resume | No ad; functions work normally | High |
| OA08 | App-open | Not first launch | No app-open on session 1; s2 only if activated | Critical |
| OA09 | App-open | Return from external apply link | Do NOT show; show tracker prompt instead | High |
| OA10 | App-open | During resume upload / mock / copilot | Do NOT show app-open | Critical |
| N01 | Native | Loading placeholder before ad | Placeholder same size as final ad | Medium |
| N02 | Native | Layout | Not overlapped or cut | Medium |
| N03 | Native | Background differs from app bg | Distinguishable | Low |
| N04 | Native | AdChoices | Inside the ad frame | Medium |
| N05 | Native | "AD" label | Visible, stands out | High |
| N06 | Native | MediaView | Min 120x120dp | Medium |
| N07 | Native | Title truncation | Ellipsize cleanly (Apero ref: ~25 chars; test long strings — do not hardcode) | Medium |
| N08 | Native | Body truncation | Ellipsize cleanly (Apero ref: ~90 chars; test long strings) | Medium |
| N09 | Native | Does not cover app content | No overlap either direction | Medium |
| N10 | Native | Click ad | Links to landing page | Medium |
| N11 | Native | No internet / no-fill | Hide loading + ad cleanly; container collapses | High |
| N12 | Native | Job-list ad labeled | Looks like a card but clearly "Sponsored/Ad" | High |
| B01 | Banner | Loading before ad | Loading then banner | Low |
| B02 | Banner | Does not cover content | Anchored, no overlap | Low |
| B03 | Banner | Click | Links to landing page | Low |
| B04 | Banner | No internet | Hide cleanly | Low |
| R01 | Rewarded | Reward sign at trigger | Clear "watch ad to..." with exact reward | High |
| R02 | Rewarded | Click ad | Links to landing page | Medium |
| R03 | Rewarded | Complete ad | Reward granted on earned-reward callback, ONCE | Critical |
| R04 | Rewarded | Two full-screen ads conflict | MUTEX honored | Critical |
| R05 | Rewarded | No internet | Clear message; NO reward | High |
| R06 | Rewarded | Close early / ad fails | NO reward; retry/upgrade fallback | Critical |
| R07 | Rewarded | No interstitial stacked after rewarded | Suppress interstitial ≥180s after rewarded | High |
| AX01 | All | No ad before first value moment (session 1) | No FULL-SCREEN ad before first value (session 1). Native is allowed only if it does not block onboarding, does not shift layout, and collapses on no-fill. | Critical |
| AX02 | All | No ad during resume upload/parsing | Clean flow | Critical |
| AX03 | All | No ad during active mock interview | Clean flow | Critical |
| AX04 | All | No ad during active copilot session | Clean flow | Critical |
| AX05 | All | No forced ad for paid users | Suppressed everywhere | Critical |
| AX06 | All | Remote key OFF hides placement | No blank container remains | High |
| AX07 | All | No infinite ad loading | Fail open after timeout | Critical |
| OA11 | App-open | Return from ad landing page | Do NOT show app-open | High |
| OA12 | App-open | Foreground after <30s background | Do NOT show app-open | High |
| N13 | Native | Paid-user suppression | No native loading placeholder remains for paid users | Critical |
| R08 | Rewarded | Double-tap / duplicate reward | Reward granted exactly once, never twice | Critical |
| AX08 | All | No ad requested before UMP consent resolves | Ad SDK init / first request gated behind canRequestAds()==true; EEA/UK/CH form shown first | Critical |
| ONB01 | Onboarding | Tour disabled | `onboarding.tour.enabled=false` skips all tour slides; onboarding continues | High |
| ONB02 | Onboarding | Tour variant none | `variant=none` skips tour even if `enabled=true` | High |
| ONB03 | Onboarding | One-card tour | `variant=one_card` + `max_cards=1` shows exactly one card | Medium |
| ONB04 | Onboarding | Three-card tour | `variant=three_card` + `max_cards=3` shows exactly three cards if available | Medium |
| ONB05 | Onboarding | Full tour | `variant=full` shows all available cards up to `max_cards` | Medium |
| ONB06 | Onboarding | Skip behavior | `show_skip=true` shows Skip; `force_completion=false` allows skip; `force_completion=true` requires completion | High |
| ONB07 | Onboarding | Placement control | before_language/after_language/after_location/before_home each place the tour correctly | Medium |
| ONB08 | Onboarding | Missing RC fail-open | Missing/invalid RC keys use safe defaults and do not block onboarding | Critical |
| ONB09 | Onboarding | Returning-user suppression | `suppress_if_returning_user=true` prevents the tour after first onboarding completion | High |
| ONB10 | Onboarding | Resume-upload suppression | `suppress_if_resume_uploaded=true` skips tour when a resume was already uploaded in onboarding | High |
| ONB11 | Onboarding | Animation disabled | `onboarding.animations.enabled=false` disables custom onboarding animations | Medium |
| ONB12 | Onboarding | Reduced motion | System reduced-motion + `reduce_motion_respect_system=true` → minimal/no animation | High |
| ONB13 | Onboarding | Animation duration | `duration_ms` controls timing within the allowed range | Low |
| ONB14 | Onboarding | Animation variant | none/subtle/standard/rich map to valid behaviors without breaking onboarding | Medium |
| ONB15 | Onboarding | No monetization conflict | Tour does not block/overlap/delay `ad_inter_after_splash`, full-screen onboarding placements, or the onboarding-complete interstitial | Critical |

## Lifecycle / no-internet

| # | scenario | expected |
|---|---|---|
| 1 | Start app offline | App opens; search shows offline/empty state |
| 2 | Search jobs offline | Clear offline state; no stuck loader |
| 3 | Open saved jobs / tracker offline | Local Room data shows |
| 4 | Start resume optimizer offline | Clear message; no infinite load |
| 5 | Lose internet during rewarded ad | No reward; retry/upgrade message |
| 6 | Reconnect while ad loading | Recovers or fails open; flow continues |
| 7 | Reconnect after failed search | Search works; no duplicate calls |
| 8 | Reconnect after failed resume upload | Upload ret[ries] cleanly |
| 9 | Interstitial not ready at trigger | Continue immediately; preload next |
| 10 | App backgrounded <15–30s then resumed | No app-open ad |
| 11 | Two full-screen triggers race | Mutex: second is suppressed |
| 12 | Push tap into active flow | No app-open over the flow |
| 13 | App killed/restarted while ad was loaded | No stale ad shown; reload cleanly |
| 14 | App killed/restarted during rewarded unlock | No reward unless earned-callback fired; safe state |
| 15 | Resume after returning from Play Billing | No app-open ad over billing return |
| 16 | Resume after file picker | No app-open ad over picker return |
| 17 | Resume after microphone permission | No app-open ad over permission return |

## Event tracking — definition of done

Every event in `event-schema.md` fires with correct params, verified in GA4 DebugView. `ad_impression` + `purchase` verified in DebugView for **2 non-USD markets** before scale. Bidding events (`ad_impression`, `purchase`) imported in Google Ads under tROAS only.


## Left for the human engineer

Secrets/signing, Play Console (product IDs, localized prices, Data Safety, listing), AdMob mediation console (adapter integration + verification, floors), final signed AAB + full-gate run, `/security-review` of consent + billing, live DebugView across non-USD markets, real EEA consent-flow test.
