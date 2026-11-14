#!/usr/bin/env python3
"""Synthesizes the sieve and drive sounds (ours, no samples from anywhere) and writes sounds.json.

    python scripts/make_sounds.py

Three variations per event, mono 44.1 kHz, converted to Ogg Vorbis with ffmpeg. Fixed random seeds: the output is repeatable.
Events: sieve.load, sieve.shake.coarse / fine / soil, sieve.reveal, sieve.finish, sieve.mesh, drive.crank, drive.tick.
"""
import json
import math
import random
import struct
import subprocess
import tempfile
import wave
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "common" / "src" / "main" / "resources" / "assets" / "primamateria"
RATE = 44100


def env(n, attack, decay, power=2.0):
    """Amplitude envelope: linear attack then a power decay over n samples."""
    a = max(1, int(attack * RATE))
    out = []
    for i in range(n):
        if i < a:
            out.append(i / a)
        else:
            out.append((1.0 - (i - a) / max(1, n - a)) ** power)
    return out


def noise(n, rng):
    return [rng.uniform(-1, 1) for _ in range(n)]


def lowpass(x, k):
    y, s = [], 0.0
    for v in x:
        s += k * (v - s)
        y.append(s)
    return y


def highpass(x, k):
    lp = lowpass(x, k)
    return [a - b for a, b in zip(x, lp)]


def sine(n, f0, f1=None):
    f1 = f0 if f1 is None else f1
    out, ph = [], 0.0
    for i in range(n):
        f = f0 + (f1 - f0) * i / n
        ph += 2 * math.pi * f / RATE
        out.append(math.sin(ph))
    return out


def mix(*parts):
    n = max(len(p) for p in parts)
    return [sum(p[i] if i < len(p) else 0.0 for p in parts) for i in range(n)]


def scale(x, g):
    return [v * g for v in x]


def shaped(x, e):
    return [a * b for a, b in zip(x, e)]


def delay(x, seconds):
    return [0.0] * int(seconds * RATE) + x


def normalize(x, peak=0.8):
    m = max(abs(v) for v in x) or 1.0
    return [v * peak / m for v in x]


def thud(rng, i):
    n = int(0.22 * RATE)
    body = shaped(sine(n, 95 + 8 * i, 48), env(n, 0.002, 0.18, 2.5))
    dust = shaped(lowpass(noise(n, rng), 0.08), env(n, 0.001, 0.12, 3))
    return mix(body, scale(dust, 0.8))


def coarse(rng, i):
    out = [0.0]
    for k in range(7 + i):
        t = k * 0.032 + rng.uniform(0, 0.012)
        n = int(rng.uniform(0.012, 0.03) * RATE)
        click = shaped(highpass(noise(n, rng), 0.3), env(n, 0.0005, 0.02, 2))
        out = mix(out, delay(scale(click, rng.uniform(0.4, 1.0)), t))
    return out


def fine(rng, i):
    n = int(0.3 * RATE)
    return shaped(highpass(noise(n, rng), 0.5), env(n, 0.04, 0.26, 1.6))


def soil(rng, i):
    n = int(0.26 * RATE)
    return shaped(lowpass(noise(n, rng), 0.05 + 0.01 * i), env(n, 0.03, 0.22, 1.4))


def reveal(rng, i):
    n = int(0.09 * RATE)
    f = 2300 + 150 * i
    ping = shaped(sine(n, f), env(n, 0.0005, 0.08, 3))
    tick = shaped(highpass(noise(n, rng), 0.4), env(n, 0.0003, 0.006, 2))
    return mix(ping, scale(tick, 0.6))


def finish(rng, i):
    n = int(0.16 * RATE)
    knock = mix(shaped(sine(n, 190 + 10 * i, 150), env(n, 0.001, 0.14, 2.5)), shaped(sine(n, 360, 300), env(n, 0.001, 0.1, 3)))
    m = int(0.5 * RATE)
    trail = shaped(highpass(noise(m, rng), 0.45), env(m, 0.1, 0.4, 1.2))
    return mix(knock, delay(scale(trail, 0.35), 0.05))


def pluck(rng, i):
    freq = 392 + 40 * i
    period = int(RATE / freq)
    buf = [rng.uniform(-1, 1) for _ in range(period)]
    out = []
    for k in range(int(0.55 * RATE)):
        j = k % period
        nxt = (j + 1) % period
        avg = 0.5 * (buf[j] + buf[nxt]) * 0.996
        buf[j] = avg
        out.append(avg)
    return shaped(out, env(len(out), 0.001, 0.55, 1.2))


def crank(rng, i):
    out = [0.0]
    for k in range(4):
        n = int(0.025 * RATE)
        c = mix(shaped(sine(n, 1400 - 80 * k), env(n, 0.0004, 0.022, 3)), shaped(highpass(noise(n, rng), 0.4), env(n, 0.0003, 0.012, 2)))
        out = mix(out, delay(c, k * (0.07 + 0.004 * i)))
    return out


def clack(rng, i):
    n = int(0.06 * RATE)
    return mix(shaped(sine(n, 820 + 40 * i, 520), env(n, 0.0004, 0.05, 3)), shaped(highpass(noise(n, rng), 0.35), env(n, 0.0003, 0.01, 2)))


def strike(rng, i):
    n = int(0.2 * RATE)
    thud = shaped(sine(n, 140 + 12 * i, 70), env(n, 0.001, 0.16, 2.6))
    clank = shaped(sine(int(0.09 * RATE), 1650 + 90 * i, 1200), env(int(0.09 * RATE), 0.0004, 0.08, 3))
    grit = shaped(highpass(noise(int(0.06 * RATE), rng), 0.4), env(int(0.06 * RATE), 0.0003, 0.05, 2))
    return mix(thud, scale(clank, 0.6), scale(grit, 0.5))


def pour(rng, i):
    n = int(0.55 * RATE)
    stream = shaped(lowpass(highpass(noise(n, rng), 0.05), 0.35), env(n, 0.06, 0.5, 0.9))
    blips = [0.0]
    for k in range(5 + i):
        m = int(0.03 * RATE)
        blips = mix(blips, delay(scale(shaped(sine(m, 600 + rng.uniform(0, 500), 300), env(m, 0.001, 0.025, 2)), 0.5), rng.uniform(0.05, 0.45)))
    return mix(stream, blips)


def cool(rng, i):
    n = int(0.6 * RATE)
    hiss = shaped(highpass(noise(n, rng), 0.55), env(n, 0.02, 0.58, 1.5))
    m = int(0.05 * RATE)
    tick = shaped(sine(m, 1900 + 100 * i, 1500), env(m, 0.0004, 0.045, 3))
    return mix(hiss, scale(delay(tick, 0.42), 0.7))


def finish_barrel(rng, i):
    out = [0.0]
    for k, f in enumerate((660, 880, 1100)):
        m = int(0.22 * RATE)
        out = mix(out, delay(scale(shaped(sine(m, f * (1 + 0.03 * i)), env(m, 0.002, 0.2, 2.2)), 0.7), 0.07 * k))
    pop = shaped(lowpass(noise(int(0.05 * RATE), rng), 0.3), env(int(0.05 * RATE), 0.0005, 0.04, 2))
    return mix(out, scale(pop, 0.5))


EVENTS = {
    "sieve.load": ("sieve/load", thud), "sieve.shake.coarse": ("sieve/shake_coarse", coarse), "sieve.shake.fine": ("sieve/shake_fine", fine),
    "sieve.shake.soil": ("sieve/shake_soil", soil), "sieve.reveal": ("sieve/reveal", reveal), "sieve.finish": ("sieve/finish", finish),
    "sieve.mesh": ("sieve/mesh", pluck), "drive.crank": ("drive/crank", crank), "drive.tick": ("drive/tick", clack),
    "auto_hammer.strike": ("auto_hammer/strike", strike), "crucible.pour": ("crucible/pour", pour),
    "casting_tray.cool": ("casting_tray/cool", cool), "barrel.finish": ("barrel/finish", finish_barrel),
}


def write_ogg(samples, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / "s.wav"
        with wave.open(str(wav), "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(RATE)
            w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, v)) * 32767)) for v in samples))
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(wav), "-c:a", "libvorbis", "-q:a", "4", str(path)], check=True)


def main():
    sounds = {}
    for event, (base, fn) in EVENTS.items():
        names = []
        for i in range(3):
            rng = random.Random(hash(event) % 1000 * 0 + sum(map(ord, event)) * 31 + i)
            write_ogg(normalize(fn(rng, i)), ASSETS / "sounds" / f"{base}_{i + 1}.ogg")
            names.append(f"primamateria:{base}_{i + 1}")
        sounds[event] = {"sounds": names}
    (ASSETS / "sounds.json").write_text(json.dumps(sounds, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {len(sounds) * 3} sounds for {len(sounds)} events")


if __name__ == "__main__":
    main()
