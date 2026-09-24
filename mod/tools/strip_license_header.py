#!/usr/bin/env python3
"""Strip the LoVisual GNU GPL file header from every Java source file.

The block being removed looks like one of:

    /*
     * This file is part of the LoVisual Client distribution.
     * Copyright (c) 2026 loki5512344.
     *
     * Licensed under the GNU General Public License v3.0.
     */
    <optional blank line>

    /*
     * This file is part of the LoVisual Client distribution.
     * Copyright (c) 2026 loki5512344.
     * Licensed under the GNU General Public License v3.0.
     */
    <optional blank line>

Files that do not start with either variant are skipped; a single blank line
is preserved before the next `package` / `import` for readability.
"""
from __future__ import annotations

import pathlib
import re
import sys

HEADER = re.compile(
    r"^/\*\n"
    r" \* This file is part of the LoVisual Client distribution\.\n"
    r" \* Copyright \(c\) 2026 loki5512344\.\n"
    r"(?: \*\n)?"
    r" \* Licensed under the GNU General Public License v3\.0\.\n"
    r" \*/\n"
)


def main(roots: list[str]) -> int:
    stripped = 0
    for root_arg in roots:
        root = pathlib.Path(root_arg)
        for path in root.rglob("*.java"):
            try:
                text = path.read_text(encoding="utf-8")
            except UnicodeDecodeError:
                continue
            m = HEADER.match(text)
            if not m:
                continue
            rest = text[m.end():]
            if rest.startswith("package ") or rest.startswith("import "):
                rest = "\n" + rest
            path.write_text(rest, encoding="utf-8")
            stripped += 1
    print(f"stripped {stripped} file(s)")
    return 0


if __name__ == "__main__":
    args = sys.argv[1:] or ["."]
    sys.exit(main(args))
