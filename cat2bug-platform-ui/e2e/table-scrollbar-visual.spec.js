/**
 * PLAYWRIGHT_NO_WEB_SERVER=1 PW_CHANNEL=chrome npx playwright test e2e/table-scrollbar-visual.spec.js
 */
const { test, expect } = require('@playwright/test');
const fs = require('fs');
const { PNG } = require('pngjs');

async function login(page, origin) {
  await page.goto(`${origin}/#/login`, { waitUntil: 'domcontentloaded', timeout: 60000 });
  const pwd = page.locator('.login-form input[type="password"]');
  try {
    await pwd.waitFor({ state: 'visible', timeout: 8000 });
  } catch {
    return;
  }
  await page.locator('.login-form input[type="text"]').first().fill('admin');
  await pwd.fill('cat2bug');
  await page.locator('.login-form button.el-button--primary').click();
  await page.waitForLoadState('networkidle', { timeout: 90000 }).catch(() => {});
}

function grayVarianceStats(png) {
  const w = png.width;
  const h = png.height;
  let sum = 0;
  let sumSq = 0;
  const vals = [];
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      const i = (png.width * y + x) << 2;
      const lum = (png.data[i] + png.data[i + 1] + png.data[i + 2]) / 3;
      vals.push(lum);
      sum += lum;
      sumSq += lum * lum;
    }
  }
  const n = vals.length;
  const mean = sum / n;
  let variance = sumSq / n - mean * mean;
  vals.sort((a, b) => a - b);
  const median = vals[Math.floor(n / 2)];
  const darkFrac = vals.filter((v) => v < 200).length / n;
  return { mean, variance, median, darkFrac };
}

test('scroll strip looks opaque enough (case page)', async ({ page, baseURL }) => {
  test.setTimeout(120000);
  const origin = (baseURL || 'http://127.0.0.1:2222').replace(/\/$/, '');
  await page.setViewportSize({ width: 1400, height: 820 });
  await login(page, origin);

  await page.goto(`${origin}/#/project/case`, { waitUntil: 'networkidle', timeout: 90000 });

  const wrap = page.locator('.case-table-x-scroll').first();
  await wrap.waitFor({ state: 'visible', timeout: 45000 });

  for (const h of [260, 180, 120, 90, 72]) {
    await wrap.evaluate((el, px) => {
      el.style.maxHeight = `${px}px`;
      el.style.overflow = 'auto';
    }, h);
    await page.waitForTimeout(250);
    const m = await wrap.evaluate((el) => ({
      ch: el.clientHeight,
      sh: el.scrollHeight,
    }));
    if (m.sh > m.ch + 20) break;
  }

  const m = await wrap.evaluate((el) => ({
    ch: el.clientHeight,
    sh: el.scrollHeight,
  }));
  expect(m.sh).toBeGreaterThan(m.ch + 15);

  await wrap.evaluate((el) => {
    const max = el.scrollHeight - el.clientHeight;
    el.scrollTop = Math.min(120, Math.max(0, Math.floor(max / 2)));
  });
  await page.waitForTimeout(200);

  /** 先把鼠标移到列表右侧，避免 overlay 滚动条在截图里完全不绘制 */
  const preBox = await wrap.boundingBox();
  expect(preBox).toBeTruthy();
  await page.mouse.move(
    Math.floor(preBox.x + preBox.width - 4),
    Math.floor(preBox.y + Math.min(80, preBox.height * 0.25))
  );
  await page.waitForTimeout(150);

  /** 竖向滚动条贴在「当前垂直滚动容器」右缘：优先取真正出现 overflow 的一层 */
  const clip = await wrap.evaluate((rootEl) => {
    const cand = [rootEl, rootEl.querySelector('.el-table__body-wrapper')].filter(Boolean);
    let el = rootEl;
    for (const c of cand) {
      if (c.scrollHeight > c.clientHeight + 10) {
        el = c;
        break;
      }
    }
    const box = el.getBoundingClientRect();
    const sw = Math.min(36, Math.max(18, Math.floor(box.width * 0.045)));
    return {
      x: Math.floor(box.left + box.width - sw),
      y: Math.floor(box.top + box.height * 0.22),
      width: sw,
      height: Math.min(200, Math.floor(box.height * 0.55)),
    };
  });

  const buf = await page.screenshot({ type: 'png', clip });
  fs.mkdirSync('e2e/artifacts', { recursive: true });
  fs.writeFileSync('e2e/artifacts/table-scrollbar-case-strip.png', buf);

  const png = PNG.sync.read(buf);
  const stats = grayVarianceStats(png);
  let thumbPx = 0;
  for (let y = 0; y < png.height; y++) {
    for (let x = 0; x < png.width; x++) {
      const i = (png.width * y + x) << 2;
      const lum = (png.data[i] + png.data[i + 1] + png.data[i + 2]) / 3;
      if (lum < 215) thumbPx += 1;
    }
  }
  const thumbishFrac = thumbPx / (png.width * png.height);

  /** 半透明叠加大条纹透出 → variance 往往偏大；含轨道+滑块正常对比也会升高 */
  expect(stats.variance, `variance (${stats.variance.toFixed(0)})`).toBeLessThan(7500);
  /** 竖条区域不应整块纯白（叠加透明常见）；允许偏浅灰轨道主导的中位数 */
  expect(stats.median, `median (${stats.median.toFixed(1)})`).toBeLessThan(253);
  expect(stats.median).toBeGreaterThan(130);

  /** 滚动后截图应含足够「滑块」级深色像素（lum < 215） */
  expect(thumbishFrac, `thumb-ish frac (${thumbishFrac.toFixed(3)})`).toBeGreaterThan(0.04);
});
