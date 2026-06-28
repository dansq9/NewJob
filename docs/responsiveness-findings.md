# Responsiveness Audit — Emerging-Market Screen Sizes

**Date:** 2026-06-28
**Method:** 5 device-profile reviewers each audited the real Compose source (read-only) and reasoned
about layout at one device's exact dimensions — sizes common in Brazil, South Africa, and similar
markets. We cannot render the build here, so findings are code-grounded with `file:line` evidence.

| Profile | Represents | Focus |
| --- | --- | --- |
| **320dp Android Go** | Itel / Tecno / old Galaxy J (mdpi, Android 8 Go) | smallest mainstream width — overflow/cram |
| **360×640 budget** | Galaxy A03/A04, Moto E/G (short HD) | short height — content below the fold |
| **412dp larger** | Galaxy A54 / Pixel-class | scale-up — wasted space, long lines |
| **fontScale 1.6 @360dp** | low-vision user on a budget phone | fixed `dp` heights vs grown `sp` text |
| **360×800 + i18n/RTL** | Redmi 9 / A13, German/Portuguese/Arabic | long locales + right-to-left |

## Verdict

**The app is broadly responsive.** The layout strategy is sound: nearly everything is
`weight(1f)` + `verticalScroll`/`LazyColumn` + `FlowRow`, and RTL is correctly handled (every
directional icon uses `AutoMirrored`; no absolute/LTR positioning). No hard horizontal overflow and
no meaning-losing truncation were found at 320dp. The real defects cluster in three places:
**(1) one non-scrolling screen, (2) fixed `height(...)` containers that clip grown text, and
(3) a few fixed-width Rows that crowd long-locale labels.**

## Fixed in this pass

| Sev | Issue | Fix |
| --- | --- | --- |
| **High** | **Interviews screen was a non-scrolling `Column`** — at 360×640 the "Recent" empty-state card fell under the bottom nav, off the fold, unreachable (`InterviewsScreen.kt:35`). | Added `verticalScroll` to the root column, matching every other screen. |
| **High** | **Gradient action tiles locked to `height(120.dp)`** — at fontScale 1.6 the subtitle clipped (`AscendComponents.kt` `AscendActionTile`, `HomeScreen.kt` `QuickActionCard`). | `height(120.dp)` → `heightIn(min = 120.dp)`; column wraps content so the tile grows. |
| **Med** | **`AscendPrimaryButton` fixed at `height(54.dp)`** — long/large-font CTAs clipped vertically (used app-wide). | `heightIn(min = 54.dp)` + internal padding + centered text so it grows and wraps. |
| **Med** | **Tracker's 5-up stat chips** wrapped to ragged heights at 320dp / large font / long locales (`TrackerScreen.kt:298`). | Label `maxLines = 1` + ellipsis + center + horizontal padding so the row stays aligned. |
| **Med** | **Job-Detail's 3 action tiles** rendered ragged (different line counts) under German (`JobDetailScreen.kt:215`). | Row `verticalAlignment = Alignment.Top` so tiles align cleanly. |
| **Low** | **Optimizer verdict column** lacked `weight(1f)` — fragile to a long verdict at 320dp (`ResumeScreen.kt`). | Added `Modifier.weight(1f)`. |

## Recommended (documented, not yet changed)

These are real but lower-risk or touch sensitive screens; listed so they're tracked.

- **Paywall comparison columns are fixed-width** (`PaywallScreen.kt:83-96`, `width(70.dp)`/`width(64.dp)`).
  The "ADS" badge and column headers can crowd/clip under long locales (e.g. German) or fontScale 1.6.
  *Recommendation:* verify the localized `paywall_basic_ads` / `paywall_col_*` strings fit, and switch
  to `widthIn(min = …)` if any locale overflows. **Left unchanged** to avoid altering a monetization
  screen's layout without a visual check.
- **Games grid is `GridCells.Fixed(2)`** (`GamesScreen.kt:101`) — stays 2 columns at 412dp+, leaving
  gutter. *Recommendation:* `GridCells.Adaptive` would add a 3rd column on wide screens — but a naive
  `minSize` drops 360dp phones to **1 column**, so it needs a carefully chosen `minSize` (~110-115dp)
  and `maxLineSpan` for the spanned header/ad. **Left unchanged** to avoid a 360dp regression.
- **No max-width caps anywhere** — long-form text (Job-Detail body, hero subtitles) runs full width on
  412dp+/foldables, hurting line-length readability. *Recommendation:* a centered `widthIn(max ≈ 600dp)`
  wrapper on long-text screens. Harmless at phone sizes; future-proofs tablets.
- **Mock report area labels** use a hard `width(120.dp)` (`MockScreen.kt:146`) — wraps/misaligns at
  large font. *Recommendation:* `widthIn(max = 120.dp)` or a weighted label column.
- **Resume "Download / Share" two-button row** — German "Herunterladen" is tight in a half-width
  `OutlinedButton` (`ResumeScreen.kt`). *Recommendation:* shrink the leading icon or stack vertically
  when the label is long.
- **Resume Edit action row** (Rename / Duplicate / Remove) — three text buttons on one line could clip
  in long locales (`ResumeEditScreen.kt`). *Recommendation:* `FlowRow`, or move Remove to an icon.
- **Bottom-nav labels** single-line; long localized labels ellipsize at large font. *Recommendation:*
  keep `nav_*` translations short (currently OK).

## What's already good

- Home, Jobs, Job-Detail, Onboarding steps, all Resume screens, Mock, Copilot, Games, Tracker,
  Settings, Paywall body are `LazyColumn`/`verticalScroll` — they fit and scroll at 320dp and 360×640.
- Filter/date/employment chips use `FlowRow` — they wrap, not overflow, in long locales.
- RTL is correctly implemented: `AutoMirrored` back/forward icons throughout; no absolute positioning.
- Touch targets on the audited interactive rows meet ~48dp.

*Artifacts: the 5 reviewer reports (one per profile) informed this consolidation; the high-severity
and convergent-medium items were fixed in the same change.*
