# Resume Hub — Refined Design (validated)

**Status:** validated by a 5-user × 1-researcher study (2026-06-27). Source artifacts:
`scratchpad/hub_reactions.json`, `scratchpad/research_features.json`, the prior `docs/ux-parity-findings.md`.
**Implements:** the two blockers from the parity study (no Build, no Edit path) + the Optimizer gaps.

## Verdict

The 3-card hub is sound — every test user self-routed to the right card (Devin→Build,
Priya→Edit, Maya→Optimize). The concept ships, with these non-obvious refinements baked in:

1. **The hub must not tax the hot paths.** Today the Home card is one tap to upload+optimize.
   Promote "use a file I already have" so upload isn't buried inside Build, and deep-link the
   Job-Detail "Tailor my resume" action straight into Optimize (job pre-attached).
2. **Score is free; only Apply-fixes and Download are rewarded-gated.** This is not a new
   monetization choice — it is what `monetization-spec.md` already says (`ad_rewarded_resume_optimize`
   = "Tap Apply AI fixes (after free score)", line 194 "score free; apply/download via rewarded").
   The current code gates the *score* behind a rewarded ad, which is the deviation we correct.
3. **Optimize's headline gap is JD-attach.** Code derives the target job only from `SelectedJobStore`.
   Add an in-screen step: pick from Tracker, type title+company, or "general score" (default, never
   blocked). Job-URL ingest is deferred (needs backend — see open questions).
4. **The library must be safe.** Editable title, rename, duplicate (save-as), and soft-delete with
   undo — replacing today's file-pointer record with hard one-tap delete.
5. **Build must be honest and forgiving.** No fabricated employers/titles/metrics (AI-added numbers
   are editable placeholders to confirm); a neutral opener (not "your last role") with a
   no-experience on-ramp; voice permission/no-speech/"type instead" fallbacks; auto-save from step 1.

## Hub cards (final copy — de-jargoned per the low-tech user)

| Label | Subtitle | Opens | Empty state |
| --- | --- | --- | --- |
| **Build a resume** | "Start fresh — speak it or fill it in, AI helps you write" | Build method chooser | Always available; the safe default ("Not sure where to start? Build a resume.") |
| **Edit a resume** | "Open one you saved here and make changes" | Resume library → editor | If nothing saved: muted "Nothing saved yet", tapping routes into Build |
| **Check a resume for a job** | "Score it and fix it for a posting — or get a general score" | Optimizer (extended) | If library empty: upload/build first; never dead-ends |

A persistent **"Use a file I have"** affordance sits at the top of the hub → file picker → optimizer,
so the already-have-a-PDF user (Alex) is one tap from value.

## Build flow

1. **Method chooser:** *Speak it* (voice) · *Fill it in* (guided form) · *Use a file I have* (upload → reformat/optimize). No blanket "FASTEST" badge on voice — label it "Hands-free".
2. **Voice:** request mic permission with rationale *before* recording; explicit fallbacks — denied → "Type it instead"; no speech/timeout → "Didn't catch that — try again or type it".
3. **Sections:** Contact → Summary → Experience → Education → Skills. Opener is neutral ("Tell us about yourself — a role, studies, or projects"), with a **"No work experience yet"** on-ramp that leads Education/Projects/Skills.
4. **AI-write per section** under an honesty contract: rephrase/structure only; never invent employers, titles, dates, metrics. AI-added quantifiers render as editable "add a number" placeholders to confirm.
5. **Live strength score** framed as fixable ("Resume strength 72/100 — here's what raises it"), never pass/fail. "ATS" explained once in a tooltip, not in primary copy.
6. **Auto-save** to the library as a draft from section 1; persistent "Saved" + Back.
7. **Finish:** "Save to my resumes" (free, no ad) and "Download / share" (rewarded-gated, disclosed up front).

## Edit flow

1. **Library list:** title, last-edited, last score. In-app **BUILT** resumes open the structured editor; **UPLOADED** file pointers are listed but marked "open to optimize, not edit".
2. **Editor** reuses the Build section UI on saved structured content.
3. **Non-destructive save:** "Update this version" vs "Save as a copy" (duplicate), plus Rename. Edits never silently overwrite the only copy.

## Optimize flow (extends today's screen)

1. **Pick a resume:** select saved/built OR upload (reuses `rememberResumePicker`).
2. **Attach a job (new):** (a) pick from Tracker / selected job, (b) type title + company, (c) **"No specific job — general score"** default. *(Job-URL deferred.)*
3. **Analyze for FREE:** analyzing state → score ring + issues with severity → **Apply fixes** (rewarded) → re-score ("Optimized to 93") with a before/after of rewritten bullets.
4. **Download/share** stays rewarded-gated (Pro bypasses), disclosed before the user invests.

## Cross-cutting

- **Reuse:** optimizer score ring, issues list, `EmptyLibrary` upload, `rememberResumePicker`, rewarded download, native slot, after-score interstitial — all owned by `MonetizationManager` (rule 2), empty slots collapse (rule 4), full-screen mutex respected (rule 3).
- **New:** Build flow (voice + guided + AI-write + live score), structured editor, library management (title/rename/duplicate/soft-delete), JD-attach step, apply-fixes→re-score loop.
- **Monetization:** move the rewarded call off `optimize()` onto **Apply-fixes** + **Download** (spec-correct). Reward fires only on the earned callback (rule 5); on fail/no-fill the saved resume + free score are kept, only export is withheld.
- **Anonymous / local-only:** all resumes live on-device (no login). Surface an on-device-only notice and a manual export; do **not** add login.
- **Analytics:** new events (`build_start`, `build_method`, `voice_fallback`, `jd_attach_method`, …) through `AnalyticsTracker` with typed constants; log bands/booleans only — never resume text, employer names, URLs, transcripts (rule 8).
- **Localization:** every new string in all 9 locales; jargon ("ATS", "wizard") kept out of primary copy.

## Implementation order (CI-verified increments)

1. **Hub shell + routing** — relabel Home card to "Resume"; 3-card hub; "Use a file I have"; Job-Detail deep-links Optimize; Edit→Build when empty. *(No model migration, no monetization change.)*
2. **Optimizer spec-fix + JD-attach** — free score; rewarded on Apply-fixes + Download; Tracker/manual/general JD step; apply-fixes→re-score + bullet diff.
3. **Library model upgrade** — title/updatedAt/source/structured content; rename, duplicate, soft-delete + undo; one shared picker; on-device notice + export.
4. **Build — guided form** — sections + AI-write + live score + auto-save + no-experience on-ramp + honesty contract.
5. **Build — voice** — permission rationale + full fallback states.
6. **Edit — structured editor** reusing Build sections; non-destructive save/save-as.

## Open questions for the product owner

- **Cloud backup:** stay strictly local with manual export, or optional anonymous backup keyed off `install_id`? (Data-Safety implications.)
- **Server JD-URL ingest:** can the platform API fetch/parse an arbitrary posting URL (legal/scraping, EEA)? Defines whether the URL option ships and its parse-failure fallback.
- **Voice transcription:** on-device `SpeechRecognizer` vs server STT (privacy, cost, offline, 9-locale coverage).
- **AI-write provider/limits** and the exact "no fabrication" wording we can stand behind in 9 locales.
- **Uploaded → editable?** Should uploaded PDFs be server-parsed into structured content so they're editable, or remain optimize-only file pointers?
