#!/usr/bin/env python3
import subprocess
import sys
from datetime import UTC, datetime

# output a version number derived from the latest commit's timestamp


def main() -> None:
    # commit timestamp
    p = subprocess.run(
        ["git", "show", "--no-patch", "--format=%ct"],
        check=True,
        capture_output=True,
        encoding="utf8",
    )
    # in gmt+8
    timestamp = int(p.stdout.strip())
    dt = datetime.fromtimestamp(timestamp + 60 * 60 * 8, UTC)
    # date+hh:mm pretending to be a version string
    print(dt.strftime("%y.%m%d.%H%M"))


try:
    main()
except KeyboardInterrupt:
    sys.exit(3)
except Exception as e:
    sys.exit(str(e))
