#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 4 种不同风格的 Cat2Bug 系统架构图（仅 PNG）。"""
from __future__ import annotations

import subprocess
from pathlib import Path

OUT_DIR = Path(__file__).parent
W, H = 1920, 1080

# ── 共享内容 ──────────────────────────────────────────────
ACCESS = [
    ("Vue SPA", "Element UI"),
    ("WebSocket", "实时消息"),
    ("Open API", "/api/**"),
    ("Chrome 插件", "浏览器扩展"),
    ("MCP 服务", "Agent 协议"),
    ("移动端", "uni-app"),
    ("钉钉/企微/飞书", "IM 机器人"),
    ("安装向导", "Setup"),
    ("升级向导", "Upgrade"),
]
INPUTS = ["缺陷录入", "Excel 导入", "用例/计划", "API Key", "JUnit/JLog", "IM 回调", "附件上传"]
GATEWAY = ["JWT 认证", "API Key", "@PreAuthorize", "数据权限", "接口限流", "CORS/验证码", "操作审计"]
CORE = [
    ("组织层", "团队·项目·成员·角色"),
    ("缺陷管理", "CRUD·流转·统计"),
    ("测试管理", "用例·计划·报告"),
    ("AI 辅助", "生成·优化·Prompt"),
    ("集成工具", "API·IM·Quartz"),
]
OUTPUTS = ["钉钉/企微/飞书", "邮件/系统通知", "WebSocket", "Excel/PDF", "API 回调", "统计导出", "分享链接"]
INFRA = [
    ("Spring Boot 3", "GraalVM Native"),
    ("MyBatis", "Druid 连接池"),
    ("H2 / MySQL", "Flyway"),
    ("J2Cache", "Caffeine+Redis"),
    ("Vue 2.7 SPA", "Embedded"),
    ("Docker", "native-minimal"),
    ("Maven", "多模块"),
    ("FastExcel", "导入导出"),
    ("OpenSpec", "规格驱动"),
    ("Quartz", "定时任务"),
]
LOOPS = {
    "defect": "上报 → 分派 → 修复 → 验证 → 关闭",
    "test": "用例 → 计划 → 执行 → 报告",
    "safety": ["双重 Security", "DataScope", "操作审计"],
}


def esc(s: str) -> str:
    return s.replace("&", "&amp;").replace("<", "&lt;")


def svg_to_png(svg: str, path: Path) -> None:
    tmp = path.with_suffix(".svg")
    tmp.write_text(svg, encoding="utf-8")
    subprocess.run(
        ["rsvg-convert", "-w", str(W), "--background-color", "white", str(tmp), "-o", str(path)],
        check=True,
    )
    tmp.unlink(missing_ok=True)


def shadow_filter(fid: str = "sh") -> str:
    return f'''<filter id="{fid}" x="-4%" y="-4%" width="108%" height="112%">
  <feDropShadow dx="0" dy="2" stdDeviation="4" flood-color="#000" flood-opacity="0.08"/>
</filter>'''


def dark_shadow_filter(fid: str = "sh") -> str:
    return f'''<filter id="{fid}" x="-4%" y="-4%" width="108%" height="112%">
  <feDropShadow dx="0" dy="4" stdDeviation="8" flood-color="#000" flood-opacity="0.45"/>
</filter>'''


# ═══════════════════════════════════════════════════════════
# Style 1 — Corporate Enterprise（企业蓝，左色条卡片）
# ═══════════════════════════════════════════════════════════
def style_corporate() -> str:
    M, G = 48, 16
    iw = W - 2 * M
    parts = [
        f'''<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}">
<defs>
  {shadow_filter()}
  <linearGradient id="hdr" x1="0" y1="0" x2="1" y2="0">
    <stop offset="0%" stop-color="#1e3a5f"/><stop offset="100%" stop-color="#2563eb"/>
  </linearGradient>
  <style>
    .t1{{font:700 22px "PingFang SC","Helvetica Neue",sans-serif;fill:#fff}}
    .t2{{font:600 14px "PingFang SC","Helvetica Neue",sans-serif;fill:#1e293b}}
    .t3{{font:500 12px "PingFang SC","Helvetica Neue",sans-serif;fill:#475569}}
    .t4{{font:400 11px "PingFang SC","Helvetica Neue",sans-serif;fill:#64748b}}
    .sec{{font:700 13px "PingFang SC","Helvetica Neue",sans-serif;fill:#1e40af;letter-spacing:.5px}}
    .en{{font:500 10px "Helvetica Neue",sans-serif;fill:#94a3b8}}
  </style>
  <marker id="ar" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto">
    <path d="M0,0 L8,4 L0,8 Z" fill="#3b82f6"/>
  </marker>
</defs>
<rect width="{W}" height="{H}" fill="#f1f5f9"/>
<rect x="0" y="0" width="{W}" height="72" fill="url(#hdr)"/>
<text x="{M}" y="46" class="t1">Cat2Bug Platform</text>
<text x="{W-M}" y="46" text-anchor="end" class="t4" fill="#bfdbfe">System Architecture</text>
'''
    ]

    # 访问层
    ay, ah = 88, 118
    parts.append(f'<rect x="{M}" y="{ay}" width="{iw}" height="{ah}" rx="10" fill="#fff" filter="url(#sh)"/>')
    parts.append(f'<rect x="{M}" y="{ay}" width="6" height="{ah}" rx="3" fill="#2563eb"/>')
    parts.append(f'<text x="{M+20}" y="{ay+28}" class="sec">访问层</text><text x="{M+76}" y="{ay+28}" class="en">ACCESS</text>')
    cols, rows = 5, 2
    cw = (iw - 40 - G * (cols - 1)) / cols
    ch = (ah - 50 - G) / rows
    for i, (t, s) in enumerate(ACCESS):
        r, c = divmod(i, cols)
        x = M + 20 + c * (cw + G)
        y = ay + 42 + r * (ch + G)
        parts.append(f'<rect x="{x:.0f}" y="{y:.0f}" width="{cw:.0f}" height="{ch:.0f}" rx="6" fill="#eff6ff" stroke="#bfdbfe"/>')
        parts.append(f'<text x="{x+10:.0f}" y="{y+ch/2-2:.0f}" class="t2">{esc(t)}</text>')
        parts.append(f'<text x="{x+10:.0f}" y="{y+ch/2+14:.0f}" class="t4">{esc(s)}</text>')

    # 主区域（四列 + 右侧栏，满宽对齐无重叠）
    my, mh = ay + ah + G, 520
    rw = 268
    w_col = 186
    w_core = iw - rw - 4 * w_col - 3 * G
    xs = [M, M + w_col + G, M + 2 * (w_col + G), M + 2 * (w_col + G) + w_core + G]
    sections = [
        ("#16a34a", "数据输入", "INPUT", INPUTS),
        ("#9333ea", "网关安全", "GATEWAY", GATEWAY),
        ("#2563eb", "核心引擎", "CORE", [f"{a} — {b}" for a, b in CORE]),
        ("#ea580c", "输出通知", "OUTPUT", OUTPUTS),
    ]
    for si, (color, zh, en, items) in enumerate(sections):
        x = xs[si]
        w = w_core if si == 2 else w_col
        parts.append(f'<rect x="{x:.0f}" y="{my}" width="{w:.0f}" height="{mh}" rx="10" fill="#fff" filter="url(#sh)"/>')
        parts.append(f'<rect x="{x:.0f}" y="{my}" width="{w:.0f}" height="36" rx="10" fill="{color}" opacity=".12"/>')
        parts.append(f'<rect x="{x:.0f}" y="{my}" width="5" height="{mh}" rx="2" fill="{color}"/>')
        parts.append(f'<text x="{x+16:.0f}" y="{my+24}" class="sec" fill="{color}">{zh}</text>')
        parts.append(f'<text x="{x+16+w-8:.0f}" y="{my+24}" text-anchor="end" class="en">{en}</text>')
        ih = (mh - 52 - G * (len(items) - 1)) / len(items)
        for j, item in enumerate(items):
            yy = my + 48 + j * (ih + G)
            parts.append(f'<rect x="{x+12:.0f}" y="{yy:.0f}" width="{w-24:.0f}" height="{ih:.0f}" rx="5" fill="#f8fafc" stroke="#e2e8f0"/>')
            parts.append(f'<circle cx="{x+24:.0f}" cy="{yy+ih/2:.0f}" r="3" fill="{color}"/>')
            parts.append(f'<text x="{x+34:.0f}" y="{yy+ih/2+4:.0f}" class="t3">{esc(item)}</text>')
        if si < 3:
            ax = x + w
            parts.append(f'<path d="M{ax:.0f} {my+mh/2:.0f} L{ax+G:.0f} {my+mh/2:.0f}" stroke="#3b82f6" stroke-width="2" marker-end="url(#ar)"/>')

    # 右侧信息
    rx = M + iw - rw
    for i, (zh, txt) in enumerate([("缺陷闭环", LOOPS["defect"]), ("测试闭环", LOOPS["test"])]):
        yy = my + i * (mh / 2 + G / 2)
        rh = mh / 2 - G / 2
        parts.append(f'<rect x="{rx:.0f}" y="{yy:.0f}" width="{rw:.0f}" height="{rh:.0f}" rx="10" fill="#fff" filter="url(#sh)"/>')
        parts.append(f'<text x="{rx+16:.0f}" y="{yy+26:.0f}" class="sec" fill="#475569">{zh}</text>')
        parts.append(f'<text x="{rx+16:.0f}" y="{yy+rh/2+6:.0f}" class="t3">{esc(txt)}</text>')

    # 基础设施
    fy = my + mh + G
    fh = H - fy - M
    parts.append(f'<rect x="{M}" y="{fy}" width="{iw}" height="{fh}" rx="10" fill="#fff" filter="url(#sh)"/>')
    parts.append(f'<rect x="{M}" y="{fy}" width="6" height="{fh}" rx="3" fill="#475569"/>')
    parts.append(f'<text x="{M+20}" y="{fy+28}" class="sec" fill="#475569">基础设施</text><text x="{M+90}" y="{fy+28}" class="en">FOUNDATION</text>')
    cols = 5
    cw = (iw - 40 - G * (cols - 1)) / cols
    ch = (fh - 50 - G) / 2
    for i, (t, s) in enumerate(INFRA):
        r, c = divmod(i, cols)
        x = M + 20 + c * (cw + G)
        y = fy + 42 + r * (ch + G)
        parts.append(f'<rect x="{x:.0f}" y="{y:.0f}" width="{cw:.0f}" height="{ch:.0f}" rx="6" fill="#f8fafc" stroke="#e2e8f0"/>')
        parts.append(f'<text x="{x+cw/2:.0f}" y="{y+ch/2-2:.0f}" text-anchor="middle" class="t2">{esc(t)}</text>')
        parts.append(f'<text x="{x+cw/2:.0f}" y="{y+ch/2+14:.0f}" text-anchor="middle" class="t4">{esc(s)}</text>')

    parts.append("</svg>")
    return "".join(parts)


# ═══════════════════════════════════════════════════════════
# Style 2 — Minimal Nordic（极简北欧，大留白细线框）
# ═══════════════════════════════════════════════════════════
def style_minimal() -> str:
    M, G = 64, 20
    iw = W - 2 * M
    accent = "#0d9488"
    parts = [
        f'''<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}">
<defs>
  <marker id="ar" markerWidth="6" markerHeight="6" refX="5" refY="3" orient="auto">
    <path d="M0,0 L6,3 L0,6 Z" fill="#cbd5e1"/>
  </marker>
  <style>
    .h{{font:300 32px "Helvetica Neue","PingFang SC",sans-serif;fill:#0f172a;letter-spacing:-.5px}}
    .sub{{font:400 13px "Helvetica Neue",sans-serif;fill:#94a3b8;letter-spacing:2px;text-transform:uppercase}}
    .lbl{{font:500 13px "PingFang SC","Helvetica Neue",sans-serif;fill:#334155}}
    .sm{{font:400 11px "PingFang SC","Helvetica Neue",sans-serif;fill:#94a3b8}}
    .sec{{font:600 11px "Helvetica Neue",sans-serif;fill:{accent};letter-spacing:1.5px;text-transform:uppercase}}
  </style>
</defs>
<rect width="{W}" height="{H}" fill="#fafafa"/>
<text x="{M}" y="72" class="h">Cat2Bug</text>
<text x="{M}" y="98" class="sub">platform architecture</text>
<line x1="{M}" y1="120" x2="{W-M}" y2="120" stroke="#e2e8f0" stroke-width="1"/>
'''
    ]

    # 三带分区
    zones = [
        (140, "Clients", ACCESS),
        (560, "Application", None),
        (200, "Foundation", INFRA),
    ]
    y = 140
    for zh, items in [("Clients", ACCESS), ("Application", None), ("Foundation", INFRA)]:
        if zh == "Application":
            zh_h = 560
        elif zh == "Clients":
            zh_h = 140
        else:
            zh_h = 200
        parts.append(f'<text x="{M}" y="{y+20}" class="sec">{zh}</text>')
        parts.append(f'<rect x="{M}" y="{y+32}" width="{iw}" height="{zh_h-32}" rx="2" fill="#fff" stroke="#e2e8f0" stroke-width="1"/>')

        if zh == "Clients":
            cols = 5
            cw = (iw - 48 - G * (cols - 1)) / cols
            ch = zh_h - 72
            for i, (t, s) in enumerate(items):
                r, c = divmod(i, cols)
                x = M + 24 + c * (cw + G)
                yy = y + 52 + r * (ch / 2 + 8)
                parts.append(f'<rect x="{x:.0f}" y="{yy:.0f}" width="{cw:.0f}" height="{ch/2-8:.0f}" fill="none" stroke="#e2e8f0"/>')
                parts.append(f'<text x="{x+12:.0f}" y="{yy+22:.0f}" class="lbl">{esc(t)}</text>')
                parts.append(f'<text x="{x+12:.0f}" y="{yy+38:.0f}" class="sm">{esc(s)}</text>')

        elif zh == "Application":
            inner_y = y + 52
            inner_h = zh_h - 72
            loop_w = 248
            main_w = iw - 48 - loop_w - G
            cols = [("Input", INPUTS, 0), ("Gateway", GATEWAY, 1), ("Core", [f"{a}" for a, _ in CORE], 2),
                    ("Output", OUTPUTS, 3)]
            cw = (main_w - 3 * G) / 4
            for name, lst, ci in cols:
                x = M + 24 + ci * (cw + G)
                parts.append(f'<line x1="{x:.0f}" y1="{inner_y:.0f}" x2="{x:.0f}" y2="{inner_y+inner_h:.0f}" stroke="{accent}" stroke-width="2"/>')
                parts.append(f'<text x="{x+12:.0f}" y="{inner_y+20:.0f}" class="sec">{name}</text>')
                ih = (inner_h - 40 - G * (len(lst) - 1)) / len(lst)
                for j, item in enumerate(lst):
                    yy = inner_y + 36 + j * (ih + G)
                    parts.append(f'<rect x="{x+8:.0f}" y="{yy:.0f}" width="{cw-16:.0f}" height="{ih:.0f}" fill="none" stroke="#f1f5f9"/>')
                    parts.append(f'<text x="{x+16:.0f}" y="{yy+ih/2+4:.0f}" class="lbl">{esc(item)}</text>')
                if ci < 3:
                    parts.append(f'<line x1="{x+cw:.0f}" y1="{inner_y+inner_h/2:.0f}" x2="{x+cw+G:.0f}" y2="{inner_y+inner_h/2:.0f}" stroke="#cbd5e1" stroke-width="1" marker-end="url(#ar)"/>')

            lx = M + iw - loop_w - 24
            parts.append(f'<rect x="{lx:.0f}" y="{inner_y:.0f}" width="{loop_w:.0f}" height="{inner_h:.0f}" fill="none" stroke="#e2e8f0"/>')
            parts.append(f'<text x="{lx+16:.0f}" y="{inner_y+28:.0f}" class="sec">Loops</text>')
            parts.append(f'<text x="{lx+16:.0f}" y="{inner_y+inner_h/2-8:.0f}" class="sm">{esc(LOOPS["defect"])}</text>')
            parts.append(f'<text x="{lx+16:.0f}" y="{inner_y+inner_h/2+16:.0f}" class="sm">{esc(LOOPS["test"])}</text>')
            parts.append(f'<text x="{lx+16:.0f}" y="{inner_y+inner_h-20:.0f}" class="sm">{" · ".join(LOOPS["safety"])}</text>')

        else:
            cols = 5
            cw = (iw - 48 - G * (cols - 1)) / cols
            ch = (zh_h - 72 - G) / 2
            for i, (t, s) in enumerate(items):
                r, c = divmod(i, cols)
                x = M + 24 + c * (cw + G)
                yy = y + 52 + r * (ch + G)
                parts.append(f'<rect x="{x:.0f}" y="{yy:.0f}" width="{cw:.0f}" height="{ch:.0f}" fill="none" stroke="#e2e8f0"/>')
                parts.append(f'<text x="{x+cw/2:.0f}" y="{yy+ch/2-2:.0f}" text-anchor="middle" class="lbl">{esc(t)}</text>')
                parts.append(f'<text x="{x+cw/2:.0f}" y="{yy+ch/2+14:.0f}" text-anchor="middle" class="sm">{esc(s)}</text>')

        y += zh_h + 24

    parts.append("</svg>")
    return "".join(parts)


# ═══════════════════════════════════════════════════════════
# Style 3 — Dark Tech（暗色科技风，渐变描边）
# ═══════════════════════════════════════════════════════════
def style_dark() -> str:
    M, G = 40, 14
    iw = W - 2 * M
    bg = "#0f1419"
    card = "#1a2332"
    parts = [
        f'''<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}">
<defs>
  {dark_shadow_filter()}
  <linearGradient id="glow" x1="0" y1="0" x2="1" y2="1">
    <stop offset="0%" stop-color="#22d3ee"/><stop offset="50%" stop-color="#818cf8"/><stop offset="100%" stop-color="#34d399"/>
  </linearGradient>
  <linearGradient id="card-border" x1="0" y1="0" x2="1" y2="0">
    <stop offset="0%" stop-color="#22d3ee" stop-opacity=".6"/>
    <stop offset="100%" stop-color="#818cf8" stop-opacity=".6"/>
  </linearGradient>
  <style>
    .title{{font:700 20px "PingFang SC","Helvetica Neue",sans-serif;fill:#f1f5f9}}
    .sec{{font:600 12px "PingFang SC","Helvetica Neue",sans-serif;fill:#22d3ee;letter-spacing:1px}}
    .lbl{{font:500 11px "PingFang SC","Helvetica Neue",sans-serif;fill:#cbd5e1}}
    .sm{{font:400 10px "PingFang SC","Helvetica Neue",sans-serif;fill:#64748b}}
    .badge{{font:700 10px "Helvetica Neue",sans-serif;fill:#0f1419}}
  </style>
  <marker id="ar" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto">
    <path d="M0,0 L8,4 L0,8 Z" fill="#22d3ee"/>
  </marker>
</defs>
<rect width="{W}" height="{H}" fill="{bg}"/>
<rect x="{M}" y="24" width="{iw}" height="3" rx="1.5" fill="url(#glow)"/>
<text x="{M}" y="58" class="title">Cat2Bug Platform Architecture</text>
<text x="{W-M}" y="58" text-anchor="end" class="sm">v0.6 · Spring Boot + Vue</text>
'''
    ]

    def dark_box(x, y, w, h, title, color="#22d3ee"):
        return (
            f'<rect x="{x:.0f}" y="{y:.0f}" width="{w:.0f}" height="{h:.0f}" rx="8" fill="{card}" filter="url(#sh)"/>'
            f'<rect x="{x:.0f}" y="{y:.0f}" width="{w:.0f}" height="{h:.0f}" rx="8" fill="none" stroke="url(#card-border)" stroke-width="1"/>'
            f'<text x="{x+14:.0f}" y="{y+26:.0f}" class="sec" fill="{color}">{esc(title)}</text>'
        )

    # 访问层
    ay, ah = 76, 110
    parts.append(dark_box(M, ay, iw, ah, "ACCESS LAYER · 访问层"))
    cols = 5
    cw = (iw - 28 - G * (cols - 1)) / cols
    ch = (ah - 44 - G) / 2
    for i, (t, s) in enumerate(ACCESS):
        r, c = divmod(i, cols)
        x = M + 14 + c * (cw + G)
        yy = ay + 38 + r * (ch + G)
        parts.append(f'<rect x="{x:.0f}" y="{yy:.0f}" width="{cw:.0f}" height="{ch:.0f}" rx="6" fill="#111827" stroke="#334155" stroke-width="1"/>')
        parts.append(f'<text x="{x+10:.0f}" y="{yy+ch/2-2:.0f}" class="lbl">{esc(t)}</text>')
        parts.append(f'<text x="{x+10:.0f}" y="{yy+ch/2+12:.0f}" class="sm">{esc(s)}</text>')

    # 主区域 4+1
    my, mh = ay + ah + G, 500
    w_in = 200
    w_core = iw - 4 * w_in - 4 * G - 260
    xs = [M, M + w_in + G, M + 2 * (w_in + G), M + 2 * (w_in + G) + w_core + G]
    blocks = [
        ("INPUT", "#34d399", INPUTS),
        ("GATEWAY", "#a78bfa", GATEWAY),
        ("CORE ENGINE", "#38bdf8", [f"{a}" for a, b in CORE]),
        ("OUTPUT", "#fb923c", OUTPUTS),
    ]
    for bi, (title, color, items) in enumerate(blocks):
        x = xs[bi] if bi < 3 else M + 3 * (w_in + G) + w_core + G
        w = w_core if bi == 2 else w_in
        parts.append(dark_box(x, my, w, mh, title, color))
        ih = (mh - 44 - G * (len(items) - 1)) / len(items)
        for j, item in enumerate(items):
            yy = my + 40 + j * (ih + G)
            parts.append(f'<rect x="{x+10:.0f}" y="{yy:.0f}" width="{w-20:.0f}" height="{ih:.0f}" rx="4" fill="#0f1419" stroke="#1e293b"/>')
            parts.append(f'<rect x="{x+10:.0f}" y="{yy:.0f}" width="3" height="{ih:.0f}" rx="1" fill="{color}"/>')
            parts.append(f'<text x="{x+20:.0f}" y="{yy+ih/2+4:.0f}" class="lbl">{esc(item)}</text>')
        if bi < 3:
            parts.append(f'<path d="M{x+w:.0f} {my+mh/2:.0f} L{x+w+G:.0f} {my+mh/2:.0f}" stroke="{color}" stroke-width="1.5" marker-end="url(#ar)" opacity=".8"/>')

    # 右侧
    rx = M + iw - 246
    for i, (title, txt) in enumerate([("DEFECT LOOP", LOOPS["defect"]), ("TEST LOOP", LOOPS["test"]), ("SECURITY", " · ".join(LOOPS["safety"]))]):
        yy = my + i * (mh / 3 + 4)
        rh = mh / 3 - 6
        parts.append(dark_box(rx, yy, 246, rh, title, "#f472b6"))
        parts.append(f'<text x="{rx+14:.0f}" y="{yy+rh/2+8:.0f}" class="sm">{esc(txt)}</text>')

    # 基础设施
    fy = my + mh + G
    fh = H - fy - M
    parts.append(dark_box(M, fy, iw, fh, "FOUNDATION · 基础设施", "#94a3b8"))
    cols = 5
    cw = (iw - 28 - G * (cols - 1)) / cols
    ch = (fh - 44 - G) / 2
    colors = ["#22d3ee", "#818cf8", "#34d399", "#f472b6", "#fbbf24"]
    for i, (t, s) in enumerate(INFRA):
        r, c = divmod(i, cols)
        x = M + 14 + c * (cw + G)
        yy = fy + 38 + r * (ch + G)
        col = colors[c % len(colors)]
        parts.append(f'<rect x="{x:.0f}" y="{yy:.0f}" width="{cw:.0f}" height="{ch:.0f}" rx="6" fill="#111827" stroke="{col}" stroke-width="1" stroke-opacity=".5"/>')
        parts.append(f'<text x="{x+cw/2:.0f}" y="{yy+ch/2-2:.0f}" text-anchor="middle" class="lbl">{esc(t)}</text>')
        parts.append(f'<text x="{x+cw/2:.0f}" y="{yy+ch/2+12:.0f}" text-anchor="middle" class="sm">{esc(s)}</text>')

    parts.append("</svg>")
    return "".join(parts)


# ═══════════════════════════════════════════════════════════
# Style 4 — Swimlane Flow（泳道流程图，横向数据流）
# ═══════════════════════════════════════════════════════════
def style_swimlane() -> str:
    M, G = 36, 12
    lane_h = 168
    label_w = 120
    parts = [
        f'''<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}">
<defs>
  {shadow_filter("sh2")}
  <style>
    .lane{{font:700 12px "PingFang SC","Helvetica Neue",sans-serif;fill:#fff;letter-spacing:.5px}}
    .lane-en{{font:500 9px "Helvetica Neue",sans-serif;fill:rgba(255,255,255,.75)}}
    .node{{font:600 11px "PingFang SC","Helvetica Neue",sans-serif;fill:#1e293b}}
    .node-sm{{font:400 9px "PingFang SC","Helvetica Neue",sans-serif;fill:#64748b}}
    .flow-lbl{{font:500 10px "Helvetica Neue",sans-serif;fill:#64748b}}
  </style>
  <marker id="ar" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto">
    <path d="M0,0 L8,4 L0,8 Z" fill="#64748b"/>
  </marker>
  <marker id="ar-g" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto">
    <path d="M0,0 L8,4 L0,8 Z" fill="#16a34a"/>
  </marker>
</defs>
<rect width="{W}" height="{H}" fill="#ffffff"/>
<text x="{W/2}" y="40" text-anchor="middle" font="700 20px PingFang SC,Helvetica Neue,sans-serif" fill="#0f172a">Cat2Bug 系统架构 · 数据流视图</text>
<text x="{W/2}" y="62" text-anchor="middle" class="flow-lbl">Layered Architecture with Horizontal Data Flow</text>
'''
    ]

    lanes = [
        ("#0ea5e9", "访问层", "Access", ACCESS, 5),
        ("#16a34a", "数据输入", "Input", [(x, "") for x in INPUTS], len(INPUTS)),
        ("#9333ea", "网关", "Gateway", [(x, "") for x in GATEWAY], len(GATEWAY)),
        ("#2563eb", "核心引擎", "Core", CORE, len(CORE)),
        ("#ea580c", "输出", "Output", [(x, "") for x in OUTPUTS], len(OUTPUTS)),
        ("#475569", "基础设施", "Infra", INFRA, 5),
    ]

    y0 = 80
    for li, (color, zh, en, items, cols_hint) in enumerate(lanes):
        y = y0 + li * (lane_h + G)
        content_w = W - M - label_w - M - 20
        parts.append(f'<rect x="{M}" y="{y}" width="{label_w}" height="{lane_h}" rx="8" fill="{color}"/>')
        parts.append(f'<text x="{M+label_w/2}" y="{y+lane_h/2-6}" text-anchor="middle" class="lane">{zh}</text>')
        parts.append(f'<text x="{M+label_w/2}" y="{y+lane_h/2+12}" text-anchor="middle" class="lane-en">{en}</text>')
        parts.append(f'<rect x="{M+label_w+8}" y="{y}" width="{content_w}" height="{lane_h}" rx="8" fill="#f8fafc" stroke="#e2e8f0" filter="url(#sh2)"/>')

        n = len(items)
        if li in (0, 5):
            cols = 5
            rows = (n + cols - 1) // cols
        else:
            cols = n
            rows = 1
        area_x = M + label_w + 20
        area_w = content_w - 24
        area_h = lane_h - 16
        cell_w = (area_w - G * (cols - 1)) / cols
        cell_h = (area_h - G * (rows - 1)) / rows if rows else area_h

        for i, item in enumerate(items):
            if isinstance(item, tuple) and len(item) == 2:
                t, s = item
            else:
                t, s = item if isinstance(item, tuple) else (str(item), "")
            r, c = divmod(i, cols)
            x = area_x + c * (cell_w + G)
            yy = y + 8 + r * (cell_h + G)
            fill = "#fff" if li % 2 == 0 else "#f1f5f9"
            stroke = color if li < 5 else "#cbd5e1"
            parts.append(f'<rect x="{x:.0f}" y="{yy:.0f}" width="{cell_w:.0f}" height="{cell_h:.0f}" rx="6" fill="{fill}" stroke="{stroke}" stroke-width="1.2" stroke-opacity=".6"/>')
            if s:
                parts.append(f'<text x="{x+cell_w/2:.0f}" y="{yy+cell_h/2-4:.0f}" text-anchor="middle" class="node">{esc(t)}</text>')
                parts.append(f'<text x="{x+cell_w/2:.0f}" y="{yy+cell_h/2+10:.0f}" text-anchor="middle" class="node-sm">{esc(s)}</text>')
            else:
                parts.append(f'<text x="{x+cell_w/2:.0f}" y="{yy+cell_h/2+4:.0f}" text-anchor="middle" class="node">{esc(t)}</text>')

        if li < len(lanes) - 1:
            cx = W / 2
            parts.append(f'<path d="M{cx:.0f} {y+lane_h:.0f} L{cx:.0f} {y+lane_h+G:.0f}" stroke="#94a3b8" stroke-width="2" marker-end="url(#ar)"/>')

    # 横向主流程箭头（右侧）
    ax = W - M - 30
    flow_y_start = y0 + lane_h / 2
    flow_y_end = y0 + 4 * (lane_h + G) + lane_h / 2
    parts.append(f'<path d="M{ax:.0f} {flow_y_start:.0f} L{ax:.0f} {flow_y_end:.0f}" stroke="#16a34a" stroke-width="2.5" marker-end="url(#ar-g)"/>')
    parts.append(f'<text x="{ax+12:.0f}" y="{(flow_y_start+flow_y_end)/2:.0f}" class="flow-lbl" fill="#16a34a" transform="rotate(90 {ax+12:.0f} {(flow_y_start+flow_y_end)/2:.0f})">Request Flow</text>')

    parts.append("</svg>")
    return "".join(parts)


STYLES = [
    ("cat2bug-architecture-style-1-corporate.png", style_corporate, "#ffffff"),
    ("cat2bug-architecture-style-2-minimal.png", style_minimal, "#fafafa"),
    ("cat2bug-architecture-style-3-dark-tech.png", style_dark, "#0f1419"),
    ("cat2bug-architecture-style-4-swimlane.png", style_swimlane, "#ffffff"),
]


def main():
    for filename, builder, bg in STYLES:
        path = OUT_DIR / filename
        svg = builder()
        tmp = path.with_suffix(".svg")
        tmp.write_text(svg, encoding="utf-8")
        subprocess.run(
            ["rsvg-convert", "-w", str(W), f"--background-color={bg}", str(tmp), "-o", str(path)],
            check=True,
        )
        tmp.unlink(missing_ok=True)
        print("PNG written:", path)


if __name__ == "__main__":
    main()
