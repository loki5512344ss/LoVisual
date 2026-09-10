#!/usr/bin/env python3
"""Rewrite import paths to match on-disk location + auto-fix missing/star imports.
Usage: fiximports.py [BASE ...]  — if no BASE given, scans whole lovisual tree.
Features vs v1:
 - handles star imports (import pkg.*) -> expands to single-type imports based on body usage
 - inserts missing single-type imports for unique classes used by simple name
 - fixes stale FQNs in body (dev.loki.lovisual.oldpkg.Class -> newpkg.Class)
 - respects shadow list (Font, Scoreboard etc.) and dupe names
 - dedupes + sorts imports
"""

import os, re, sys

ROOT = "src/main/java/dev/loki/lovisual"
# Known shadows where simple name collides with JDK/vanilla/GUI types.
# Auto-insert would break these (seen in ecfe76f fix).
SHADOW = {
    "Font",
    "GlyphAtlasManager",
    "Scoreboard",
    "Inventory",
    "MediaPlayer",
    "Matrix4f",
    "Vector3f",
    "Button",
    "List",
    "Map",
    "Set",
    "Player",
    "Entity",
    "Level",
    "World",
}

# Build class -> pkg map from disk
cls_pkg = {}  # cls -> set(pkgs)
cls_pkg_unique = {}  # cls -> pkg if unique
file_for_cls = {}  # cls -> path (for dupes debug)

scan_roots = sys.argv[1:] if len(sys.argv) > 1 else [ROOT]
for base in scan_roots:
    # base may be like src/main/java/dev/loki/lovisual/util/media
    # we walk it, but for global map we walk ROOT anyway to fix all imports
    pass
# always build from ROOT for correctness
for dp, dns, fns in os.walk(ROOT):
    pkg = os.path.relpath(dp, ROOT).replace("/", ".")
    if pkg == ".":
        pkg = ""
    for fn in fns:
        if not fn.endswith(".java"):
            continue
        cls = fn[:-5]
        # full pkg for this class
        full_pkg = f"dev.loki.lovisual.{pkg}" if pkg else "dev.loki.lovisual"
        cls_pkg.setdefault(cls, set()).add(full_pkg)
        file_for_cls[cls] = os.path.join(dp, fn)

dupes = {cls for cls, pkgs in cls_pkg.items() if len(pkgs) > 1}
for cls, pkgs in cls_pkg.items():
    if len(pkgs) == 1:
        cls_pkg_unique[cls] = next(iter(pkgs))

# For FQN fix: build old->new map for stale FQNs (where file moved but old import still points)
# We already have unique map, so any FQN that doesn't match current pkg is stale.
changed = 0
FQN_RE = re.compile(r"dev\.loki\.lovisual\.[\w.]+?\.([A-Z]\w*)\b")

for tree in ("src/main/java", "src/test/java"):
    for dp, dns, fns in os.walk(tree):
        for fn in fns:
            if not fn.endswith(".java"):
                continue
            p = os.path.join(dp, fn)
            text = open(p, encoding="utf-8", errors="ignore").read()
            orig = text

            # package of this file
            m_pkg = re.search(r"^package\s+([\w.]+);", text, re.M)
            my_pkg = m_pkg.group(1) if m_pkg else ""

            lines = text.splitlines(keepends=True)
            # separate header vs body
            # find last import line
            import_lines = [i for i, l in enumerate(lines) if l.startswith("import ")]
            body_start = (
                (max(import_lines) + 1)
                if import_lines
                else next(
                    (i for i, l in enumerate(lines) if l.startswith("package")), 0
                )
                + 1
            )
            body = "".join(lines[body_start:])

            # 1) Fix existing single-type imports where path is stale (unique classes only)
            def fix_import(m):
                full = m.group(0).strip()  # import dev.loki.lovisual.foo.Bar;
                # extract pkg and cls
                mm = re.match(
                    r"import\s+(?:static\s+)?dev\.loki\.lovisual\.([\w.]+)\.([A-Z]\w*);",
                    full,
                )
                if not mm:
                    return m.group(0)
                pkg_part, cls = mm.group(1), mm.group(2)
                full_pkg_prefix = f"dev.loki.lovisual.{pkg_part}"
                if cls in dupes or cls in SHADOW:
                    return m.group(0)
                want = cls_pkg_unique.get(cls)
                if want is None:
                    return m.group(0)
                if want == full_pkg_prefix or want == f"dev.loki.lovisual.{pkg_part}":
                    return m.group(0)
                # keep static prefix if present
                is_static = "static " in full
                prefix = "import static " if is_static else "import "
                return (
                    f"{prefix}{want}.{cls};\n"
                    if full.endswith("\n")
                    else f"{prefix}{want}.{cls};"
                )

            text = re.sub(
                r"import\s+(?:static\s+)?dev\.loki\.lovisual\.[\w.]*?\.([A-Z]\w*);[ \t]*\n?",
                lambda m: fix_import(m) if m.group(1) in cls_pkg_unique else m.group(0),
                text,
            )

            # Re-split after fix
            lines = text.splitlines(keepends=True)
            import_lines = [i for i, l in enumerate(lines) if l.startswith("import ")]
            body_start = (
                (max(import_lines) + 1)
                if import_lines
                else next(
                    (i for i, l in enumerate(lines) if l.startswith("package")), 0
                )
                + 1
            )
            body = "".join(lines[body_start:])

            # 2) Expand star imports: import pkg.* -> needed singles
            # DISABLED by default for clean-tree runs (avoids 57-file churn).
            # split_struct.py handles star expansion per-wave when BASE becomes empty.
            # Enable here only if --expand-stars flag is given.
            stars = []
            if "--expand-stars" in sys.argv:
                for l in lines:
                    ms = re.match(r"import\s+dev\.loki\.lovisual\.([\w.]+)\.\*;", l)
                    if ms:
                        stars.append(f"dev.loki.lovisual.{ms.group(1)}")
            if stars:
                needed = set()
                body_tokens = set(re.findall(r"\b[A-Z][A-Za-z0-9_]*\b", body))
                for star_pkg in stars:
                    for cls in body_tokens:
                        want = cls_pkg_unique.get(cls)
                        if want == star_pkg:
                            if cls not in SHADOW:
                                needed.add(f"import {want}.{cls};")
                if needed:
                    # remove star lines, insert needed
                    new_lines = []
                    for l in lines:
                        if re.match(r"import\s+dev\.loki\.lovisual\.[\w.]+\.\*;", l):
                            continue
                        new_lines.append(l)
                    # insert needed after last import or after package
                    lines = new_lines
                    text = "".join(lines)
                    lines = text.splitlines(keepends=True)
                    import_lines = [
                        i for i, l in enumerate(lines) if l.startswith("import ")
                    ]
                    idx = (
                        max(import_lines)
                        if import_lines
                        else next(
                            (i for i, l in enumerate(lines) if l.startswith("package")),
                            0,
                        )
                    )
                    # dedup against existing
                    existing = set(l.strip() for l in lines if l.startswith("import "))
                    to_add = sorted(n for n in needed if n not in existing)
                    if to_add:
                        lines[idx + 1 : idx + 1] = [n + "\n" for n in to_add]
                    text = "".join(lines)
                    # re-derive body
                    lines = text.splitlines(keepends=True)
                    body = "".join(lines[body_start:])

            # 3) Insert missing imports for unique classes used by simple name
            # DISABLED by default: too aggressive (inner class UiState etc.) — only needed
            # when split_struct moved classes and body uses simple name without import.
            # split_struct.py already handles this for moved files. For fiximports standalone,
            # we skip auto-insert to avoid 160-file churn on clean tree. Enable with --insert.
            missing = []
            if "--insert" in sys.argv:
                body_simple = body
                body_simple = re.sub(r'"(\\.|[^"\\])*"', '""', body_simple)
                body_simple = re.sub(r"//[^\n]*", "", body_simple)
                body_simple = re.sub(r"/\*.*?\*/", "", body_simple, flags=re.S)
                tokens = set(re.findall(r"\b[A-Z][A-Za-z0-9_]*\b", body_simple))
                for cls in tokens:
                    if cls in SHADOW or cls in dupes:
                        continue
                    want = cls_pkg_unique.get(cls)
                    if not want:
                        continue
                    if want == my_pkg:
                        continue
                    if f"import {want}.{cls};" in text:
                        continue
                    if cls == fn[:-5]:
                        continue
                    if f"{want}.{cls}" in body:
                        continue
                    # require unqualified usage (not Outer.Inner)
                    if not re.search(
                        r"(?<![\w.])" + re.escape(cls) + r"\b", body_simple
                    ):
                        continue
                    missing.append(f"import {want}.{cls};")
            if missing:
                missing = sorted(set(missing))
                lines = text.splitlines(keepends=True)
                import_lines = [
                    i for i, l in enumerate(lines) if l.startswith("import ")
                ]
                idx = (
                    max(import_lines)
                    if import_lines
                    else next(
                        (i for i, l in enumerate(lines) if l.startswith("package")), 0
                    )
                )
                lines[idx + 1 : idx + 1] = [m + "\n" for m in missing]
                text = "".join(lines)

            # 4) Fix stale FQNs in body (where old pkg.Class still written fully qualified)
            def fqn_fix(m):
                cls = m.group(1)
                full = m.group(0)
                if cls in dupes or cls in SHADOW:
                    return full
                want = cls_pkg_unique.get(cls)
                if not want:
                    return full
                # full is like dev.loki.lovisual.old.pkg.Cls
                cur_pkg = ".".join(full.split(".")[:-1])
                if cur_pkg == want:
                    return full
                return f"{want}.{cls}"

            # only in body part, but apply to whole file (imports already fixed)
            text = FQN_RE.sub(
                lambda m: fqn_fix(m) if m.group(1) in cls_pkg_unique else m.group(0),
                text,
            )

            # 5) Dedupe + sort imports block (keep static separate)
            lines = text.splitlines(keepends=True)
            # find import block range
            imp_start = next(
                (i for i, l in enumerate(lines) if l.startswith("import ")), None
            )
            if imp_start is not None:
                imp_end = imp_start
                while imp_end < len(lines) and lines[imp_end].startswith("import "):
                    imp_end += 1
                block = lines[imp_start:imp_end]
                # dedupe preserving order (no global sort to avoid 490-file churn)
                seen = set()
                uniq = []
                for l in block:
                    if l not in seen:
                        seen.add(l)
                        uniq.append(l)
                lines = lines[:imp_start] + uniq + lines[imp_end:]
                text = "".join(lines)

            if text != orig:
                open(p, "w", encoding="utf-8").write(text)
                changed += 1

print(f"import paths corrected: {changed}")
if dupes:
    print(
        f"dupes skipped ({len(dupes)}): {sorted(list(dupes))[:10]}{' ...' if len(dupes) > 10 else ''}"
    )
if SHADOW:
    print(f"shadow protected: {sorted(SHADOW)}")
