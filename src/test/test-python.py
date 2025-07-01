#!/usr/bin/env python3
import subprocess

subprocess.run(
    ["uvx", "ruff", "check", "--config", ".ruff.toml"],
    check=True,
)
subprocess.run(
    ["uvx", "ruff", "format", "--config", ".ruff.toml", "--check"],
    check=True,
)
