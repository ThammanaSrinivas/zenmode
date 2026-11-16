# ADR-0004: The Zen Island, drawn by the app over other apps

- **Status**: Proposed
- **Date**: 2026-11-16
- **Deciders**: srinivas, kamal

## Context

On iPhone, ZenMode OS lives in the Dynamic Island through a Live Activity: a won week, a day that decides the week, a new week. People asked for the same on Android, along with what makes an island useful all day: a Quiet session counting down, music, a call, a timer, a message passing through.

Android has no Dynamic Island and no API for one. Android 16's Live Update puts a chip in the status bar, but only for something the person started, and the system draws it. Showing anything over other apps takes one of two windows:

| Window | What it takes | Where it can sit |
|---|---|---|
| `TYPE_ACCESSIBILITY_OVERLAY` | The accessibility service, which the Distraction Blocker already uses | Above everything, the status bar and lock screen included, so round the camera, and a tap there reaches it |
| `TYPE_APPLICATION_OVERLAY` | "Display over other apps" | Above apps but below the status bar, so docked just under it, where a tap still reaches it |

## Decision

One island (`island/`), drawn with Compose in a window ZenMode OS adds itself, carried by whichever of the two the phone allows: the accessibility overlay when the service is on, otherwise the application overlay, otherwise none, and Settings says what it needs. `IslandHost` decides; `IslandWindow` owns the window; `IslandSurface` draws, and is what the goldens draw (`IslandScreenshotTest`).

- **Where.** Round a centred punch hole the pill wraps the camera and both sides match; with a corner camera, a wide notch or none, it sits centred in the status bar (docked under it on the overlay), and the open card keeps its top row clear of a corner camera (`IslandGeometry`, pure and tested).
- **The window is only as big as what is drawn.** It is centred on the island and grows downwards, so it is resized only when the island grows (first) or has finished shrinking (last), never every frame, and touches beside the island reach the app under it.
- **What it shows.** Live things stay until they end, ranked: a call, a Quiet session, another app's timer or Live Update, music, the last hour of the promise, a day that counts for the week. The next one waits beside the pill as a bubble. Passing things take it for a few seconds: a message, the charger, a Quiet session ending, the promise reached. The rules (`NoteRules`, `MomentPlan`, `IslandStage`) are pure and unit-tested. ZenMode's own moments come from the snapshot the widgets read, so the island and the widgets never disagree; the week's moment follows iPhone's planner (one a day, never at night, gone once swiped away).
- **Chosen, not noisy.** A message only widens the pill when Android already shows it as a banner, and opens the card only when it would otherwise arrive unseen. During a Quiet session, and for apps the person quieted, messages are held back and counted, never shown. Nothing from the app on screen, nothing silent, nothing Do Not Disturb holds back.
- **On by default only where we are sure.** `IslandDevice.tier` starts it on for phones Android grades for performance, with 6 GB, or with 4 GB and eight cores on Android 12+; on lower-memory or older phones it starts off, and runs without glow or moving bars if turned on.
- **It steps aside only when the phone is sideways.** Upright it stays over full-screen apps and the pulled-down shade, as iPhone's island does. On Android 17 an overlay gets no reliable word of either: the accessibility window list keeps a full-screen system window in it whether the shade is down or not, and a window above the status bar is always told the bar is hidden.

## Privacy

What the island shows is read on the phone, held only while it shows it, and never stored or sent. Analytics carry kinds only (`island_opened {state, source}`, `island_action {state, action}`), never a word of a notification or a song. On a locked phone with a secure lock, a message shows only what its app allows on the lock screen.

## Alternatives considered

- **Only the system's Live Update.** Rejected as the whole answer: only for things the person started, a chip we don't draw, Android 16+ only. It stays the right place for a Quiet session's countdown outside the launcher.
- **Drawn on Home only.** Rejected: the island would vanish the moment the person leaves the launcher, which is when it is useful.
- **An island in the status bar with the system bar hidden.** Rejected: an app cannot move or remove individual status icons.

## Consequences

- Play's accessibility declaration has to name the island as a use of the service, with its own prominent disclosure, before release. "Display over other apps" is a separate opt-in for people without the service.
- Android 17's Advanced Protection limits accessibility services to verified accessibility tools; there the island falls back to the overlay, if the person allows it.
- With the overlay, Android shows its own "displaying over other apps" notification; that is the system's and can be turned off in Android settings.
- Timers that draw their own countdown in a custom layout (some clock apps) carry no time the island can read, so they don't appear; apps using the system chronometer or Live Updates do.
- Debug builds drive every path from adb (`IslandDemoReceiver`): a real Quiet session, a real media session, a won week.
