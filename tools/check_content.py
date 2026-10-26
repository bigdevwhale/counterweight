"""Checks the catalogue in app/src/main/assets/content against docs/CONTENT.md.

    python tools/check_content.py            # every file
    python tools/check_content.py money law  # some spheres

Prints problems per file and a count; exits 1 if any file has errors.
"""
import re
import sys
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DIR = ROOT / "app/src/main/assets/content"
KINDS = {"FACT", "AFTER", "POWER", "BRIGHT", "QUESTION", "VOICE", "FEATHER", "TRUTH"}
NEED = {"AFTER": 2, "POWER": 2, "BRIGHT": 2, "QUESTION": 2, "VOICE": 1, "FEATHER": 1, "FACT": 1}
KEY = re.compile(r"^[a-z0-9_]+$")
CRISIS = re.compile(r"суицид|покончить с собой|самоубий|suicid|kill myself", re.I)


def braces_ok(s):
    depth = 0
    for ch in s:
        if ch == "{":
            depth += 1
            if depth > 1:
                return False
        elif ch == "}":
            depth -= 1
            if depth < 0:
                return False
    return depth == 0


def check(path, keys, strict):
    errs, warns = [], []
    sphere = path.stem
    worries, weights, wide = 0, 0, 0
    cur, kinds, heavy = None, Counter(), False
    seen_text = set()

    def close():
        if cur is None or not strict:
            return
        n = sum(kinds.values())
        if not 12 <= n <= 18:
            warns.append(f"{cur}: {n} weights")
        for k, m in NEED.items():
            if kinds[k] < m:
                warns.append(f"{cur}: {k} {kinds[k]}<{m}")
        if heavy and kinds["TRUTH"] < 1:
            errs.append(f"{cur}: HEAVY without TRUTH")
        if not heavy and kinds["TRUTH"]:
            errs.append(f"{cur}: TRUTH on a light worry")

    for no, line in enumerate(path.read_text(encoding="utf-8").split("\n"), 1):
        if not line.strip() or line.startswith("#"):
            continue
        c = line.split("\t")
        where = f"{path.name}:{no}"
        if c[0] == "W":
            close()
            if len(c) != 7:
                errs.append(f"{where}: W needs 7 columns, has {len(c)}")
                cur = None
                continue
            _, key, ru, en, kwr, kwe, flags = c
            if not KEY.match(key) or not key.startswith(sphere + "_"):
                errs.append(f"{where}: bad key {key}")
            if key in keys:
                errs.append(f"{where}: duplicate key {key}")
            keys.add(key)
            if len(ru) > 80 or len(en) > 90:
                warns.append(f"{where}: worry too long")
            if flags not in ("-", "HEAVY"):
                errs.append(f"{where}: flags {flags}")
            if len([k for k in kwr.split(",") if k.strip()]) < 3 or len([k for k in kwe.split(",") if k.strip()]) < 3:
                warns.append(f"{where}: few keywords")
            if CRISIS.search(ru + en):
                errs.append(f"{where}: crisis topic")
            cur, kinds, heavy = key, Counter(), flags == "HEAVY"
            worries += 1
        elif c[0] in ("G", "@"):
            if len(c) != 5:
                errs.append(f"{where}: {c[0]} needs 5 columns, has {len(c)}")
                continue
            _, kind, ru, en, src = c
            if kind not in KINDS:
                errs.append(f"{where}: kind {kind}")
            if not ru.strip() or not en.strip():
                errs.append(f"{where}: empty text")
            if not braces_ok(ru) or "{" in en:
                errs.append(f"{where}: braces")
            if kind in ("FACT", "VOICE") and src.strip() in ("", "-"):
                errs.append(f"{where}: {kind} without source")
            if kind == "AFTER" and "→" not in ru:
                warns.append(f"{where}: AFTER without steps")
            if len(ru) > 260:
                warns.append(f"{where}: long ({len(ru)})")
            if ru in seen_text:
                errs.append(f"{where}: repeated text")
            seen_text.add(ru)
            if c[0] == "@":
                wide += 1
            elif cur is None:
                errs.append(f"{where}: G before any W")
            else:
                kinds[kind] += 1
                weights += 1
        else:
            errs.append(f"{where}: unknown line type {c[0]!r}")
    close()
    return errs, warns, worries, weights, wide


def main(args):
    files = sorted(DIR.glob("*.tsv"))
    if args:
        files = [f for f in files if f.stem in args]
    keys, bad, tw, tg, ta = set(), False, 0, 0, 0
    for f in files:
        strict = f.stem not in ("universal", "night")
        errs, warns, w, g, a = check(f, keys, strict)
        tw, tg, ta = tw + w, tg + g, ta + a
        print(f"{f.stem:10} worries {w:4}  weights {g:5}  sphere-wide {a:3}  errors {len(errs)}  warnings {len(warns)}")
        for e in errs[:30]:
            print("   E", e)
        for e in warns[:12]:
            print("   w", e)
        bad |= bool(errs)
    print(f"TOTAL worries {tw}, weights {tg + ta}")
    sys.exit(1 if bad else 0)


if __name__ == "__main__":
    main(sys.argv[1:])
