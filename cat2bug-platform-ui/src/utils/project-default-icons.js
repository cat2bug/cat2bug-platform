const projectIconModules = import.meta.glob('@/assets/images/project/project_icon*.svg', {
  eager: true,
  query: '?url',
  import: 'default'
})

/** @type {Record<number, string>} */
const projectIconUrlByIndex = {}

Object.keys(projectIconModules).forEach((path) => {
  const match = path.match(/project_icon(\d+)\.svg$/)
  if (match) {
    projectIconUrlByIndex[Number(match[1])] = projectIconModules[path]
  }
})

export const PROJECT_DEFAULT_ICON_COUNT = 10

/**
 * @param {number} [index] 1–10
 * @returns {string}
 */
export function getDefaultProjectIconUrl(index = 1) {
  const n = Number(index)
  if (Number.isFinite(n) && projectIconUrlByIndex[n]) {
    return projectIconUrlByIndex[n]
  }
  return projectIconUrlByIndex[1] || ''
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
