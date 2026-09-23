#!/usr/bin/env python3
"""Depth-aware sweep: promote ALL package-private member declarations
(fields, methods, constructors, nested types) inside class bodies of a tree.
Skips private members deliberately? No — the wave policy: everything that was
package-private becomes public; existing public/private untouched.
Usage: member_sweep.py BASE"""
import os, re, sys

PRIMS = {"int","float","double","boolean","long","byte","short","char","void"}
MODS = {"public","private","protected","static","final","abstract","transient","volatile","native","strictfp","synchronized","default"}
TYPE_TOK = re.compile(r"^(?:[A-Za-z_]\w*\.)*[A-Za-z_]\w*(?:<[^>]*>)?(?:\[\])?(?:\?|extends\s+\w+)*$")

def clean(l):
    s = re.sub(r"//.*", "", l)
    s = re.sub(r'"(\\.|[^"\\])*"', '""', s)
    s = re.sub(r"'(\\.|[^'\\])'", "''", s)
    return s

def process(p):
    lines = open(p).read().splitlines(keepends=True)
    stack = []
    out = []
    changed = 0
    for l in lines:
        in_class = not stack or stack[-1] == "class"
        s = l.strip()
        ind = l[:len(l)-len(l.lstrip())]
        if in_class and s and ind and l[:4] == "    " and not s.startswith(("public","private","protected","//","*","@","}","{","import","package","else","return","if","for","while","switch","case","try","catch","finally","throw","new ","super.","this.","synchronized(")):
            m = re.match(r"^(?:(?:static|final|abstract|transient|volatile|synchronized)\s+)*(<[\w ,<>]+>\s+)?([A-Za-z_][\w.]*(?:<[^>]*>)?)(?:\[\])?\s+(\w+)\s*(\(|=|;)", s)
            ctor = re.match(r"^(\w+)\s*\(", s) and s.split("(")[0][0].isupper()
            typ_ok = False
            if m:
                typ = m.group(2)
                typ_ok = typ in PRIMS or typ[0].isupper()
            is_field = m and typ_ok and m.group(4) in ("=",";")
            is_method = m and typ_ok and m.group(4) == "(" and not s.startswith("return")
            is_ctor = bool(ctor) and ind.strip() != "" and not s.startswith(("class","record","interface","enum","public","static","final","abstract","void","new ")) and re.match(rf"^\w+\(", s) and s.split("(")[0] in [fn for fn in [os.path.basename(p)[:-5]]]
            is_type = bool(re.match(r"^(?:(?:static|final|abstract|sealed)\s+)*(class|interface|enum|record)\s+\w+", s))
            if is_field or is_method or is_ctor or is_type:
                l = ind + "public " + s + ("\n" if l.endswith("\n") else "")
                changed += 1
        out.append(l)
        c = clean(l)
        pos = 0
        while True:
            o = c.find("{", pos); cl = c.find("}", pos)
            if o == -1 and cl == -1: break
            if cl != -1 and (o == -1 or cl < o):
                stack.pop() if stack else None
                pos = cl + 1
            else:
                stack.append("class" if re.search(r"\b(class|interface|enum|record)\s+\w", c[:o]) else "other")
                pos = o + 1
    if changed:
        open(p, "w").write("".join(out))
    return changed

def main(base):
    total = 0
    for dp, dns, fns in os.walk(base):
        for fn in fns:
            if fn.endswith(".java"):
                total += process(os.path.join(dp, fn))
    print("member sweep promotions:", total)

if __name__ == "__main__":
    main(sys.argv[1])
