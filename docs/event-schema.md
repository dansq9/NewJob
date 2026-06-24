# Event Schema — Ascend / NewJob (GA4 / Firebase first)

Tracking contract. GA4/Firebase is the source of truth (tROAS-ARO + Remote Config read GA4). Event names match the QA workbook's Event Tracking tab 1:1.

## Identity & rules

- **No login.** Identity = anonymous `install_id` (UUID, first launch) set as a user property = distinct_id.
- GA4 limits: event name ≤40, param name ≤40, param value ≤100, **user property name ≤24, value ≤36**, 25 params/event, 25 custom user properties.
- **No PII** in params (no email/name/raw query/resume text/filenames). Use bands & booleans.
- Monetization: reserved `value`+`currency` (ISO 4217). `ad_impression` imports to Google Ads only under the tROAS bid type.
- **Purchase = one source of truth:** manual Play Billing `purchase` with REAL local value+currency; do NOT also count `in_app_purchase` auto event; dedupe on `transaction_id`.
- Use `session_start_enriched`, not GA4 auto `session_start`.


**`Trigger`** — Event Definition
- `Property Name` (Data Type) — Sample Values

## 1 · ACQUISITION & ACTIVATION


**`first_open`** — GA4 auto on first launch. Capture attribution.
- `referrer_source` (String) — google_ads / organic
- `install_id` (String) — uuid-v4

**`onboarding_step`** — Each onboarding advance/skip.
- `step` (String) — language/role/location/resume
- `skipped` (Boolean) — true/false

**`onboarding_complete`** — Activation gate; tCPA event. Use bands, not free text (raw role/location stay local/backend only).
- `target_role_present` (Boolean) — true/false
- `target_role_category` (String) — product/engineering/sales/marketing/ops/other
- `location_type` (String) — remote/hybrid/onsite/unknown
- `resume_uploaded` (Boolean) — true/false

**`core_action_done`** — First core action; activation + App-Open gate.
- `action_type` (String) — search/save/upload/apply/mock_start
- `session_number` (Numeric) — 1,2,3...

## 2 · CORE JOB LOOP


**`job_search`** — Search executes (post-debounce).
- `query_present` (Boolean) — true/false
- `filters_used` (Boolean) — true/false
- `results_count` (Numeric) — 0,12,50
- `source` (String) — home/jobs_tab/push

**`job_detail_view`** — Detail opens.
- `match_score_band` (String) — high/med/low
- `employment_type` (String) — full_time/contract
- `remote_type` (String) — remote/hybrid/onsite

**`job_save`** — Saved to tracker.
- `from_screen` (String) — search/detail

**`job_apply_click`** — Apply tap; intent signal.
- `apply_type` (String) — external/internal

**`tracker_stage_change`** — Tracked job changes stage.
- `from_stage` (String) — saved/applied/interview
- `to_stage` (String) — applied/interview/offer

## 3 · RESUME TOOLS


**`resume_upload`** — File accepted.
- `file_type` (String) — pdf/docx
- `file_size_band` (String) — small/med/large

**`resume_optimize_start`** — Optimization begins.
- `has_target_job` (Boolean) — true/false

**`resume_optimize_complete`** — Result shown; quality signal.
- `score_band` (String) — high/med/low
- `gated_by` (String) — free/rewarded/pro

**`resume_download`** — Export of optimized resume.
- `format` (String) — pdf/docx
- `gated_by` (String) — rewarded/pro

**`cover_letter_generate`** — Cover letter generated.
- `gated_by` (String) — rewarded/pro

## 4 · INTERVIEW


**`mock_interview_start`** — Mock starts.
- `gated_by` (String) — free/rewarded/pro
- `role_source` (String) — target_role/job

**`mock_interview_complete`** — Mock finishes; quality signal.
- `questions_answered` (Numeric) — 0-N
- `gated_by` (String) — free/rewarded/pro

**`copilot_session_start`** — Copilot session starts (Pro-only feature).
- `gated_by` (String) — pro

## 5 · MONETIZATION SPINE   value + currency REQUIRED


**`ad_impression`** — EVERY ILRD paid callback, all formats. Only event tROAS-ARO consumes; import to Google Ads.
- `value` (Numeric) — 0.0123
- `currency` (String) — USD
- `ad_format` (String) — native/interstitial/rewarded/app_open/banner
- `ad_source` (String) — admob/pangle/mintegral/applovin/liftoff
- `ad_unit` (String) — {native_job_list}
- `placement` (String) — ad_native_job_list
- `precision` (String) — estimated/publisher_provided/precise
- `session_number` (Numeric) — 1,2,3...

**`rewarded_ad_start`** — Rewarded shown.
- `placement` (String) — ad_rewarded_resume_download
- `reward_type` (String) — resume_optimize/resume_download/cover_letter/mock_start/mock_score/game_hint

**`rewarded_ad_complete`** — Earned-reward callback ONLY. reward_granted=true = unlocked once.
- `reward_type` (String) — resume_optimize/resume_download/cover_letter/mock_start/mock_score/game_hint
- `placement` (String) — ad_rewarded_resume_download
- `reward_granted` (Boolean) — true/false

**`purchase`** — Play Billing success only. Feeds tROAS-Hybrid + IAP.
- `value` (Numeric) — 9.99
- `currency` (String) — INR/BRL/EUR/USD
- `product_id` (String) — ascend.weekly/yearly/lifetime
- `product_type` (String) — weekly/yearly/lifetime
- `trial` (Boolean) — true/false
- `is_renewal` (Boolean) — true/false
- `transaction_id` (String) — order-id / token hash
- `purchase_source` (String) — play_billing_direct

## 6 · PAYWALL FUNNEL


**`paywall_view`** — Paywall shown.
- `variant` (String) — control/discount/trial/lifetime
- `trigger_placement` (String) — copilot/resume_download/mock_score

**`paywall_start_trial_click`** — Subscribe/trial tapped.
- `variant` (String) — control/discount/trial

**`paywall_dismiss`** — Closed without purchase; rewarded fallback may follow.
- `variant` (String) — control/discount/trial
- `trigger_placement` (String) — copilot/resume_download/mock_score

## 6b · FAILURES & ERRORS  (low-cardinality; separate "broke" from "lost interest")


**`job_search_failed`** — Fires on failure of this flow.
- `error_type` (String) — network/timeout/validation/no_results/unsupported_file/api_error

**`resume_upload_failed`** — Fires on failure of this flow.
- `error_type` (String) — network/timeout/validation/no_results/unsupported_file/api_error

**`resume_optimize_failed`** — Fires on failure of this flow.
- `error_type` (String) — network/timeout/validation/no_results/unsupported_file/api_error

**`mock_interview_failed`** — Fires on failure of this flow.
- `error_type` (String) — network/timeout/validation/no_results/unsupported_file/api_error

**`copilot_answer_failed`** — Fires on failure of this flow.
- `error_type` (String) — network/timeout/validation/no_results/unsupported_file/api_error

**`external_apply_failed`** — Fires on failure of this flow.
- `error_type` (String) — network/timeout/validation/no_results/unsupported_file/api_error

**`file_picker_cancelled`** — User cancels the document picker. NOT a failure — behavior event; exclude from error/crash dashboards.
- `reason` (String) — user_cancel

**`permission_result`** — After a runtime permission prompt.
- `permission` (String) — record_audio/storage
- `granted` (Boolean) — true/false

## 7 · RETENTION & PUSH


**`session_start_enriched`** — First foreground of a NEW session. NOT GA4 auto session_start (leave that untouched).
- `session_number` (Numeric) — 1,2,3...
- `user_ad_segment` (String) — new/activated/ad_tolerant/ad_sensitive/payer/lapsed

**`app_open_resume`** — Foreground + App-Open eligible.
- `was_ad_shown` (Boolean) — true/false
- `suppressed_reason` (String) — first_launch/billing/apply_return/not_ready/paid/consent

**`push_sent`** — Server-side dispatch (not GA4 client).
- `channel` (String) — jobs_fresh/tracker/resume_interview/winback/monetization

**`push_open`** — Push tapped.
- `channel` (String) — jobs_fresh/tracker/winback
- `deep_link_target` (String) — jobs/tracker/resume

## 8 · AD LIFECYCLE DIAGNOSTICS & SCREENS  (DEBUG — do NOT import to Google Ads)


**`ad_request`** — Ad requested.
- `placement_id` (String) — ad_native_job_list
- `ad_format` (String) — native/interstitial/rewarded/app_open

**`ad_loaded`** — Ad fills.
- `placement_id` (String)
- `ad_format` (String)
- `ad_source` (String) — admob/pangle/mintegral/applovin/liftoff

**`ad_load_failed`** — No-fill / load error.
- `placement_id` (String)
- `ad_format` (String)
- `reason` (String) — no_fill/timeout/network

**`ad_show_attempt`** — Show attempted.
- `placement_id` (String)
- `ad_format` (String)

**`ad_show_failed`** — Show fails.
- `placement_id` (String)
- `reason` (String) — not_ready/mutex

**`ad_suppressed`** — Suppressed by a rule.
- `placement_id` (String)
- `reason` (String) — low-cardinality only: first_session / not_activated_session_2 / paid / consent / appopen_eligible / mutex / cooldown / not_ready / remote_config_off / global_off / not_eligible / entitlement_unknown / bg_too_short / suppress_zone / not_preloaded

**`ad_dismissed`** — Full-screen ad closed.
- `placement_id` (String)
- `ad_format` (String)

**`ad_reward_earned`** — SDK says user EARNED reward.
- `placement` (String)
- `reward_type` (String) — resume_optimize/resume_download/cover_letter/mock_start/mock_score/game_hint

**`ad_reward_granted`** — App ACTUALLY unlocked. earned w/o granted = bug.
- `placement` (String)
- `reward_type` (String)

**`screen_view`** — GA4 screen_view for major screens.
- `screen_name` (String) — home/jobs/job_detail/tracker/resume/resume_result/mock_interview/copilot/games/paywall
- `screen_class` (String) — Compose route

## Registered / user properties (attached to every event)

| property | scope | sample | used by |
|---|---|---|---|
| `install_id` | User property (identity) | uuid-v4 | identity, caps, BigQuery joins |
| `user_account_status` | User property | Free / Pro | paywall, ad suppression |
| `user_plan` | User property | Free/Weekly/Yearly/Lifetime | LTV, gates |
| `user_ad_segment` | User property | new/activated/ad_tolerant/ad_sensitive/payer/lapsed | ad intensity, push, RC tier |
| `ad_aggressiveness_tier` | User property | retention_protect/balanced/arbitrage_high/paid_or_high_intent | which ad ladder |
| `rc_variant` | User property | A/B/C/D | experiment analysis |
| `session_number` | Event param (every event) | 1,2,3... | progressive ad logic |
| `app_version` | User property | 1.0.0 | release debugging |
| `os_version` | User property | Android 14 | device matrix |
| `country` | User property | US/PH/BR | geo tier, network mix |
| `referrer_source` | User property | google_ads/organic | attribution |