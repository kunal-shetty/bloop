#!/usr/bin/env python3
"""Synthesises the game's background music into app/src/main/assets/music/*.ogg.

Everything is generated from scratch (no samples), so there is nothing to license. Soft sine and
triangle voices only, because this plays for hours under the sound effects and must never grate.
Each track's echoes wrap round to its start, so it ends without a click into whatever plays next.

Run:  python tools/gen_music.py      (needs numpy and soundfile)
"""
import os

import numpy as np
import soundfile

SR = 22050
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets", "music")
NOTE = {"C": 0, "C#": 1, "D": 2, "D#": 3, "E": 4, "F": 5, "F#": 6, "G": 7, "G#": 8, "A": 9, "A#": 10, "B": 11}

# A chord is a list of notes. Melodies walk up and down a pentatonic scale, which can't clash badly.
TRACKS = {
    "dawn": dict(bpm=72, seed=11, scale=["C5", "D5", "E5", "G5", "A5", "C6"], chords=[
        ["C3", "E3", "G3", "C4"], ["A2", "E3", "A3", "C4"], ["F2", "C3", "F3", "A3"], ["G2", "D3", "G3", "B3"]]),
    "drift": dict(bpm=64, seed=23, scale=["A4", "C5", "D5", "E5", "G5", "A5"], chords=[
        ["A2", "E3", "A3", "C4"], ["F2", "C3", "F3", "A3"], ["C3", "E3", "G3", "C4"], ["G2", "D3", "G3", "B3"]]),
    "lantern": dict(bpm=78, seed=37, scale=["F4", "G4", "A4", "C5", "D5", "F5"], chords=[
        ["F2", "C3", "F3", "A3"], ["D3", "F3", "A3", "D4"], ["A#2", "F3", "A#3", "D4"], ["C3", "E3", "G3", "C4"]]),
}


def hz(note):
    return 440.0 * 2 ** ((NOTE[note[:-1]] + 12 * (int(note[-1]) + 1) - 69) / 12)


def place(track, start, sound):
    """Adds a sound into the track at a time in seconds. Anything past the end wraps to the start."""
    at = int(start * SR) % len(track)
    idx = (at + np.arange(len(sound))) % len(track)
    np.add.at(track, idx, sound)


def wave(freq, n, kind):
    phase = freq * np.arange(n) / SR
    if kind == "sine":
        return np.sin(2 * np.pi * phase)
    return 4 * np.abs(phase % 1.0 - 0.5) - 1


def pluck(freq, dur, vol):
    """A music-box note: a quick soft attack, then a long fade."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    attack = np.minimum(1.0, t / 0.012)
    body = wave(freq, n, "sine") + 0.25 * wave(freq * 2, n, "sine") * np.exp(-t * 7)
    return body * attack * np.exp(-t * 3.2) * vol


def pad(freq, dur, vol):
    """A held note that swells in and out, with a slow drift so it never sits dead still."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    swell = np.minimum(1.0, t / 0.7) * np.minimum(1.0, (dur - t) / 0.9)
    return wave(freq, n, "tri") * np.clip(swell, 0, 1) * (1 + 0.08 * np.sin(2 * np.pi * 0.23 * t)) * vol


def bass(freq, dur, vol):
    n = int(dur * SR)
    t = np.arange(n) / SR
    return wave(freq, n, "tri") * np.minimum(1.0, t / 0.03) * np.exp(-t * 1.1) * vol


def melody_phrase(rng, scale, beats):
    """A stepwise walk of (beat, degree) pairs. Rests give it room to breathe."""
    notes, beat, degree = [], 0.0, rng.integers(1, len(scale) - 1)
    while beat < beats:
        if rng.random() > 0.3:
            notes.append((beat, int(degree)))
        beat += rng.choice([1.0, 1.0, 2.0, 0.5])
        degree = np.clip(degree + rng.choice([-2, -1, -1, 1, 1, 2]), 0, len(scale) - 1)
    return notes


def low_pass(x, cutoff):
    """One pass, then wraps the filter's start state round so the loop point stays clean."""
    a = 1 - np.exp(-2 * np.pi * cutoff / SR)
    out = np.empty_like(x)
    y = 0.0
    for _ in range(2):
        for i, v in enumerate(x):
            y += a * (v - y)
            out[i] = y
    return out


def echo(x, delay, feedback, repeats):
    out = x.copy()
    for r in range(1, repeats + 1):
        out += np.roll(x, int(r * delay * SR)) * feedback ** r
    return out


def render(bpm, seed, scale, chords):
    rng = np.random.default_rng(seed)
    beat = 60.0 / bpm
    bar = beat * 4
    bars_per_section = len(chords) * 2  # each section runs the chords twice
    sections = "AABA"
    track = np.zeros(int(bar * bars_per_section * len(sections) * SR))
    lead = np.zeros_like(track)

    phrases = {name: melody_phrase(rng, scale, bars_per_section * 4) for name in "AB"}
    arps = {name: rng.permutation([0, 1, 2, 3, 2, 1, 3, 2]) for name in "AB"}

    for s, name in enumerate(sections):
        section_start = s * bars_per_section * bar
        for b in range(bars_per_section):
            chord = chords[b % len(chords)]
            bar_start = section_start + b * bar
            for note in chord[1:]:
                place(track, bar_start, pad(hz(note), bar + 1.0, 0.055))
            place(track, bar_start, bass(hz(chord[0]), bar, 0.17))
            for i, step in enumerate(arps[name]):
                place(track, bar_start + i * beat / 2, pluck(hz(chord[step]) * 2, 1.4, 0.05))
        for at, degree in phrases[name]:
            place(lead, section_start + at * beat, pluck(hz(scale[degree]), 2.4, 0.2))

    mixed = track + echo(lead, beat * 0.75, 0.32, 3)
    mixed = low_pass(mixed, 3600)
    return mixed / np.max(np.abs(mixed)) * 0.7


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, spec in TRACKS.items():
        audio = render(**spec)
        path = os.path.join(OUT, name + ".ogg")
        # Small writes: libsndfile's Vorbis encoder can crash when handed a whole track at once.
        with soundfile.SoundFile(path, "w", SR, 1, format="OGG", subtype="VORBIS") as f:
            for i in range(0, len(audio), 4096):
                f.write(audio[i:i + 4096])
        print(f"{name}: {len(audio) / SR:.0f}s, {os.path.getsize(path) // 1024} KB, "
              f"edge jump {abs(audio[0] - audio[-1]):.4f}")


if __name__ == "__main__":
    main()
