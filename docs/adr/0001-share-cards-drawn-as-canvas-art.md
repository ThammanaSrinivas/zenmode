# ADR-0001: Share cards drawn as Canvas art at fixed export sizes

- **Status**: Proposed
- **Date**: 2026-10-08
- **Deciders**: srinivas, kamal

## Context

Every share point (Home's streak flame and gold row, the Zen Score page, the weekly recap and
Settings → Weekly reports, the Zen Circle leaderboard) used one fixed Compose template and
exported whatever the on-screen card happened to look like, captured from a `GraphicsLayer`:

- the card looked the same whether it celebrated day 3 or day 300;
- its size followed the phone (`rdp`/`rsp` scale with screen width), so exports differed by
  device, and a capture taken mid-animation saved a half-drawn card;
- the weekly report could only leave as text or a PDF, never as something you'd post.

We wanted each card to reflect what the user actually reached (a 30-day streak is a full moon,
365 days an orbit; a 9.5 Zen Score is still water, a 2.5 a storm), the weekly report to play as
a short animated story, and all of it shareable as a crisp image or a clip with sound.

## Decision

Each card is a `ShareArt` (`app/.../share/`): a picture as a pure function of time, drawn with
plain `android.graphics` calls onto a fixed social canvas — 1080×1350 (4:5 post) or 1080×1920
(9:16 story). One drawing serves three sinks: the in-app preview (Compose `Canvas` scaled to fit),
the PNG (software bitmap at full size, drawn on a fresh instance off the main thread) and the
MP4 (`ShareClip`: frames through EGL into `MediaCodec`, cues mixed into an AAC soundtrack,
`MediaMuxer`). Tiers and copy live in plain data classes (`StreakShare`, `ScoreShare`,
`GoldShare`, `WeeklyShare`) so the words are unit-tested apart from the pictures.

## Alternatives considered

- **Keep Compose cards + `GraphicsLayer` capture** — export size and look still depend on the
  phone, and a clip would need the composition driven frame by frame offscreen.
- **Lottie / pre-rendered video templates** — a new dependency, assets per tier, and numbers,
  dates and names would have to be patched into baked art.
- **Server-side rendering** — sends personal usage off the device; ZenMode keeps it on the phone.
- **Animated GIF/WebP export** — GIF bands every gradient; Android has no animated-WebP encoder.

## Consequences

- Same card, same pixels, on every phone; exports are always the settled frame; a new tier is a
  new scene class, not a new layout.
- Text is laid out by hand (`StaticLayout`, `Paint`) rather than by Compose; `PosterChrome` keeps
  that in one place.
- Clip export relies on the device's H.264 and AAC encoders. If a phone can't make one, the
  studio falls back to sharing the image. The encoder is covered by an instrumented test
  (`ShareClipTest`) because MediaCodec and EGL don't exist on the JVM.
- Goldens (`ShareCardsScreenshotTest`) pin every tier; `ShareCopyTest` pins every line of copy.
