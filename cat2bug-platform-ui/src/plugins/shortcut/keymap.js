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
  document: 'W', // 文档管理
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
  { key: 'new', defaultLetter: 'N', titleKey: 'keyboard.act.new-defect' },
  { key: 'export', defaultLetter: 'E', titleKey: 'keyboard.act.export' },
  { key: 'import', defaultLetter: 'I', titleKey: 'keyboard.act.import' },
  { key: 'query', defaultLetter: 'Q', titleKey: 'keyboard.act.query' },
  { key: 'switchTab', defaultLetter: 'J', titleKey: 'keyboard.act.switch-tab' },
  { key: 'statistic', defaultLetter: 'V', titleKey: 'keyboard.act.statistic' },
  { key: 'statScrollNext', defaultLetter: 'R', titleKey: 'keyboard.act.stat-scroll-next' },
  { key: 'trendExport', defaultLetter: 'G', titleKey: 'keyboard.act.trend-export' },
  { key: 'switchView', defaultLetter: 'O', titleKey: 'keyboard.act.switch-view' },
  { key: 'displayFields', defaultLetter: 'F', titleKey: 'keyboard.act.display-fields' },
  { key: 'prevPage', defaultLetter: 'B', titleKey: 'keyboard.act.prev-page' },
  { key: 'nextPage', defaultLetter: 'P', titleKey: 'keyboard.act.next-page' }
]

const LETTER_POOL = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')
const DIGIT_POOL = '123456789'.split('')

/** 规范化按键为可比较字符（单字母大写 / 数字原样） */
export function normalizeKey(raw) {
  if (!raw) return ''
  const k = String(raw)
  if (k.length === 1) {
    if (/[a-zA-Z]/.test(k)) return k.toUpperCase()
    if (/[0-9]/.test(k)) return k
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
