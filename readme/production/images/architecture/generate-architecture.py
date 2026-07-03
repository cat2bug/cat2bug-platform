#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 Cat2Bug 系统架构图 SVG（QuantDinger 风格）。"""
from pathlib import Path

OUT_DIR = Path(__file__).parent
SVG_PATH = OUT_DIR / "cat2bug-platform-architecture.svg"
PNG_PATH = OUT_DIR / "cat2bug-platform-architecture.png"

W, H = 1680, 1120
M = 40
PAD = 16
GAP = 14
HDR = 44

ICONS = r'''
  <symbol id="ic-vue" viewBox="0 0 1024 1024"><path d="M615.6 123.6h-207L64 823.4h190.2l82.4-142.2 153.2 142.2H960L615.6 123.6zM512 636.2L392.4 430.2h239.2L512 636.2z" fill="#41b883"/><path d="M512 201.8l118.6 228.4H393.4L512 201.8z" fill="#35495e"/></symbol>
  <symbol id="ic-chrome" viewBox="0 0 1024 1024"><circle cx="512" cy="512" r="420" fill="#fff" stroke="#ddd" stroke-width="24"/><path d="M512 92c-85 150-127 255-127 315h254C639 347 597 242 512 92z" fill="#ea4335"/><path d="M232 362c130 75 220 127 270 155l127-220C558 218 468 218 382 250 296 282 248 322 232 362z" fill="#fbbc04"/><path d="M232 662c16 40 64 80 150 112 86 32 176 32 247 5L502 559c-50 28-140 80-270 155z" fill="#34a853"/><circle cx="512" cy="512" r="155" fill="#fff"/><circle cx="512" cy="512" r="120" fill="#4285f4"/></symbol>
  <symbol id="ic-mcp" viewBox="0 0 1024 1024"><rect x="128" y="160" width="768" height="704" rx="80" fill="#eef2ff" stroke="#6366f1" stroke-width="32"/><circle cx="320" cy="360" r="72" fill="#6366f1"/><circle cx="704" cy="360" r="72" fill="#8b5cf6"/><circle cx="512" cy="680" r="72" fill="#3b82f6"/><path d="M368 390h112M544 390h112M350 420l130 220M674 420l-130 220" stroke="#6366f1" stroke-width="28" stroke-linecap="round"/></symbol>
  <symbol id="ic-ws" viewBox="0 0 1024 1024"><path d="M128 512c80-160 224-240 384-240s304 80 384 240" fill="none" stroke="#0ea5e9" stroke-width="56" stroke-linecap="round"/><path d="M224 512c56-112 156-168 288-168s232 56 288 168" fill="none" stroke="#38bdf8" stroke-width="48" stroke-linecap="round"/><circle cx="512" cy="640" r="48" fill="#0284c7"/></symbol>
  <symbol id="ic-api" viewBox="0 0 1024 1024"><rect x="160" y="224" width="704" height="576" rx="64" fill="#eff6ff" stroke="#2563eb" stroke-width="32"/><path d="M320 448l-96 64 96 64M704 448l96 64-96 64M560 352L464 672" fill="none" stroke="#2563eb" stroke-width="48" stroke-linecap="round"/></symbol>
  <symbol id="ic-mobile" viewBox="0 0 1024 1024"><rect x="288" y="96" width="448" height="832" rx="80" fill="#f0f9ff" stroke="#0284c7" stroke-width="32"/><rect x="352" y="176" width="320" height="560" rx="24" fill="#fff"/><circle cx="512" cy="820" r="40" fill="#0284c7"/></symbol>
  <symbol id="ic-im" viewBox="0 0 1024 1024"><path d="M160 224h704v448H384L160 768V224z" fill="#fff7ed" stroke="#ea580c" stroke-width="32" stroke-linejoin="round"/><circle cx="360" cy="448" r="40" fill="#ea580c"/><circle cx="512" cy="448" r="40" fill="#16a34a"/><circle cx="664" cy="448" r="40" fill="#2563eb"/></symbol>
  <symbol id="ic-wizard" viewBox="0 0 1024 1024"><path d="M512 96l108 332h352L640 604l108 332L512 760 276 936l108-332L52 428h352z" fill="#fef3c7" stroke="#f59e0b" stroke-width="24" stroke-linejoin="round"/></symbol>
  <symbol id="ic-upgrade" viewBox="0 0 1024 1024"><circle cx="512" cy="512" r="384" fill="#ecfdf5" stroke="#10b981" stroke-width="32"/><path d="M512 704V320M352 480l160-160 160 160" fill="none" stroke="#10b981" stroke-width="56" stroke-linecap="round" stroke-linejoin="round"/></symbol>
  <symbol id="ic-bug" viewBox="0 0 1024 1024"><ellipse cx="512" cy="560" rx="200" ry="240" fill="#fef2f2" stroke="#ef4444" stroke-width="32"/><path d="M512 320V192M352 400l-128-96M672 400l128-96M288 560H128M896 560H736" stroke="#ef4444" stroke-width="36" stroke-linecap="round"/><path d="M384 720l-96 128M640 720l96 128" stroke="#ef4444" stroke-width="36" stroke-linecap="round"/></symbol>
  <symbol id="ic-excel" viewBox="0 0 1024 1024"><rect x="192" y="128" width="640" height="768" rx="48" fill="#f0fdf4" stroke="#16a34a" stroke-width="32"/><path d="M352 352l320 320M672 352L352 672" stroke="#16a34a" stroke-width="48"/></symbol>
  <symbol id="ic-case" viewBox="0 0 1024 1024"><rect x="224" y="128" width="576" height="768" rx="48" fill="#fff" stroke="#16a34a" stroke-width="32"/><path d="M320 352h384M320 512h384M320 672h256" stroke="#16a34a" stroke-width="36" stroke-linecap="round"/></symbol>
  <symbol id="ic-key" viewBox="0 0 1024 1024"><circle cx="384" cy="544" r="160" fill="#faf5ff" stroke="#9333ea" stroke-width="32"/><path d="M480 544h320M720 544v96M832 544v64" stroke="#9333ea" stroke-width="40" stroke-linecap="round"/></symbol>
  <symbol id="ic-log" viewBox="0 0 1024 1024"><rect x="224" y="128" width="576" height="768" rx="48" fill="#f8fafc" stroke="#64748b" stroke-width="32"/><path d="M320 320h384M320 480h384M320 640h256" stroke="#64748b" stroke-width="32" stroke-linecap="round"/></symbol>
  <symbol id="ic-file" viewBox="0 0 1024 1024"><path d="M288 128h320l192 192v576H288z" fill="#fff" stroke="#0891b2" stroke-width="32" stroke-linejoin="round"/><path d="M608 128v192h192" fill="none" stroke="#0891b2" stroke-width="32"/></symbol>
  <symbol id="ic-lock" viewBox="0 0 1024 1024"><rect x="256" y="448" width="512" height="384" rx="48" fill="#faf5ff" stroke="#9333ea" stroke-width="32"/><path d="M352 448V320a160 160 0 11320 0v128" fill="none" stroke="#9333ea" stroke-width="36"/></symbol>
  <symbol id="ic-shield" viewBox="0 0 1024 1024"><path d="M512 96l352 128v256c0 224-160 384-352 416-192-32-352-192-352-416V224z" fill="#fef2f2" stroke="#dc2626" stroke-width="32" stroke-linejoin="round"/><path d="M384 512l96 96 160-160" fill="none" stroke="#dc2626" stroke-width="40" stroke-linecap="round"/></symbol>
  <symbol id="ic-scope" viewBox="0 0 1024 1024"><circle cx="512" cy="512" r="320" fill="#faf5ff" stroke="#9333ea" stroke-width="32"/><circle cx="512" cy="512" r="128" fill="#9333ea"/><path d="M512 192V96M512 928v-96M192 512H96M928 512h-96" stroke="#9333ea" stroke-width="32" stroke-linecap="round"/></symbol>
  <symbol id="ic-rate" viewBox="0 0 1024 1024"><circle cx="512" cy="512" r="320" fill="#faf5ff" stroke="#9333ea" stroke-width="32"/><path d="M512 192v128M512 704v128M192 512h128M704 512h128" stroke="#9333ea" stroke-width="32" stroke-linecap="round"/></symbol>
  <symbol id="ic-audit" viewBox="0 0 1024 1024"><rect x="224" y="160" width="576" height="704" rx="48" fill="#fff" stroke="#9333ea" stroke-width="32"/><path d="M320 512l128 128 256-256" fill="none" stroke="#9333ea" stroke-width="40" stroke-linecap="round"/></symbol>
  <symbol id="ic-team" viewBox="0 0 1024 1024"><circle cx="384" cy="384" r="128" fill="#dbeafe" stroke="#2563eb" stroke-width="28"/><circle cx="672" cy="416" r="96" fill="#dbeafe" stroke="#2563eb" stroke-width="28"/><path d="M192 800c0-128 96-224 192-224s192 96 192 224M480 800c0-96 72-168 160-168" fill="none" stroke="#2563eb" stroke-width="32" stroke-linecap="round"/></symbol>
  <symbol id="ic-test" viewBox="0 0 1024 1024"><rect x="192" y="256" width="640" height="512" rx="48" fill="#f0fdf4" stroke="#16a34a" stroke-width="32"/><path d="M384 512l96 96 160-192" fill="none" stroke="#16a34a" stroke-width="40" stroke-linecap="round"/></symbol>
  <symbol id="ic-ai" viewBox="0 0 1024 1024"><circle cx="512" cy="512" r="320" fill="#ede9fe" stroke="#7c3aed" stroke-width="32"/><path d="M384 512h256M512 384v256" stroke="#7c3aed" stroke-width="48" stroke-linecap="round"/><circle cx="384" cy="384" r="48" fill="#a78bfa"/><circle cx="640" cy="384" r="48" fill="#a78bfa"/><circle cx="384" cy="640" r="48" fill="#a78bfa"/><circle cx="640" cy="640" r="48" fill="#a78bfa"/></symbol>
  <symbol id="ic-tool" viewBox="0 0 1024 1024"><path d="M640 224l160 160-384 384H256V608z" fill="#fff7ed" stroke="#ea580c" stroke-width="32" stroke-linejoin="round"/><circle cx="704" cy="320" r="96" fill="none" stroke="#ea580c" stroke-width="32"/></symbol>
  <symbol id="ic-mail" viewBox="0 0 1024 1024"><rect x="128" y="256" width="768" height="512" rx="48" fill="#fff7ed" stroke="#ea580c" stroke-width="32"/><path d="M128 320l384 256 384-256" fill="none" stroke="#ea580c" stroke-width="32"/></symbol>
  <symbol id="ic-report" viewBox="0 0 1024 1024"><rect x="224" y="128" width="576" height="768" rx="48" fill="#fff" stroke="#ea580c" stroke-width="32"/><path d="M320 640l128-192 128 96 160-224" fill="none" stroke="#ea580c" stroke-width="32" stroke-linecap="round"/></symbol>
  <symbol id="ic-chart" viewBox="0 0 1024 1024"><path d="M192 768V448M416 768V320M640 768V512M864 768V384" stroke="#ea580c" stroke-width="56" stroke-linecap="round"/><path d="M160 832h704" stroke="#cbd5e1" stroke-width="24"/></symbol>
  <symbol id="ic-share" viewBox="0 0 1024 1024"><circle cx="736" cy="288" r="96" fill="#fff7ed" stroke="#ea580c" stroke-width="28"/><circle cx="288" cy="512" r="96" fill="#fff7ed" stroke="#ea580c" stroke-width="28"/><circle cx="736" cy="736" r="96" fill="#fff7ed" stroke="#ea580c" stroke-width="28"/><path d="M368 464l288-128M368 560l288 128" stroke="#ea580c" stroke-width="28"/></symbol>
  <symbol id="ic-callback" viewBox="0 0 1024 1024"><path d="M192 512a320 320 0 01560-200M832 512a320 320 0 01-560 200" fill="none" stroke="#ea580c" stroke-width="40" stroke-linecap="round"/><path d="M832 256v160H672M192 768V608h160" fill="none" stroke="#ea580c" stroke-width="40" stroke-linecap="round"/></symbol>
  <symbol id="ic-spring" viewBox="0 0 1024 1024"><path d="M512 96c200 160 280 288 280 416s-80 256-280 416C312 768 232 640 232 512s80-256 280-416z" fill="#f0fdf4" stroke="#16a34a" stroke-width="28"/><path d="M352 512c80-80 160-120 240-120" fill="none" stroke="#16a34a" stroke-width="32" stroke-linecap="round"/></symbol>
  <symbol id="ic-db" viewBox="0 0 1024 1024"><ellipse cx="512" cy="256" rx="288" ry="96" fill="#dbeafe" stroke="#2563eb" stroke-width="28"/><path d="M224 256v512c0 53 129 96 288 96s288-43 288-96V256" fill="#eff6ff" stroke="#2563eb" stroke-width="28"/></symbol>
  <symbol id="ic-cache" viewBox="0 0 1024 1024"><rect x="160" y="160" width="320" height="320" rx="48" fill="#ede9fe" stroke="#7c3aed" stroke-width="24"/><rect x="544" y="160" width="320" height="320" rx="48" fill="#ede9fe" stroke="#7c3aed" stroke-width="24"/><rect x="160" y="544" width="320" height="320" rx="48" fill="#ede9fe" stroke="#7c3aed" stroke-width="24"/><rect x="544" y="544" width="320" height="320" rx="48" fill="#ede9fe" stroke="#7c3aed" stroke-width="24"/></symbol>
  <symbol id="ic-docker" viewBox="0 0 1024 1024"><rect x="192" y="448" width="128" height="96" fill="#0ea5e9"/><rect x="352" y="448" width="128" height="96" fill="#0ea5e9"/><rect x="512" y="448" width="128" height="96" fill="#0ea5e9"/><rect x="352" y="336" width="128" height="96" fill="#38bdf8"/><path d="M192 576h576v96a96 96 0 01-96 96H288a96 96 0 01-96-96z" fill="#e0f2fe" stroke="#0284c7" stroke-width="24"/></symbol>
  <symbol id="ic-maven" viewBox="0 0 1024 1024"><path d="M192 832l320-576 320 576z" fill="#fff7ed" stroke="#ea580c" stroke-width="28" stroke-linejoin="round"/><path d="M320 832h384" stroke="#ea580c" stroke-width="28"/></symbol>
  <symbol id="ic-quartz" viewBox="0 0 1024 1024"><circle cx="512" cy="544" r="288" fill="#f8fafc" stroke="#64748b" stroke-width="28"/><path d="M512 352v192l128 96" stroke="#64748b" stroke-width="32" stroke-linecap="round"/><path d="M512 192V128M512 128h64" stroke="#64748b" stroke-width="28" stroke-linecap="round"/></symbol>
  <symbol id="ic-spec" viewBox="0 0 1024 1024"><rect x="224" y="96" width="576" height="832" rx="48" fill="#eef2ff" stroke="#6366f1" stroke-width="28"/><path d="M320 288h384M320 448h384M320 608h256" stroke="#6366f1" stroke-width="32" stroke-linecap="round"/></symbol>
  <symbol id="ic-auth" viewBox="0 0 1024 1024"><rect x="288" y="416" width="448" height="384" rx="48" fill="#faf5ff" stroke="#9333ea" stroke-width="28"/><path d="M384 416V320a128 128 0 11256 0v96" fill="none" stroke="#9333ea" stroke-width="32"/></symbol>
  <symbol id="ic-idea" viewBox="0 0 1024 1024"><path d="M512 128c-120 0-216 96-216 216 0 80 44 150 108 188v84h216v-84c64-38 108-108 108-188 0-120-96-216-216-216z" fill="#fef9c3" stroke="#eab308" stroke-width="28"/><rect x="384" y="640" width="256" height="48" rx="16" fill="#fde047"/></symbol>
  <symbol id="ic-assign" viewBox="0 0 1024 1024"><circle cx="384" cy="384" r="128" fill="#dbeafe" stroke="#2563eb" stroke-width="28"/><path d="M256 704c0-96 72-168 160-168h32M576 536l128 128M704 536L576 664" stroke="#2563eb" stroke-width="32" stroke-linecap="round"/></symbol>
  <symbol id="ic-fix" viewBox="0 0 1024 1024"><path d="M640 224l160 160-384 384H256V608z" fill="#dcfce7" stroke="#16a34a" stroke-width="28" stroke-linejoin="round"/></symbol>
  <symbol id="ic-verify" viewBox="0 0 1024 1024"><circle cx="512" cy="512" r="320" fill="#f0fdf4" stroke="#16a34a" stroke-width="28"/><path d="M384 512l96 96 160-160" fill="none" stroke="#16a34a" stroke-width="40" stroke-linecap="round"/></symbol>
  <symbol id="ic-close" viewBox="0 0 1024 1024"><circle cx="512" cy="512" r="320" fill="#f1f5f9" stroke="#64748b" stroke-width="28"/><path d="M416 416l192 192M608 416L416 608" stroke="#64748b" stroke-width="40" stroke-linecap="round"/></symbol>
'''


def ic(x, y, s, href):
    return f'<use href="#{href}" x="{x:.1f}" y="{y:.1f}" width="{s}" height="{s}"/>'


def esc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;")


def num_badge(n, x, y, color):
    return (f'<circle cx="{x}" cy="{y}" r="13" fill="{color}"/>'
            f'<text x="{x}" y="{y+4.5}" text-anchor="middle" class="num">{n}</text>')


def section_title(n, color, zh, en, x, y):
    return (num_badge(n, x + 13, y + 13, color)
            + f'<text x="{x+32}" y="{y+10}" class="sec-zh">{esc(zh)}</text>'
            + f'<text x="{x+32}" y="{y+26}" class="sec-en">{esc(en)}</text>')


def card(x, y, w, h, fill="#fff", stroke="#e2e8f0", sw=1.5, rx=10, shadow=True):
    sh = ' filter="url(#cardShadow)"' if shadow else ""
    return f'<rect x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" rx="{rx}" fill="{fill}" stroke="{stroke}" stroke-width="{sw}"{sh}/>'


def arrow_h(x1, y, x2, color, mid=False):
    m = ' marker-end="url(#ar)"' if mid else f' marker-end="url(#ar-{color.replace("#","")})"'
    return f'<path d="M{x1:.0f} {y:.0f} L{x2:.0f} {y:.0f}" stroke="{color}" stroke-width="2.5" fill="none"{m}/>'


def check_item(x, y, text, color="#64748b"):
    return (f'<circle cx="{x+5}" cy="{y-3}" r="5" fill="none" stroke="{color}" stroke-width="1.2"/>'
            f'<path d="M{x+2} {y-3} L{x+5} {y} L{x+10} {y-6}" fill="none" stroke="{color}" stroke-width="1.2"/>'
            f'<text x="{x+16}" y="{y}" class="check">{esc(text)}</text>')


# ── 核心引擎行右侧迷你图 ──────────────────────────────────
def mini_bars(x, y, w, h, color):
    bars = [0.5, 0.75, 0.6, 0.9, 0.55]
    out = [f'<rect x="{x:.0f}" y="{y:.0f}" width="{w:.0f}" height="{h:.0f}" rx="4" fill="#f8fafc" stroke="#e2e8f0"/>']
    bw = (w - 20) / len(bars)
    for i, r in enumerate(bars):
        bh = (h - 16) * r
        out.append(f'<rect x="{x+10+i*bw:.0f}" y="{y+h-8-bh:.0f}" width="{bw-4:.0f}" height="{bh:.0f}" rx="2" fill="{color}" opacity=".7"/>')
    return "".join(out)


def mini_spark(x, y, w, h, color="#ef4444"):
    pts = " ".join(f"{x+10+i*(w-20)/8:.0f},{y+h/2+{8:-12,3:8,5:-4,2:10,7:-8,4:6,6:-2,1:14,0:0}[i%9]}" for i in range(9))
    return (f'<rect x="{x:.0f}" y="{y:.0f}" width="{w:.0f}" height="{h:.0f}" rx="4" fill="#fef2f2" stroke="#fecaca"/>'
            f'<polyline points="{pts}" fill="none" stroke="{color}" stroke-width="1.8" stroke-linecap="round"/>')


def mini_checks(x, y, w, h, color="#16a34a"):
    items = ["用例✓", "计划✓", "报告✓"]
    out = [f'<rect x="{x:.0f}" y="{y:.0f}" width="{w:.0f}" height="{h:.0f}" rx="4" fill="#f0fdf4" stroke="#bbf7d0"/>']
    for i, t in enumerate(items):
        out.append(check_item(x + 8, y + 18 + i * 16, t, "#16a34a"))
    return "".join(out)


def mini_ai(x, y, w, h, color="#9333ea"):
    cx, cy = x + w / 2, y + h / 2
    return (f'<rect x="{x:.0f}" y="{y:.0f}" width="{w:.0f}" height="{h:.0f}" rx="4" fill="#faf5ff" stroke="#e9d5ff"/>'
            f'<circle cx="{cx:.0f}" cy="{cy:.0f}" r="14" fill="#ede9fe" stroke="#9333ea" stroke-width="1.2"/>'
            f'<circle cx="{cx-16:.0f}" cy="{cy-10:.0f}" r="5" fill="#c4b5fd"/>'
            f'<circle cx="{cx+16:.0f}" cy="{cy-10:.0f}" r="5" fill="#c4b5fd"/>'
            f'<circle cx="{cx-16:.0f}" cy="{cy+10:.0f}" r="5" fill="#c4b5fd"/>'
            f'<circle cx="{cx+16:.0f}" cy="{cy+10:.0f}" r="5" fill="#c4b5fd"/>')


def mini_tools(x, y, w, h, color="#ea580c"):
    tags = ["API", "IM", "Gen", "Excel"]
    out = [f'<rect x="{x:.0f}" y="{y:.0f}" width="{w:.0f}" height="{h:.0f}" rx="4" fill="#fff7ed" stroke="#fed7aa"/>']
    tw = (w - 20) / len(tags)
    for i, t in enumerate(tags):
        out.append(f'<rect x="{x+8+i*tw:.0f}" y="{y+10:.0f}" width="{tw-4:.0f}" height="18" rx="4" fill="#fff" stroke="#fdba74"/>')
        out.append(f'<text x="{x+8+i*tw+tw/2-2:.0f}" y="{y+23:.0f}" text-anchor="middle" class="pill">{t}</text>')
    return "".join(out)


MINI = [mini_bars, mini_spark, mini_checks, mini_ai, mini_tools]


def build():
    iw = W - 2 * M
    p = [f'''<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}">
<defs>
  <filter id="cardShadow" x="-3%" y="-3%" width="106%" height="108%">
    <feDropShadow dx="0" dy="2" stdDeviation="3" flood-color="#64748b" flood-opacity="0.12"/>
  </filter>
  <linearGradient id="mcpBar" x1="0" y1="0" x2="1" y2="0">
    <stop offset="0%" stop-color="#dbeafe"/><stop offset="50%" stop-color="#eff6ff"/><stop offset="100%" stop-color="#dbeafe"/>
  </linearGradient>
  <style>
    .logo {{ font: 700 26px "PingFang SC","Helvetica Neue",system-ui,sans-serif; fill:#0f172a; }}
    .tagline {{ font: 400 13px "PingFang SC","Helvetica Neue",system-ui,sans-serif; fill:#64748b; }}
    .sec-zh {{ font: 700 14px "PingFang SC","Microsoft YaHei",system-ui,sans-serif; fill:#0f172a; }}
    .sec-en {{ font: 500 10px "Helvetica Neue",system-ui,sans-serif; fill:#94a3b8; letter-spacing:.3px; }}
    .num {{ font: 700 11px system-ui,sans-serif; fill:#fff; }}
    .chip {{ font: 600 11px "PingFang SC",system-ui,sans-serif; fill:#1e293b; }}
    .chip-sub {{ font: 400 9px "PingFang SC",system-ui,sans-serif; fill:#94a3b8; }}
    .item-title {{ font: 600 11px "PingFang SC",system-ui,sans-serif; fill:#1e293b; }}
    .item-sub {{ font: 400 9px "PingFang SC",system-ui,sans-serif; fill:#64748b; }}
    .row-title {{ font: 600 12px "PingFang SC",system-ui,sans-serif; fill:#1e293b; }}
    .row-sub {{ font: 400 10px "PingFang SC",system-ui,sans-serif; fill:#64748b; }}
    .check {{ font: 400 10px "PingFang SC",system-ui,sans-serif; fill:#475569; }}
    .pill {{ font: 600 9px system-ui,sans-serif; fill:#ea580c; }}
    .fund-title {{ font: 600 10px "PingFang SC",system-ui,sans-serif; fill:#334155; }}
    .fund-sub {{ font: 400 9px "PingFang SC",system-ui,sans-serif; fill:#94a3b8; }}
    .flow-title {{ font: 600 11px "PingFang SC",system-ui,sans-serif; fill:#334155; }}
    .flow-sub {{ font: 400 9px "PingFang SC",system-ui,sans-serif; fill:#94a3b8; }}
    .mcp-label {{ font: 600 12px "PingFang SC",system-ui,sans-serif; fill:#1d4ed8; }}
    .route {{ font: 500 10px "Helvetica Neue",monospace; fill:#94a3b8; }}
  </style>
  {ICONS}
  <marker id="ar" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto"><path d="M0,0 L8,4 L0,8 Z" fill="#94a3b8"/></marker>
  <marker id="ar-16a34a" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto"><path d="M0,0 L8,4 L0,8 Z" fill="#16a34a"/></marker>
  <marker id="ar-9333ea" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto"><path d="M0,0 L8,4 L0,8 Z" fill="#9333ea"/></marker>
  <marker id="ar-2563eb" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto"><path d="M0,0 L8,4 L0,8 Z" fill="#2563eb"/></marker>
  <marker id="ar-0ea5e9" markerWidth="7" markerHeight="7" refX="6" refY="3.5" orient="auto"><path d="M0,0 L7,3.5 L0,7 Z" fill="#0ea5e9"/></marker>
</defs>
<rect width="{W}" height="{H}" fill="#ffffff"/>
''']

    # ── 顶栏 Logo ──
    p.append(f'<circle cx="{W/2-108}" cy="42" r="18" fill="#fef2f2" stroke="#ef4444" stroke-width="2"/>')
    p.append(ic(W / 2 - 120, 30, 24, "ic-bug"))
    p.append(f'<text x="{W/2-78}" y="50" class="logo">Cat2Bug</text>')
    p.append(f'<text x="{W/2}" y="68" text-anchor="middle" class="tagline">AI 辅助轻量级 BUG 管理平台 · for Teams &amp; Agents</text>')

    # ── 纵向分区 ──
    ay, ah = 82, 148
    my, mh = ay + ah + GAP, 498
    fy, fh = my + mh + GAP, 178
    wy, wh = fy + fh + GAP, 92
    sy, sh = wy + wh + GAP, 108

    # ═══ 1 访问层 ═══
    p.append(card(M, ay, iw, ah, "#fafbfc", "#cbd5e1"))
    p.append(section_title(1, "#0ea5e9", "访问层", "Access Layer", M + PAD, ay + 10))

    access = [
        ("ic-vue", "Vue SPA"), ("ic-ws", "WebSocket"), ("ic-api", "Open API"),
        ("ic-chrome", "Chrome 插件"), ("ic-mcp", "MCP 服务"), ("ic-mobile", "移动端"),
        ("ic-im", "钉钉/企微"), ("ic-wizard", "安装向导"), ("ic-upgrade", "升级向导"),
    ]
    chip_area_x = M + PAD + 150
    chip_area_w = iw - PAD - 150 - PAD
    chip_area_y = ay + HDR + 4
    chip_h = 44
    cols = 5
    cw = (chip_area_w - GAP * (cols - 1)) / cols
    centers = []
    for i, (href, label) in enumerate(access):
        r, c = divmod(i, cols)
        x = chip_area_x + c * (cw + GAP)
        y = chip_area_y + r * (chip_h + 8)
        cx, cy = x + cw / 2, y + chip_h
        centers.append((cx, cy))
        p.append(card(x, y, cw, chip_h, "#fff", "#bae6fd", 1.2, 8, False))
        p.append(ic(x + 8, y + 8, 28, href))
        p.append(f'<text x="{x+42:.0f}" y="{y+20:.0f}" class="chip">{esc(label)}</text>')

    # MCP 总线
    mcp_y = ay + ah - 28
    mcp_x = M + PAD + 120
    mcp_w = iw - PAD - 120 - PAD
    p.append(f'<rect x="{mcp_x:.0f}" y="{mcp_y:.0f}" width="{mcp_w:.0f}" height="22" rx="11" fill="url(#mcpBar)" stroke="#93c5fd" stroke-width="1.2"/>')
    p.append(f'<text x="{mcp_x+mcp_w/2:.0f}" y="{mcp_y+15:.0f}" text-anchor="middle" class="mcp-label">MCP / Open API Gateway  ·  /api/**  ·  WebSocket</text>')
    for cx, cy in centers:
        p.append(f'<line x1="{cx:.0f}" y1="{cy:.0f}" x2="{cx:.0f}" y2="{mcp_y:.0f}" stroke="#93c5fd" stroke-width="1.2"/>')
        p.append(f'<circle cx="{cx:.0f}" cy="{mcp_y:.0f}" r="3" fill="#3b82f6"/>')

    # ═══ 主区域列宽 ═══
    w_right = 268
    w_col = 196
    w_core = iw - w_right - 3 * w_col - 4 * GAP
    x_in = M
    x_gw = x_in + w_col + GAP
    x_core = x_gw + w_col + GAP
    x_out = x_core + w_core + GAP
    x_right = x_out + w_col + GAP
    mid_y = my + mh / 2

    # ═══ 2 数据输入 ═══
    p.append(card(x_in, my, w_col, mh, "#fafffe", "#22c55e", 2))
    p.append(section_title(2, "#16a34a", "数据输入", "Data &amp; Inputs", x_in + PAD, my + 10))
    inputs = [
        ("ic-bug", "缺陷录入", "手动 / 表单"),
        ("ic-excel", "Excel 导入", "批量缺陷"),
        ("ic-case", "用例 / 计划", "测试数据"),
        ("ic-key", "API Key", "Open API"),
        ("ic-log", "JUnit / JLog", "日志采集"),
        ("ic-im", "IM 回调", "群消息"),
        ("ic-file", "附件上传", "文档 / 截图"),
    ]
    top = my + HDR + 8
    ih = (mh - HDR - PAD - GAP * (len(inputs) - 1) - 8) / len(inputs)
    for i, (href, title, sub) in enumerate(inputs):
        y = top + i * (ih + GAP)
        p.append(card(x_in + PAD, y, w_col - 2 * PAD, ih, "#f0fdf4", "#86efac", 1.2, 7, False))
        p.append(ic(x_in + PAD + 8, y + (ih - 22) / 2, 22, href))
        p.append(f'<text x="{x_in+PAD+36:.0f}" y="{y+ih/2-2:.0f}" class="item-title">{esc(title)}</text>')
        p.append(f'<text x="{x_in+PAD+36:.0f}" y="{y+ih/2+10:.0f}" class="item-sub">{esc(sub)}</text>')
    p.append(arrow_h(x_in + w_col, mid_y, x_gw - 4, "#16a34a"))

    # ═══ 3 网关 ═══
    p.append(card(x_gw, my, w_col, mh, "#fefcff", "#a855f7", 2))
    p.append(section_title(3, "#9333ea", "网关与安全", "Gateway &amp; Security", x_gw + PAD, my + 10))
    p.append(f'<text x="{x_gw+w_col-PAD:.0f}" y="{my+26:.0f}" text-anchor="end" class="route">/system/**</text>')
    gateway = [
        ("ic-lock", "JWT 认证", "用户登录"),
        ("ic-key", "API Key", "Open API"),
        ("ic-shield", "@PreAuthorize", "方法权限"),
        ("ic-scope", "DataScope", "数据权限 AOP"),
        ("ic-rate", "接口限流", "@RateLimiter"),
        ("ic-auth", "CORS / 验证码", "跨域安全"),
        ("ic-audit", "操作审计", "登录 / 操作日志"),
    ]
    for i, (href, title, sub) in enumerate(gateway):
        y = top + i * (ih + GAP)
        p.append(card(x_gw + PAD, y, w_col - 2 * PAD, ih, "#faf5ff", "#d8b4fe", 1.2, 7, False))
        p.append(ic(x_gw + PAD + 8, y + (ih - 22) / 2, 22, href))
        p.append(f'<text x="{x_gw+PAD+36:.0f}" y="{y+ih/2-2:.0f}" class="item-title">{esc(title)}</text>')
        p.append(f'<text x="{x_gw+PAD+36:.0f}" y="{y+ih/2+10:.0f}" class="item-sub">{esc(sub)}</text>')
    p.append(arrow_h(x_gw + w_col, mid_y, x_core - 4, "#9333ea"))

    # ═══ 4 核心引擎 ═══
    p.append(card(x_core, my, w_core, mh, "#f8fbff", "#3b82f6", 2.5))
    p.append(section_title(4, "#2563eb", "Cat2Bug 核心引擎", "Core Engine", x_core + PAD, my + 10))
    p.append(f'<text x="{x_core+w_core-PAD:.0f}" y="{my+26:.0f}" text-anchor="end" class="route">/system/** · /ai/**</text>')
    core_rows = [
        ("ic-team", "组织层", "团队 · 项目 · 成员 · 角色 · 模块", "#eff6ff", "#93c5fd", "#3b82f6"),
        ("ic-bug", "缺陷管理", "CRUD · 状态流转 · 自定义字段 · 统计", "#fef2f2", "#fca5a5", "#ef4444"),
        ("ic-test", "测试管理", "用例 · 计划 · 执行记录 · 报告", "#f0fdf4", "#86efac", "#16a34a"),
        ("ic-ai", "AI 辅助", "用例生成 · 描述优化 · OpenAI · Prompt", "#faf5ff", "#d8b4fe", "#9333ea"),
        ("ic-tool", "集成与工具", "Open API · IM · 代码生成 · Quartz · Excel", "#fff7ed", "#fdba74", "#ea580c"),
    ]
    row_top = my + HDR + 12
    row_h = (mh - HDR - PAD - GAP * 4 - 12) / 5
    mini_w, mini_h = 108, 38
    for i, (href, title, sub, fill, stroke, color) in enumerate(core_rows):
        y = row_top + i * (row_h + GAP)
        ix = x_core + PAD
        iw_row = w_core - 2 * PAD
        p.append(f'<rect x="{ix:.0f}" y="{y:.0f}" width="{iw_row:.0f}" height="{row_h:.0f}" rx="8" fill="{fill}" stroke="{stroke}" stroke-width="1.2"/>')
        p.append(f'<rect x="{ix:.0f}" y="{y:.0f}" width="4" height="{row_h:.0f}" rx="2" fill="{color}"/>')
        p.append(ic(ix + 12, y + (row_h - 26) / 2, 26, href))
        p.append(f'<text x="{ix+46:.0f}" y="{y+row_h/2-6:.0f}" class="row-title">{esc(title)}</text>')
        p.append(f'<text x="{ix+46:.0f}" y="{y+row_h/2+10:.0f}" class="row-sub">{esc(sub)}</text>')
        p.append(MINI[i](ix + iw_row - mini_w - 10, y + (row_h - mini_h) / 2, mini_w, mini_h, color))
    p.append(arrow_h(x_core + w_core, mid_y, x_out - 4, "#2563eb"))

    # ═══ 5 输出 ═══
    p.append(card(x_out, my, w_col, mh, "#fffbf7", "#f97316", 2))
    p.append(section_title(5, "#ea580c", "输出与通知", "Output &amp; Notify", x_out + PAD, my + 10))
    outputs = [
        ("ic-im", "钉钉 / 企微 / 飞书", "IM 机器人"),
        ("ic-mail", "邮件 / 系统通知", "站内消息"),
        ("ic-ws", "WebSocket", "实时推送"),
        ("ic-report", "Excel / PDF", "报告导出"),
        ("ic-callback", "API 回调", "Webhook"),
        ("ic-chart", "统计图表", "看板导出"),
        ("ic-share", "分享链接", "缺陷外链"),
    ]
    for i, (href, title, sub) in enumerate(outputs):
        y = top + i * (ih + GAP)
        p.append(card(x_out + PAD, y, w_col - 2 * PAD, ih, "#fff7ed", "#fdba74", 1.2, 7, False))
        p.append(ic(x_out + PAD + 8, y + (ih - 22) / 2, 22, href))
        p.append(f'<text x="{x_out+PAD+36:.0f}" y="{y+ih/2-2:.0f}" class="item-title">{esc(title)}</text>')
        p.append(f'<text x="{x_out+PAD+36:.0f}" y="{y+ih/2+10:.0f}" class="item-sub">{esc(sub)}</text>')

    # ═══ 6 缺陷闭环工作流（右下）═══
    wf_h = mh * 0.58
    sf_h = mh - wf_h - GAP
    p.append(card(x_right, my, w_right, wf_h, "#f8fafc", "#94a3b8", 1.5))
    p.append(section_title(6, "#475569", "缺陷闭环", "Defect Workflow", x_right + PAD, my + 10))
    agents = [
        ("ic-bug", "上报", "录入 / 导入"),
        ("ic-assign", "分派", "指派处理人"),
        ("ic-fix", "修复", "开发修复"),
        ("ic-verify", "验证", "测试确认"),
        ("ic-close", "关闭", "归档闭环"),
    ]
    ag_y = my + HDR + 20
    ag_w = (w_right - 2 * PAD - 4 * 20) / 5
    for i, (href, t, s) in enumerate(agents):
        ax = x_right + PAD + i * (ag_w + 20)
        p.append(card(ax, ag_y, ag_w, 72, "#fff", "#cbd5e1", 1.2, 8, False))
        p.append(ic(ax + ag_w / 2 - 14, ag_y + 8, 28, href))
        p.append(f'<text x="{ax+ag_w/2:.0f}" y="{ag_y+48:.0f}" text-anchor="middle" class="flow-title">{esc(t)}</text>')
        p.append(f'<text x="{ax+ag_w/2:.0f}" y="{ag_y+62:.0f}" text-anchor="middle" class="flow-sub">{esc(s)}</text>')
        if i < len(agents) - 1:
            x0 = ax + ag_w + 4
            x1 = ax + ag_w + 16
            p.append(f'<path d="M{x0:.0f} {ag_y+36:.0f} L{x1:.0f} {ag_y+36:.0f}" stroke="#94a3b8" stroke-width="1.5" marker-end="url(#ar)"/>')

    # ═══ 9 安全合规（右下小框）═══
    p.append(card(x_right, my + wf_h + GAP, w_right, sf_h, "#fff5f5", "#ef4444", 2))
    p.append(section_title(9, "#dc2626", "安全合规", "Safety &amp; Compliance", x_right + PAD, my + wf_h + GAP + 10))
    safety = ["双重 Security 链 (JWT + API Key)", "数据权限 DataScope AOP", "登录 / 操作审计日志"]
    sx = x_right + PAD + 8
    sy0 = my + wf_h + GAP + HDR + 12
    for i, t in enumerate(safety):
        p.append(check_item(sx, sy0 + i * 22, t, "#dc2626"))

    # ═══ 7 基础设施 ═══
    p.append(card(M, fy, iw, fh, "#f8fafc", "#94a3b8", 2))
    p.append(section_title(7, "#475569", "基础设施", "Foundation / Infrastructure", M + PAD, fy + 10))
    found = [
        ("ic-spring", "Spring Boot 3", "GraalVM Native"),
        ("ic-db", "MyBatis + Druid", "PageHelper"),
        ("ic-db", "H2 / MySQL", "Flyway 迁移"),
        ("ic-cache", "J2Cache", "Caffeine + Redis"),
        ("ic-vue", "Vue 2.7 SPA", "Embedded 部署"),
        ("ic-docker", "Docker", "native-minimal"),
        ("ic-maven", "Maven", "多模块构建"),
        ("ic-excel", "FastExcel", "导入导出"),
        ("ic-spec", "OpenSpec", "规格驱动"),
        ("ic-quartz", "Quartz", "定时任务"),
    ]
    grid_x = M + PAD + 130
    grid_w = iw - PAD - 130 - PAD
    grid_y = fy + HDR + 6
    grid_h = fh - HDR - 36
    cols = 5
    cw = (grid_w - GAP * (cols - 1)) / cols
    ch = (grid_h - GAP) / 2
    for i, (href, t, s) in enumerate(found):
        r, c = divmod(i, cols)
        x = grid_x + c * (cw + GAP)
        y = grid_y + r * (ch + GAP)
        p.append(card(x, y, cw, ch, "#fff", "#e2e8f0", 1.2, 8, False))
        p.append(ic(x + cw / 2 - 12, y + 8, 24, href))
        p.append(f'<text x="{x+cw/2:.0f}" y="{y+ch/2+4:.0f}" text-anchor="middle" class="fund-title">{esc(t)}</text>')
        p.append(f'<text x="{x+cw/2:.0f}" y="{y+ch/2+16:.0f}" text-anchor="middle" class="fund-sub">{esc(s)}</text>')

    # 底部标签 pills
    pills = ["Java 8+", "Spring Boot", "Vue 2.7", "H2/MySQL", "Native Image", "Docker", "OpenSpec"]
    pill_y = fy + fh - 26
    px = M + PAD + 130
    for i, t in enumerate(pills):
        pw = len(t) * 7 + 20
        p.append(f'<rect x="{px:.0f}" y="{pill_y:.0f}" width="{pw:.0f}" height="20" rx="10" fill="#eff6ff" stroke="#bfdbfe"/>')
        p.append(f'<text x="{px+pw/2:.0f}" y="{pill_y+14:.0f}" text-anchor="middle" class="pill" fill="#2563eb">{esc(t)}</text>')
        px += pw + 8

    # ═══ 8 测试闭环（底部流程条）═══
    p.append(card(M, wy, iw, wh, "#fafbfc", "#cbd5e1", 1.5))
    p.append(section_title(8, "#0ea5e9", "测试闭环", "Closed-Loop Test Workflow", M + PAD, wy + 8))
    steps = [
        ("ic-case", "用例", "Case"), ("ic-test", "计划", "Plan"),
        ("ic-tool", "执行", "Execute"), ("ic-report", "报告", "Report"),
        ("ic-chart", "统计", "Dashboard"), ("ic-share", "交付", "Deliver"),
    ]
    step_area_w = iw - PAD - 150 - PAD
    step_x0 = M + PAD + 150
    step_y0 = wy + wh / 2 - 4
    sw = (step_area_w - (len(steps) - 1) * 36) / len(steps)
    for i, (href, zh, en) in enumerate(steps):
        sx = step_x0 + i * (sw + 36)
        p.append(f'<circle cx="{sx+sw/2:.0f}" cy="{step_y0-18:.0f}" r="18" fill="#fff" stroke="#bae6fd" stroke-width="1.5"/>')
        p.append(ic(sx + sw / 2 - 12, step_y0 - 30, 24, href))
        p.append(f'<text x="{sx+sw/2:.0f}" y="{step_y0+10:.0f}" text-anchor="middle" class="flow-title">{esc(zh)}</text>')
        p.append(f'<text x="{sx+sw/2:.0f}" y="{step_y0+22:.0f}" text-anchor="middle" class="flow-sub">{esc(en)}</text>')
        if i < len(steps) - 1:
            x0 = sx + sw + 6
            x1 = sx + sw + 30
            p.append(f'<path d="M{x0:.0f} {step_y0-18:.0f} L{x1:.0f} {step_y0-18:.0f}" stroke="#0ea5e9" stroke-width="1.5" marker-end="url(#ar-0ea5e9)"/>')

    p.append("</svg>")
    return "".join(p)


if __name__ == "__main__":
    svg = build()
    SVG_PATH.write_text(svg, encoding="utf-8")
    print("SVG written:", SVG_PATH)
    import subprocess
    subprocess.run(
        ["rsvg-convert", "-w", str(W), "--background-color=white", str(SVG_PATH), "-o", str(PNG_PATH)],
        check=True,
    )
    print("PNG written:", PNG_PATH)
