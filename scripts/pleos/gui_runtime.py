"""Check the trusted SDK's GUI ELF dependencies without starting or downloading an AVD."""
from pathlib import Path
import subprocess

from adb_readiness import diagnostic


def loader_environment(sdk, environment):
    root = Path(sdk) / "emulator"
    directories = [str(root / "lib64"), str(root / "lib64/qt/lib")]
    if environment.get("LD_LIBRARY_PATH"):
        directories.append(environment["LD_LIBRARY_PATH"])
    return environment | {"LD_LIBRARY_PATH": ":".join(directories), "QT_QPA_PLATFORM": "xcb"}


def check_gui_libraries(sdk, environment):
    binary = Path(sdk) / "emulator/qemu/linux-x86_64/qemu-system-x86_64"
    result = subprocess.run(["ldd", str(binary)], env=loader_environment(sdk, environment),
                            capture_output=True, text=True, timeout=20)
    missing = [line.strip() for line in result.stdout.splitlines() if "not found" in line]
    return {"ready": result.returncode == 0 and not missing, "returncode": result.returncode,
            "missing": missing, "stderr": diagnostic(result.stderr)}
