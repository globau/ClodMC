#!/usr/bin/env python3
import re
import sys
from pathlib import Path

build_filepath = Path("build.gradle.kts")
plugin_filepath = Path("src/main/resources/paper-plugin.yml")

for line in build_filepath.read_text().splitlines():
    if m := re.search(r'^\s*compileOnly\("io\.papermc\.paper:paper-api:([^-]+)', line):
        build_version = m[1]
        break
else:
    sys.exit(f"failed to find paper-api version in {build_filepath}")

for line in plugin_filepath.read_text().splitlines():
    if m := re.search(r"api-version:(.+)$", line):
        plugin_version = m[1].strip("\" '")
        break
else:
    sys.exit(f"failed to find paper-api version in {plugin_filepath}")

if build_version != plugin_version:
    sys.exit(
        f"build paper-api version '{build_version}' does not match plugin '{plugin_version}'"
    )
