# Rokid AOE UI Review — 2026-07-13

Scope: current Compose HUD (`HudScreen.kt`, `ScannerActivity.kt`) across Sessions, Terminal, Reply Menu, Text Input, New Session, permission/model prompt, and scanner states.

Companion specification: [`ROKID_AOE_DESIGN_SYSTEM.md`](ROKID_AOE_DESIGN_SYSTEM.md).

## Executive finding

The current UI is not using a design system. `HudScreen.kt` acts as state store, page router, layout shell, page renderer, typography source, and component library simultaneously. The screenshot defects are therefore structural rather than isolated alignment bugs. Production pages should not receive more one-off font/padding fixes; the shared scaffold and tokens must be extracted first.

## Current screenshot review

1. `AOE TERM` creates a redundant first visual row and pushes the actual session identity down.
2. The connection icon is visually tied to `AOE TERM`, while the real terminal header (`CODEX / Code-Review2 / 运行`) sits on the same continuous black surface as agent output.
3. There is no opaque/bordered Header component, so the first agent line appears to begin immediately under app metadata.
4. `Enter 回复` is plain text at the end of the terminal content, not an affordance. It visually reads like another terminal row.
5. Terminal output, model/status lines, and footer share one uninterrupted typographic stream.
6. Multiple horizontal separator lines originate from terminal content itself; without fixed region boundaries they can be mistaken for page chrome.

## Measured implementation inconsistencies

Current `HudScreen.kt` uses all of these visible sizes:

| Element | Current size |
|---|---:|
| terminal body | 8sp |
| footer | 9sp |
| metadata | 10sp |
| session/body rows | 11sp |
| text input | 12sp |
| reply/new-session options | 13sp |
| connection icon and permission summary | 14sp |
| scanner text | 14sp |

Spacing is also page-specific: 1/2/5/8/10/14/20dp values are mixed rather than following a shared grid. Focus rows, footer text, permission overlays, and scanner overlays each invent separate styles.

## Structural causes

- `HudScreen` defines local `body` and `meta` styles, then every page overrides them with `.copy(fontSize = ...)`.
- Header, content, and footer are anonymous blocks inside a single `Column`; they are not reusable components with enforced bounds.
- `AoeTerminalView` places its metadata `Text` and a `LazyColumn.fillMaxSize()` as siblings inside the same outer `Box`. They can draw from the same origin, creating a real overlap risk rather than merely weak visual hierarchy.
- Tool naming is inconsistent: the UI emits abbreviated `CDEX`, while the requested product label is `CODEX`.
- `state.lines` continues to collect non-AoE agent events, but the active AoE page router does not render that collection; this is orphaned UI state that must either receive a real Body component or be removed after protocol consolidation.
- Footer is a plain `Text`, not a button component.
- Reply and New Session duplicate the same manual focused-row loop.
- Permission/model prompt uses a rounded 14dp modal that does not belong to the BBS terminal visual language.
- Scanner uses a separate color (`#00FF88`) and a separate 14sp typography system.
- The single `HudScreen.kt` file owns page rendering and terminal algorithms, making visual changes regression-prone.

## Approved-direction prototype rules

- Remove `AOE TERM` everywhere.
- Terminal header becomes `CODEX / Code-Review2 / 运行` with the icon at the right edge.
- Header, BodyViewport, and ActionDock have separate opaque bounds and divider lines.
- Agent output is the only content allowed in Terminal BodyViewport.
- `Enter 回复` becomes a compact 30px bracketed BBS command row inside a fixed 36px ActionDock.
- All app-visible text uses one 10sp monospace token and one 14sp line-height.
- Hierarchy uses placement, muted color, separators, and focus fill—not larger text.
- Reply Method contains only Dictation and Keyboard; Back cancels.
- Dictation review uses the sole two-row ActionDock variant.

### 2026-07-14 real-device density correction

The first migrated version was still too phone-like: 44dp session rows and a full-width inverse Enter row reduced usable information. The corrected production baseline is 9sp/12sp monospace, 24dp flat table rows, 28dp Header, 24dp Footer, and a 20dp left-aligned inverse command cluster. Sessions render in one navigation-ordered table without group-header cards. Terminal uses a 70-cell, 32-row CJK-aware viewport. Interactive Claude/Codex prompts use the primary agent `/sessions/:id/live-ws` stream so Up/Down controls the real option cursor; the auxiliary `/terminal/live-ws` stream must not be used for agent prompt input.

## Refactor sequence

### Phase 1 — Foundation

Create tokens, theme, `TerminalScaffold`, `HeaderBar`, `BodyViewport`, `ActionDock`, and `TerminalMenuRow`. Add screenshot/layout tests for fixed 480×640 bounds.

### Phase 2 — Read-only pages

Migrate Sessions and Terminal first. These establish row density, terminal wrapping, header truncation, scroll behavior, and compact footer commands. Verify on the named `Code-Review2` and `security-review` sessions.

### Phase 3 — Input pages

Migrate Reply Method, Keyboard Input, and New Session to the shared menu/action components.

### Phase 4 — Dictation state machine

Add Listening and Segment Review pages using `TranscriptPanel` and the review ActionDock. Do not auto-send ASR results.

### Phase 5 — System overlays

Migrate permission/model prompts and QR scanner chrome. Remove rounded modal and scanner-only typography/color.

### Phase 6 — Delete legacy styling

Remove local `TextStyle.copy(fontSize=...)`, one-off spacers, page-specific greens, and anonymous footer text. `HudScreen` remains a router only.

## Verification matrix

For each migrated page:

1. Unit/state test for navigation and action semantics.
2. Compose or deterministic layout assertion for region bounds.
3. Build/install on connected Rokid glasses.
4. UIAutomator text and bounds capture.
5. 480×640 screenshot review for hierarchy, clipping, and consistent type.
6. Positive and negative assertion (e.g. Footer is a compact command line rather than a CTA; footer text is absent from terminal body).

## Decision gate

**Passed 2026-07-14.** The user approved Design System v2 typography, region geometry, Header content, and compact BBS action behavior and instructed implementation to begin.
