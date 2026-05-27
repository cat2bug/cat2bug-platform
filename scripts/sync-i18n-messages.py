#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
以 messages.properties（简体中文）为基准，同步各语言 messages_*.properties：
- 键集合、顺序与基准一致
- 保留各语言已有译文；缺失时回退 en_US，再回退中文
- 修复「多键合并一行」等损坏行
"""

from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
I18N_DIR = ROOT / "cat2bug-platform-admin" / "src" / "main" / "resources" / "i18n"
BASE_FILE = I18N_DIR / "messages.properties"
EN_FILE = I18N_DIR / "messages_en_US.properties"

# 与前端 i18n.js、LocaleUtils.MESSAGE_BUNDLE_LOCALES 一致
LOCALE_FILES = {
    "zh_TW": I18N_DIR / "messages_zh_TW.properties",
    "en_US": EN_FILE,
    "ar": I18N_DIR / "messages_ar.properties",
    "ja_JP": I18N_DIR / "messages_ja_JP.properties",
    "ko_KR": I18N_DIR / "messages_ko_KR.properties",
    "ru": I18N_DIR / "messages_ru.properties",
}

KEY_VALUE_RE = re.compile(r"([A-Za-z][\w.-]*)=(.*)")

# 历史错误键名 -> 基准键名（取值迁移）
KEY_ALIASES = {
    "dashboard.defect-state.day": "dashboard.defect-line.day",
    "dashboard.defect-state.month": "dashboard.defect-line.month",
}

# 各语言对新增/曾损坏键的审定译文（优先于 en/中文回退）
LOCALE_OVERRIDES: dict[str, dict[str, str]] = {
    "messages_zh_TW.properties": {
        "defect.deleted_cannot_edit": "缺陷已刪除，無法編輯",
        "defect.not_deleted": "缺陷未處於已刪除狀態",
        "defect.severity-rate": "缺陷嚴重率",
        "title": "標題",
    },
    "messages_ar.properties": {
        "defect.deleted_cannot_edit": "تم حذف العيب ولا يمكن تحريره",
        "defect.not_deleted": "العيب ليس في حالة محذوفة",
        "defect.severity-rate": "نسبة خطورة العيوب",
        "title": "العنوان",
    },
    "messages_ja_JP.properties": {
        "defect.deleted_cannot_edit": "欠陥は削除済みのため編集できません",
        "defect.not_deleted": "欠陥は削除状態ではありません",
        "defect.severity-rate": "欠陥重症率",
        "title": "タイトル",
    },
    "messages_ko_KR.properties": {
        "defect.deleted_cannot_edit": "결함이 삭제되어 편집할 수 없습니다",
        "defect.not_deleted": "결함이 삭제 상태가 아닙니다",
        "defect.severity-rate": "결함 심각도 비율",
        "title": "제목",
    },
    "messages_ru.properties": {
        "defect.deleted_cannot_edit": "Дефект удалён и не может быть отредактирован",
        "defect.not_deleted": "Дефект не находится в удалённом состоянии",
        "defect.severity-rate": "Коэффициент серьёзности дефектов",
        "title": "Заголовок",
    },
}


def parse_entries(path: Path) -> dict[str, str]:
    """宽松解析 properties，支持一行多键。"""
    if not path.is_file():
        return {}
    text = path.read_text(encoding="utf-8")
    result: dict[str, str] = {}
    for line in text.splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or stripped.startswith("!"):
            continue
        pos = 0
        while pos < len(line):
            m = KEY_VALUE_RE.search(line, pos)
            if not m:
                break
            key = m.group(1).strip()
            val = m.group(2)
            next_key = KEY_VALUE_RE.search(val)
            if next_key:
                val = val[: next_key.start()]
            result[key] = val.rstrip()
            pos = m.end()
    # 别名迁移
    for old, new in KEY_ALIASES.items():
        if old in result and new not in result:
            result[new] = result[old]
    return result


def load_base_structure(path: Path) -> list[tuple[str, str | None]]:
    """返回 [(type, content)]，type 为 'comment' | 'kv'，kv 时 content 为 key。"""
    structure: list[tuple[str, str | None]] = []
    for line in path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            structure.append(("comment", line))
            continue
        m = re.match(r"^([^=:#\s][^=]*)=(.*)$", line)
        if m:
            structure.append(("kv", m.group(1).strip()))
        else:
            structure.append(("comment", line))
    return structure


def resolve_value(
    key: str,
    locale_map: dict[str, str],
    en_map: dict[str, str],
    base_map: dict[str, str],
    overrides: dict[str, str] | None = None,
) -> str:
    if overrides and key in overrides:
        return overrides[key]
    if key in locale_map and locale_map[key] != "":
        return locale_map[key]
    if key in en_map and en_map[key] != "":
        return en_map[key]
    return base_map.get(key, "")


def write_locale_file(
    path: Path,
    structure: list[tuple[str, str | None]],
    locale_map: dict[str, str],
    en_map: dict[str, str],
    base_map: dict[str, str],
) -> None:
    overrides = LOCALE_OVERRIDES.get(path.name, {})
    lines: list[str] = []
    for kind, content in structure:
        if kind == "comment":
            lines.append(content if content is not None else "")
            continue
        key = content
        value = resolve_value(key, locale_map, en_map, base_map, overrides)
        lines.append(f"{key}={value}")
    path.write_text("\n".join(lines).rstrip() + "\n", encoding="utf-8")


def validate_no_merged_lines(path: Path) -> list[str]:
    """检测一行内多个 key= 的损坏行（会导致 Excel 表头显示为「類型title=標題」）。"""
    errors: list[str] = []
    for i, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        matches = list(KEY_VALUE_RE.finditer(line))
        if len(matches) > 1:
            errors.append(f"{path.name}:{i} 合并行: {stripped[:80]}")
    return errors


def main() -> int:
    if not BASE_FILE.is_file():
        print(f"基准文件不存在: {BASE_FILE}")
        return 1

    structure = load_base_structure(BASE_FILE)
    base_keys = [c for t, c in structure if t == "kv"]
    base_map = parse_entries(BASE_FILE)
    en_map = parse_entries(EN_FILE)

    print(f"基准键数量: {len(base_keys)}")

    all_errors: list[str] = []
    for name, path in LOCALE_FILES.items():
        old_map = parse_entries(path)
        write_locale_file(path, structure, old_map, en_map, base_map)
        new_map = parse_entries(path)
        missing = [k for k in base_keys if k not in new_map]
        extra = [k for k in new_map if k not in base_map]
        print(f"  {path.name}: {len(new_map)} keys", end="")
        if missing:
            print(f"  MISSING={missing}", end="")
        if extra:
            print(f"  EXTRA={extra}", end="")
        print()
        all_errors.extend(validate_no_merged_lines(path))

    all_errors.extend(validate_no_merged_lines(BASE_FILE))
    if all_errors:
        print("校验失败，存在合并行：")
        for e in all_errors:
            print(f"  {e}")
        return 1
    print("同步完成，校验通过。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
