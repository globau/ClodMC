#!/usr/bin/env python3
import re
import sys
from pathlib import Path

exit_code = 0
for filepath in Path("src").rglob("*.java"):
    lines = filepath.read_text().splitlines()
    for i, line in enumerate(lines):
        if i == 0 or lines[i - 1].strip() != "@EventHandler":
            continue

        m = re.search(r"^\s*public void ([^(]+)\(([^)]+)\)", line)
        method = m[1]
        args = m[2]

        if not args.endswith(" event"):
            print(f"{line}\n> event argument is not named 'event'")
            exit_code = 1
            continue
        args = args.removesuffix(" event")

        if not args.endswith("Event"):
            print(f"{line}\n> event class is not named an Event")
            exit_code = 1
            continue
        args = args.removesuffix("Event")

        if method != f"on{args}":
            print(f"{line}\n> incorrect method name, expected: on{args}")
            exit_code = 1

sys.exit(exit_code)
