/** @type {import('@playwright/test').PlaywrightTestConfig} */
const { devices } = require('@playwright/test');
const path = require('path');

const authFile = path.join(__dirname, 'e2e/.auth/admin.json');

module.exports = {
  testDir: './e2e',
  globalSetup: path.join(__dirname, 'e2e/global-setup.cjs'),
  timeout: 120000,
  expect: { timeout: 15000 },
  fullyParallel: false,
  workers: 1,
  projects: [
    {
      name: 'chromium-login',
      testMatch: ['**/login.spec.cjs', '**/command-palette-login.spec.cjs'],
      use: {
        ...devices['Desktop Chrome'],
        channel: process.env.PW_CHANNEL === 'chrome' ? 'chrome' : undefined,
      },
    },
    {
      name: 'chromium',
      testIgnore: ['**/login.spec.cjs', '**/command-palette-login.spec.cjs'],
      use: {
        ...devices['Desktop Chrome'],
        channel: process.env.PW_CHANNEL === 'chrome' ? 'chrome' : undefined,
        storageState: authFile,
      },
    },
    {
      name: 'webkit',
      testIgnore: ['**/login.spec.cjs', '**/command-palette-login.spec.cjs'],
      use: {
        ...devices['Desktop Safari'],
        storageState: authFile,
      },
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
  webServer: process.env.PLAYWRIGHT_NO_WEB_SERVER
    ? undefined
    : {
        command: 'npm run dev -- --force',
        url: 'http://127.0.0.1:2222',
        timeout: 180000,
        reuseExistingServer: !process.env.CI,
      },
};
