#!/usr/bin/env python3
import re
from pathlib import Path

for filepath in Path("src").rglob("*.java"):
    lines = filepath.read_text().splitlines()
    for i, line in enumerate(lines):
        if i == 0 or lines[i - 1].strip() != "@EventHandler":
            continue

        m = re.search(r"^\s*public void [^(]+\(([^)]+)\)", line)
        args = m[1]

        args = args.removeprefix("@NotNull ").removesuffix(" event")
        filename = str(filepath).removeprefix("src/main/java/au/com/glob/clodmc/")
        print(f"{filename}: {args}")
