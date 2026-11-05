#!/usr/bin/env python3
"""Synthesises the game's chiptune sound pack into app/src/main/assets/sfx/*.wav.

Everything is generated from scratch (no samples), so there is nothing to license.
Run:  python tools/gen_sfx.py      (needs numpy)
"""
import os
import wave

import numpy as np

SR = 32000
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets", "sfx")
rng = np.random.default_rng(7)

NOTE = {"C": 0, "C#": 1, "D": 2, "D#": 3, "E": 4, "F": 5, "F#": 6, "G": 7, "G#": 8, "A": 9, "A#": 10, "B": 11}


def hz(n):
    """Note name such as C5 to a frequency."""
    return 440.0 * 2 ** ((NOTE[n[:-1]] + 12 * (int(n[-1]) + 1) - 69) / 12)


def osc(freq, d, kind="square", duty=0.5):
    """freq is a number or an array with one value per sample."""
    n = int(d * SR)
    f = np.full(n, float(freq)) if np.isscalar(freq) else np.asarray(freq)[:n]
    phase = np.cumsum(f) / SR
    frac = phase % 1.0
    if kind == "square":
        return np.where(frac < duty, 1.0, -1.0)
    if kind == "tri":
        return 4 * np.abs(frac - 0.5) - 1
    if kind == "saw":
        return 2 * frac - 1
    return np.sin(2 * np.pi * phase)


def env(n, a=0.002, d=0.05, s=0.6, r=0.05):
    a_n, d_n, r_n = int(a * SR), int(d * SR), int(r * SR)
    s_n = max(0, n - a_n - d_n - r_n)
    e = np.concatenate([np.linspace(0, 1, a_n, False), np.linspace(1, s, d_n, False),
                        np.full(s_n, s), np.linspace(s, 0, r_n)])
    return np.pad(e, (0, max(0, n - len(e))))[:n]


def noise(d, step=1):
    """Chunky NES-like noise; a bigger step sounds lower."""
    n = int(d * SR)
    return np.repeat(rng.uniform(-1, 1, n // step + 1), step)[:n]


def tone(freq, dur, kind="square", vol=0.5, duty=0.5, **e):
    x = osc(freq, dur, kind, duty)
    return x * env(len(x), **e) * vol


def sweep(f0, f1, dur, kind="square", vol=0.5, duty=0.5, curve=1.0, **e):
    n = int(dur * SR)
    f = f0 * (f1 / f0) ** (np.linspace(0, 1, n) ** curve)
    x = osc(f, dur, kind, duty)
    return x * env(n, **e) * vol


def seq(notes, step, kind="square", vol=0.45, duty=0.5, length=1.4, **e):
    """notes: note names or None for a rest, one per step."""
    out = np.zeros(int(step * len(notes) * SR) + int(step * length * SR) + SR // 4)
    for i, n in enumerate(notes):
        if n is None:
            continue
        x = tone(hz(n), step * length, kind, vol, duty, **e)
        p = int(i * step * SR)
        out[p:p + len(x)] += x
    return out


def mix(*parts):
    n = max(len(p) for p in parts)
    out = np.zeros(n)
    for p in parts:
        out[:len(p)] += p
    return out


def at(x, sec):
    return np.concatenate([np.zeros(int(sec * SR)), x])


def echo(x, delay=0.11, fb=0.35, n=3):
    pad = int(delay * SR * n) + 1
    out = np.concatenate([x, np.zeros(pad)])
    for i in range(1, n + 1):
        p = int(delay * SR * i)
        out[p:p + len(x)] += x * fb ** i
    return out


def noise_hit(dur, step, vol, **e):
    return noise(dur, step) * env(int(dur * SR), **e) * vol


def save(name, x, peak=0.85):
    x = np.array(x, dtype=np.float64)
    nz = np.nonzero(np.abs(x) > 1e-4)[0]
    if len(nz):
        x = x[: nz[-1] + 1]
    fade = min(len(x), 64)
    x[-fade:] *= np.linspace(1, 0, fade)  # no click at the end
    x = x / max(1e-6, np.abs(x).max()) * peak
    with wave.open(os.path.join(OUT, name + ".wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((x * 32767).astype("<i2").tobytes())


def main():
    os.makedirs(OUT, exist_ok=True)

    # ---- interface
    save("ui_tap", sweep(900, 1500, 0.045, "square", duty=0.25, a=0.001, d=0.02, s=0.4, r=0.015), 0.5)
    save("ui_back", sweep(900, 420, 0.075, "square", duty=0.25, a=0.001, d=0.03, s=0.4, r=0.02), 0.5)
    save("ui_toggle", mix(tone(hz("E6"), 0.04, "square", duty=0.25, r=0.015),
                          at(tone(hz("B6"), 0.05, "square", duty=0.25, r=0.02), 0.04)), 0.45)
    save("modal_open", echo(sweep(260, 1300, 0.16, "tri", curve=0.6, a=0.002, d=0.05, s=0.7, r=0.05), 0.07, 0.25, 2), 0.6)
    save("modal_close", sweep(1100, 240, 0.12, "tri", curve=0.7, a=0.001, d=0.04, s=0.6, r=0.04), 0.5)

    # ---- wheel: one pluck that the app re-pitches note by note
    save("letter", mix(tone(hz("C5"), 0.13, "square", 0.5, duty=0.25, a=0.001, d=0.05, s=0.3, r=0.05),
                       tone(hz("C6"), 0.08, "tri", 0.4, a=0.001, d=0.04, s=0.2, r=0.03)), 0.6)
    save("backtrack", sweep(620, 380, 0.07, "tri", a=0.001, d=0.03, s=0.4, r=0.02), 0.5)
    blips = [at(tone(hz(n), 0.05, "square", 0.4, duty=0.125, r=0.02), i * 0.045)
             for i, n in enumerate(["G5", "C6", "E5", "A5", "D6", "G6"])]
    save("shuffle", mix(*blips, noise_hit(0.3, 6, 0.12, a=0.05, d=0.1, s=0.3, r=0.1)), 0.55)

    # ---- word results
    save("word_found", echo(mix(seq(["E5", "G5", "C6"], 0.06, "square", 0.4, duty=0.25, length=1.5, r=0.04),
                                seq(["C5", "E5", "G5"], 0.06, "tri", 0.4, length=1.5, r=0.04)), 0.09, 0.3, 3), 0.75)
    save("tile_pop", sweep(500, 1200, 0.05, "square", duty=0.25, a=0.001, d=0.02, s=0.3, r=0.02), 0.4)
    save("word_bonus", echo(seq(["A5", "C#6", "E6", "A6", "C#7"], 0.05, "square", 0.35, duty=0.125, length=1.6, r=0.05), 0.08, 0.35, 3), 0.7)
    save("word_wrong", mix(sweep(240, 120, 0.2, "saw", 0.5, a=0.001, d=0.08, s=0.6, r=0.05),
                           sweep(246, 124, 0.2, "square", 0.3, a=0.001, d=0.08, s=0.5, r=0.05),
                           noise_hit(0.08, 4, 0.3, a=0.001, d=0.04, s=0.2, r=0.03)), 0.65)
    save("word_already", mix(tone(hz("A4"), 0.07, "square", duty=0.25, r=0.02),
                             at(tone(hz("A4"), 0.09, "square", duty=0.25, r=0.03), 0.11)), 0.45)
    save("hint", echo(mix(sweep(600, 2200, 0.22, "sine", 0.5, a=0.01, d=0.05, s=0.8, r=0.1),
                          seq(["E6", "B6", "E7"], 0.07, "square", 0.22, duty=0.125, r=0.05)), 0.1, 0.4, 3), 0.65)
    save("coin", mix(tone(hz("B5"), 0.06, "square", 0.45, duty=0.25, r=0.01),
                     at(tone(hz("E6"), 0.2, "square", 0.45, duty=0.25, d=0.05, s=0.5, r=0.12), 0.055)), 0.55)
    save("count_tick", tone(hz("A6"), 0.025, "square", duty=0.125, a=0.001, d=0.01, s=0.3, r=0.01), 0.3)
    save("star", echo(mix(sweep(800, 1800, 0.1, "square", duty=0.25, a=0.001, d=0.04, s=0.5, r=0.04),
                          at(tone(hz("E7"), 0.14, "tri", 0.5, r=0.1), 0.08)), 0.09, 0.35, 2), 0.65)

    # ---- round flow
    save("count_beep", tone(hz("A5"), 0.14, "square", duty=0.25, a=0.002, d=0.05, s=0.6, r=0.05), 0.6)
    save("count_go", echo(mix(tone(hz("A6"), 0.32, "square", 0.45, duty=0.25, d=0.1, s=0.7, r=0.12),
                              tone(hz("A5"), 0.32, "tri", 0.45, r=0.12)), 0.09, 0.3, 2), 0.75)
    save("clock_tick", mix(noise_hit(0.018, 2, 0.6, a=0.001, d=0.008, s=0.2, r=0.005),
                           tone(hz("E6"), 0.02, "square", 0.2, duty=0.125, r=0.008)), 0.4)
    save("clock_urgent", mix(tone(hz("E6"), 0.07, "square", 0.5, duty=0.25, r=0.02),
                             tone(hz("F6"), 0.07, "square", 0.3, duty=0.5, r=0.02)), 0.55)

    lead = seq(["C5", "E5", "G5", "C6", None, "G5", "C6", "E6", "G6"], 0.085, "square", 0.34, duty=0.25, length=1.1, d=0.05, s=0.7, r=0.04)
    bass = seq(["C4", None, "G4", None, "C4", None, "E4", "G4", "C5"], 0.085, "tri", 0.5, length=1.3, r=0.04)
    tail = at(tone(hz("C7"), 0.55, "square", 0.25, duty=0.125, d=0.1, s=0.5, r=0.3), 0.72)
    save("level_complete", echo(mix(lead, bass, tail), 0.13, 0.28, 3), 0.8)

    lead = seq(["G5", "G5", "G5", "C6", None, "E6", "D6", "C6", "G6", None, "C7"], 0.09, "square", 0.34, duty=0.25, length=1.1, d=0.05, s=0.7, r=0.04)
    bass = seq(["C4", None, "G4", None, "A4", None, "F4", None, "G4", None, "C5"], 0.09, "tri", 0.5, length=1.8, r=0.05)
    sparkle = mix(*[at(tone(hz(n), 0.1, "sine", 0.3, r=0.08), 0.8 + i * 0.05) for i, n in enumerate(["C7", "E7", "G7", "C8"])])
    save("daily_complete", echo(mix(lead, bass, sparkle), 0.13, 0.28, 3), 0.8)

    save("time_up", echo(mix(seq(["A5", "F5", "D5", "A4"], 0.15, "square", 0.4, duty=0.25, length=1.05, r=0.06),
                             seq(["D4", "D4", "A#3", "A3"], 0.15, "tri", 0.5, length=1.1, r=0.06)), 0.14, 0.25, 2), 0.75)
    save("duel_win", echo(mix(seq(["D5", "D5", "D5", "F#5", "A5", None, "D6", "A5", "D6"], 0.09, "square", 0.34, duty=0.25, length=1.1, r=0.04),
                              seq(["D4", None, "A4", None, "D4", None, "A4", None, "D5"], 0.09, "tri", 0.5, length=1.6, r=0.05)), 0.13, 0.28, 3), 0.8)
    save("duel_lose", echo(mix(sweep(520, 110, 0.9, "square", 0.3, duty=0.25, curve=1.5, a=0.01, d=0.3, s=0.7, r=0.3),
                               seq(["E4", "D#4", "D4", "C#4"], 0.2, "tri", 0.5, length=1.1, r=0.08)), 0.15, 0.25, 2), 0.7)
    rise = [at(tone(hz(n), 0.12, "square", 0.35, duty=0.25, r=0.08), i * 0.07)
            for i, n in enumerate(["C6", "D6", "E6", "G6", "A6", "C7"])]
    save("streak_up", echo(mix(*rise, noise_hit(0.5, 3, 0.1, a=0.2, d=0.1, s=0.5, r=0.2)), 0.1, 0.35, 3), 0.75)

    # ---- splash: a tile landing, then the logo sting
    thud = mix(sweep(300, 70, 0.12, "tri", 0.8, a=0.001, d=0.05, s=0.5, r=0.04),
               noise_hit(0.05, 8, 0.3, a=0.001, d=0.03, s=0.1, r=0.01))
    save("tile_land", mix(thud, tone(hz("C6"), 0.05, "square", 0.2, duty=0.125, r=0.03)), 0.7)
    logo = mix(seq(["C5", "G5", "C6", "E6", "G6", "C7"], 0.07, "square", 0.33, duty=0.25, length=2.2, d=0.06, s=0.6, r=0.1),
               seq(["C4", None, "G4", None, "C5"], 0.07, "tri", 0.5, length=4.0, r=0.2),
               at(sweep(200, 4000, 0.4, "sine", 0.15, a=0.1, d=0.1, s=0.6, r=0.15), 0.1))
    save("logo_sting", echo(logo, 0.14, 0.33, 4), 0.8)

    # ---- the cat: a two-part chirp that rises then falls, with a little vibrato
    n = int(0.3 * SR)
    t = np.linspace(0, 1, n)
    curve = 620 + 420 * np.sin(np.pi * t ** 0.7) + 16 * np.sin(2 * np.pi * 26 * t)
    save("meow", mix(osc(curve, 0.3, "tri") * env(n, 0.012, 0.08, 0.7, 0.12) * 0.7,
                     osc(curve * 2, 0.3, "square", 0.25) * env(n, 0.012, 0.06, 0.3, 0.12) * 0.12), 0.55)
    save("buy", echo(mix(seq(["E6", "G6", "B6", "E7"], 0.06, "square", 0.34, duty=0.25, length=1.3, r=0.05),
                         at(tone(hz("E5"), 0.3, "tri", 0.4, r=0.15), 0.18),
                         at(tone(hz("B7"), 0.22, "sine", 0.3, r=0.16), 0.24)), 0.1, 0.32, 3), 0.75)

    total = sum(os.path.getsize(os.path.join(OUT, f)) for f in os.listdir(OUT))
    print(len(os.listdir(OUT)), "sounds,", total // 1024, "KB")


if __name__ == "__main__":
    main()
