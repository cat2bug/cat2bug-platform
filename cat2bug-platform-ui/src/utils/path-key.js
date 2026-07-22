/** 团队/项目路径键：与后端 PathKeyValidator 保持同步 */
export const PATH_KEY_PATTERN = /^[a-zA-Z0-9](?:[a-zA-Z0-9._-]{0,98}[a-zA-Z0-9])?$/

export const PATH_KEY_RESERVED = new Set([
  'login', 'register', 'logout', 'setup', 'upgrade', 'static', 'profile', 'prod-api',
  'git', 'system', 'project', 'docs', 'websocket', 'captchaImage', 'version', 'index',
  'home', 'api', 'oauth', 'actuator', 'error', 'favicon.ico', 'code'
])

export function isPathKeyReserved(key) {
  if (!key) return false
  return PATH_KEY_RESERVED.has(String(key).trim().toLowerCase())
}

export function isPathKeyFormatValid(key) {
  return !!key && PATH_KEY_PATTERN.test(String(key).trim())
}

export function normalizePathKey(key) {
  if (!key) return null
  const trimmed = String(key).trim()
  if (!isPathKeyFormatValid(trimmed) || isPathKeyReserved(trimmed)) return null
  return trimmed
}

/** Element UI 表单校验（与后端 PathKeyValidator 一致） */
export function createPathKeyValidator(messages) {
  return (rule, value, callback) => {
    const trimmed = value != null ? String(value).trim() : ''
    if (!trimmed) {
      callback(new Error(messages.required))
      return
    }
    if (isPathKeyReserved(trimmed)) {
      callback(new Error(messages.reserved))
      return
    }
    if (!isPathKeyFormatValid(trimmed)) {
      callback(new Error(messages.format))
      return
    }
    callback()
  }
}

export function buildProjectPathSegment(teamKey, projectKey) {
  if (!teamKey || !projectKey) return ''
  return `/${teamKey}/${projectKey}`
}

/** 完整分享 URL（History 路由，无 `#`） */
export function buildProjectShareUrl(teamKey, projectKey) {
  const segment = buildProjectPathSegment(teamKey, projectKey)
  if (!segment || typeof window === 'undefined') return segment
  const base = (process.env.BASE_URL || '/').replace(/\/$/, '')
  return `${window.location.origin}${base}${segment}`
}

export function confirmPathKeyRename(vm, originalKey, newKey, onConfirm) {
  const orig = (originalKey != null ? String(originalKey) : '').trim()
  const next = (newKey != null ? String(newKey) : '').trim()
  if (orig && next && orig !== next) {
    vm.$modal.confirm(vm.$t('path-key.rename-warning').toString()).then(onConfirm).catch(() => {})
  } else {
    onConfirm()
  }
}
