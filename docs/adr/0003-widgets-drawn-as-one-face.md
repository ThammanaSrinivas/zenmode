# ADR-0003: Widgets drawn as one face

- **Status**: Proposed
- **Date**: 2026-11-16
- **Deciders**: srinivas, kamal

## Context

ZenMode OS ships seven home-screen widgets for people who keep another launcher: Zeno, This week, Screen time, Zen Score, Quiet, Zen Gold and Zen Circle. Each resizes from one row to four, and each changes with the day and the week. They sit in albeit's light, as the iPhone widgets do: the time of day, a wash for today's mood, a glow in the moment's hue behind Zeno, soft stars for the weekly streak, gold when the week is won, night during a Quiet session.

The brand's type is Departure Mono for numbers, Clash Display for headlines and Geist for reading. Another launcher inflates a widget's views in a restricted context where bundled fonts don't resolve, whether the layout comes from Glance or from classic `RemoteViews` XML. Glance has no Canvas either, and the light, Zeno and the bloom are drawings.

## Decision

Each widget is one **face**: a `Face` (`widget/faces/`) that draws the whole widget into one bitmap with `android.graphics`, in our own process, where the fonts load. It does this at exactly the size the launcher reports (`SizeMode.Exact`) and at the device's density. Glance only frames it (`widget/glance/ZenGlanceWidget.kt`): the image with a spoken description, tap targets laid over the parts that act (Zeno, Start, End, Invest, Invite), and for a Quiet session a system `Chronometer` in an `AndroidRemoteViews`. That countdown is the one live number, so it ticks inside the host with our process asleep.

Every widget reads one snapshot (`WidgetData`) built by one loader. Every rule that decides a widget's look (the surface, today's mood, the circle's ranking) lives there, pure and unit-tested. Live widgets recompose on a shared tick, so a refresh also reaches widgets whose Glance session is still open.

## Alternatives considered

- **Glance layouts with Glance `Text`.** Rejected: the system font in place of the brand's, and no way to draw the light, Zeno or the bloom.
- **Bitmaps for numbers only, Glance layout around them.** Rejected: positions then depend on how each launcher measures system text, so brand pieces drift away from the sentences they belong to, and nothing can sit in the light precisely.
- **Remote Compose.** Rejected for now: alpha, Android 16+ hosts only, and it names fonts rather than bundling them.

## Consequences

- One drawing per widget, unit-testable as Paparazzi goldens (`WidgetFacesScreenshotTest`), and the picker previews are drawn by the same code (`WidgetPreviewImagesTest`), so they can't drift.
- Text in a face does not follow the system font size; TalkBack reads the face's description instead. Faces never wrap past their lines and never clip: they shrink a size step and then ellipsise.
- Bitmap memory: a face is held to a third of Android's per-update budget (6 × screen width × height bytes) and drawn at a lower scale only if it must be (`WidgetRenderer`).
- A system theme switch reaches a widget at its next refresh (15 minutes at most), or at once while the app is running.
