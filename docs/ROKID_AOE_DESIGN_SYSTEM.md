# Rokid AOE Terminal Design System — v2

Status: **Approved by the user on 2026-07-14; production migration is in progress.**

## 1. Product principles

1. **Terminal, not phone UI.** Pure black canvas, one-color green content, monospace typography, no Material cards or decorative branding.
2. **Three permanent regions.** Every page uses `Header / Body / Action Dock`; page content must never draw across region boundaries.
3. **One compact typography size.** Header, body, labels, rows, and commands all use the same 9sp monospace token with 12sp line height. Hierarchy comes from position, color, weight, separators, and focus fill — never from larger text.
4. **One focus language.** Normal = transparent black with green text. Focused = solid green with black text. No mixed outline-only and filled focus states.
5. **Outcome labels only.** Remove redundant product branding such as `AOE TERM`. The header identifies the current tool/session/state.
6. **Glasses-safe actions.** Up/Down navigates or scrolls; Enter activates the visible primary action; Back returns one layer.

## 2. Canvas and regions

Target canvas: `480 × 640`.

| Token | Value | Purpose |
|---|---:|---|
| `safe_top` | 36px | Avoid upper optical blind zone |
| `safe_bottom` | 24px | Keep action clear of lower edge |
| `page_inset_x` | 4px | Maximum usable terminal width |
| `header_height` | 28px | Compact session identity and connection state |
| `region_gap` | 4px | Visible separation between regions |
| `footer_single_height` | 24px | One-line BBS command/status bar |
| `footer_action_height` | 20px | Inverse command cluster inside the footer |
| `footer_grid_height` | 56px | Compact two-row review actions only |
| `divider` | 1px | Dim-green region separator |

The body always uses the remaining height. It clips/scrolls internally and cannot overlap the header or action dock.

## 3. Visual tokens

### Color

| Token | Value | Use |
|---|---|---|
| `bg` | `#000000` | Entire canvas and all unfocused surfaces |
| `fg_primary` | `#00FF40` | Main text, active icon, borders |
| `fg_muted` | `#78DA8C` | Secondary metadata and inactive icon |
| `divider` | `#164C22` | Region and row separators |
| `focus_bg` | `#00FF40` | Focus fill |
| `focus_fg` | `#000000` | Focused text |

No gradients, shadows, rounded cards, emoji status markers, or additional accent colors.

### Typography

```text
Font family : Monospace
Font size   : 9sp everywhere
Line height : 12sp
Font weight : Normal; Medium only for focused text
Letter space: 0
Max scale   : 1.0 inside the app HUD
```

Dynamic terminal output, Chinese UI, English UI, headers, and buttons all use the same token.

### Spacing

Use a 4px base grid only: `4 / 8 / 12 / 16`. Avoid one-off values such as 1px padding, 5px margins, 10px top gaps, 14px popup text, or 20px page padding.

## 4. Core components

### `TerminalScaffold`

Owns the fixed three-region layout:

```text
┌──────────────────────────────────────────────┐
│ HeaderBar                                    │
├──────────────────────────────────────────────┤
│                                              │
│ BodyViewport                                 │
│                                              │
├──────────────────────────────────────────────┤
│ ActionDock                                   │
└──────────────────────────────────────────────┘
```

No page may add another global title, global footer hint, or independent outer padding.

### `HeaderBar`

Terminal page format:

```text
CODEX / Code-Review2 / 运行                       ●
```

- No `AOE TERM` label.
- Single line, clipped with ellipsis before the status/icon if needed.
- `●` connected, `○` disconnected.
- Opaque black background plus bottom divider isolates it from terminal output.

Other pages replace the third field with the page state:

```text
CODEX / Code-Review2 / 回复                       ●
CODEX / Code-Review2 / 聆听中                     ●
最近会话 / 9                                     ●
```

### `BodyViewport`

- Opaque black and independently clipped.
- Terminal output starts below the header divider and ends above the footer divider.
- Uses one line-height and one font size.
- Terminal row wrapping preserves each real terminal row; neighboring rows are never reflowed into paragraphs.

### `ActionDock`

#### Single action

A compact left-aligned BBS command/status row inside the 24px footer:

```text
────────────────────────────────────────────────
                 [ ENTER 回复 ]
────────────────────────────────────────────────
```

Normal state uses black background, bracketed key names, and a bottom rule. Focused/pressed state inverses only the command cluster to green/black rather than filling the full viewport. It must not resemble a phone CTA.

#### Review action grid

Only dictation review uses a 2×2 action dock:

```text
┌──────────────────────┬───────────────────────┐
│ 发送                 │ 继续                  │
├──────────────────────┼───────────────────────┤
│ 重录                 │ 取消本段              │
└──────────────────────┴───────────────────────┘
```

Default focus is `继续`, not `发送`, to prevent accidental submission.

### `TerminalMenuRow`

- Height: 44px.
- Transparent normal state with a dim divider.
- Focused state: solid green / black text.
- Up/Down changes one row at a time.

### `TranscriptPanel`

Two clearly named areas using separators, not cards:

```text
发送预览 · 2 段
01 ...
02 ...
──────────────────────────────────────────────
本段识别结果
...
```

`取消本段` clears only the current candidate and never clears committed preview segments.

## 5. Page templates

### Session list

- Header: `最近会话 / count` + connection icon.
- Body: one column-label row, one `[+] New session` row, then a flat one-line table in navigation order: `TOOL / GROUP / SESSION / ST / AGE`. No cards or group-header rows.
- Footer: compact `[ENTER] 打开` or `[ENTER] 新建` command text based on focus.

### Terminal

- Header: `TOOL / TITLE / STATE` + connection icon.
- Body: terminal rows only, 70 display cells × 32 visible rows, with CJK-aware per-row wrapping.
- Footer: `↑↓ 滚动  [ENTER] 回复  [BACK] 会话`; interactive prompts switch to `↑↓ 选择  [ENTER] 确认  [BACK] 会话`.
- Back: sessions.

### Reply method

- Header: `TOOL / TITLE / 回复`.
- Body: only `语音转文字` and `键盘输入`; Back cancels, so no redundant Cancel row.
- Footer: compact `[ENTER] 确认` command text.

### Dictation listening

- Header: `TOOL / TITLE / 聆听中`.
- Body: committed send preview at top; current listening state below a divider.
- Footer: compact `[BACK] 停止本段` command text.

### Dictation review

- Header: `TOOL / TITLE / 核对第 N 段`.
- Body: committed preview + current candidate.
- Footer: 2×2 review action dock; default `继续`.

### Keyboard input

- Header: `TOOL / TITLE / 键盘输入`.
- Body: one editable terminal input area.
- Footer: compact `[ENTER] 发送` command text.

### Permission/model prompt

- Uses the same scaffold and typography. No rounded modal or typography exception.
- Prompt summary in body; options are `TerminalMenuRow`; footer is `ENTER 确认`.

### QR scanner

- Camera remains full-bleed only behind `BodyViewport`.
- Header and ActionDock use the same opaque terminal components and 9sp type.

## 6. Refactor boundaries

Production migration should create these reusable Compose modules before individual pages are moved:

```text
ui/design/TerminalTokens.kt
ui/design/TerminalTheme.kt
ui/components/TerminalScaffold.kt
ui/components/HeaderBar.kt
ui/components/ActionDock.kt
ui/components/TerminalMenuRow.kt
ui/components/TranscriptPanel.kt
ui/pages/SessionsPage.kt
ui/pages/TerminalPage.kt
ui/pages/ReplyPage.kt
ui/pages/DictationPage.kt
ui/pages/KeyboardPage.kt
ui/pages/PromptPage.kt
```

`HudScreen.kt` should become a state router only; it must not own per-page font sizes, spacers, or colors.

## 7. Acceptance checks

- [ ] `AOE TERM` appears on no page.
- [ ] Tool labels use the approved full names (`CODEX`, `CLAUDE`, `OPENCODE`) rather than `CDEX`/`CLDE` abbreviations.
- [ ] Every page has exactly one HeaderBar, one BodyViewport, and one ActionDock.
- [ ] Header/body/footer bounds never overlap on 480×640.
- [ ] All visible text resolves to the single 9sp/12sp token.
- [ ] Connection status is icon-only.
- [ ] Terminal body contains agent output only.
- [ ] Terminal footer is a compact BBS command line, never a full-width CTA.
- [ ] Focus always uses green fill + black text.
- [ ] Simplified Chinese is used only for a Simplified Chinese system locale; all other locales use English.
- [ ] Real-device screenshot and UIAutomator bounds confirm each migrated page.
