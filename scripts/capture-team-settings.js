const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

const BASE_URL = 'http://localhost:2222';
const USERNAME = 'admin';
const PASSWORD = 'admin123';
const SCREENSHOT_DIR = path.join(__dirname, '../cat2bug-platform-ui/docs/img/screenshots/team-setting');

// 确保截图目录存在
if (!fs.existsSync(SCREENSHOT_DIR)) {
  fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
}

async function captureTeamSettings() {
  const browser = await chromium.launch({
    headless: false,
    executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'
  });
  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 }
  });
  const page = await context.newPage();

  try {
    console.log('正在访问登录页面...');
    await page.goto(BASE_URL);
    await page.waitForTimeout(3000);

    // 登录
    console.log('正在登录...');
    const usernameSelectors = [
      'input[placeholder*="用户名"]',
      'input[name="username"]',
      'input[type="text"]',
      '#username'
    ];

    const passwordSelectors = [
      'input[placeholder*="密码"]',
      'input[name="password"]',
      'input[type="password"]',
      '#password'
    ];

    let filled = false;
    for (const selector of usernameSelectors) {
      try {
        await page.fill(selector, USERNAME, { timeout: 5000 });
        filled = true;
        break;
      } catch (e) {
        continue;
      }
    }

    if (!filled) {
      throw new Error('无法找到用户名输入框');
    }

    filled = false;
    for (const selector of passwordSelectors) {
      try {
        await page.fill(selector, PASSWORD, { timeout: 5000 });
        filled = true;
        break;
      } catch (e) {
        continue;
      }
    }

    if (!filled) {
      throw new Error('无法找到密码输入框');
    }

    const loginButtonSelectors = [
      'button[type="submit"]',
      'button:has-text("登录")',
      '.el-button--primary',
      'button.login-btn'
    ];

    let clicked = false;
    for (const selector of loginButtonSelectors) {
      try {
        await page.click(selector, { timeout: 5000 });
        clicked = true;
        break;
      } catch (e) {
        continue;
      }
    }

    if (!clicked) {
      throw new Error('无法找到登录按钮');
    }

    await page.waitForTimeout(5000);

    // 访问团队设置页面
    console.log('正在访问团队设置页面...');

    const teamSettingSelectors = [
      'text=团队设置',
      'a:has-text("团队设置")',
      '[href*="team/option"]',
      '.el-menu-item:has-text("团队设置")'
    ];

    clicked = false;
    for (const selector of teamSettingSelectors) {
      try {
        await page.click(selector, { timeout: 5000 });
        clicked = true;
        console.log(`成功点击: ${selector}`);
        break;
      } catch (e) {
        continue;
      }
    }

    if (!clicked) {
      console.log('无法找到团队设置菜单，尝试直接访问URL...');
      await page.goto(`${BASE_URL}/#/system/team/option`);
    }

    await page.waitForTimeout(3000);

    // 截取团队设置整体页面
    console.log('截取团队设置页面...');
    await page.screenshot({
      path: path.join(SCREENSHOT_DIR, '01-team-setting-overview.png'),
      fullPage: true
    });

    console.log('截图完成！');

  } catch (error) {
    console.log('发生错误:', error.message);
  } finally {
    await browser.close();
  }
}

captureTeamSettings();
