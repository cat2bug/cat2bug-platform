#!/usr/bin/env python3
"""Render readable card previews for preset report templates (Pillow)."""
from __future__ import annotations

import json
import re
import textwrap
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
DEST = ROOT / "cat2bug-platform-admin/src/main/resources/报告/模版"
MANIFEST = DEST / "manifest.json"

W, H = 480, 360
# 浅色预览：卡片在深色 UI 中也要能看清正文
BG = (236, 238, 242)
CARD = (255, 255, 255)
BORDER = (210, 214, 220)
TITLE = (28, 32, 40)
BODY = (55, 62, 72)
ACCENT = (212, 148, 18)
MUTED = (120, 128, 138)


def load_font(size: int, bold: bool = False) -> ImageFont.ImageFont:
    candidates = [
        "/System/Library/Fonts/PingFang.ttc",
        "/System/Library/Fonts/Hiragino Sans GB.ttc",
        "/System/Library/Fonts/STHeiti Light.ttc",
        "/Library/Fonts/Arial Unicode.ttf",
        "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
        "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
        "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
    ]
    for path in candidates:
        p = Path(path)
        if not p.exists():
            continue
        try:
            return ImageFont.truetype(str(p), size=size, index=0)
        except OSError:
            try:
                return ImageFont.truetype(str(p), size=size)
            except OSError:
                continue
    return ImageFont.load_default()


def excerpt_from_md(md_path: Path, max_lines: int = 12) -> list[str]:
    text = md_path.read_text(encoding="utf-8", errors="replace")
    lines: list[str] = []
    for raw in text.splitlines():
        line = raw.strip()
        if not line:
            if lines and lines[-1] != "":
                lines.append("")
            continue
        if line.startswith("<!--") or line.startswith("-->"):
            continue
        if line.startswith("$") or line.startswith("|---") or line.startswith("| --"):
            continue
        if line.startswith("|"):
            # keep a compact table hint
            cells = [c.strip() for c in line.strip("|").split("|") if c.strip()]
            if cells:
                line = " · ".join(cells[:4])
            else:
                continue
        line = re.sub(r"^#+\s*", "", line)
        line = re.sub(r"\*\*|__", "", line)
        line = re.sub(r"`([^`]+)`", r"\1", line)
        line = re.sub(r"\$\{[^}]+\}", "…", line)
        if not line or line.startswith("参考文档"):
            continue
        lines.append(line)
        if sum(1 for x in lines if x) >= max_lines:
            break
    # drop trailing blanks
    while lines and not lines[-1]:
        lines.pop()
    return lines


def wrap_lines(draw: ImageDraw.ImageDraw, font: ImageFont.ImageFont, text: str, max_width: int) -> list[str]:
    # approximate wrap by characters for CJK
    out: list[str] = []
    buf = ""
    for ch in text:
        trial = buf + ch
        if draw.textlength(trial, font=font) <= max_width:
            buf = trial
        else:
            if buf:
                out.append(buf)
            buf = ch
    if buf:
        out.append(buf)
    return out or [""]


def render_card(title: str, lines: list[str], out_path: Path) -> None:
    img = Image.new("RGB", (W, H), BG)
    draw = ImageDraw.Draw(img)
    margin = 18
    draw.rounded_rectangle(
        (margin, margin, W - margin, H - margin),
        radius=10,
        fill=CARD,
        outline=BORDER,
        width=1,
    )
    title_font = load_font(20, bold=True)
    body_font = load_font(13)
    badge_font = load_font(11)

    x0, y0 = margin + 16, margin + 14
    draw.rectangle((x0, y0 + 4, x0 + 4, y0 + 18), fill=ACCENT)

    title_lines = wrap_lines(draw, title_font, title, W - margin * 2 - 40)
    ty = y0
    for i, tl in enumerate(title_lines[:2]):
        draw.text((x0 + 12, ty), tl, font=title_font, fill=TITLE)
        ty += 26
    ty += 6
    draw.line((x0, ty, W - margin - 16, ty), fill=BORDER, width=1)
    ty += 10

    max_y = H - margin - 28
    for raw in lines:
        if not raw:
            ty += 8
            continue
        for wl in wrap_lines(draw, body_font, raw, W - margin * 2 - 32):
            if ty + 18 > max_y:
                draw.text((x0, max_y - 2), "…", font=body_font, fill=MUTED)
                out_path.parent.mkdir(parents=True, exist_ok=True)
                img.save(out_path, format="PNG", optimize=True)
                return
            draw.text((x0, ty), wl, font=body_font, fill=BODY)
            ty += 18

    draw.text((x0, H - margin - 18), "Cat2Bug 报告模版预览", font=badge_font, fill=MUTED)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    img.save(out_path, format="PNG", optimize=True)


def main() -> None:
    if not MANIFEST.exists():
        raise SystemExit(f"missing manifest: {MANIFEST}")
    data = json.loads(MANIFEST.read_text(encoding="utf-8"))
    count = 0
    for group in data.get("groups", []):
        for item in group.get("items", []):
            content = DEST / item["contentPath"]
            preview = DEST / item["previewPath"]
            if not content.exists():
                print("skip missing", content)
                continue
            title = item.get("title") or content.stem
            lines = excerpt_from_md(content)
            render_card(title, lines, preview)
            count += 1
            print("ok", preview.relative_to(DEST), preview.stat().st_size)
    print(f"rendered {count} preview(s)")


if __name__ == "__main__":
    main()
