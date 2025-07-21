#!/usr/bin/env python3
import re
import sys
from pathlib import Path

exit_code = 0

for filepath in Path("src/main/java/au/com/glob/clodmc").rglob("*.java"):
    content = filepath.read_text()

    top_level_declarations = []
    lines = content.splitlines()
    brace_depth = 0
    in_comment = False
    in_string = False
    for i, line in enumerate(lines):
        stripped = line.strip()
        if not stripped or stripped.startswith("//"):
            continue

        if "/*" in stripped and not in_string:
            in_comment = True
        if "*/" in stripped and in_comment:
            in_comment = False
            continue
        if in_comment:
            continue

        brace_depth += stripped.count("{") - stripped.count("}")
        if (
            brace_depth <= 1
            and re.search(
                r"^\s*(?:public\s+)?(?:class|interface|enum|record)\s+\w+", stripped
            )
            and not re.search(
                r"\bstatic\b.*\b(?:class|interface|enum|record)\b", stripped
            )
        ):
            top_level_declarations.append((i + 1, stripped))

    if not top_level_declarations:
        continue

    if "@NullMarked" not in content:
        print(f"{filepath} is missing @NullMarked annotation")
        for line_num, declaration in top_level_declarations:
            print(f"> {line_num}: {declaration}")
        exit_code = 1

sys.exit(exit_code)
