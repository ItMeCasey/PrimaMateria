"""Headless dedicated-server load check for one layer.

Usage: python scripts/server_check.py <layer> [<layer> ...]      e.g. neoforge-1.21.1 forge-26.2
       python scripts/server_check.py all

Starts `gradlew runServer` with PRIMAMATERIA_SELFTEST set (so the mod's self test runs once the server is up) in the layer with eula accepted and the layer's registered test port (8734-8737), waits for the
server's "Done" line, checks that the log names the mod and has no mod-loading errors, then stops the server. Prints
`SERVER PASS <layer>` or `SERVER FAIL <layer>: why`. Needs no display.
"""
import os
import re
import subprocess
import sys
import threading
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PORTS = {
    "neoforge-26.2": 8734, "forge-26.2": 8735, "neoforge-26.3": 8736, "forge-26.3": 8737,
}
TIMEOUT = 900
SLOT = "/c/Users/casey/.claude/tools/gradle-slot.sh"
GIT_BASH = Path("C:/Program Files/Git/bin/bash.exe")
BASH = str(GIT_BASH) if GIT_BASH.exists() else "bash"
# Real load problems: the loader could not load a mod, or an error names this mod. Netty and log4j probe lines about native
# transports on the wrong OS ("Only supported on Linux") are normal noise on Windows and are ignored.
ERROR_WORDS = re.compile(r"(ModLoadingException|Failed to load mod|Encountered an unexpected exception|FATAL)")
OWN_ERROR = re.compile(r"primamateria.*(Exception|Error)|(Exception|Error).*com\.itmecasey\.primamateria", re.IGNORECASE)


def ps(command):
    return subprocess.run(["powershell", "-NoProfile", "-Command", command], capture_output=True, text=True).stdout.split()


def stop(proc, port, folder):
    """Stops the server and the Gradle run above it. The run is started through Git Bash, whose children are not in the
    Windows process tree of the bash pid, so the server is found by its port and its parent chain is closed with it."""
    me = os.getpid()
    for pid in ps(f"(Get-NetTCPConnection -State Listen -LocalPort {port} -ErrorAction SilentlyContinue).OwningProcess"):
        chain = []
        while pid.isdigit() and int(pid) not in (0, me) and len(chain) < 6:
            chain.append(pid)
            parent = ps(f"(Get-CimInstance Win32_Process -Filter 'ProcessId={pid}').ParentProcessId")
            name = ps(f"(Get-CimInstance Win32_Process -Filter 'ProcessId={pid}').Name")
            if not parent or not name or name[0].lower() not in ("java.exe", "bash.exe", "sh.exe", "cmd.exe"):
                chain.pop()
                break
            pid = parent[0]
        for p in chain:
            subprocess.run(["taskkill", "/T", "/F", "/PID", p], capture_output=True)
    subprocess.run(["taskkill", "/T", "/F", "/PID", str(proc.pid)], capture_output=True)
    # A force-closed wrapper cannot remove its Gradle slot; free the slot this folder held.
    for lock in Path.home().glob(".claude/gradle.lock.*"):
        info = lock / "info"
        try:
            if info.read_text().splitlines()[2].lower().endswith(folder.as_posix().lower()[2:]):
                info.unlink()
                lock.rmdir()
        except (OSError, IndexError):
            pass


def check(layer):
    folder = ROOT / layer
    run = folder / "run"
    run.mkdir(exist_ok=True)
    (run / "eula.txt").write_text("eula=true\n")
    (run / "server.properties").write_text(
        f"server-port={PORTS[layer]}\nonline-mode=false\nlevel-type=minecraft\\:flat\nspawn-protection=0\n"
        "motd=PrimaMateria server check\nmax-tick-time=-1\n")
    log = []
    env = dict(os.environ, PRIMAMATERIA_SELFTEST="1")
    # Every Gradle command on this PC goes through the slot wrapper (a global lock with a few slots).
    proc = subprocess.Popen([BASH, SLOT, "./gradlew", "runServer", "--no-daemon", "-q", "-PdevTools"], cwd=folder, env=env,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, errors="replace")

    def pump():
        for line in proc.stdout:
            log.append(line.rstrip())

    t = threading.Thread(target=pump, daemon=True)
    t.start()
    start = time.time()
    done = False
    while time.time() - start < TIMEOUT and proc.poll() is None:
        if any(re.search(r"Done \(|For help, type", l) for l in log):
            done = True
            break
        time.sleep(1)
    for _ in range(40):
        if any('SELFTEST DONE' in l for l in log):
            break
        time.sleep(1)
    time.sleep(2)
    stop(proc, PORTS[layer], folder)
    t.join(timeout=5)
    out = "\n".join(log)
    (run / "server_check.log").write_text(out, encoding="utf-8")
    mod_seen = bool(re.search(r"mod:primamateria|\(primamateria\)|Loading mod primamateria|\[PrimaMateria/\]", out))
    errors = [l for l in log if ERROR_WORDS.search(l) or OWN_ERROR.search(l)]
    if not done:
        return f"SERVER FAIL {layer}: server never reported Done (see {run / 'server_check.log'})"
    if not mod_seen:
        return f"SERVER FAIL {layer}: log never lists the primamateria mod as loaded"
    if errors:
        return f"SERVER FAIL {layer}: {errors[0]}"
    passes = len(re.findall(r"SELFTEST PASS", out))
    if not re.search(r"SELFTEST DONE failures=0", out) or passes == 0:
        bad = [l for l in log if "SELFTEST FAIL" in l or "SELFTEST error" in l]
        return f"SERVER FAIL {layer}: self test did not finish clean (passes={passes}) {bad[:2]}"
    return f"SERVER PASS {layer} (port {PORTS[layer]}, {passes} self-test checks, {int(time.time() - start)}s)"


if __name__ == "__main__":
    layers = sys.argv[1:]
    if layers == ["all"] or not layers:
        layers = list(PORTS)
    bad = 0
    for layer in layers:
        result = check(layer)
        print(result, flush=True)
        bad += result.startswith("SERVER FAIL")
    sys.exit(1 if bad else 0)
