#!/usr/bin/env python3
"""Structure split helper (Phase 8.5.1): move classes into leaf subpackages by spec JSON,
fix package lines, rewrite FQNs repo-wide, expand star imports of emptied bases,
insert single-type imports for moved classes, promote the PRIMARY top-level type
(class name == file name) to public when it was package-private.
Does NOT run the compile loop — call promote.py afterwards.
Usage: python3 split_struct.py spec.json  (run from project root, e.g. combatant-client-26.2)"""

import os, re, subprocess, json, sys

ROOT = "src/main/java/dev/loki/lovisual"


def git_mv(src, dstdir, name):
    os.makedirs(dstdir, exist_ok=True)
    dst = os.path.join(dstdir, name + ".java")
    if os.path.abspath(src) == os.path.abspath(dst):
        return
    subprocess.run(["git", "mv", "-f", src, dst], check=True)


def main(specpath):
    spec = json.load(open(specpath))
    BASE = spec["base"]
    files = {}
    for dp, dns, fns in os.walk(BASE):
        for fn in fns:
            if fn.endswith(".java"):
                files[fn[:-5]] = os.path.join(dp, fn)
    reldots = {}
    for sub, names in spec["move"].items():
        dst = os.path.join(BASE, sub)
        os.makedirs(dst, exist_ok=True)
        for n in names:
            src = files.get(n)
            if src is None:
                print("MISSING:", n)
                continue
            cur = os.path.relpath(os.path.dirname(src), ROOT).replace("/", ".")
            newp = os.path.relpath(dst, ROOT).replace("/", ".")
            if cur == newp:
                continue
            reldots[f"{cur}.{n}"] = f"{newp}.{n}"
            git_mv(src, dst, n)
    if not reldots:
        print("nothing moved")

    # package lines for whole base tree
    for dp, dns, fns in os.walk(BASE):
        pkg = "dev.loki.lovisual." + os.path.relpath(dp, ROOT).replace("/", ".")
        for fn in fns:
            if not fn.endswith(".java"):
                continue
            p = os.path.join(dp, fn)
            t = open(p).read()
            n = re.sub(r"^package [^;]*;", f"package {pkg};", t, count=1, flags=re.M)
            if n != t:
                open(p, "w").write(n)

    moved_pkg = {}
    for dp, dns, fns in os.walk(BASE):
        pkg = os.path.relpath(dp, ROOT).replace("/", ".")
        for fn in fns:
            if fn.endswith(".java"):
                moved_pkg[fn[:-5]] = pkg

    # global dupes (WorldCommandBuffer, GlyphAtlas* etc.) should not be auto-imported
    global_cls = {}
    for dp, dns, fns in os.walk(ROOT):
        for fn in fns:
            if fn.endswith(".java"):
                global_cls.setdefault(fn[:-5], set()).add(
                    os.path.relpath(dp, ROOT).replace("/", ".")
                )
    dupes = {k for k, v in global_cls.items() if len(v) > 1}

    base_pkg = "dev.loki.lovisual." + os.path.relpath(BASE, ROOT).replace("/", ".")
    WILD_STAR = f"import {base_pkg}.*;"
    WILD_LEGACY = f"import {base_pkg}.;"
    SHADOW = {
        "Font",
        "GlyphAtlasManager",
        "Scoreboard",
        "Inventory",
        "MediaPlayer",
        "Player",
        "Entity",
        "Level",
        "World",
    }
    for tree in ("src/main/java", "src/test/java"):
        for dp, dns, fns in os.walk(tree):
            my = None
            rel = os.path.relpath(dp, tree)
            if rel.startswith("dev/loki/lovisual"):
                my = rel.replace("/", ".")
            for fn in fns:
                if not fn.endswith(".java"):
                    continue
                p = os.path.join(dp, fn)
                t = open(p).read()
                o = t
                # replace longest FQNs first to avoid prefix collision (InventorySwap vs InventorySwapVisibility)
                for old, new in sorted(reldots.items(), key=lambda kv: -len(kv[0])):
                    # exact FQN replace: old must be followed by non-word or end, use regex
                    t = re.sub(
                        r"dev\.loki\.lovisual\." + re.escape(old) + r"(?=[\W])",
                        f"dev.loki.lovisual.{new}",
                        t,
                    )
                # expand star imports (both .* and legacy .%;) only if base became empty
                wild_hit = (WILD_STAR in t) or (WILD_LEGACY in t)
                if wild_hit:
                    base_still_has_top = any(
                        cp == os.path.relpath(BASE, ROOT).replace("/", ".")
                        for cp in moved_pkg.values()
                    )
                    if not base_still_has_top:
                        body = "\n".join(t.splitlines()[8:])
                        need = [
                            f"import dev.loki.lovisual.{cp}.{cls};"
                            for cls, cp in moved_pkg.items()
                            if cls not in SHADOW
                            and cls not in dupes
                            and re.search(r"(?<![\w.])%s\b" % re.escape(cls), body)
                        ]
                        repl = "".join(n + "\n" for n in sorted(need))
                        if WILD_STAR in t:
                            t = t.replace(WILD_STAR + "\n", repl)
                        if WILD_LEGACY in t:
                            t = t.replace(WILD_LEGACY + "\n", repl)
                # insert missing single-type imports for moved classes used by simple name
                body = "\n".join(t.splitlines()[8:])
                need = []
                for cls, cp in moved_pkg.items():
                    if cls in dupes or cls in SHADOW:
                        continue
                    if my is not None and cp == my:
                        continue
                    if cls == fn[:-5]:
                        continue
                    if not re.search(r"(?<![\w.])%s\b" % re.escape(cls), body):
                        continue
                    if f"import dev.loki.lovisual.{cp}.{cls};" in t:
                        continue
                    need.append(f"import dev.loki.lovisual.{cp}.{cls};")
                if need:
                    lines = t.splitlines(keepends=True)
                    imps = [i for i, l in enumerate(lines) if l.startswith("import ")]
                    idx = (
                        max(imps)
                        if imps
                        else next(
                            (i for i, l in enumerate(lines) if l.startswith("package")),
                            0,
                        )
                    )
                    lines.insert(idx + 1, "\n".join(sorted(need)) + "\n")
                    t = "".join(lines)

                # promote PRIMARY type only (name == file stem) if package-private
                def prom_primary(m):
                    if m.group(3) != fn[:-5]:
                        return m.group(0)
                    return "public " + m.group(0) if not m.group(1) else m.group(0)

                t = re.sub(
                    r"^(public\s+)?((?:(?:final|abstract|sealed|strictfp)\s+)*)(class|interface|enum|record)\s+(\w+)",
                    lambda m: (
                        m.group(0)
                        if (m.group(4) != fn[:-5] or m.group(1))
                        else "public " + m.group(2) + m.group(3) + " " + m.group(4)
                    ),
                    t,
                    count=1,
                    flags=re.M,
                )
                if t != o:
                    open(p, "w").write(t)
    print("structure done for", BASE)


if __name__ == "__main__":
    main(sys.argv[1])
