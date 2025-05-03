#!/usr/bin/env python3
import re
import sys
from pathlib import Path

ANNOTATION = '@SuppressWarnings({"all"})'

exit_code = 0
for filepath in Path("src/main/java/vendored").rglob("*.java"):
    lines = filepath.read_text().splitlines()

    for i, line in enumerate(lines):
        if "class" not in line:
            continue
        if line.startswith(("final public class", "public class", "final class")):
            assert i > 0
            if lines[i-1] != ANNOTATION:
                print(f"{filepath}: missing warning supression")
                print(f"     found: {lines[i-1]}")
                print(f"  expected: {ANNOTATION}")
            break
    else:
        raise RuntimeError(f"{filepath}: failed to find class definition")

sys.exit(exit_code)
