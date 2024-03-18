import re
from pathlib import Path

from util import Error


class BuildConfig:
    def __init__(self) -> None:
        self._lines = Path("build.gradle").read_text().splitlines()
        self._vars = {}
        for line in self._lines:
            if m := re.search(r'^\s*static def (\S+) = "([^"]+)"', line):
                self._vars[m[1]] = m[2]
        self.unmodified = {}
        self.modified = {}

    def get(self, name: str, *, unmodified: bool = False) -> str:
        if unmodified:
            return self._vars.get(name)
        return self.modified.get(name, self._vars.get(name))

    def set(self, name: str, value: str) -> None:
        if name not in self._vars:
            raise Error(f"failed to find variable: {name}")
        if self._vars[name] != value:
            self.modified[name] = value
        else:
            self.unmodified[name] = value

    def commit(self) -> None:
        if not self.modified:
            return

        for name, value in self.modified.items():
            for i, line in enumerate(self._lines):
                m = re.search(r'^(\s*static def) (\S+) = "([^"]+)"', line)
                if m and m[2] == name:
                    self._lines[i] = f'{m[1]} {name} = "{value}"'
                    break
            else:
                raise RuntimeError(f"failed to find {name}")

        content = "\n".join(self._lines)
        Path("build.gradle").write_text(f"{content}\n")


def get_var(name: str) -> str | None:
    return BuildConfig().get(name)
