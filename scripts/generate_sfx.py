#!/usr/bin/env python3
"""
Synthesises ZenMode's interface sounds into app/src/main/res/raw/.

Every sound is generated, not sampled, so the set stays licence-free and can be retuned
here and regenerated. Pure standard library (no numpy):

    python3 scripts/generate_sfx.py

Sound brief: quiet, warm, short. Nothing here should be noticed on its own — it should
make a tap feel finished. Everything sits in one key (C major / A minor pentatonic) so
sounds that overlap never clash. Peaks are kept well under full scale; the app plays them
at a further reduced volume (see ZenSound.kt).
"""
import math
import os
import random
import struct
import wave

RATE = 44100
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")

random.seed(7)  # deterministic noise → byte-identical regeneration


def note(name: str) -> float:
    names = {"C": -9, "C#": -8, "D": -7, "D#": -6, "E": -5, "F": -4, "F#": -3,
             "G": -2, "G#": -1, "A": 0, "A#": 1, "B": 2}
    pitch, octave = name[:-1], int(name[-1])
    return 440.0 * 2 ** ((names[pitch] + (octave - 4) * 12) / 12)


def silence(seconds):
    return [0.0] * int(RATE * seconds)


def env(i, n, attack=0.004, decay=6.0):
    """Fast attack, exponential decay. `decay` is how many e-folds across the sound."""
    t = i / RATE
    a = min(1.0, t / attack) if attack > 0 else 1.0
    return a * math.exp(-decay * i / n)


def bell(freq, seconds, amp=0.5, decay=5.0, partials=((1, 1.0), (2.76, 0.28), (5.4, 0.08))):
    """Soft bell/marimba: a sine with a few inharmonic partials that die faster."""
    n = int(RATE * seconds)
    out = []
    for i in range(n):
        t = i / RATE
        s = 0.0
        for mult, weight in partials:
            s += weight * math.sin(2 * math.pi * freq * mult * t) * math.exp(-decay * mult * 0.35 * i / n)
        out.append(amp * s * env(i, n, attack=0.003, decay=decay))
    return out


def tick(freq, seconds, amp=0.35):
    """Woody tick: very short sine + a pinch of filtered noise for the transient."""
    n = int(RATE * seconds)
    out, lp = [], 0.0
    for i in range(n):
        t = i / RATE
        noise = random.uniform(-1, 1)
        lp += 0.35 * (noise - lp)
        transient = lp * math.exp(-i / (RATE * 0.0025))
        tone = math.sin(2 * math.pi * freq * t) * math.exp(-i / (RATE * seconds * 0.22))
        out.append(amp * (0.75 * tone + 0.45 * transient))
    return out


def whoosh(seconds, rising=True, amp=0.22):
    """Air: noise through a one-pole band that sweeps up (open) or down (close)."""
    n = int(RATE * seconds)
    out, lp, hp_prev, hp = [], 0.0, 0.0, 0.0
    for i in range(n):
        p = i / n
        sweep = p if rising else 1 - p
        cutoff = 0.03 + 0.25 * sweep
        noise = random.uniform(-1, 1)
        lp += cutoff * (noise - lp)
        hp = 0.96 * (hp + lp - hp_prev)
        hp_prev = lp
        shape = math.sin(math.pi * p) ** 1.6
        out.append(amp * hp * shape)
    return out


def glide(f0, f1, seconds, amp=0.3):
    """A sine that glides between two pitches, rounded at both ends."""
    n = int(RATE * seconds)
    out, phase = [], 0.0
    for i in range(n):
        p = i / n
        f = f0 * (f1 / f0) ** p
        phase += 2 * math.pi * f / RATE
        shape = math.sin(math.pi * min(1.0, p * 1.4)) if p < 0.7 else math.sin(math.pi * p) ** 0.5
        out.append(amp * math.sin(phase) * shape * math.exp(-2.2 * p))
    return out


def wind(seconds, amp=0.22, gusts=3):
    """Air through leaves: brown-ish noise under a band that breathes in slow gusts, with a
    thin layer of high rustle on top so it reads as a wood, not as a hiss."""
    n = int(RATE * seconds)
    out, low, band, band_prev, high = [], 0.0, 0.0, 0.0, 0.0
    for i in range(n):
        p = i / n
        # Overlapping sines make gusts that swell and fall without ever landing on a beat.
        gust = 0.55 + 0.45 * sum(math.sin(2 * math.pi * (g + 1) * 0.37 * p * gusts + g)
                                 for g in range(3)) / 3
        noise = random.uniform(-1, 1)
        low += 0.08 * (noise - low)
        band = 0.94 * (band + low - band_prev)
        band_prev = low
        high += 0.5 * (noise - high)
        rustle = (noise - high) * 0.18 * max(0.0, gust - 0.6)
        shape = math.sin(math.pi * min(1.0, p * 3.2)) if p < 0.3 else math.sin(math.pi * p) ** 0.35
        out.append(amp * (band * 2.4 + rustle) * gust * shape)
    return out


def bird(base, seconds, amp=0.18, chirps=3, gap=0.085, rise=True):
    """A call from the treeline: two or three short whistles, each a quick pitch sweep with a
    touch of vibrato. Far enough away to be quiet and a little breathy."""
    layers = []
    for c in range(chirps):
        length = seconds / (chirps + 1)
        f0 = base * (1.0 + 0.06 * c)
        f1 = f0 * (1.45 if rise else 0.68)
        n = int(RATE * length)
        samples, phase = [], 0.0
        for i in range(n):
            t = i / n
            f = f0 * (f1 / f0) ** t * (1 + 0.012 * math.sin(2 * math.pi * 22 * i / RATE))
            phase += 2 * math.pi * f / RATE
            body = math.sin(phase) + 0.18 * math.sin(2 * phase)
            shape = math.sin(math.pi * t) ** 1.3
            samples.append(amp * body * shape)
        layers.append((c * (length + gap), samples))
    return mix(*layers)


def mix(*layers):
    """Overlay (offset_seconds, samples) layers."""
    length = max(int(RATE * off) + len(s) for off, s in layers)
    out = [0.0] * length
    for off, s in layers:
        start = int(RATE * off)
        for i, v in enumerate(s):
            out[start + i] += v
    return out


def seq(*parts):
    out = []
    for p in parts:
        out += p
    return out


def fade_tail(samples, ms=12):
    n = min(len(samples), int(RATE * ms / 1000))
    for k in range(n):
        samples[-1 - k] *= k / n
    return samples


def write(name, samples, peak=0.7):
    samples = fade_tail(list(samples))
    m = max(1e-9, max(abs(v) for v in samples))
    scale = peak / m
    path = os.path.join(OUT, f"{name}.wav")
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, v * scale)) * 32767)) for v in samples))
    print(f"{name:18s} {len(samples) / RATE * 1000:6.0f} ms")


def main():
    os.makedirs(OUT, exist_ok=True)

    # Buttons and rows: a dry, woody tick. Short enough to sit under a finger.
    write("sfx_tap", tick(note("E6"), 0.045), peak=0.45)
    # Radio / segment / chip selection: a slightly rounder, lower tick.
    write("sfx_select", seq(tick(note("A5"), 0.06, amp=0.4)), peak=0.5)
    # Switches: two notes up for on, two down for off.
    write("sfx_toggle_on", mix((0, tick(note("E5"), 0.05)), (0.045, bell(note("A5"), 0.12, amp=0.35, decay=7))), peak=0.5)
    write("sfx_toggle_off", mix((0, tick(note("A5"), 0.05)), (0.045, bell(note("E5"), 0.12, amp=0.35, decay=7))), peak=0.45)
    # Sheets rise and fall on air.
    write("sfx_sheet_open", whoosh(0.2, rising=True), peak=0.28)
    write("sfx_sheet_close", whoosh(0.17, rising=False), peak=0.22)
    # Onboarding page advance: one soft marimba note.
    write("sfx_step", bell(note("G5"), 0.28, amp=0.5, decay=6, partials=((1, 1.0), (4.0, 0.12))), peak=0.5)
    # A thing got done: C–E–G rising.
    write("sfx_success", mix(
        (0.00, bell(note("C5"), 0.5, decay=5)),
        (0.07, bell(note("E5"), 0.5, decay=5)),
        (0.14, bell(note("G5"), 0.6, decay=4.5)),
    ), peak=0.6)
    # Pro unlocked: the success chord, an octave lift and a shimmer of air on top.
    shimmer = [v * 0.5 for v in whoosh(0.9, rising=True, amp=0.2)]
    write("sfx_pro_unlock", mix(
        (0.00, bell(note("C5"), 1.2, decay=4)),
        (0.08, bell(note("E5"), 1.2, decay=4)),
        (0.16, bell(note("G5"), 1.2, decay=4)),
        (0.26, bell(note("C6"), 1.3, decay=3.6)),
        (0.36, bell(note("E6"), 1.1, amp=0.3, decay=4)),
        (0.10, shimmer),
    ), peak=0.62)
    # Couldn't do that: two low, soft knocks. Never a buzzer.
    write("sfx_error", mix(
        (0.00, bell(note("A3"), 0.16, amp=0.6, decay=9, partials=((1, 1.0), (2.0, 0.2)))),
        (0.11, bell(note("E3"), 0.2, amp=0.6, decay=9, partials=((1, 1.0), (2.0, 0.2)))),
    ), peak=0.5)
    # Zen Gold: a small bright coin.
    write("sfx_coin", mix(
        (0.00, bell(note("B6"), 0.22, amp=0.4, decay=6, partials=((1, 1.0), (2.4, 0.3)))),
        (0.06, bell(note("E7"), 0.35, amp=0.45, decay=5, partials=((1, 1.0), (2.4, 0.25)))),
    ), peak=0.45)
    # Theme change: a soft pitch glide under a breath of air — down into ink, up into paper.
    write("sfx_theme_dark", mix((0, glide(note("G4"), note("C4"), 0.42, amp=0.35)), (0, whoosh(0.4, rising=False, amp=0.12))), peak=0.4)
    write("sfx_theme_light", mix((0, glide(note("C5"), note("G5"), 0.42, amp=0.35)), (0, whoosh(0.4, rising=True, amp=0.12))), peak=0.4)
    # Onboarding finale: a slow open fifth that lands the "entering ZenMode" moment.
    write("sfx_enter", mix(
        (0.00, bell(note("C4"), 1.6, amp=0.5, decay=3)),
        (0.00, bell(note("G4"), 1.6, amp=0.35, decay=3)),
        (0.30, bell(note("C5"), 1.4, amp=0.35, decay=3.2)),
        (0.55, bell(note("E5"), 1.2, amp=0.25, decay=3.4)),
        (0.05, whoosh(1.1, rising=True, amp=0.12)),
    ), peak=0.55)

    # Pro welcome: the clearing. Wind through a wood with two birds far off in it, over a low
    # open fifth so it still belongs to the same key as everything else. The only ambience in
    # the app, and it plays on exactly one screen - it sits *under* sfx_pro_unlock, which
    # lands first and does the actual confirming.
    write("sfx_forest_dawn", mix(
        (0.00, wind(3.6, amp=0.26)),
        (0.05, bell(note("C3"), 2.8, amp=0.22, decay=2.2, partials=((1, 1.0), (2.0, 0.12)))),
        (0.05, bell(note("G3"), 2.8, amp=0.14, decay=2.2, partials=((1, 1.0), (2.0, 0.10)))),
        (0.55, bird(note("A6"), 0.40, amp=0.16, chirps=3)),
        (1.45, bird(note("E6"), 0.44, amp=0.10, chirps=2, rise=False)),
        (2.40, bird(note("B6"), 0.30, amp=0.08, chirps=2)),
    ), peak=0.5)


if __name__ == "__main__":
    main()
