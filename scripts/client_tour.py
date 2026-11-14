"""Runs the dev screenshot tour on a real game client for one layer, through the studio's isolated launcher.

Usage: python scripts/client_tour.py <layer>     e.g. neoforge-26.2 or forge-26.3

Starts `gradlew runClient -PdevTools` on the hidden MCTest desktop (launch-isolated.ps1), so no window, focus change or cursor
move reaches the real desktop. The run folder's options.txt gets the toast and tutorial switches first. The tour (ShotTour) builds
sieves, plays made-up sifts and takes the frames, then measures frame time with 20 sieves and quits the client itself.
Frames land in <layer>/run/screenshots and are copied to docs/frames/ with the layer in the name.
Check `who-owns-minecraft.ps1` first: one client at a time.
"""
import os
import re
import shutil
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LAUNCH = r"C:\Users\casey\.claude\tools\launch-isolated.ps1"
BASH = r"C:\Program Files\Git\usr\bin\bash.exe"
TIMEOUT = 900
OPTIONS = {
    "tutorialStep": "none", "onboardAccessibility": "false", "skipMultiplayerWarning": "true", "joinedFirstServer": "true",
    "pauseOnLostFocus": "false", "enableVsync": "false", "maxFps": "260", "fullscreen": "false", "guiScale": "3", "lang": "en_us",
}


def write_options(run):
    run.mkdir(exist_ok=True)
    path = run / "options.txt"
    lines = path.read_text(encoding="utf-8").splitlines() if path.exists() else []
    out, seen = [], set()
    for line in lines:
        key = line.split(":", 1)[0]
        if key in OPTIONS:
            out.append(f"{key}:{OPTIONS[key]}")
            seen.add(key)
        else:
            out.append(line)
    out += [f"{k}:{v}" for k, v in OPTIONS.items() if k not in seen]
    path.write_text("\n".join(out) + "\n", encoding="utf-8")


def launcher_log():
    path = Path.home() / "AppData" / "Local" / "launch-isolated" / "MCTest.log"
    return path.read_text(encoding="utf-8", errors="replace") if path.exists() else ""


def tour(layer):
    folder = ROOT / layer
    run = folder / "run"
    write_options(run)
    shots = run / "screenshots"
    if shots.exists():
        shutil.rmtree(shots)
    latest = run / "logs" / "latest.log"
    if latest.exists():
        latest.unlink()
    theme = ''
    # The client holds one of the global Gradle slots (at most 3 builds on this PC) for as long as it runs.
    mode = os.environ.get("PRIMA_TOUR", "1")  # "fluids" runs the fluid tour (FluidTour) instead of the sieve tour
    command = (r'set PRIMAMATERIA_SHOTS=' + mode + r'&& "' + BASH + r'" /c/Users/casey/.claude/tools/gradle-slot.sh ./gradlew runClient --no-daemon -q -PdevTools -PwindowSize=1904x1041')
    start = time.time()
    subprocess.run(["powershell", "-NoProfile", "-File", LAUNCH, "-Run", "-WorkDir", str(folder), "-Command", command], check=True)
    done = False
    try:
        while time.time() - start < TIMEOUT:
            time.sleep(3)
            if "BUILD FAILED" in launcher_log().split("started on MCTest")[-1] and not latest.exists():
                break
            if latest.exists() and "PRIMAMATERIA-SHOTS DONE" in latest.read_text(encoding="utf-8", errors="replace"):
                done = True
                break
    finally:
        time.sleep(4)
        subprocess.run(["powershell", "-NoProfile", "-File", LAUNCH, "-Kill", "-WorkDir", str(folder)], capture_output=True)
    text = latest.read_text(encoding="utf-8", errors="replace") if latest.exists() else ""
    frames = ROOT / "docs" / "frames"
    frames.mkdir(parents=True, exist_ok=True)
    names = sorted(p.name for p in shots.glob("*.png")) if shots.exists() else []
    for n in names:
        if mode == "fluids":
            shutil.copy2(shots / n, frames / n.replace("primamateria_fluids_", f"fluid_{layer}_", 1))
        else:
            shutil.copy2(shots / n, frames / n.replace("primamateria_", f"primamateria_{layer}{'-' + theme if theme else ''}_", 1))
    errors = [l for l in text.splitlines() if re.search(r"PRIMAMATERIA-SHOTS failed|ModLoadingException|Crash report saved|primamateria.*(Exception|Error)", l, re.I)]
    status = "TOUR PASS" if done and not errors else "TOUR FAIL"
    detail = "" if status == "TOUR PASS" else f": done={done} first_error={errors[:1]}"
    return f"{status} {layer} ({len(names)} frames, {int(time.time() - start)}s){detail}"


if __name__ == "__main__":
    bad = 0
    for layer in sys.argv[1:]:
        result = tour(layer)
        print(result, flush=True)
        bad += result.startswith("TOUR FAIL")
    sys.exit(1 if bad else 0)
