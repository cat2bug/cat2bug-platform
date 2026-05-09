/** @type {import('@playwright/test').PlaywrightTestConfig} */
module.exports = {
  testDir: './e2e',
  timeout: 120000,
  expect: { timeout: 15000 },
  fullyParallel: false,
  workers: 1,
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:2222',
    locale: 'zh-CN',
    viewport: { width: 1280, height: 800 },
    trace: 'on-first-retry',
    /* 本机已装 Chrome 时可 PW_CHANNEL=chrome 跳过 playwright install 下载 */
    channel: process.env.PW_CHANNEL === 'chrome' ? 'chrome' : undefined,
    /**
     * Playwright Chromium 默认带 --hide-scrollbars，截图永远捕不到轨道/滑块。
     * OverlayScrollbar 关掉后更易得到稳定非叠加滚动条。
     */
    launchOptions: {
      ignoreDefaultArgs: ['--hide-scrollbars'],
      args: ['--disable-features=OverlayScrollbar'],
    },
  },
  /* 仅前端时登录仍会失败；需本地已启动后端 :2020 再跑 */
  webServer: process.env.PLAYWRIGHT_NO_WEB_SERVER
    ? undefined
    : {
        command: 'npm run dev',
        url: 'http://127.0.0.1:2222',
        timeout: 180000,
        reuseExistingServer: true,
      },
};
