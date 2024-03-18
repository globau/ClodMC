import contextlib
import json
import os
import sys
import time
import urllib.request as request
from collections.abc import Generator
from math import floor
from pathlib import Path
from typing import Any


class Error(Exception):
    pass


@contextlib.contextmanager
def main_wrapper() -> Generator[None, None, None]:
    try:
        yield
    except KeyboardInterrupt:
        sys.exit(3)
    except Error as e:
        print(e, file=sys.stderr)
        sys.exit(1)


def fetch_json(url: str) -> Any:
    req = request.Request(url)
    req.add_header("accept", "application/json")
    req.add_header("user-agent", "globau/mc-qol <me@glob.au>")
    with request.urlopen(req) as f:
        return json.load(f)


def version_tuple(version: str) -> tuple[int, ...]:
    if version == "":
        return (0,)
    return tuple(int(n) for n in version.split("."))


def download_file(url: str, output_file: Path) -> None:
    def _size(num: int | float, dot_zero: bool = False, precision: int = 1) -> str:
        num = round(num)
        units = ("b", "k", "m", "g")
        i = 0
        while num >= 1024.0:
            num /= 1024.0
            i += 1
            if i == len(units) - 1:
                break
        unit = units[i]
        if not dot_zero and round(num * 10) / 10 - round(num) == 0:
            return f"{num:.0f}{unit}"
        template = f"%.{precision}f%s"
        return template % (num, unit)

    def _seconds(seconds: int) -> str:
        hours, seconds = divmod(seconds, 3600)
        minutes, seconds = divmod(seconds, 60)
        if hours > 0:
            parts = [hours, minutes, seconds]
        elif minutes > 0:
            parts = [minutes, seconds]
        else:
            parts = [seconds]

        units = ["s", "m", "h"]
        result = []
        for value, unit in zip(reversed(parts), units, strict=False):
            result.append(f"{value:d}{unit}")

        return "".join(reversed(result))

    def _mmss(s: int) -> str:
        s = round(s)
        m = floor(s / 60)
        s -= m * 60
        return f"{m:02d}:{s:02d}"

    show_progress = os.environ.get("CI") is None
    bar_len = 20
    in_progress_label_len = 79 - (1 + bar_len + len(" 000b/000b 00:00 000b/s"))
    completed_label_len = 79 - len(" ✔ 000.0b in 000s")
    in_progress_label = url
    if len(url) > in_progress_label_len:
        in_progress_label = f"…{url[len(url) - in_progress_label_len:]}"
    completed_label = url
    if len(url) > completed_label_len:
        completed_label = f"…{url[len(url) - completed_label_len:]}"

    part_file = output_file.parent / f"{output_file.name}.part"
    part_file.parent.mkdir(exist_ok=True)
    part_file.unlink(missing_ok=True)

    start_time = time.time()
    received_bytes = 0
    if show_progress:
        print(f"{in_progress_label}\033[K\r", end="", flush=True)
    req = request.Request(url)
    req.add_header("user-agent", "globau/mc-qol <me@glob.au>")
    with request.urlopen(req) as r:
        assert r.length
        total_bytes = r.length
        with Path(part_file).open("wb") as f:
            bs = 1024 * 8
            while True:
                block = r.read(bs)
                if not block:
                    break
                f.write(block)
                received_bytes += len(block)

                if show_progress:
                    elapsed = time.time() - start_time

                    perc_completed = received_bytes / total_bytes
                    if perc_completed > 0:
                        time_remaining = elapsed / perc_completed - elapsed
                    else:
                        time_remaining = 0

                    filled = int(round(perc_completed * bar_len))
                    status = [
                        in_progress_label,
                        f"{'●' * filled}{'○' * (bar_len - filled)}",
                        f"{_size(received_bytes, precision=0)}/"
                        f"{_size(total_bytes, precision=0)}",
                        _mmss(int(time_remaining)),
                        f"{_size(received_bytes / elapsed * 8, precision=0)}/s",
                    ]
                    print(f"{' '.join(status)}\033[K\r", end="", flush=True)

        status = [
            completed_label,
            "✔",
            _size(total_bytes, dot_zero=True),
            "in",
            _seconds(int(time.time() - start_time)),
        ]
        print(f"{' '.join(status)}\033[K")

        part_file.rename(output_file)
