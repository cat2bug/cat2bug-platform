import { ElMessageBox } from 'element-plus'
import { pinia } from '@/store/pinia.js'
import { useUserStore } from '@/stores/user'
import { updateConfig } from '@/api/system/user-config'
import i18n from '@/utils/i18n/i18n'

/** 与后端 ProjectMembershipAuth.PROJECT_REMOVED_MESSAGE 保持一致 */
export const PROJECT_REMOVED_MESSAGE = '您已被移出该项目'

const REDIRECT_SECONDS = 10
const PROJECT_LIST_PATH = '/team/project-list'

let prompting = false
let countdownTimer = null
let autoRedirectTimer = null

export function isProjectRemovedMessage(msg) {
  if (!msg) return false
  const text = String(msg)
  return text === PROJECT_REMOVED_MESSAGE || text.includes(PROJECT_REMOVED_MESSAGE)
}

function clearTimers() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  if (autoRedirectTimer) {
    clearTimeout(autoRedirectTimer)
    autoRedirectTimer = null
  }
}

function hasCurrentProjectContext() {
  const id = Number((useUserStore(pinia).config || {}).currentProjectId || 0)
  return Number.isFinite(id) && id > 0
}

function updateCountdownDom(seconds) {
  const el = document.getElementById('project-removed-countdown')
  if (el) {
    el.textContent = String(seconds)
  }
}

function extractProjectIdFromRequestUrl(url) {
  if (!url) return null
  const m = String(url).match(/\/system\/project\/(\d+)(?:\/|$|\?)/)
  if (!m) return null
  const id = Number(m[1])
  return Number.isFinite(id) && id > 0 ? id : null
}

/**
 * 当前上下文仍指向某项目、但接口返回「已被移出」时：弹框提示，10 秒后（或点确定）跳转项目列表。
 * @returns {boolean} 是否已接管该错误（调用方勿再 toast）
 */
export function handleProjectRemovedResponse(msg, axiosConfig) {
  if (!isProjectRemovedMessage(msg) || !hasCurrentProjectContext()) {
    return false
  }
  const currentId = Number((useUserStore(pinia).config || {}).currentProjectId)
  const urlProjectId = extractProjectIdFromRequestUrl(axiosConfig && axiosConfig.url)
  // 切到其它无权项目时 URL 带目标 id，勿当成「当前项目被移出」
  if (urlProjectId != null && urlProjectId !== currentId) {
    return false
  }
  if (prompting) {
    return true
  }
  prompting = true
  let seconds = REDIRECT_SECONDS
  let redirected = false

  const redirect = () => {
    if (redirected) return
    redirected = true
    clearTimers()
    try {
      ElMessageBox.close()
    } catch (_e) {
      /* ignore */
    }
    const userStore = useUserStore(pinia)
    Promise.resolve()
      .then(() => updateConfig({ currentProjectId: 0 }).catch(() => {}))
      .then(() => userStore.GetInfo().catch(() => {}))
      .finally(() => {
        prompting = false
        const path = ((window.location && window.location.pathname) || '').split('?')[0] || '/'
        if (path === PROJECT_LIST_PATH || path.startsWith(PROJECT_LIST_PATH + '/')) {
          return
        }
        window.location.assign(PROJECT_LIST_PATH)
      })
  }

  const bodyHtml =
    '<p>' +
    i18n.global.t('project.removed-from-project-desc') +
    '</p><p>' +
    i18n.global.t('project.removed-from-project-countdown-prefix') +
    '<strong id="project-removed-countdown">' +
    seconds +
    '</strong>' +
    i18n.global.t('project.removed-from-project-countdown-suffix') +
    '</p>'

  ElMessageBox.alert(bodyHtml, i18n.global.t('project.removed-from-project-title'), {
    dangerouslyUseHTMLString: true,
    type: 'warning',
    confirmButtonText: i18n.global.t('project.go-project-list-now'),
    showClose: false,
    closeOnClickModal: false,
    closeOnPressEscape: false,
    distinguishCancelAndClose: false,
    callback: () => redirect()
  })

  countdownTimer = setInterval(() => {
    seconds -= 1
    if (seconds <= 0) {
      clearInterval(countdownTimer)
      countdownTimer = null
      return
    }
    updateCountdownDom(seconds)
  }, 1000)

  autoRedirectTimer = setTimeout(() => redirect(), REDIRECT_SECONDS * 1000)
  return true
}
