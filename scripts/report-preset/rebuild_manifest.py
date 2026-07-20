#!/usr/bin/env python3
"""Copy skills Markdown presets and rebuild manifest.json (Node-free fallback)."""
from __future__ import annotations

import json
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SKILLS = ROOT / "readme/development/skills/report-templates"
DEST = ROOT / "cat2bug-platform-admin/src/main/resources/报告/模版"

TINY_PNG = bytes.fromhex(
    "89504e470d0a1a0a0000000d49484452000000010000000108060000001f15c489"
    "0000000a49444154789c63000100000500010d0a2db40000000049454e44ae426082"
)

SOURCES = [
    ("29119-3", SKILLS / "29119-3", None),
    ("9001/manuals", SKILLS / "9001/manuals", "manuals"),
    ("9001/records", SKILLS / "9001/records", "records"),
]


def title_of(md: Path) -> str:
    text = md.read_text(encoding="utf-8", errors="replace")
    for line in text.splitlines():
        m = re.match(r"^#\s+(.+)$", line.strip())
        if m:
            return m.group(1).strip()
    return md.stem


def main() -> None:
    DEST.mkdir(parents=True, exist_ok=True)
    all_2919 = []
    all_9001 = []

    for rel, src_dir, key_prefix in SOURCES:
        if not src_dir.is_dir():
            raise SystemExit(f"missing skills dir: {src_dir}")
        dest_dir = DEST / rel
        dest_dir.mkdir(parents=True, exist_ok=True)
        for md in sorted(src_dir.glob("*.md")):
            if md.name == "README.md":
                continue
            shutil.copy2(md, dest_dir / md.name)
            stem = md.stem
            key = stem if not key_prefix else f"{key_prefix}/{stem}"
            content_path = f"{rel}/{md.name}"
            preview_path = f"{rel}/{stem}.png"
            png = DEST / preview_path
            if not png.exists():
                png.write_bytes(TINY_PNG)
            item = {
                "key": key,
                "title": title_of(md),
                "contentPath": content_path,
                "previewPath": preview_path,
            }
            if rel == "29119-3":
                all_2919.append(item)
            else:
                all_9001.append(item)

    manifest = {
        "groups": [
            {
                "code": "iso-29119-3",
                "title": "ISO/IEC/IEEE 29119-3:2021",
                "items": all_2919,
            },
            {"code": "iso-9001", "title": "ISO 9001", "items": all_9001},
        ]
    }
    out = DEST / "manifest.json"
    out.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(
        f"Synced {len(all_2919)} + {len(all_9001)} templates → {DEST} (manifest ok)"
    )


if __name__ == "__main__":
    main()
