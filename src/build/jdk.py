import platform
import sys
import urllib.error
from pathlib import Path
from urllib.parse import quote, urlencode

from util import Error, fetch_json

PATH = Path().cwd() / "jdk"
VERSION = 17


def qs() -> str:
    return urlencode(
        {
            "architecture": {"x86_64": "x64", "arm64": "aarch64"}[platform.machine()],
            "image_type": "jdk",
            "os": {"Darwin": "mac", "Linux": "linux"}[platform.system()],
            "page": 0,
            "page_size": 25,
            "sort_method": "DEFAULT",
            "sort_order": "DESC",
        },
        doseq=True,
    )


def latest_version() -> str:
    versions_res = fetch_json(
        f"https://marketplace-api.adoptium.net/v1/info/release_versions/microsoft?{qs()}"
    )

    version_name = None
    latest_ver = (0, 0, 0, 0)
    for version in [v for v in versions_res["versions"] if v["major"] == VERSION]:
        ver = (
            version["major"],
            version["minor"],
            version["security"],
            version["patch"] or 0,
        )
        if ver > latest_ver:
            latest_ver = ver
            version_name = version["openjdk_version"]

    if not version_name:
        raise Error(f"failed to find Java v{VERSION} for {sys.platform}")

    return version_name


def download_url(version: str) -> str:
    try:
        assets_res = fetch_json(
            "https://marketplace-api.adoptium.net/v1/assets/release_name/microsoft/"
            f"jdk-{quote(version)}?{qs()}"
        )
    except urllib.error.HTTPError as e:
        if e.code == 404:
            raise Error(f"failed to find asset url for jdk-{version}")
        raise Error(str(e))
    if len(assets_res["binaries"]) != 1:
        raise Error(f"failed to find assets for jdk-{version}")
    try:
        return assets_res["binaries"][0]["package"]["link"]
    except IndexError:
        raise Error(f"malformed response for assets for jdk-{version}")
