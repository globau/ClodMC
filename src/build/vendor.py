#!/usr/bin/env python3
import argparse
import configparser
import shutil
import subprocess
import sys
from pathlib import Path
from typing import Any


def main() -> None:
    vendored_filepath = Path.cwd() / "src/vendored/vendored.ini"
    vendored_config = configparser.ConfigParser()
    vendored_config.read(vendored_filepath)

    parser = argparse.ArgumentParser()
    parser.add_argument("name", choices=vendored_config.sections(), help="library name")
    args = parser.parse_args()
    name = args.name

    print(f"vendoring {name}")

    config: dict[str, Any] = dict(vendored_config.items(name))
    config["files"] = config["files"].strip().splitlines()

    src_path = Path(f".git/vendored/{name}")
    dst_path = Path(f"src/main/java/vendored/{config['path']}")

    # clone repo
    shutil.rmtree(src_path.parent, ignore_errors=True)
    src_path.parent.mkdir(parents=True)
    subprocess.run(
        ["git", "clone", "--depth", "1", config["repo"], name],
        cwd=src_path.parent,
        check=True,
    )

    # build list of files
    src_filepaths = []
    for src_filename in config["files"]:
        src_filepath = src_path / src_filename.rstrip("/")
        if src_filename.endswith("/"):
            # directory
            src_filepaths.extend(
                fp for fp in src_filepath.rglob("*") if not fp.is_dir()
            )
        else:
            src_filepaths.append(src_filepath)

    # copy files
    expected_filepaths = set()
    for src_filepath in sorted(src_filepaths):
        dst_filepath = dst_path / src_filepath.name
        dst_filepath.parent.mkdir(parents=True, exist_ok=True)
        print(f"{src_filepath.relative_to(src_path)} -> {dst_filepath}")
        shutil.copy(src_filepath, dst_filepath)
        expected_filepaths.add(dst_filepath.relative_to(dst_path))

    # delete extra
    actual_filepaths = {fp.relative_to(dst_path) for fp in dst_path.rglob("*")}
    for rel_filename in actual_filepaths - expected_filepaths:
        filepath = dst_path / rel_filename
        print(f"deleting {filepath}")
        filepath.unlink()

    # format
    subprocess.run(["make", "format"], check=False)

    # apply patches
    for filepath in sorted(
        Path(f"src/vendored/{name}").glob("*.patch"),
        key=lambda fp: int(fp.stem.split("-")[0]),
    ):
        print(f"patching {filepath}")
        subprocess.run(["git", "apply", filepath], check=True)

    # update commit in .ini
    p = subprocess.run(
        ["git", "rev-parse", "HEAD"],
        cwd=src_path,
        check=True,
        capture_output=True,
        encoding="utf8",
    )
    sha = p.stdout.strip()
    vendored_config.set(name, "commit", sha)
    with vendored_filepath.open("w") as f:
        vendored_config.write(f)

    print(f"\n{name} updated to {sha}")


try:
    main()
except KeyboardInterrupt:
    sys.exit(3)
except Exception as e:
    sys.exit(str(e))
