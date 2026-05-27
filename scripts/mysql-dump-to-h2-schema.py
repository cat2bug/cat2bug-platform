#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""将 MySQL Navicat 导出的 sql/cat2bug_platform.sql 转为 H2 可用的 h2-schema.sql。"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "sql" / "cat2bug_platform.sql"
TARGETS = [
    ROOT / "cat2bug-platform-admin" / "src" / "main" / "resources" / "h2-schema.sql",
    ROOT / "cat2bug-platform-admin" / "src" / "main" / "resources" / "h2.sql",
]


def convert(content: str) -> str:
    lines = content.splitlines()
    out: list[str] = ["SET FOREIGN_KEY_CHECKS = 0;"]
    skip_prefixes = (
        "SET NAMES ",
        "SET FOREIGN_KEY_CHECKS",
    )

    for line in lines:
        stripped = line.strip()
        if stripped.startswith(skip_prefixes):
            continue
        if stripped.startswith("/*") and "Navicat" in line:
            continue
        if re.search(r"\bCONSTRAINT\b.*\bFOREIGN\s+KEY\b", line, re.IGNORECASE):
            continue
        if re.search(r"\bFULLTEXT\s+KEY\b", line, re.IGNORECASE):
            continue
        # H2 索引名全局唯一，跳过普通 KEY（保留 PRIMARY/UNIQUE）
        if re.match(r"^\s+KEY\s+", line, re.IGNORECASE):
            continue

        line = re.sub(
            r"\s+CHARACTER SET utf8mb4 COLLATE utf8mb4[_a-z0-9]+",
            "",
            line,
            flags=re.IGNORECASE,
        )
        line = re.sub(r"\s+unsigned\s+zerofill", "", line, flags=re.IGNORECASE)
        line = re.sub(r"\s+USING BTREE", "", line, flags=re.IGNORECASE)
        line = re.sub(
            r"\)\s*ENGINE\s*=\s*InnoDB[^;]*;",
            ");",
            line,
            flags=re.IGNORECASE,
        )
        line = re.sub(
            r"CREATE\s+ALGORITHM\s*=\s*UNDEFINED\s+SQL\s+SECURITY\s+DEFINER\s+VIEW",
            "CREATE VIEW",
            line,
            flags=re.IGNORECASE,
        )
        out.append(line)

    if out[-1].strip() != "SET FOREIGN_KEY_CHECKS = 1;":
        out.append("SET FOREIGN_KEY_CHECKS = 1;")
    text = "\n".join(out)
    text = re.sub(r",(\s*\);)", r"\1", text)
    return text.rstrip() + "\n"


def main() -> int:
    if not SOURCE.is_file():
        print(f"源文件不存在: {SOURCE}", file=sys.stderr)
        return 1

    result = convert(SOURCE.read_text(encoding="utf-8"))
    for target in TARGETS:
        target.write_text(result, encoding="utf-8")
        print(f"已写入: {target}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
