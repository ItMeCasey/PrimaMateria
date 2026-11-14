"""Runs the recipe viewer tour (IntegrationTour) on a real NeoForge client, through the studio's isolated launcher.

Usage: python scripts/integration_tour.py <layer> <viewer>     e.g. neoforge-26.2 bei | jei | rei

Starts `gradlew runClient -PdevTools -Pviewers=<mods>` on the hidden MCTest desktop (launch-isolated.ps1), so no window, focus
change or cursor move reaches the real desktop. The run folder's options.txt gets the toast and tutorial switches first. The tour opens the viewer's recipe screen for a few
items through its own code, saves a frame each and quits the client itself.
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
TIMEOUT = 900
OPTIONS = {
    "tutorialStep": "none", "onboardAccessibility": "false", "skipMultiplayerWarning": "true", "joinedFirstServer": "true",
    "pauseOnLostFocus": "false", "fullscreen": "false", "guiScale": "3", "lang": "en_us",
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


VIEWERS = {"bei": "bei", "jei": "jei,mezz", "rei": "rei,architectury,cloth", "smwti": "smwti", "jade": "jade"}


def tour(layer, viewer):
    folder = ROOT / layer
    run = folder / "run"
    write_options(run)
    shots = run / "screenshots"
    if shots.exists():
        shutil.rmtree(shots)
    latest = run / "logs" / "latest.log"
    if latest.exists():
        latest.unlink()
    command = (r"set PRIMAMATERIA_TOUR=" + viewer + r"&& set PRIMAMATERIA_DEMO=1&& .\gradlew.bat runClient --no-daemon -q -PdevTools "
               r"-PwindowSize=1904x1041 -Pviewers=" + VIEWERS[viewer])
    start = time.time()
    subprocess.run(["powershell", "-NoProfile", "-File", LAUNCH, "-Run", "-WorkDir", str(folder), "-Command", command], check=True)
    done = False
    try:
        while time.time() - start < TIMEOUT:
            time.sleep(3)
            if "BUILD FAILED" in launcher_log().split("started on MCTest")[-1] and not latest.exists():
                break
            if latest.exists() and "PRIMAMATERIA-TOUR DONE" in latest.read_text(encoding="utf-8", errors="replace"):
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
        shutil.copy2(shots / n, frames / n.replace("primamateria_", f"primamateria_{layer}_", 1))
    errors = [l for l in text.splitlines() if re.search(r"PRIMAMATERIA-TOUR (failed|could not)|ModLoadingException|Crash report saved|primamateria.*(Exception|Error)", l, re.I)]
    status = "TOUR PASS" if done and not errors else "TOUR FAIL"
    detail = "" if status == "TOUR PASS" else f": done={done} first_error={errors[:1]}"
    return f"{status} {layer} {viewer} ({len(names)} frames, {int(time.time() - start)}s){detail}"


if __name__ == "__main__":
    result = tour(sys.argv[1], sys.argv[2])
    print(result, flush=True)
    sys.exit(1 if result.startswith("TOUR FAIL") else 0)
