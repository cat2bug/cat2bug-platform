import { useSettingsStore } from '@/stores/settings'

const projectIconModules = import.meta.glob('@/assets/images/project/project_icon*.svg', {
  eager: true,
  query: '?url',
  import: 'default'
})

/** @type {Record<number, string>} */
const projectIconUrlByIndex = {}
/** @type {Record<number, string>} */
const projectIconDarkUrlByIndex = {}

Object.keys(projectIconModules).forEach((path) => {
  const darkMatch = path.match(/project_icon(\d+)\.dark\.svg$/)
  if (darkMatch) {
    projectIconDarkUrlByIndex[Number(darkMatch[1])] = projectIconModules[path]
    return
  }
  const match = path.match(/project_icon(\d+)\.svg$/)
  if (match) {
    projectIconUrlByIndex[Number(match[1])] = projectIconModules[path]
  }
})

/** 供封面地址计算依赖当前主题，切换明暗时预览和列表会一起换图 */
export function projectIconThemeKey() {
  try {
    const mode = useSettingsStore().themeMode
    if (mode === 'dark' || mode === 'light') {
      return mode
    }
  } catch (e) {
    /* 单测或 Pinia 尚未安装时按页面 class 判断 */
  }
  if (typeof document !== 'undefined' && document.documentElement.classList.contains('dark')) {
    return 'dark'
  }
  return 'light'
}

export const PROJECT_DEFAULT_ICON_COUNT = 10

/**
 * @param {number} [index] 1–10
 * @returns {string}
 */
export function getDefaultProjectIconUrl(index = 1) {
  const n = Number(index)
  const dark = projectIconThemeKey() === 'dark'
  const table = dark && projectIconDarkUrlByIndex[n] ? projectIconDarkUrlByIndex : projectIconUrlByIndex
  const fallback = dark && projectIconDarkUrlByIndex[1] ? projectIconDarkUrlByIndex : projectIconUrlByIndex
  if (Number.isFinite(n) && table[n]) {
    return table[n]
  }
  return fallback[1] || projectIconUrlByIndex[1] || ''
}

/** 与历史 Webpack 构建一致，供后端持久化（无 contenthash） */
export function getProjectIconStoragePath(index = 1) {
  const n = Number(index)
  const safe = Number.isFinite(n) && n >= 1 && n <= PROJECT_DEFAULT_ICON_COUNT ? n : 1
  return `/static/img/project_icon${safe}.svg`
}

/**
 * 从任意项目封面路径解析预设图标序号（1–10）。
 * @param {string | null | undefined} path
 * @returns {number | null}
 */
export function parseProjectIconIndex(path) {
  if (path == null || typeof path !== 'string') {
    return null
  }
  const match = path.trim().match(/project_icon(\d{1,2})(?:\.[a-f0-9]+)?\.svg/i)
  if (!match) {
    return null
  }
  const index = Number(match[1])
  if (!Number.isFinite(index) || index < 1 || index > PROJECT_DEFAULT_ICON_COUNT) {
    return null
  }
  return index
}
