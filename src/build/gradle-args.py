#!/usr/bin/env python3
import re
import shutil
import subprocess
import sys
from pathlib import Path

# output gradle args to force use of the correct java version
# ./gradlew `./src/build/gradle-args.py` ...

JDK_VERSION = 21


def is_correct_version(java_bin: str) -> bool:
    p = subprocess.run(
        [java_bin, "--version"],
        check=True,
        capture_output=True,
        encoding="utf8",
    )
    ver_line = p.stdout.splitlines()[0]
    if m := re.search(r"^\S+ (\d+)\.", ver_line):
        return int(m[1]) == JDK_VERSION
    return False


def main() -> None:
    # if the default java version is ok, nothing to do
    if (java_bin := shutil.which("java")) and is_correct_version(java_bin):
        return

    # try to find the correct jdk version in the standard macOS locations
    for base_path in (
        Path("~/Library/Java/JavaVirtualMachines").expanduser(),
        Path("/Library/Java/JavaVirtualMachines"),
    ):
        for java_path in sorted(
            d / "Contents/Home" for d in base_path.glob("*") if d.is_dir()
        ):
            if is_correct_version(str(java_path / "bin/java")):
                print(f"-Dorg.gradle.java.home={java_path}")
                return


try:
    main()
except KeyboardInterrupt:
    sys.exit(3)
except Exception as e:
    sys.exit(str(e))
