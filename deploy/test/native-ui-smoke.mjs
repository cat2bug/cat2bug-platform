#!/usr/bin/env node
/**
 * Native UI 冒烟测试（Playwright）
 *
 * 用法:
 *   node deploy/test/native-ui-smoke.mjs
 *   BASE_URL=http://127.0.0.1:2020 HEADLESS=0 node deploy/test/native-ui-smoke.mjs
 *
 * 前置:
 *   1. Node.js >= 18
 *   2. 实例已启动并监听 2020（嵌入式前端 + 后端）
 *   3. 已完成 /setup 安装；demo / 123456 可登录
 *   4. cat2bug-platform-ui 已 npm install（提供 playwright 依赖）
 *   5. 首次需: cd cat2bug-platform-ui && npx playwright install chromium
 *
 * 流程: 登录 demo/123456 →（无团队时）创建团队并上传 PNG → 断言页面无 Native 反射/类加载错误
 */
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, '../..');
const UI_ROOT = path.join(ROOT, 'cat2bug-platform-ui');

const require = createRequire(path.join(UI_ROOT, 'package.json'));
const { chromium } = require('playwright');

const BASE_URL = process.env.BASE_URL || process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:2020';
const HEADLESS = process.env.HEADLESS !== '0';
const TIMEOUT = Number(process.env.SMOKE_TIMEOUT_MS || 120_000);

/** 需在页面上不可出现的 Native 运行时错误关键字 */
const NATIVE_ERROR_PATTERNS = [
  'MissingReflectionRegistrationError',
  'NoClassDefFoundError',
];

/** 1×1 透明 PNG（与 native-api-smoke 相同） */
const PNG_BASE64 =
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==';

/**
 * 解析数学验证码表达式（如 "1+2=?"），字符型则原样返回
 */
function solveCaptchaExpr(expr) {
  if (!expr) return '';
  const stripped = String(expr).replace(/[=?？\s]/g, '');
  if (/^[\d+\-*/().]+$/.test(stripped)) {
    try {
      // eslint-disable-next-line no-new-func
      return String(Function(`"use strict"; return (${stripped})`)());
    } catch {
      return stripped;
    }
  }
  return stripped;
}

/** 断言页面正文与弹层均不含 Native 错误文本 */
async function assertNoNativeErrors(page, step) {
  const chunks = [];
  chunks.push(await page.locator('body').innerText().catch(() => ''));
  const msgNodes = page.locator('.el-message, .el-notification, .el-message-box');
  const count = await msgNodes.count();
  for (let i = 0; i < count; i += 1) {
    chunks.push(await msgNodes.nth(i).innerText().catch(() => ''));
  }
  const haystack = chunks.join('\n');
  for (const pattern of NATIVE_ERROR_PATTERNS) {
    if (haystack.includes(pattern)) {
      throw new Error(`[${step}] 页面含 Native 错误: ${pattern}`);
    }
  }
}

/** 登录 demo / 123456，自动处理验证码（若启用） */
async function login(page) {
  let captchaMeta = null;
  await page.route('**/captchaImage**', async (route) => {
    const response = await route.fetch();
    captchaMeta = await response.json();
    await route.fulfill({ response });
  });

  await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded', timeout: TIMEOUT });
  await page.waitForLoadState('networkidle', { timeout: TIMEOUT }).catch(() => {});

  if (page.url().includes('/setup')) {
    throw new Error('实例未完成安装，请先完成 /setup 向导');
  }

  await page.getByPlaceholder(/账号|account/i).fill('demo');
  await page.getByPlaceholder(/密码|password/i).fill('123456');

  if (captchaMeta?.captchaEnabled) {
    const answer = solveCaptchaExpr(captchaMeta.captchaExpr);
    const captchaInput = page.getByPlaceholder(/验证码|verification/i);
    await captchaInput.waitFor({ state: 'visible', timeout: 10_000 });
    await captchaInput.fill(answer);
  }

  await page.getByRole('button', { name: /登[录陆]|login/i }).click();
  await page.waitForFunction(
    () => {
      const href = window.location.href;
      return href.includes('#/') && !href.endsWith('/login') && !href.includes('#/login');
    },
    { timeout: TIMEOUT }
  );

  if (page.url().includes('/setup')) {
    throw new Error('登录后被重定向到 /setup，请确认实例已安装');
  }

  await assertNoNativeErrors(page, '登录后');
  console.log('OK   登录 demo/123456');
}

/** 判断是否处于「需创建团队」引导态（侧栏显示「创建团队」） */
async function needsTeamCreation(page) {
  if (page.url().includes('/system/team/add')) {
    return true;
  }
  const hint = page.locator('.prefix-team-name').filter({ hasText: /创建团队|Create Team/i });
  return (await hint.count()) > 0 && (await hint.first.isVisible().catch(() => false));
}

/** 经团队引导进入创建页：优先点侧栏下拉，否则直链 */
async function gotoTeamCreate(page) {
  const teamSelect = page.locator('.team-select .el-input--prefix');
  if ((await teamSelect.count()) > 0) {
    await teamSelect.first.click();
    const createBtn = page
      .locator('.team-select-kbd-popper button, .team-select-footer button')
      .filter({ hasText: /创建团队|Create Team/i });
    if ((await createBtn.count()) > 0) {
      await createBtn.first.click();
      await page.waitForURL(/\/system\/team\/add/, { timeout: TIMEOUT });
      return;
    }
  }
  await page.goto(`${BASE_URL}/system/team/add-member`, {
    waitUntil: 'domcontentloaded',
    timeout: TIMEOUT,
  });
}

/** 上传小 PNG、填写团队名并提交 */
async function createTeam(page) {
  await gotoTeamCreate(page);
  await page.waitForURL(/\/system\/team\/add/, { timeout: TIMEOUT });
  await assertNoNativeErrors(page, '进入创建团队');

  const teamName = `ui-smoke-${Date.now()}`;
  await page.getByPlaceholder(/请输入团队名称|Please enter team name/i).fill(teamName);

  const tmpPng = path.join(os.tmpdir(), `native-ui-smoke-${Date.now()}.png`);
  fs.writeFileSync(tmpPng, Buffer.from(PNG_BASE64, 'base64'));
  try {
    const fileInput = page.locator('.component-upload-image input[type="file"]');
    await fileInput.waitFor({ state: 'attached', timeout: 15_000 });
    await fileInput.setInputFiles(tmpPng);
    await page.locator('.el-upload-list__item, .cat2bug-upload .el-upload-list__item').first.waitFor({
      timeout: 60_000,
    });
  } finally {
    fs.unlinkSync(tmpPng);
  }

  await page.getByRole('button', { name: /创建团队|Create Team/i }).click();
  await page.waitForURL(/\/team\/project-list|\/index/, { timeout: TIMEOUT });
  await assertNoNativeErrors(page, '创建团队后');
  console.log(`OK   创建团队 "${teamName}" 并上传图标`);
}

async function main() {
  console.log(`==> Native UI smoke @ ${BASE_URL}\n`);

  let browser;
  try {
    browser = await chromium.launch({ headless: HEADLESS });
  } catch (err) {
    console.error('无法启动 Chromium，请先执行: cd cat2bug-platform-ui && npx playwright install chromium');
    throw err;
  }

  const context = await browser.newContext({
    baseURL: BASE_URL,
    locale: 'zh-CN',
    viewport: { width: 1280, height: 800 },
  });
  const page = await context.newPage();
  page.setDefaultTimeout(TIMEOUT);

  try {
    await login(page);

    if (await needsTeamCreation(page)) {
      console.log('..   检测到团队引导，进入创建团队');
      await createTeam(page);
    } else {
      console.log('SKIP 已有团队，跳过创建团队');
      await assertNoNativeErrors(page, '首页');
    }

    console.log('\n==> UI smoke passed');
    process.exitCode = 0;
  } catch (err) {
    console.error('\nFAIL', err.message || err);
    process.exitCode = 1;
  } finally {
    await browser.close();
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
