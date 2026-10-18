#!/usr/bin/env python3
"""Fail if a translation file is missing string names defined in values/strings.xml.

Usage: scripts/check-translations.py   (exits non-zero when a translation is incomplete)
"""
import pathlib
import re
import sys

RES_DIR = pathlib.Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "res"
STRING_NAME_RE = re.compile(r'<string name="([^"]+)"')


def string_names(path):
    return set(STRING_NAME_RE.findall(path.read_text(encoding="utf-8")))


def main():
    expected = string_names(RES_DIR / "values" / "strings.xml")
    incomplete = False
    for translation in sorted(RES_DIR.glob("values-*/strings.xml")):
        missing = expected - string_names(translation)
        if missing:
            incomplete = True
            print(f"{translation.relative_to(RES_DIR)}: missing {sorted(missing)}")
    if incomplete:
        print("translations INCOMPLETE")
        return 1
    print("translations OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
