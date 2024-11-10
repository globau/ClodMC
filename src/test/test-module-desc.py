#!/usr/bin/env python3
import re
import sys
from pathlib import Path

exit_code = 0
for filepath in Path("src/main/java/au/com/glob/clodmc/modules/").rglob("*.java"):
    java = filepath.read_text()

    # only modules
    m = re.search(r"\npublic class " + filepath.stem + r" implements ([^{]+)", java)
    if not m or "Module" not in m[1] or "<Module>" in m[1]:
        continue

    lines = java.splitlines()
    for i, line in enumerate(lines):
        if line.startswith("public class"):
            j = 1
            found = False
            while j < i:
                doc_string = lines[i - j]
                if not doc_string:
                    break
                if doc_string.startswith("/**"):
                    found = True
                    break
                j += 1
            if not found:
                print(f"{filepath}: Module missing docstring")
                exit_code = 1
            break

sys.exit(exit_code)
