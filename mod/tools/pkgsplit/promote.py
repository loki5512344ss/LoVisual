#!/usr/bin/env python3
"""Compile-fix helper (Phase 8.5.1): promote declarations to public until
compileJava+compileTestJava are green. Brace-depth-aware class-body tracking;
statement lines are never touched. Run from project root.
Usage: python3 promote.py BASE_DIR"""
import os, re, subprocess, sys

MODS = {"public","private","protected","static","final","abstract","transient","volatile","native","strictfp"}
PRIMS = {"int","float","double","boolean","long","byte","short","char","void"}
IDENT = re.compile(r"[A-Za-z_][\w.]*")
BASE = "."


def clean_line(l):
    s = re.sub(r"//[^\n]*", "", l)
    s = re.sub(r'"(\\.|[^"\\])*"', '""', s)
    s = re.sub(r"'(\\.|[^'\\])*'", "''", s)
    return s


def class_body_flags(p):
    """(line, in_class_body) per line using a stack over '{' scopes."""
    lines = open(p).read().splitlines(keepends=True)
    stack = []
    out = []
    for l in lines:
        out.append((l, not stack or stack[-1] == "class"))
        c = clean_line(l)
        pos = 0
        while True:
            o = c.find("{", pos)
            cl = c.find("}", pos)
            if o == -1 and cl == -1:
                break
            if cl != -1 and (o == -1 or cl < o):
                if stack:
                    stack.pop()
                pos = cl + 1
            else:
                head = c[:o]
                kind = "class" if re.search(r"\b(class|interface|enum|record|@interface)\s+\w", head) else "other"
                stack.append(kind)
                pos = o + 1
    return out


def build_maps():
    typemap, cfiles = {}, {}
    for dp, dns, fns in os.walk(BASE):
        for fn in fns:
            if not fn.endswith(".java"):
                continue
            p = os.path.join(dp, fn)
            cfiles.setdefault(fn[:-5], p)
            txt = open(p).read()
            for m in re.finditer(r"^ {0,8}(?:public\s+|private\s+|protected\s+|static\s+|final\s+|abstract\s+|sealed\s+)*(class|interface|enum|record)\s+(\w+)", txt, re.M):
                typemap.setdefault(m.group(2), p)
    return typemap, cfiles


def promote(typemap, cfiles, cls, member):
    p = typemap.get(cls) or cfiles.get(cls)
    if not p:
        return False
    for i, (l, in_class) in enumerate(class_body_flags(p)):
        if not in_class:
            continue
        s = l.lstrip()
        ind = l[:len(l) - len(s)]
        if ind not in ("    ", "        "):
            continue
        if s.startswith(("public ", "protected ", "private ", "@", "//", "*", "}", "{")):
            continue
        if re.match(r"^(?:(?:public|static|final|abstract)\s+)*(class|interface|enum|record)\s+%s\b" % re.escape(member), s):
            lines = open(p).read().splitlines(keepends=True)
            lines[i] = ind + "public " + s + "\n"
            open(p, "w").write("".join(lines))
            return True
        # constructor of a nested class named `member`
        if re.match(r"^%s\s*\(" % re.escape(member), s):
            lines = open(p).read().splitlines(keepends=True)
            lines[i] = ind + "public " + s + "\n"
            open(p, "w").write("".join(lines))
            return True
        m = re.search(r"(?<![\w.])%s\b\s*(\(|=|;|,)" % re.escape(member), s)
        if not m or not s[:m.start()].strip():
            continue
        toks = s[:m.start()].replace("<", " < ").replace(">", " > ").replace(",", " , ").split()
        ok = bool(toks)
        for t in toks:
            b = IDENT.match(t)
            core = b.group(0) if b else ""
            if core in MODS or core in PRIMS or (core and core[0].isupper()) or t in ("<", ">", ",", "?", "extends"):
                continue
            ok = False
            break
        if not ok:
            continue
        lines = open(p).read().splitlines(keepends=True)
        if s.startswith("private "):
            s = s[len("private "):]
        lines[i] = ind + "public " + s + ("\n" if l.endswith("\n") else "")
        open(p, "w").write("".join(lines))
        return True
    return False


def main():
    global BASE
    BASE = sys.argv[1] if len(sys.argv) > 1 else BASE
    typemap, cfiles = build_maps()
    for it in range(150):
        r = subprocess.run(["./gradlew", "compileJava", "compileTestJava", "--offline", "-q"],
                           capture_output=True, text=True)
        err = r.stderr + r.stdout
        if r.returncode == 0:
            print("COMPILE GREEN after", it, "iterations")
            return 0
        pend = set()
        for msg, c in re.findall(r"error: (.*?) is not public in (\w+);", err):
            tok = re.sub(r"<N>", "", msg).split("(")[0].split(".")[-1].strip()
            if tok:
                pend.add((tok, c))
        for msg, c in re.findall(r"reason:\s+(?:<N>)?([\w.]+)\(.*?\) is not public in (\w+);", err):
            pend.add((msg.split(".")[-1], c))
        for m2, c2 in re.findall(r"method ([\w]+) in class (\w+) cannot be accessed", err):
            pend.add((m2, c2))
        for m2, c2 in re.findall(r"([A-Z]\w+)\.[\w.]* is defined in an inaccessible class", err):
            pass
        for cls_name in re.findall(r"([A-Z]\w*) is defined in an inaccessible class or interface", err):
            pend.add((cls_name, cls_name))
        done = 0
        for mem, cls in sorted(pend):
            if promote(typemap, cfiles, cls, mem):
                done += 1
        typemap, cfiles = build_maps()
        if done == 0:
            print("STUCK at iter", it)
            seen, uniq = set(), []
            for line in err.splitlines():
                if "error:" in line and line not in seen:
                    seen.add(line)
                    uniq.append(line)
            for u in uniq[:30]:
                print("  ", u.replace("/storage/project/jvm/LoVisual/combatant-client-26.2/", "")[:170])
            return 1
        print(f"iter {it}: promoted {done}/{len(pend)}")
    return 1


if __name__ == "__main__":
    sys.exit(main())
