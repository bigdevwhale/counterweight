"""Writes the launcher icon vectors: a brass balance on green baize, the weight's pan down, the stone's up.

    python tools/icon.py
then `./gradlew testDebugUnitTest -Prender --tests '*RenderArtIcon*'` draws docs/logo.png from them.
"""
import math
from pathlib import Path

DRAW = Path(__file__).resolve().parent.parent / "app/src/main/res/drawable"
HEAD = ('<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="108dp"\n    android:height="108dp"\n    android:viewportWidth="108"\n    android:viewportHeight="108">\n')


def circle(cx, cy, r):
    return f"M{cx - r:.2f},{cy:.2f}A{r},{r} 0,1 1,{cx + r:.2f},{cy:.2f}A{r},{r} 0,1 1,{cx - r:.2f},{cy:.2f}Z"


def poly(pts):
    return "M" + "L".join(f"{x:.2f},{y:.2f}" for x, y in pts) + "Z"


def bowl(cx, cy, r, d):
    return f"M{cx - r:.2f},{cy:.2f}L{cx + r:.2f},{cy:.2f}A{r},{d} 0,0 1,{cx - r:.2f},{cy:.2f}Z"


def stone(cx, cy, r):
    pts = []
    for i, k in enumerate([1, .86, 1.05, .9, 1.08, .84, 1, .92, 1.04]):
        a = i * 2 * math.pi / 9
        pts.append((cx + math.cos(a) * r * k, cy + math.sin(a) * r * k * .82))
    return poly(pts)


def main():
    px, py, arm, drop, tilt = 54, 36, 23, 20, math.radians(7)
    left = (px - arm * math.cos(tilt), py - arm * math.sin(tilt))
    right = (px + arm * math.cos(tilt), py + arm * math.sin(tilt))
    lp, rp = (left[0], left[1] + drop), (right[0], right[1] + drop)
    n = (math.sin(tilt) * 1.6, -math.cos(tilt) * 1.6)
    beam = poly([(left[0] + n[0], left[1] + n[1]), (right[0] + n[0], right[1] + n[1]), (right[0] - n[0], right[1] - n[1]), (left[0] - n[0], left[1] - n[1])])
    p = [
        ("#FF85631F", f"M52.2,36h3.6v39h-3.6z"),
        ("#FFF2DA92", f"M53,36h1v39h-1z"),
        ("#FF5A3A22", poly([(40, 80), (68, 80), (64, 75), (44, 75)])),
        ("#FFC9A04A", "M45,73h18v2.4h-18z"),
        ("#FFC9A04A", beam),
        ("#FF85631F", circle(px, py, 4.2)),
        ("#FFF2DA92", circle(px - .8, py - .8, 2.2)),
        ("#FFC9A04A", circle(px, py - 9, 2.4)),
    ]
    chains = []
    for (ex, ey), (cx, cy) in ((left, lp), (right, rp)):
        chains.append(f"M{ex:.2f},{ey:.2f}L{cx - 10.5:.2f},{cy:.2f}M{ex:.2f},{ey:.2f}L{cx + 10.5:.2f},{cy:.2f}")
    body = [f'    <path android:strokeColor="#FFC9A04A" android:strokeWidth="1.1" android:pathData="{"".join(chains)}" />']
    body += [f'    <path android:fillColor="{c}" android:pathData="{d}" />' for c, d in p]
    # The stone rides high on the left, outweighed; a brass weight sits low on the right.
    body.append(f'    <path android:fillColor="#FF6F6A64" android:pathData="{stone(lp[0], lp[1] - 4.6, 6.4)}" />')
    body.append(f'    <path android:fillColor="#FF9C968E" android:pathData="{stone(lp[0] - 1.6, lp[1] - 6.2, 2.6)}" />')
    wx, wy = rp
    body.append(f'    <path android:fillColor="#FFC9A04A" android:pathData="M{wx - 4:.2f},{wy - 7.5:.2f}h8v7.5h-8z" />')
    body.append(f'    <path android:fillColor="#FFF2DA92" android:pathData="M{wx - 2.6:.2f},{wy - 7.5:.2f}h1.6v7.5h-1.6z" />')
    body.append(f'    <path android:fillColor="#FFC9A04A" android:pathData="M{wx - 1:.2f},{wy - 10:.2f}h2v2.6h-2z{circle(wx, wy - 11, 2)}" />')
    for cx, cy in (lp, rp):
        body.append(f'    <path android:fillColor="#FFC9A04A" android:pathData="{bowl(cx, cy, 11, 4.6)}" />')
        body.append(f'    <path android:fillColor="#FF85631F" android:pathData="M{cx - 11:.2f},{cy - .6:.2f}h22v1.2h-22z" />')
    (DRAW / "ic_launcher_foreground.xml").write_text(HEAD + "\n".join(body) + "\n</vector>\n", encoding="utf-8", newline="\n")
    (DRAW / "ic_launcher_background.xml").write_text(HEAD + '    <path android:fillColor="#FF1E3A2C" android:pathData="M0,0h108v108h-108z" />\n</vector>\n', encoding="utf-8", newline="\n")


if __name__ == "__main__":
    main()
