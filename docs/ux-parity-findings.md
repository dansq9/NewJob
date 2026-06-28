# UX Parity Findings — Prototype vs. Coded App

**Date:** 2026-06-25
**Source of truth:** `Ascend.dc.html` (HTML prototype — reference only)
**Coded app:** `android/app/src/main/java/app/ascend/ui` (+ `app/ascend/games`)

## Method

Three persona test-users each walked **both** the HTML prototype and the coded Android
app and logged every divergence. Three UX researchers then independently audited those
reports against the prototype and the Kotlin/Compose source, each through a different lens:

| Persona | Coverage |
| --- | --- |
| **Maya Chen** — returning seeker, has a resume + target role | Onboarding, Home, Jobs, Job Detail |
| **Devin Park** — resume-first user building a document from scratch | Resume Generator/Builder/Optimizer, Tracker, Profile |
| **Priya Nair** — interview-prep user | Games, Mock interview, Live Copilot, Interviews hub |

| Researcher lens | Findings |
| --- | --- |
| Visual & Interaction Design | 10 |
| Information Architecture & Navigation | 9 |
| Feature Completeness & Functional Gaps | 11 |

After dedup across the three lenses, the divergences collapse to **16 distinct issues**,
prioritized below. The coded app reproduces the prototype's *visual language* faithfully —
color tokens, card radii, 1.5dp borders, gradient quick-action tiles, match badges, the dark
Copilot hero, the games engine, and the filter sheet all land on-brand. The regressions are
almost entirely about **missing flows** and **collapsed multi-state screens**, not styling.

## Scorecard

| # | Issue | Severity | Type | Screens |
| --- | --- | --- | --- | --- |
| 1 | AI Resume Generator missing | **Blocker** | Missing | Resume, Home, Onboarding |
| 2 | Multi-step Resume Builder missing | **Blocker** | Missing | Resume |
| 3 | Onboarding order re-engineered + resume parse/auto-fill dropped | High | Missing + Divergent | Onboarding |
| 4 | Post-tour paywall never fires | High | Missing | Onboarding → Paywall |
| 5 | Home search dead button + Jobs location picker gone | High | Stubbed + Missing | Home, Jobs |
| 6 | Mock report gutted + live session lacks Skip/End/timer | High | Missing/Divergent | Mock |
| 7 | Resume Optimizer drops target-picker, Apply-fixes re-score, severity | High | Missing/Divergent | Resume |
| 8 | Games hub drops streak/solved/badges header + per-tile state | High | Missing | Games |
| 9 | Dead controls styled as interactive | High | Stubbed | Home, Jobs, Tracker |
| 10 | Live Copilot thin (no detected-questions, Copy/Regen, summary, grounding) | Medium | Missing/Divergent | Copilot |
| 11 | Interviews recent-sessions permanent empty state + hero drops LIVE framing | Medium | Stubbed + Divergent | Interviews |
| 12 | Tracker drops View-all drill-down, tappable stat chips, stage picker | Medium | Missing/Stubbed | Tracker |
| 13 | Job Detail raw truncated text vs structured responsibilities/requirements | Medium | Divergent | Job Detail |
| 14 | Filter sheet "Show results" lacks live count | Low | Divergent | Jobs |
| 15 | Profile flattened into Settings | Low | Divergent | Profile |
| 16 | Misleading "prototype order" comment in code | Low | Doc defect | Onboarding |

---

## Blockers

### 1. AI Resume Generator is entirely missing

**Missing.** The prototype ships a full AI Resume Creator: a *"How do you want to start?"*
chooser (`Ascend.dc.html:753`) with three paths — **Build by voice** (mic transcription,
"FASTEST" badge), **Upload existing**, **Fill in guided form** — then a *"Structuring your
resume…"* state and a generated *"Draft ready"* resume with AI bullets (~743–819). Entry points
exist via `goGenerator` / `go('generator')` (lines 326, 1423, 2120).

In code there is **no generator route** in `Navigation.kt` (`Routes` has only `RESUME`), no
screen, and no voice-creation logic anywhere under `app/ascend`. The Home Resume tile and the
onboarding "Build one by voice" card never reach any generator.

**Recommendation:** Build a `ResumeGeneratorScreen` + `Routes.RESUME_GENERATOR` with the method
chooser (voice/upload/guided), wire it to the platform API for draft generation, and point the
entry points at it. If it can't ship for launch, replace the "Build by voice" affordances with an
honest "coming soon" state rather than silently routing to the file picker.

### 2. Multi-step Resume Builder is entirely missing

**Missing.** The prototype is a 6-step builder (`Ascend.dc.html` ~822–948) with a live
`Score x/100` header; step screens for Contact / Summary (AI Write) / Work experience (per-bullet
AI optimize + "Optimize all", add/remove roles) / Education / Skills chip input; a final ATS-ring
Score screen with verdict + checklist; a toggleable live Preview pane; and a sticky Next/Preview
footer.

In code there is no builder route and `resume/` contains only `ResumeScreen.kt` +
`ResumeViewModel.kt` (optimizer only). No wizard, no AI-write of summaries/bullets, no
skills/education editor, no live preview, no editable resume document. **Users can upload a
finished file and get an ATS score — they cannot author or edit resume content in-app.**

**Recommendation:** Scope a `ResumeBuilderScreen` as a multi-step wizard backed by a structured
resume model (contact/summary/experience/education/skills) with per-section AI assist and a live
ATS score. Stage it: ship Contact+Summary+Experience with AI-write first, then
Education/Skills/Preview. Add the route + a Home/Resume entry point; flag deferred steps explicitly.

> **Together, #1 and #2 mean the "resume" product is optimizer-only.** A user who arrives without
> a finished resume — exactly persona Devin — has no in-app path to create one. This is the single
> largest functional gap between prototype and code.

---

## High

### 3. Onboarding order re-engineered; resume parse/auto-fill dropped

**Missing + Divergent.** Prototype order is **Language → Location → Resume → Job Title → Tour →
Paywall** (`goResume` after location, `Ascend.dc.html:131`; `resumeContinue → jobtitle` line 2107).
`uploadResume` runs a *"Reading your resume…"* parsing state then a *"Here's what we found"* card
that **auto-fills location + job title** (`San Francisco, CA` / `Senior Product Manager`, line 1748)
*before* the user picks a role.

Coded order is **Language → Location → Job Title → Tour → Resume → Home** (`OnboardingScreen.kt`
STEP constants ~57–63) — resume is **last**, after the tour. `ResumeUploadStep` (~260–300) only flips
an upload tile to a filename; there is no parsing animation, no results card, and no auto-fill. The
prototype's headline *"AI reads your resume and pre-fills your profile"* moment is gone — and because
resume is last, even a real parser couldn't pre-fill the role the user already typed.

**Recommendation:** Restore the prototype sequence (resume right after location, before job title)
and add a parsing → results state to `ResumeUploadStep` that calls the platform parse endpoint and
pre-fills role/location/experience (editable "Looks right, continue"). If parsing can't ship, keep
resume early with an explicit manual-entry fallback.

### 4. Post-tour paywall never fires during onboarding

**Missing.** The prototype ends the tour on the Paywall: the last tour slide CTA is "See your plan"
and `nextTour` calls `go('paywall')` (`Ascend.dc.html:1743, 2138`); START FREE TRIAL / close both
lead Home. In code the tour's `onResolve` advances to `STEP_RESUME` and `finish` navigates straight
to HOME (`OnboardingScreen.kt` ~93, 141–154). `Routes.PAYWALL` / `PaywallScreen.kt` exist but are
never shown at the end of acquisition, so a fresh user is **never offered the trial**.

**Recommendation:** Present `PaywallScreen` as the final onboarding step (after resume, per prototype)
with a START FREE TRIAL CTA and a skip/close to Home. **Monetization-relevant — verify trigger timing
against `/docs/monetization-spec.md`** so the placement is Remote-Config-controlled and honors the
full-screen mutex as a single surface before wiring.

### 5. Home search is a dead button; Jobs location picker is gone

**Stubbed + Missing.** *Home:* the prototype has a live text input ("Search jobs, titles, companies")
whose `submitHomeSearch` carries the typed query into search (`Ascend.dc.html:273–277, 1739`). Coded
Home "search" is a non-editable `Surface` button (`HomeScreen.kt:101–115`) that only navigates to Jobs
with **no query carried** and a decorative arrow — tapping it bounces the user to an empty Jobs search
to retype. *Jobs:* the prototype has a tappable location chip (`openLocPicker`, line 464) opening a
bottom-sheet picker with search, "Use my current location", and city options (564–577). Coded
`JobsScreen.kt:78–86` renders location as a plain non-interactive `Row` — **no way to change location
on the Jobs screen at all**; it's stuck with whatever onboarding set.

**Recommendation:** Make the Home search box a real `TextField` that submits its query into
`JobsViewModel`. Add a location-picker bottom sheet on Jobs (reuse the onboarding geocode + popular
cities + free-text search) bound to `JobsViewModel.location` so results re-query on change.

### 6. Mock interview report gutted; live session lacks Skip/End/timer

**Missing/Divergent.** Prototype report (`Ascend.dc.html` ~1083–1136): success header (role · N
answered · clock), a 3-up Avg/Answered/Skipped row, an all-skipped empty state, a "Question by
Question" review with per-question scores, Strengths / Focus-areas cards, and Practice-again +
Back-to-interviews. Coded `Report()` (`MockScreen.kt:134–157`) renders only the title, one
average-score card, the per-area bars, and a single "Practice again" — `MockUi.Report` holds only
`MockScoreResponse(averageScore, areas)`. The live screen (`MockScreen.kt:89–132`) also drops the
prototype's Skip button, running clock, explicit End-Session control, and character counter, so the
"Skipped" stat can never even be produced.

**Recommendation:** Extend the score API/response and `Report()` to include per-question scores,
strengths/focus, and answered/skipped counts (with an all-skipped empty state) and add a
Back-to-interviews action. Add Skip + End-Session controls (and optionally timer/char counter) to
`Live()`.

### 7. Resume Optimizer drops target-job picker, "Apply fixes" re-score, and issue severity

**Missing/Divergent.** Prototype optimizer (`Ascend.dc.html` ~644–740) lets you choose/Change the
target role via a bottom sheet ("FROM YOUR TRACKER" / "RECOMMENDED FOR YOU"); shows an *"Analyzing
against the role…"* state; issue cards carry per-issue severity dots/labels; and an **"Apply fixes"**
CTA visibly re-scores ("Optimized to 93", `applyFixes` line 1787). Coded `ResumeScreen.kt` shows the
target role read-only from `SelectedJobStore` with no "Change" affordance (falls back to "General
optimization"), a bare `CircularProgressIndicator` with no analyzing copy, issue cards with only a
green/amber check (no severity label), and **no "Apply fixes"** — it jumps straight to Download/Share.
The optimizer is diagnostic-only and never delivers the "Optimized to 93" payoff.

> The rewarded-ad gate on download **matches** the monetization spec's intent — flagged for awareness,
> not as a defect.

**Recommendation:** Add an in-screen target-role picker sheet (Tracker + recommendations), render
per-issue severity from the API, add an "Analyzing against the role…" label, and add an "Apply fixes"
action that re-scores before Download/Share. Keep the rewarded download gate; verify analyze-vs-download
ad placement against `/docs/monetization-spec.md`.

### 8. Games hub drops the streak / solved-today / badges header and per-tile state

**Missing.** Prototype hub (`Ascend.dc.html:1296–1315`) leads with a header row (streak date) and two
prominent stat cards — a gradient "N day streak 🔥" card and a "gSolvedToday/gTotal solved today" card —
plus a horizontally-scrolling badges row, and each puzzle card shows a per-game completion-state line.
Coded `GamesScreen.kt` opens straight into the 2-column grid with a one-line blurb header; no
streak/solved/badges, no per-tile solved state. (The per-game accent colors added recently are a good
step but don't restore the progress framing.)

**Recommendation:** Add the streak + solved-today stat cards and badges row to the games hub header,
and a per-tile completion-state line, backed by a small games-progress store (DataStore/Room).

### 9. Controls styled as interactive are actually dead

**Stubbed.** Four places render a component that looks tappable but isn't (or doesn't do what its
affordance implies): the **Home search box** (`HomeScreen.kt:101–115`), the **Jobs location row**
(`JobsScreen.kt:78–86`) — both covered in #5 — plus the **Tracker stat chips** (`TrackerScreen.kt:297–305`,
display-only yet styled to invite a tap) and the **Tracker stage label** on each card
(`TrackerScreen.kt:356–374`, a non-interactive `Surface` where the prototype has a stage picker).
Static surfaces styled to invite a tap read as **broken**, not intentional.

**Recommendation:** Wire each control to its prototype behavior (see #5, #12), or remove the
interactive styling so inert elements don't read as broken.

---

## Medium

### 10. Live Copilot on-call experience is thin

**Missing/Divergent.** Prototype live Copilot (`Ascend.dc.html` ~1222–1290) is a continuous surface: a
"Waiting for the interviewer…" empty state with audio bars, a "Detected questions" list with a count
badge, an answer card with **Copy + Regenerate**, a "Listening…" bar with **JD/Resume grounding chips**,
and an end-of-session summary (Duration/Questions/Answers + rating). Setup collects an optional Job
Description and shows an attached Resume ("Used to ground your answers"). Coded `CopilotScreen.kt` is a
single-question flow: `SetupView` collects only Job title + Company (no JD, no resume attach); `LiveView`
has one transcription field + one answer card with no Copy/Regenerate, no detected-questions history, no
waiting state, no grounding chips; `end()` just pops back with no summary. The Pro gate itself works
(paywall redirect) — but the premium "copilot listening to my whole call" feel and answer grounding are
absent.

**Recommendation:** Add JD paste + resume attach to `SetupView` and pass them as grounding; in `LiveView`
keep a scrollable detected-questions list with a count, add Copy + Regenerate, and a post-call summary
screen. Keep `AdFlow.COPILOT` app-open suppression.

### 11. Interviews hub recent-sessions is a permanent empty state; hero drops LIVE framing

**Stubbed + Divergent.** Prototype "RECENT SESSIONS" lists tappable past mock/copilot sessions with
score, linking back into a report (`Ascend.dc.html` ~967–977). Coded `InterviewsScreen.kt:53–63`
hardcodes `AscendEmptyState` with no data source, so the section is always empty even after sessions
exist — progress over time is invisible. The Copilot hero (~67–95) drops the prototype's "LIVE" pill,
the bold "AI Interview Copilot" product title, and the radial glow, reading as a generic promo.

**Recommendation:** Persist mock/copilot session summaries (Room) and render them with tap-through to a
stored report; until then keep the empty state but ensure it disappears once history exists. Restore the
LIVE badge + "AI Interview Copilot" title on the hero.

### 12. Tracker drops View-all drill-down, tappable stat chips, one-tap stage picker

**Missing/Stubbed.** Prototype groups jobs by stage with a "View all" button opening a dedicated
full-screen Stage View (`Ascend.dc.html` ~390–470), makes the top pipeline stat chips tappable to jump
into a stage, and gives each card a center "pick stage" button to jump directly to any stage. Coded
`TrackerScreen.kt` renders one continuous `LazyColumn` with no "View all"; `StatChips` are display-only
yet look tappable (reads as broken); and the card's center element is a non-interactive stage-label
`Surface` — users can only step one stage at a time via up/down arrows.

**Recommendation:** Make `StatChips`/`StageHeader` scroll/filter to that stage (or add a `StageDetail`
route), and replace the center stage label with a stage-chooser sheet for one-tap jumps. At minimum,
remove the tappable affordance from inert stat chips.

### 13. Job Detail shows raw truncated text instead of structured lists

**Divergent.** Prototype renders "About the role" plus structured "What you'll do" (`jobResp`) and "What
we're looking for" (`jobReq`) bulleted lists with check/arrow icons (`Ascend.dc.html` ~600–627). Coded
`JobDetailScreen.kt:220–226` renders the raw API description truncated to 1200 chars as a single block —
no responsibilities/requirements breakdown, and long descriptions are abruptly cut. Minor: the gradient
"Tailor my resume" banner and the "Resume" action tile both route to `Routes.RESUME` (two identical
entry points), and the Copilot tile gives no Pro-only hint.

**Recommendation:** Segment the description into "What you'll do" / "What we're looking for" (server-side
or client heuristics) and render as lists; replace the 1200-char hard cut with expandable text.
Differentiate the banner vs. resume tile (or drop the redundant tile) and mark the Copilot tile Pro-only.

---

## Low

### 14. Filter sheet "Show results" lacks the live count

Prototype ends the filter sheet with a "Show {count} jobs" button reflecting the current selection
(`Ascend.dc.html` ~545–558). Coded `FilterSheet` (`JobsScreen.kt:183–186`) shows a static "Show results"
with no count, losing the pre-apply reassurance. *Recommendation:* compute the predicted result count and
render it in the button label.

### 15. Profile is flattened into Settings

The prototype's Profile is its own surface; the coded app reaches it through the Settings screen rework
(`SettingsScreen.kt`). The recent rework restored the avatar card, Pro/upgrade card, and stats row, so
this is now largely cosmetic — noted for completeness. *Recommendation:* confirm the Profile entry point
and naming match the prototype's mental model.

### 16. Misleading "prototype order" comment in code

`OnboardingScreen.kt:57` comments that its order (Language → Location → Job Title → Tour → Resume) **is**
the "Prototype onboarding order." It is not (see #3 — the prototype is Language → Location → Resume → Job
Title → Tour → Paywall). The comment will mislead the next engineer. *Recommendation:* correct or remove
it alongside the #3 reorder.

---

## What the coded app gets right

So the gaps above are read in context — these are faithful and should be preserved:

- Color tokens, card radii, 1.5dp borders, gradient quick-action tiles, match badges.
- The dark Copilot hero treatment and overall on-brand typography.
- The games engine itself and the per-game accent colors.
- The Jobs filter sheet structure (minus the live count, #14).
- Monetization wiring that matches spec intent (rewarded download gate, Copilot Pro gate,
  app-open suppression) — none of the above asks to weaken it.

## Cross-cutting themes

1. **The resume product is half-built.** Two blockers (#1, #2) plus #3 and #7 all converge on resume:
   no create path, no edit path, no parse/auto-fill, no re-score payoff. This is the highest-leverage area.
2. **Multi-state screens collapsed to one state.** Resume import (#3), Mock report (#6), optimizer (#7),
   Live Copilot (#10), Games hub (#8) each lost their intermediate/result states — the app shows the
   *start* and the *bare end* but drops the rewarding middle.
3. **Dead-but-styled controls (#9).** Home search, Jobs location, Tracker chips/stage label look tappable
   but aren't — small individually, but together they make the app feel broken on first touch.
4. **A monetization moment was dropped (#4).** The post-tour paywall is the prototype's primary
   acquisition→trial handoff and currently never fires; wire it only after checking the spec.

---

*Generated from a 3-persona × 3-researcher UX-parity study. Raw artifacts:
`scratchpad/user_reports.json`, `research_visual.json`, `research_features.json`.*
