const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const BASE_URL = 'http://localhost:2222';
const USERNAME = 'admin';
const PASSWORD = 'cat2bug';
const SCREENSHOT_DIR = path.join(__dirname, '../docs/screenshots/admin');

// 确保截图目录存在
if (!fs.existsSync(SCREENSHOT_DIR)) {
  fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
}

async function captureAdminDocs() {
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
    // 尝试多种可能的选择器
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

    // 尝试填写用户名
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
      console.log('无法找到用户名输入框，尝试截图查看页面...');
      await page.screenshot({ path: path.join(SCREENSHOT_DIR, '00-login-page.png'), fullPage: true });
      throw new Error('无法找到用户名输入框');
    }

    // 尝试填写密码
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
      console.log('无法找到密码输入框');
      await page.screenshot({ path: path.join(SCREENSHOT_DIR, '00-login-page.png'), fullPage: true });
      throw new Error('无法找到密码输入框');
    }

    await page.screenshot({ path: path.join(SCREENSHOT_DIR, '01-login.png') });

    // 尝试点击登录按钮
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
      console.log('无法找到登录按钮');
      throw new Error('无法找到登录按钮');
    }

    await page.waitForTimeout(5000);

    // 截取首页
    console.log('截取首页...');
    await page.screenshot({ path: path.join(SCREENSHOT_DIR, '02-home.png'), fullPage: true });

    // 先尝试展开系统管理菜单
    console.log('尝试展开系统管理菜单...');
    const systemMenuSelectors = [
      'text=系统管理',
      'text=系统设置',
      'text=管理',
      '.el-submenu__title:has-text("系统")',
      '[data-v-a83bd3b0]:has-text("系统管理")'
    ];

    for (const selector of systemMenuSelectors) {
      try {
        await page.click(selector, { timeout: 5000 });
        console.log(`成功点击: ${selector}`);
        await page.waitForTimeout(2000);
        break;
      } catch (e) {
        continue;
      }
    }

    // 查找并访问系统管理相关菜单
    const adminMenus = [
      { name: '团队管理', selectors: ['text=团队管理', 'a:has-text("团队管理")', '[href*="team"]'], file: '03-team-manage.png' },
      { name: '项目管理', selectors: ['text=项目管理', 'a:has-text("项目管理")', '[href*="project"]'], file: '04-project-manage.png' },
      { name: '角色管理', selectors: ['text=角色管理', 'a:has-text("角色管理")', '[href*="role"]'], file: '05-role-manage.png' },
      { name: '成员管理', selectors: ['text=成员管理', 'text=用户管理', 'a:has-text("成员管理")', '[href*="user"]', '[href*="member"]'], file: '06-member-manage.png' }
    ];

    for (const menu of adminMenus) {
      try {
        console.log(`正在访问 ${menu.name}...`);
        let clicked = false;

        for (const selector of menu.selectors) {
          try {
            await page.click(selector, { timeout: 5000 });
            clicked = true;
            break;
          } catch (e) {
            continue;
          }
        }

        if (clicked) {
          await page.waitForTimeout(3000);
          await page.screenshot({
            path: path.join(SCREENSHOT_DIR, menu.file),
            fullPage: true
          });
          console.log(`成功截取 ${menu.name}`);
        } else {
          console.log(`无法找到 ${menu.name} 菜单`);
        }
      } catch (error) {
        console.log(`无法访问 ${menu.name}: ${error.message}`);
      }
    }

    console.log('截图完成！');
  } catch (error) {
    console.error('发生错误:', error);
  } finally {
    await browser.close();
  }
}

captureAdminDocs();
