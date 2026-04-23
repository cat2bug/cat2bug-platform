const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

const BASE_URL = 'http://localhost:2222';
const USERNAME = 'admin';
const PASSWORD = 'admin123';
const SCREENSHOT_DIR = path.join(__dirname, '../cat2bug-platform-ui/docs/img/screenshots/user');

// 确保截图目录存在
if (!fs.existsSync(SCREENSHOT_DIR)) {
  fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
}

async function captureUserPages() {
  const browser = await chromium.launch({
    headless: false,
    executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'
  });
  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 }
  });
  const page = await context.newPage();

  try {
    // 1. 截取登录页面
    console.log('正在访问登录页面...');
    await page.goto(BASE_URL);
    await page.waitForTimeout(3000);

    console.log('截取登录页面...');
    await page.screenshot({
      path: path.join(SCREENSHOT_DIR, '01-login.png'),
      fullPage: false
    });

    // 2. 截取注册页面
    console.log('正在访问注册页面...');
    const registerSelectors = [
      'text=注册',
      'a:has-text("注册")',
      '[href*="register"]',
      'button:has-text("注册")'
    ];

    let clicked = false;
    for (const selector of registerSelectors) {
      try {
        await page.click(selector, { timeout: 5000 });
        clicked = true;
        console.log(`成功点击注册: ${selector}`);
        break;
      } catch (e) {
        continue;
      }
    }

    if (!clicked) {
      console.log('无法找到注册链接，尝试直接访问URL...');
      await page.goto(`${BASE_URL}/#/register`);
    }

    await page.waitForTimeout(3000);

    console.log('截取注册页面...');
    await page.screenshot({
      path: path.join(SCREENSHOT_DIR, '02-register.png'),
      fullPage: false
    });

    // 3. 登录系统
    console.log('正在登录系统...');
    await page.goto(BASE_URL);
    await page.waitForTimeout(3000);

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

    clicked = false;
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

    // 4. 访问个人中心
    console.log('正在访问个人中心...');

    const profileSelectors = [
      'text=个人中心',
      'a:has-text("个人中心")',
      '[href*="profile"]',
      '.user-avatar',
      '.el-dropdown-link'
    ];

    clicked = false;
    for (const selector of profileSelectors) {
      try {
        await page.click(selector, { timeout: 5000 });
        clicked = true;
        console.log(`成功点击: ${selector}`);
        await page.waitForTimeout(2000);

        // 如果是下拉菜单，再点击个人中心
        try {
          await page.click('text=个人中心', { timeout: 2000 });
        } catch (e) {
          // 可能直接跳转了
        }
        break;
      } catch (e) {
        continue;
      }
    }

    if (!clicked) {
      console.log('无法找到个人中心入口，尝试直接访问URL...');
      await page.goto(`${BASE_URL}/#/user/profile`);
    }

    await page.waitForTimeout(3000);

    console.log('截取个人中心页面...');
    await page.screenshot({
      path: path.join(SCREENSHOT_DIR, '03-profile.png'),
      fullPage: true
    });

    console.log('截图完成！');

  } catch (error) {
    console.log('发生错误:', error.message);
  } finally {
    await browser.close();
  }
}

captureUserPages();
