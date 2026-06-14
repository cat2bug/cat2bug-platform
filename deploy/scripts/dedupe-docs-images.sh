#!/usr/bin/env bash
# 删除 readme/production/images 内 MD5 重复的 PNG，保留 canonical 路径（优先 user-guide/**）。
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
IMAGES_DIR="$ROOT/readme/production/images"
PRODUCTION_DIR="$ROOT/readme/production"
DRY_RUN=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run) DRY_RUN=1; shift ;;
    *) echo "unknown arg: $1" >&2; exit 1 ;;
  esac
done

python3 - "$IMAGES_DIR" "$PRODUCTION_DIR" "$DRY_RUN" <<'PY'
import hashlib
import sys
from pathlib import Path

images_dir = Path(sys.argv[1])
production_dir = Path(sys.argv[2])
dry_run = sys.argv[3] == "1"

groups: dict[str, list[Path]] = {}
for p in sorted(images_dir.rglob("*.png")):
    h = hashlib.md5(p.read_bytes()).hexdigest()
    groups.setdefault(h, []).append(p)

def canonical_key(p: Path) -> tuple:
    rel = p.relative_to(images_dir).as_posix()
    user_guide = 0 if rel.startswith("user-guide/") else 1
    depth = -len(rel.split("/"))
    return (user_guide, depth, rel)

deleted = []
kept = []

for h, paths in sorted(groups.items(), key=lambda x: -len(x[1])):
    if len(paths) < 2:
        continue
    paths.sort(key=canonical_key)
    keep = paths[0]
    kept.append(keep)
    for dup in paths[1:]:
        rel_dup = dup.relative_to(images_dir).as_posix()
        rel_keep = keep.relative_to(images_dir).as_posix()
        print(f"duplicate: {rel_dup} -> keep {rel_keep}")
        if not dry_run:
            dup.unlink()
            deleted.append(dup)

        # 更新 Markdown 引用：images/<dup> -> images/<keep>
        old_fragments = [
            f"images/{rel_dup}",
            f"../images/{rel_dup}",
            rel_dup,
        ]
        new_fragment = f"images/{rel_keep}"
        for md in production_dir.rglob("*.md"):
            text = md.read_text(encoding="utf-8")
            new_text = text
            for old in old_fragments:
                new_text = new_text.replace(old, new_fragment)
            if new_text != text and not dry_run:
                md.write_text(new_text, encoding="utf-8")
                print(f"  updated refs in {md.relative_to(production_dir.parent)}")

print(f"\nsummary: deleted {len(deleted)} duplicate PNG(s), kept {len(set(kept))} canonical file(s)")
if dry_run:
    print("dry-run: no files modified")
PY
