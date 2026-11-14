#!/usr/bin/env python3
"""Every sound event in Ids.SOUNDS and every particle in Ids.PARTICLES must have a trigger in the code, every sound event must
have an entry in sounds.json and every file named there must exist, and sounds.json must not list an event that is not registered.

    python scripts/check_sound_triggers.py
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
IDS = (ROOT / "common/src/main/java/com/itmecasey/primamateria/Ids.java").read_text(encoding="utf-8")
ASSETS = ROOT / "common/src/main/resources/assets/primamateria"


def ids(name):
    m = re.search(name + r" = List\.of\((.*?)\);", IDS, re.S)
    return re.findall(r'"([^"]+)"', m.group(1))


sources = [p for p in (ROOT / "version-26.2/src/main").rglob("*.java") if p.name not in ("ModContent.java",)]
text = "\n".join(p.read_text(encoding="utf-8") for p in sources)
problems = []
sounds = ids("SOUNDS")
table = json.loads((ASSETS / "sounds.json").read_text(encoding="utf-8"))
for s in sounds:
    if f'"{s}"' not in text:
        problems.append(f"sound {s} has no trigger in the code")
    if s not in table:
        problems.append(f"sound {s} has no entry in sounds.json")
for event, entry in table.items():
    if event not in sounds:
        problems.append(f"sounds.json lists {event}, which is not in Ids.SOUNDS")
    for f in entry["sounds"]:
        if not (ASSETS / "sounds" / (f.split(":")[1] + ".ogg")).exists():
            problems.append(f"{f} is missing")
for p in ids("PARTICLES"):
    if f'particle("{p}")' not in text:
        problems.append(f"particle {p} has no trigger in the code")
    if not (ASSETS / "particles" / f"{p}.json").exists():
        problems.append(f"particle {p} has no particles/{p}.json")
used = {f.split(":")[1] for e in table.values() for f in e["sounds"]}
for ogg in (ASSETS / "sounds").rglob("*.ogg"):
    if str(ogg.relative_to(ASSETS / "sounds").with_suffix("")).replace("\\", "/") not in used:
        problems.append(f"{ogg.name} is not used by sounds.json")
print("\n".join(problems) if problems else f"OK: {len(sounds)} sounds and {len(ids('PARTICLES'))} particles all have triggers and files")
sys.exit(1 if problems else 0)
