# Design System

## 1. Color Primitives (The "Source" Palette)
These are raw values. They should be defined in your `colors.xml` or as top-level constants in `Color.kt`.

| Name | Hex Value | Visual Reference |
|---|---|---|
| Color-white | `#FFFFFF` | Primary Light |
| Color-black | `#000000` | Primary Dark |
| color-grey-100 | `#F5F5F5` | Light Surface |
| color-grey-200 | `#E5E5E5` | Light Border |
| color-grey-400 | `#A3A3A3` | Dark Text (Sec) |
| color-grey-600 | `#525252` | Light Text (Sec) |
| color-grey-800 | `#1A1A1A` | Dark Surface |
| Color-zen-base | `#00C700` | Brand Primary |
| Color-zen-glow | `#24FF24` | Brand Accent |
| color-zen-dark | `#007700` | Brand Action |

## 2. Semantic Tokens (Theme Logic)
Use these names in your Jetpack Compose `Theme.kt`. The app will automatically swap based on the system theme.

| Name | Light Mode Value | Dark Mode Value |
|---|---|---|
| bg-primary | Color-white | color-black |
| bg-secondary | color-grey-100 | color-grey-800 |
| surface-elevated | Color-white | color-grey-600 |
| border-subtle | color-grey-200 | color-grey-800 |
| border-focus | Color-zen-base | Color-zen-glow |
| text-primary | color-black | Color-white |
| text-secondary | color-grey-600 | color-grey-400 |
| text-brand | Color-zen-base | Color-zen-glow |
| action-primary | color-zen-dark | Color-zen-base |
| action-primary-text | Color-white | color-black |
| action-hover&pressed | Color-zen-base | Color-zen-glow |

## 3. Typography Rules
The font pairing strategy for ZenMode v2 is designed for high readability and a technical, "clean" aesthetic.
Refer only `Type.kt` for accessing fonts

### Primary Font: Cabinet Grotesque
* **Usage:** Used for all prose, headings, labels, and UI instructions.
* **Style:** High-contrast Grotesque for a premium editorial feel.

### Secondary Font: Reddit Mono
* **Usage:** Used for all numerical data (e.g., Screen time counters, `0/5` checklist numbers, timestamps).
* **Rationale:** Monospaced numbers prevent "layout shift" when digits change (like a timer ticking) and provide an "engineering-grade" precision look.

## 4. Layout & Rhythm
* **Margins:** Global screen margin is 20dp.
* **Gutter:** Space between list items or cards is 20dp.

### Corner Radius
* **Standard Cards:** 16dp
* **Buttons:** 8dp
* **Inputs:** 4dp
* **Refer the particular screen for details**