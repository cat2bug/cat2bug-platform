/** @type {import('@playwright/test').PlaywrightTestConfig} */
const { devices } = require('@playwright/test');

module.exports = {
  testDir: './e2e',
  timeout: 120000,
  expect: { timeout: 15000 },
  fullyParallel: false,
  workers: 1,
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        channel: process.env.PW_CHANNEL === 'chrome' ? 'chrome' : undefined,
      },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
  ],
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:2222',
    locale: 'zh-CN',
    viewport: { width: 1280, height: 800 },
    trace: 'on-first-retry',
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
