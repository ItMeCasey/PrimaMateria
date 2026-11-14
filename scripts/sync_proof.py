"""Proof of the recipe lookup sync: a real dedicated server and a real client that joins it.

Usage: PRIMA_PORT_BASE=8742 python scripts/sync_proof.py <layer> <viewer>     e.g. neoforge-26.2 bei | jei | rei

1. Starts `gradlew runServer` (no dev tools, no demo recipes, no self test; with BEI added for a BEI check) in <layer>/runs/syncserver and waits for "Done".
2. Starts `gradlew runClient -PdevTools -Pviewers=<mods>` through the isolated launcher in <layer>/runs/syncclient with
   PRIMAMATERIA_TOUR=<viewer> and PRIMAMATERIA_TOUR_SERVER=127.0.0.1:<port>. The client has no integrated server, so it never builds
   the lookup itself: what the viewer shows came from the server's payload. The tour opens the viewer for a few items, saves frames
   and quits.
3. Stops the server by the pid chain of its port, checks both logs and copies the frames to docs/frames/sync_<layer>_*.png and the
   evidence lines to docs/testing/recipe_sync_<layer>_<viewer>.log.
Check `who-owns-minecraft.ps1` first: one client at a time.
"""
import os
import re
import shutil
import subprocess
import sys
import threading
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import integration_tour as it  # noqa: E402
import server_check as sc  # noqa: E402

ROOT = it.ROOT
TIMEOUT = 900


def proof(layer, viewer):
    folder = ROOT / layer
    port = sc.PORTS[layer]
    server_run = folder / "runs" / "syncserver"
    client_run = folder / "runs" / "syncclient"
    shutil.rmtree(server_run / "world", ignore_errors=True)
    server_run.mkdir(parents=True, exist_ok=True)
    (server_run / "eula.txt").write_text("eula=true\n")
    (server_run / "server.properties").write_text(
        f"server-port={port}\nonline-mode=false\nlevel-type=minecraft\\:flat\nspawn-protection=0\nmotd=PrimaMateria sync proof\nmax-tick-time=-1\n")
    # some builds start with the whitelist on whatever server.properties says: list the dev player (offline uuid) in it
    import hashlib
    import uuid
    offline = uuid.UUID(bytes=hashlib.md5(b"OfflinePlayer:Dev").digest(), version=3)
    (server_run / "whitelist.json").write_text('[{"uuid": "%s", "name": "Dev"}]' % offline)
    it.write_options(client_run)
    shots = client_run / "screenshots"
    if shots.exists():
        shutil.rmtree(shots)
    latest = client_run / "logs" / "latest.log"
    if latest.exists():
        latest.unlink()

    env = {k: v for k, v in os.environ.items() if k not in ("PRIMAMATERIA_DEMO", "PRIMAMATERIA_SELFTEST", "PRIMAMATERIA_TOUR")}
    log = []
    # BEI syncs the vanilla recipes itself and only talks to servers that run BEI too, so a BEI check needs it on the server as well.
    server_viewers = ["-Pviewers=bei"] if viewer == "bei" else []
    proc = subprocess.Popen([sc.BASH, sc.SLOT, "./gradlew", "runServer", "--no-daemon", "-q", "-PrunDir=runs/syncserver",
                             *server_viewers], cwd=folder,
                            env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, errors="replace")

    def pump():
        for line in proc.stdout:
            log.append(line.rstrip())

    threading.Thread(target=pump, daemon=True).start()
    start = time.time()
    up = False
    result = None
    done = False
    try:
        while time.time() - start < TIMEOUT and proc.poll() is None:
            if any(re.search(r"Done \(", l) for l in log):
                up = True
                break
            time.sleep(1)
        if not up:
            return f"SYNC FAIL {layer} {viewer}: the server never reported Done"
        command = (r"set PRIMAMATERIA_TOUR=" + viewer + r"&& set PRIMAMATERIA_TOUR_SERVER=127.0.0.1:" + str(port)
                   + r"&& .\gradlew.bat runClient --no-daemon -q -PdevTools -PwindowSize=1904x1041 -PrunDir=runs/syncclient -Pviewers="
                   + it.VIEWERS[viewer])
        subprocess.run(["powershell", "-NoProfile", "-File", it.LAUNCH, "-Run", "-WorkDir", str(folder), "-RunDir", str(client_run),
                        "-Command", command], check=True)
        try:
            while time.time() - start < TIMEOUT:
                time.sleep(3)
                if latest.exists() and "PRIMAMATERIA-TOUR DONE" in latest.read_text(encoding="utf-8", errors="replace"):
                    done = True
                    break
        finally:
            time.sleep(4)
            subprocess.run(["powershell", "-NoProfile", "-File", it.LAUNCH, "-Kill", "-WorkDir", str(folder)], capture_output=True)
        time.sleep(2)
    finally:
        sc.stop(proc, port, folder)

    server_text = "\n".join(log)
    client_text = latest.read_text(encoding="utf-8", errors="replace") if latest.exists() else ""
    frames = ROOT / "docs" / "frames"
    frames.mkdir(parents=True, exist_ok=True)
    names = sorted(p.name for p in shots.glob("*.png")) if shots.exists() else []
    for n in names:
        shutil.copy2(shots / n, frames / n.replace("primamateria_", f"sync_{layer}_", 1))
    wanted = re.compile(r"Recipe lookup|PRIMAMATERIA-TOUR (lookup|joining|in the world|saved|DONE)|Done \(|Disconnected|Player.*(joined|left)|logged in")
    evidence = [f"server: {l}" for l in dict.fromkeys(server_text.splitlines()) if wanted.search(l)]
    evidence += [f"client: {l}" for l in client_text.splitlines() if wanted.search(l)]
    out = ROOT / "docs" / "testing"
    out.mkdir(parents=True, exist_ok=True)
    (out / f"recipe_sync_{layer}_{viewer}.log").write_text("\n".join(evidence) + "\n", encoding="utf-8")

    sent = re.search(r"Recipe lookup sent to .*: (\d+) sifting recipes, (\d+) frame", server_text)
    got = re.search(r"Recipe lookup from the server payload: (\d+) sifting", client_text)
    before = re.search(r"lookup before joining: 0 sifting", client_text)
    after = re.search(r"lookup after joining: (\d+) sifting.*singleplayer server: false", client_text)
    errors = [l for l in (server_text + "\n" + client_text).splitlines()
              if re.search(r"PRIMAMATERIA-TOUR (failed|could not)|ModLoadingException|Crash report saved|primamateria.*(Exception|Error)", l, re.I)]
    problems = []
    if not done:
        problems.append("tour did not finish")
    if not sent:
        problems.append("server never sent the lookup")
    if not got:
        problems.append("client never received the lookup")
    if not before:
        problems.append("client lookup was not empty before joining")
    if not after or int(after.group(1)) == 0:
        problems.append("client lookup empty after joining")
    if sent and got and sent.group(1) != got.group(1):
        problems.append("sent and received counts differ")
    if not names:
        problems.append("no frames")
    if errors:
        problems.append(errors[0])
    status = "SYNC PASS" if not problems else "SYNC FAIL"
    detail = "" if not problems else ": " + "; ".join(problems)
    return (f"{status} {layer} {viewer} (sent {sent.group(1) if sent else '?'} sifting in {sent.group(2) if sent else '?'} frame(s), "
            f"{len(names)} frames, {int(time.time() - start)}s){detail}")


if __name__ == "__main__":
    result = proof(sys.argv[1], sys.argv[2])
    print(result, flush=True)
    sys.exit(1 if result.startswith("SYNC FAIL") else 0)
