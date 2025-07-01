#!/usr/bin/env python3
import re
import subprocess
import sys
from pathlib import Path


def main() -> None:
    root_path = subprocess.run(
        ["git", "rev-parse", "--show-toplevel"],
        capture_output=True,
        check=True,
        encoding="utf8",
    ).stdout.strip()
    with Path(f"{root_path}/README.md").open() as f:
        print(f.read())

    print("## Recent Changes")

    tag_count = 0
    last_tag = None
    commit_log = subprocess.run(
        ["git", "log", "--pretty=format:[%d] %h %s"],
        capture_output=True,
        encoding="utf8",
        check=True,
    ).stdout
    for commit_line in [ln.strip() for ln in commit_log.splitlines()]:
        m = re.search(r"^\[([^]]*)] (\S+) (.+)$", commit_line)
        assert m
        meta, sha, desc = m[1].strip(), m[2], m[3]
        if sha == "8895c45":
            break

        tags = [
            m.removeprefix("tag: ")
            for m in meta.strip("()").split(", ")
            if m.startswith("tag: v")
        ]
        tag = tags[0] if tags else None
        if tag:
            tag_count += 1
            if tag_count == 6:
                break
            if tag != last_tag:
                print(f"\n#### {tag}")
                last_tag = tag

        print(f"- {sha} {desc}")


try:
    main()
except KeyboardInterrupt:
    sys.exit(3)
except Exception as e:
    sys.exit(str(e))
