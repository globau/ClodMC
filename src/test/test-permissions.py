#!/usr/bin/env python3
import os
import sys
from pathlib import Path

x_files = []
for filepath in Path("src/main").rglob("*.*"):
    if not filepath.is_dir() and os.access(filepath, os.X_OK):
        x_files.append(filepath)
if x_files:
    print("bad file permissions (+x):")
    print("\n".join(str(f) for f in sorted(x_files)))
    sys.exit(1)
