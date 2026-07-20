import fs from 'fs';
import path from 'path';
import { pathToFileURL } from 'url';
import { UI_NODE_MODULES } from './paths.mjs';

let markdownItFactory = null;

async function loadMarkdownIt() {
  if (markdownItFactory) return markdownItFactory;
  const entry = path.join(UI_NODE_MODULES, 'markdown-it/index.js');
  if (fs.existsSync(entry)) {
    const mod = await import(pathToFileURL(entry).href);
    markdownItFactory = mod.default || mod;
    return markdownItFactory;
  }
  return null;
}

/** Minimal fallback when markdown-it is not installed. */
function escapeHtml(s) {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function fallbackMarkdownToHtml(md) {
  const blocks = md.split(/\n\n+/);
  return blocks
    .map((block) => {
      const t = block.trim();
      if (!t) return '';
      if (/^#\s+/.test(t)) {
        const level = t.match(/^#+/)[0].length;
        const text = t.replace(/^#+\s+/, '');
        const tag = level === 1 ? 'h1' : level === 2 ? 'h2' : 'h3';
        return `<${tag}>${escapeHtml(text)}</${tag}>`;
      }
      if (t.startsWith('|')) {
        return `<pre class="table-fallback">${escapeHtml(t)}</pre>`;
      }
      return `<p>${escapeHtml(t.replace(/\n/g, ' '))}</p>`;
    })
    .filter(Boolean)
    .join('\n');
}

/**
 * @param {string} md
 * @returns {Promise<string>}
 */
export async function markdownToHtml(md) {
  const Md = await loadMarkdownIt();
  if (Md) {
    const mdIt = Md({ html: false, linkify: true, typographer: false });
    return mdIt.render(md);
  }
  return fallbackMarkdownToHtml(md);
}

/**
 * @param {{ title: string, bodyHtml: string }} opts
 * @returns {string}
 */
export function wrapPreviewDocument({ title, bodyHtml }) {
  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8" />
  <title>${escapeHtml(title)}</title>
  <style>
    * { box-sizing: border-box; }
    body {
      margin: 0;
      padding: 24px;
      width: 480px;
      background: #eceef2;
      color: #373e48;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Microsoft YaHei", sans-serif;
      font-size: 13px;
      line-height: 1.55;
    }
    .card {
      background: #ffffff;
      border: 1px solid #d2d6dc;
      border-radius: 8px;
      padding: 20px 22px;
      max-height: 640px;
      overflow: hidden;
    }
    h1 { font-size: 18px; margin: 0 0 12px; color: #1c2028; }
    h2 { font-size: 15px; margin: 16px 0 8px; color: #2a3038; }
    h3 { font-size: 14px; margin: 12px 0 6px; }
    p { margin: 0 0 10px; }
    table { border-collapse: collapse; width: 100%; margin: 8px 0; font-size: 12px; }
    th, td { border: 1px solid #d2d6dc; padding: 6px 8px; text-align: left; }
    th { background: #f3f5f8; }
    pre.table-fallback { white-space: pre-wrap; font-size: 11px; opacity: 0.9; }
    code { background: #f3f5f8; padding: 1px 4px; border-radius: 3px; font-size: 12px; }
  </style>
</head>
<body>
  <div class="card">
    ${bodyHtml}
  </div>
</body>
</html>`;
}
