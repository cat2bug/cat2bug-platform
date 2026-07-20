import fs from 'fs';
import os from 'os';
import path from 'path';
import { pathToFileURL } from 'url';
import { markdownToHtml, wrapPreviewDocument } from './markdown-html.mjs';
import { writeSolidPng } from './placeholder-png.mjs';
import { UI_NODE_MODULES } from './paths.mjs';

const PREVIEW_WIDTH = 480;
const PREVIEW_HEIGHT = 360;
const PLACEHOLDER_RGB = [0x25, 0x25, 0x29];

/**
 * @returns {Promise<{ usePlaywright: boolean, chromium: import('playwright').BrowserType | null }>}
 */
async function resolvePlaywright() {
  const pwTest = path.join(UI_NODE_MODULES, '@playwright/test/index.js');
  const pwCore = path.join(UI_NODE_MODULES, 'playwright/index.js');
  const entry = fs.existsSync(pwTest) ? pwTest : fs.existsSync(pwCore) ? pwCore : null;
  if (!entry) {
    return { usePlaywright: false, chromium: null };
  }
  try {
    const mod = await import(pathToFileURL(entry).href);
    const chromium = mod.chromium || mod.default?.chromium;
    if (!chromium) return { usePlaywright: false, chromium: null };
    return { usePlaywright: true, chromium };
  } catch {
    return { usePlaywright: false, chromium: null };
  }
}

/**
 * @param {{ mdPath: string, pngPath: string, title: string }} item
 * @param {import('playwright').Browser | null} browser
 * @param {string} tmpDir
 */
async function renderOne(item, browser, tmpDir) {
  const md = fs.readFileSync(item.mdPath, 'utf8');
  const bodyHtml = await markdownToHtml(md);
  const html = wrapPreviewDocument({ title: item.title, bodyHtml });
  fs.mkdirSync(path.dirname(item.pngPath), { recursive: true });

  if (browser) {
    const htmlFile = path.join(tmpDir, `${path.basename(item.pngPath, '.png')}.html`);
    fs.writeFileSync(htmlFile, html, 'utf8');
    const page = await browser.newPage({
      viewport: { width: PREVIEW_WIDTH, height: PREVIEW_HEIGHT + 48 },
    });
    try {
      await page.goto(pathToFileURL(htmlFile).href, { waitUntil: 'load' });
      const card = page.locator('.card');
      await card.screenshot({ path: item.pngPath, type: 'png' });
    } finally {
      await page.close();
    }
    return;
  }

  writeSolidPng(item.pngPath, PREVIEW_WIDTH, PREVIEW_HEIGHT, PLACEHOLDER_RGB);
}

/**
 * @param {{ mdPath: string, pngPath: string, title: string }[]} items
 * @returns {Promise<{ usePlaywright: boolean, ok: number, failed: number }>}
 */
export async function generatePreviews(items) {
  const { usePlaywright, chromium } = await resolvePlaywright();
  let browser = null;
  let playwrightUsed = false;
  const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'cat2bug-report-preset-'));

  if (usePlaywright && chromium) {
    try {
      browser = await chromium.launch({ headless: true });
      playwrightUsed = true;
    } catch (err) {
      console.warn(
        'Playwright present but browser launch failed; using placeholder PNGs.',
        err instanceof Error ? err.message : err
      );
      browser = null;
    }
  }

  let ok = 0;
  let failed = 0;
  for (const item of items) {
    try {
      await renderOne(item, browser, tmpDir);
      ok += 1;
    } catch (err) {
      failed += 1;
      console.error(`preview failed: ${item.mdPath}`, err);
      try {
        writeSolidPng(item.pngPath, PREVIEW_WIDTH, PREVIEW_HEIGHT, PLACEHOLDER_RGB);
        ok += 1;
      } catch {
        /* keep failed count */
      }
    }
  }

  if (browser) {
    await browser.close();
  }
  fs.rmSync(tmpDir, { recursive: true, force: true });

  return { usePlaywright: playwrightUsed, ok, failed };
}
