/**
 * 快捷键默认映射与字母分配工具。
 *
 * 设计见 openspec/changes/keyboard-shortcuts。采用「引导键 + 单字母」模型：
 *   - 导航引导键 g：打开导航面板（左侧菜单页面 + 顶部工具项）
 *   - 动作引导键 Space：打开当前页动作面板（如缺陷页）
 * 不使用 Cmd/Ctrl 修饰组合，规避浏览器/OS 保留键冲突。
 */

export const STORAGE_KEY = 'cat2bug.shortcuts.v1'

export const DEFAULT_LEADERS = {
  nav: 'g',
  action: 'Space'
}

/** 布局级浮层：侧栏折叠（⇧⌘/Shift+Ctrl + 单键，与 g 导航字母表独立展示） */
export const LAYOUT_SIDEBAR_TOGGLE_BINDING = 'nav.layout.sidebarToggle'
export const LAYOUT_SIDEBAR_TOGGLE_DEFAULT_LETTER = '-'

/** 布局级浮层：左上角团队选择 */
export const LAYOUT_TEAM_SELECT_BINDING = 'nav.layout.teamSelect'
export const LAYOUT_TEAM_SELECT_DEFAULT_LETTER = 'L'

/** 引导态等待第二段按键的超时（毫秒） */
export const LEADER_TIMEOUT = 2000

/** 左侧菜单页面默认字母：按路由最后一段匹配；匹配不到则自动分配 */
export const DEFAULT_MENU_LETTERS = {
  defect: 'D', // 缺陷管理
  case: 'C', // 测试用例
  plan: 'P', // 测试计划
  module: 'J', // 交付物
  dashboard: 'Y', // 仪表盘
  report: 'R', // 项目报告
  document: 'F', // 文档管理（避免 ⇧⌘W 与浏览器冲突）
  option: 'O', // 项目设置
  'project-list': 'X', // 项目管理
  'team-option': 'S' // 团队设置
}

/**
 * 顶部工具栏快捷项（静态）。
 * type: 'route' 直接跳转 | 'action' 执行内置动作 | 'dropdown' 打开二级面板
 */
export const TOP_ITEMS = [
  { key: 'notice', type: 'route', defaultLetter: 'N', titleKey: 'notice', to: '/notice/index' },
  { key: 'help', type: 'route', defaultLetter: 'H', titleKey: 'keyboard.help-doc', to: '/system/doc' },
  {
    key: 'lang',
    type: 'dropdown',
    defaultLetter: 'I',
    titleKey: 'keyboard.i18n',
    children: [
      { key: 'zh_CN', defaultLetter: '1', title: '简体中文', action: 'lang:zh_CN' },
      { key: 'zh_TW', defaultLetter: '2', title: '繁體中文', action: 'lang:zh_TW' },
      { key: 'en_US', defaultLetter: '3', title: 'English', action: 'lang:en_US' },
      { key: 'ru', defaultLetter: '4', title: 'Русский', action: 'lang:ru' },
      { key: 'ja_JP', defaultLetter: '5', title: '日本語', action: 'lang:ja_JP' },
      { key: 'ko_KR', defaultLetter: '6', title: '한국어', action: 'lang:ko_KR' },
      { key: 'ar', defaultLetter: '7', title: 'العربية', action: 'lang:ar' }
    ]
  },
  {
    key: 'user',
    type: 'dropdown',
    defaultLetter: 'U',
    titleKey: 'my-center',
    children: [
      { key: 'profile', defaultLetter: 'P', titleKey: 'my-center', to: '/member/profile' },
      { key: 'keyboard', defaultLetter: 'K', titleKey: 'keyboard.title', to: '/member/keyboard' },
      { key: 'logout', defaultLetter: 'Q', titleKey: 'logout', action: 'logout' }
    ]
  },
  {
    key: 'theme',
    type: 'dropdown',
    defaultLetter: 'M',
    titleKey: 'keyboard.theme',
    children: [
      { key: 'light', defaultLetter: '1', titleKey: 'keyboard.theme-light', action: 'theme:light' },
      { key: 'dark', defaultLetter: 'D', titleKey: 'keyboard.theme-dark', action: 'theme:dark' }
    ]
  },
  { key: 'site', type: 'action', defaultLetter: 'B', titleKey: 'website', action: 'open:site' },
  { key: 'git', type: 'action', defaultLetter: 'K', titleKey: 'source-code-address', action: 'open:git' }
]

/** 缺陷页默认动作（页面运行时通过 registerPage 提供执行回调） */
export const DEFECT_ACTION_DEFAULTS = [
  { key: 'query', defaultLetter: 'S', titleKey: 'keyboard.act.query' },
  { key: 'newDefect', defaultLetter: 'E', titleKey: 'keyboard.act.new-defect' },
  { key: 'switchTab', defaultLetter: 'J', titleKey: 'keyboard.act.switch-tab' },
  { key: 'statistic', defaultLetter: 'I', titleKey: 'keyboard.act.statistic-panel' },
  { key: 'statisticNav', defaultLetter: 'G', titleKey: 'keyboard.act.statistic-nav' },
  { key: 'switchView', defaultLetter: 'O', titleKey: 'keyboard.act.switch-view' },
  { key: 'prevPage', defaultLetter: 'B', titleKey: 'keyboard.act.prev-page' },
  { key: 'nextPage', defaultLetter: 'P', titleKey: 'keyboard.act.next-page' }
]

/** 统计模版页默认动作 */
export const STATISTIC_TEMPLATE_ACTION_DEFAULTS = [
  { key: 'preview', defaultLetter: 'P', titleKey: 'keyboard.act.statistic-template-preview' },
  { key: 'personal', defaultLetter: 'G', titleKey: 'keyboard.act.statistic-template-personal' },
  { key: 'team', defaultLetter: 'H', titleKey: 'keyboard.act.statistic-template-team' }
]

/** 登录页语言切换（与顶栏国际化下拉 1–7 一致） */
export const LOGIN_LANG_ACTION_DEFAULTS = [
  { key: 'langZhCN', locale: 'zh_CN', icon: 'lang_zh_CN', defaultLetter: '1', titleKey: 'keyboard.act.login-lang-zh-cn' },
  { key: 'langZhTW', locale: 'zh_TW', icon: 'lang_zh_TW', defaultLetter: '2', titleKey: 'keyboard.act.login-lang-zh-tw' },
  { key: 'langEnUS', locale: 'en_US', icon: 'lang_en_US', defaultLetter: '3', titleKey: 'keyboard.act.login-lang-en' },
  { key: 'langRu', locale: 'ru', icon: 'lang_ru', defaultLetter: '4', titleKey: 'keyboard.act.login-lang-ru' },
  { key: 'langJaJP', locale: 'ja_JP', icon: 'lang_ja_JP', defaultLetter: '5', titleKey: 'keyboard.act.login-lang-ja' },
  { key: 'langKoKR', locale: 'ko_KR', icon: 'lang_ko_KR', defaultLetter: '6', titleKey: 'keyboard.act.login-lang-ko' },
  { key: 'langAr', locale: 'ar', icon: 'lang_ar', defaultLetter: '7', titleKey: 'keyboard.act.login-lang-ar' }
]

/** 登录页默认动作 */
export const LOGIN_ACTION_DEFAULTS = [
  { key: 'username', defaultLetter: 'U', titleKey: 'keyboard.act.login-username' },
  { key: 'password', defaultLetter: 'P', titleKey: 'keyboard.act.login-password' },
  { key: 'submit', defaultLetter: 'L', titleKey: 'keyboard.act.login-submit' },
  { key: 'remember', defaultLetter: 'O', titleKey: 'keyboard.act.login-remember' },
  { key: 'register', defaultLetter: 'E', titleKey: 'keyboard.act.login-register' },
  { key: 'captcha', defaultLetter: 'K', titleKey: 'keyboard.act.login-captcha' },
  { key: 'refreshCaptcha', defaultLetter: 'F', titleKey: 'keyboard.act.login-captcha-refresh' },
  ...LOGIN_LANG_ACTION_DEFAULTS
]

/** 注册页默认动作 */
export const REGISTER_ACTION_DEFAULTS = [
  { key: 'username', defaultLetter: 'U', titleKey: 'keyboard.act.register-username' },
  { key: 'nickName', defaultLetter: 'H', titleKey: 'keyboard.act.register-nickname' },
  { key: 'password', defaultLetter: 'P', titleKey: 'keyboard.act.register-password' },
  { key: 'confirmPassword', defaultLetter: 'D', titleKey: 'keyboard.act.register-confirm-password' },
  { key: 'submit', defaultLetter: 'E', titleKey: 'keyboard.act.register-submit' },
  { key: 'goLogin', defaultLetter: 'L', titleKey: 'keyboard.act.register-go-login' },
  { key: 'refreshPage', defaultLetter: 'R', titleKey: 'keyboard.act.register-refresh-page' }
]

/** 通知页默认动作 */
export const NOTICE_ACTION_DEFAULTS = [
  { key: 'query', defaultLetter: 'S', titleKey: 'keyboard.act.query' },
  { key: 'switchTab', defaultLetter: 'J', titleKey: 'keyboard.act.switch-tab' },
  { key: 'config', defaultLetter: 'G', titleKey: 'keyboard.act.notice-config' },
  { key: 'send', defaultLetter: 'E', titleKey: 'keyboard.act.notice-send' },
  { key: 'prevPage', defaultLetter: 'B', titleKey: 'keyboard.act.prev-page' },
  { key: 'nextPage', defaultLetter: 'P', titleKey: 'keyboard.act.next-page' }
]

/** 系统文档页（/system/doc）默认动作 */
export const DOC_ACTION_DEFAULTS = [
  { key: 'query', defaultLetter: 'S', titleKey: 'keyboard.act.query' },
  { key: 'back', defaultLetter: 'B', titleKey: 'keyboard.act.doc-back' },
  { key: 'treeNav', defaultLetter: 'L', titleKey: 'keyboard.act.doc-tree-nav' },
  { key: 'outlineNav', defaultLetter: 'D', titleKey: 'keyboard.act.doc-outline-nav' },
  { key: 'print', defaultLetter: 'P', titleKey: 'keyboard.act.doc-print' }
]

/** 测试用例页默认动作 */
export const CASE_ACTION_DEFAULTS = [
  { key: 'query', defaultLetter: 'S', titleKey: 'keyboard.act.case-query' },
  { key: 'create', defaultLetter: 'E', titleKey: 'keyboard.act.case-create' },
  { key: 'aiCreate', defaultLetter: 'I', titleKey: 'keyboard.act.case-ai-create' },
  { key: 'batchDelete', defaultLetter: 'D', titleKey: 'keyboard.act.case-batch-delete' },
  { key: 'toggleTree', defaultLetter: 'M', titleKey: 'keyboard.act.case-toggle-tree' },
  { key: 'treeNav', defaultLetter: 'G', titleKey: 'keyboard.act.case-tree-nav' },
  { key: 'prevPage', defaultLetter: 'B', titleKey: 'keyboard.act.prev-page' },
  { key: 'nextPage', defaultLetter: 'P', titleKey: 'keyboard.act.next-page' }
]

/** 测试计划页默认动作 */
export const PLAN_ACTION_DEFAULTS = [
  { key: 'query', defaultLetter: 'S', titleKey: 'keyboard.act.plan-query' },
  { key: 'create', defaultLetter: 'E', titleKey: 'keyboard.act.plan-create' },
  { key: 'prevPage', defaultLetter: 'B', titleKey: 'keyboard.act.prev-page' },
  { key: 'nextPage', defaultLetter: 'P', titleKey: 'keyboard.act.next-page' }
]

/** 交付物页默认动作 */
export const MODULE_ACTION_DEFAULTS = [
  { key: 'query', defaultLetter: 'S', titleKey: 'keyboard.act.module-query' },
  { key: 'toggleExpand', defaultLetter: 'F', titleKey: 'keyboard.act.module-toggle-expand' },
  { key: 'treeNav', defaultLetter: 'G', titleKey: 'keyboard.act.module-tree-nav' },
  { key: 'create', defaultLetter: 'E', titleKey: 'keyboard.act.module-create' }
]

/** 项目报告页默认动作 */
export const REPORT_ACTION_DEFAULTS = [
  { key: 'query', defaultLetter: 'S', titleKey: 'keyboard.act.report-query' },
  { key: 'create', defaultLetter: 'E', titleKey: 'keyboard.act.report-create' },
  { key: 'batchDelete', defaultLetter: 'D', titleKey: 'keyboard.act.report-batch-delete' },
  { key: 'prevPage', defaultLetter: 'B', titleKey: 'keyboard.act.prev-page' },
  { key: 'nextPage', defaultLetter: 'P', titleKey: 'keyboard.act.next-page' }
]

/** 文档管理页默认动作 */
export const DOCUMENT_ACTION_DEFAULTS = [
  { key: 'query', defaultLetter: 'S', titleKey: 'keyboard.act.document-query' },
  { key: 'createFolder', defaultLetter: 'O', titleKey: 'keyboard.act.document-create-folder' },
  { key: 'createFile', defaultLetter: 'I', titleKey: 'keyboard.act.document-create-file' },
  { key: 'goUp', defaultLetter: 'U', titleKey: 'keyboard.act.document-go-up' },
  { key: 'prevPage', defaultLetter: 'B', titleKey: 'keyboard.act.prev-page' },
  { key: 'nextPage', defaultLetter: 'P', titleKey: 'keyboard.act.next-page' }
]

/** 项目设置枢纽页默认动作（子页返回；枢纽页入口按路由动态注册） */
export const PROJECT_OPTION_ACTION_DEFAULTS = [
  { key: 'back', defaultLetter: 'B', titleKey: 'keyboard.act.project-option-back' }
]

/** 团队设置枢纽页默认动作（子页返回；枢纽页入口按路由动态注册） */
export const TEAM_OPTION_ACTION_DEFAULTS = [
  { key: 'back', defaultLetter: 'B', titleKey: 'keyboard.act.team-option-back' }
]

/** 团队成员列表页默认动作 */
export const TEAM_MEMBER_ACTION_DEFAULTS = [
  { key: 'query', defaultLetter: 'S', titleKey: 'keyboard.act.team-member-query' },
  { key: 'create', defaultLetter: 'E', titleKey: 'keyboard.act.team-member-create' },
  { key: 'invite', defaultLetter: 'V', titleKey: 'keyboard.act.team-member-invite' },
  { key: 'back', defaultLetter: 'B', titleKey: 'keyboard.act.team-member-back' }
]

/** 个人中心页默认动作 */
export const PROFILE_ACTION_DEFAULTS = [
  { key: 'switchTab', defaultLetter: 'J', titleKey: 'keyboard.act.profile-switch-tab' },
  { key: 'back', defaultLetter: 'B', titleKey: 'keyboard.act.profile-back' }
]

const LETTER_POOL = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')
const DIGIT_POOL = '123456789'.split('')

/** 规范化按键为可比较字符（单字母大写 / 数字原样） */
/** 布局级等特殊单键（侧栏折叠 `` ` `` 等），与字母/数字并列参与映射 */
const LAYOUT_SINGLE_KEYS = new Set(['`', '~', '-', '=', '+', ',', '.', '/'])

export function normalizeKey(raw) {
  if (!raw) return ''
  const k = String(raw)
  if (k.length === 1) {
    if (/[a-zA-Z]/.test(k)) return k.toUpperCase()
    if (/[0-9]/.test(k)) return k
    if (LAYOUT_SINGLE_KEYS.has(k)) return k
  }
  return ''
}

/**
 * 为一组项分配唯一字母。
 * @param {Array<{preferred?:string,title?:string}>} items 每项含 preferred(首选) 与 title(用于回退取首字母)
 * @returns {Array} 原数组，写入 item.letter
 */
export function assignLetters(items) {
  const used = new Set()
  const take = (letter) => {
    const n = normalizeKey(letter)
    if (n && !used.has(n)) {
      used.add(n)
      return n
    }
    return ''
  }
  // 第一轮：满足首选
  items.forEach((it) => {
    it.letter = take(it.preferred)
  })
  // 第二轮：回退到标题首字母，再回退字母池 / 数字池
  items.forEach((it) => {
    if (it.letter) return
    let letter = ''
    const title = (it.title || '').replace(/[^a-zA-Z]/g, '')
    for (const ch of title) {
      letter = take(ch)
      if (letter) break
    }
    if (!letter) {
      for (const ch of LETTER_POOL) {
        letter = take(ch)
        if (letter) break
      }
    }
    if (!letter) {
      for (const ch of DIGIT_POOL) {
        letter = take(ch)
        if (letter) break
      }
    }
    it.letter = letter
  })
  return items
}

/** 按页面 scope 返回默认动作表（供动作面板解析 titleKey / defaultLetter） */
export function getPageActionDefaults(scopeKey) {
  switch (scopeKey) {
    case 'login':
      return LOGIN_ACTION_DEFAULTS
    case 'register':
      return REGISTER_ACTION_DEFAULTS
    case 'notice':
      return NOTICE_ACTION_DEFAULTS
    case 'statistic-template':
      return STATISTIC_TEMPLATE_ACTION_DEFAULTS
    case 'case':
      return CASE_ACTION_DEFAULTS
    case 'plan':
      return PLAN_ACTION_DEFAULTS
    case 'module':
      return MODULE_ACTION_DEFAULTS
    case 'report':
      return REPORT_ACTION_DEFAULTS
    case 'document':
      return DOCUMENT_ACTION_DEFAULTS
    case 'project-option':
      return PROJECT_OPTION_ACTION_DEFAULTS
    case 'team-option':
      return TEAM_OPTION_ACTION_DEFAULTS
    case 'team-member':
      return TEAM_MEMBER_ACTION_DEFAULTS
    case 'profile':
      return PROFILE_ACTION_DEFAULTS
    case 'doc':
      return DOC_ACTION_DEFAULTS
    default:
      return DEFECT_ACTION_DEFAULTS
  }
}

export function findPageActionDefault(key, scopeKey) {
  return getPageActionDefaults(scopeKey).find((d) => d.key === key)
}
