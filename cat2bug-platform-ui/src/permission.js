import router from './router'
import { pinia } from '@/store/pinia.js'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import { useSettingsStore } from '@/stores/settings'
import { ElMessage as Message } from 'element-plus'
import NProgress from 'nprogress'
import { getToken } from '@/utils/auth'
import { dismissReloginPrompt, isAuthWhitePath, isRelogin } from '@/utils/request'
import { isLockTeamPath } from '@/utils/team'
import { isLockProjectPath } from '@/utils/project'
import { isCurrentTeamMember, isTeamManagementPath } from '@/utils/team-membership'
import { resolveUpgradeStatus } from '@/utils/upgrade-status'
import { resolveSetupStatus } from '@/utils/setup-status'
import { isUpgradeAwaitingReady } from '@/utils/upgrade-await'
import { getCodeImg } from '@/api/login'
import i18n from '@/utils/i18n/i18n'
import { cancelColdBootComplete, installNProgressColdBootGuard, isColdBootPending, scheduleColdBootComplete } from '@/utils/bootstrap-progress'
import { ensureProjectScopedContext } from '@/utils/project-scoped-guard'
import { ensureGitRepoIdAttached, routeNeedsGitRepoId } from '@/utils/git-repo-route-guards'

NProgress.configure({ showSpinner: false })
installNProgressColdBootGuard(NProgress)

const PAGE_PROGRESS_COLOR = '#409eff'

function paintNProgressBar() {
  const root = document.documentElement
  const color = getComputedStyle(root).getPropertyValue('--page-progress-color').trim() || PAGE_PROGRESS_COLOR
  const bar = document.querySelector('#nprogress .bar')
  if (bar) {
    bar.style.setProperty('background', color, 'important')
  }
  const peg = document.querySelector('#nprogress .peg')
  if (peg) {
    peg.style.setProperty('box-shadow', `0 0 10px ${color}, 0 0 5px ${color}`, 'important')
  }
}

;['start', 'set', 'inc'].forEach((method) => {
  const original = NProgress[method].bind(NProgress)
  NProgress[method] = function (...args) {
    const result = original(...args)
    requestAnimationFrame(paintNProgressBar)
    return result
  }
})

const whiteList = ['/login', '/register', '/tools/browser', '/shard/defect', '/setup', '/upgrade']

function isPublicPath(path) {
  if (whiteList.indexOf(path) !== -1) {
    return true
  }
  return /^\/invite\/[^/]+$/.test(path)
}

function startRouteProgress() {
  if (isColdBootPending()) return
  NProgress.start()
}

function finishRouteProgress() {
  if (isColdBootPending()) return
  NProgress.done()
}

function handleUpgradeAwaitingReadyRoute(to, next) {
  if (!isUpgradeAwaitingReady()) {
    return false
  }
  if (to.path === '/upgrade') {
    next()
  } else {
    next('/upgrade')
  }
  return true
}

/**
 * Inject repositoryId for code/:slug routes. Route `beforeEnter` does not re-run
 * when only the slug param changes (repo switch), so this must live in beforeEach.
 */
function proceedAfterGitRepoId(to, next) {
  if (!routeNeedsGitRepoId(to)) {
    next()
    return
  }
  ensureGitRepoIdAttached(to).then(result => {
    if (result === 'ok') {
      next()
      return
    }
    next({ path: '/404', replace: true })
  }).catch(() => {
    next({ path: '/404', replace: true })
  })
}

/**
 * After auth + dynamic routes are ready: align current project from path keys.
 * Keeps setup/upgrade/login flows untouched (only reached via proceedWithAuth).
 */
function proceedAfterProjectContext(to, next) {
  ensureProjectScopedContext(to).then(result => {
    if (result === 'skip' || result === 'ok') {
      proceedAfterGitRepoId(to, next)
      return
    }
    if (result === 'switched') {
      // Route table may have been rebuilt with the new project; re-enter cleanly.
      next({ ...to, replace: true })
      return
    }
    if (result === 'forbidden') {
      next({ path: '/401', replace: true })
      return
    }
    next({ path: '/404', replace: true })
  }).catch(() => {
    next({ path: '/404', replace: true })
  })
}

function proceedWithAuth(to, from, next) {
  const userStore = useUserStore(pinia)
  const permissionStore = usePermissionStore(pinia)
  const settingsStore = useSettingsStore(pinia)

  if (getToken()) {
    to.meta.title && settingsStore.setTitle(to.meta.title)
    if (to.path === '/login') {
      next({ path: '/' })
    } else if (isLockTeamPath(to.path)) {
      next({ path: '/error/team-lock', replace: true })
    } else if (isLockProjectPath(to.path)) {
      next({ path: '/error/project-lock', replace: true })
    } else if (!isCurrentTeamMember() && isTeamManagementPath(to.path)) {
      next({ path: '/404', replace: true })
    } else if (userStore.roles.length === 0 || permissionStore.addRoutes.length === 0) {
      isRelogin.show = true
      userStore.GetInfo().then(() => {
        isRelogin.show = false
        permissionStore.GenerateRoutes().then(() => {
          next({ ...to, replace: true })
        }).catch(() => {
          // 路由生成失败：清本地会话后去登录，勿再弹「重新登录」/错误条
          userStore.FedLogOut()
          dismissReloginPrompt()
          next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
        })
      }).catch(() => {
        // getInfo 401/会话失效：FedLogOut 避免再请求 /logout 触发二次弹框
        userStore.FedLogOut()
        dismissReloginPrompt()
        next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
      })
    } else {
      proceedAfterProjectContext(to, next)
    }
  } else if (isPublicPath(to.path)) {
    next()
  } else {
    next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
  }
}

function handleInstallRestartWait(to, next, upgradeStatus, setupStatus) {
  const upgradeRestart = upgradeStatus.state === 'restart_required' || upgradeStatus.restartRequired
  const upgradeFlowHint = isUpgradeAwaitingReady()
  if (upgradeRestart || (setupStatus.installed && upgradeFlowHint)) {
    if (to.path === '/upgrade' || to.path === '/login') {
      next()
    } else {
      next('/upgrade')
    }
    return true
  }
  if (setupStatus.installed) {
    // 安装完成待重启：允许 /setup 等待页与 /login；其它路径去登录
    if (to.path === '/setup' || to.path === '/login') {
      next()
    } else {
      next('/login')
    }
    return true
  }
  if (to.path === '/setup') {
    next()
  } else {
    next('/setup')
  }
  return true
}

function handleInstalledRouteGuards(to, from, next, setupStatus, upgradeStatus) {
  const state = upgradeStatus.state || ''
  const wizardLocked = upgradeStatus.upgradeRequired
    && ['pending', 'running', 'failed'].includes(state)

  if (wizardLocked) {
    if (to.path !== '/upgrade') {
      next('/upgrade')
      return
    }
    next()
    return
  }

  if (upgradeStatus.upgradeRequired
      && state !== 'restart_required'
      && !upgradeStatus.restartRequired
      && to.path !== '/upgrade') {
    next('/upgrade')
    return
  }

  if (state === 'restart_required' || upgradeStatus.restartRequired) {
    if (to.path === '/upgrade') {
      next()
      return
    }
    next('/upgrade')
    return
  }

  if (to.path === '/upgrade') {
    if (upgradeStatus.upgradeRequired || wizardLocked) {
      next()
      return
    }
    if (isUpgradeAwaitingReady()) {
      next()
      return
    }
    next({ path: '/login' })
    return
  }

  if (to.path === '/setup') {
    next({ path: '/login' })
    return
  }

  if (to.path === '/login' && getToken()) {
    next({ path: '/' })
    return
  }

  if (to.path === '/register') {
    getCodeImg().then(res => {
      const registerEnabled = res.registerEnabled === undefined ? true : res.registerEnabled
      if (!registerEnabled) {
        Message.warning(i18n.global.t('registration-closed').toString())
        next('/login')
      } else {
        proceedWithAuth(to, from, next)
      }
    }).catch(() => {
      next('/login')
    })
    return
  }

  proceedWithAuth(to, from, next)
}

router.beforeEach((to, from, next) => {
  cancelColdBootComplete()
  startRouteProgress()
  // 进入登录/安装等页时关掉可能残留的会话过期弹窗（冷启动 401 或业务页跳转后）
  if (isAuthWhitePath(to.path)) {
    dismissReloginPrompt()
  }
  if (handleUpgradeAwaitingReadyRoute(to, next)) {
    return
  }

  // 进出安装/登录页时强制刷新，避免 session 缓存把已安装误判为未安装而锁死在向导
  const refreshSetupStatus = (from.path === '/login' && !!getToken())
    || to.path === '/login'
    || to.path === '/setup'
    || from.path === '/setup'
  const refreshUpgradeStatus = to.path === '/upgrade' || from.path === '/upgrade'

  Promise.all([
    resolveSetupStatus(refreshSetupStatus),
    resolveUpgradeStatus(refreshUpgradeStatus)
  ]).then(([setupStatus, upgradeStatus]) => {
    if (setupStatus.restartRequired) {
      resolveUpgradeStatus(true).then(freshUpgradeStatus => {
        handleInstallRestartWait(to, next, freshUpgradeStatus, setupStatus)
      }).catch(() => {
        handleInstallRestartWait(to, next, { state: '', restartRequired: false }, setupStatus)
      })
      return
    }

    if (!setupStatus.installed) {
      if (to.path === '/setup') {
        next()
      } else {
        next('/setup')
      }
      return
    }

    handleInstalledRouteGuards(to, from, next, setupStatus, upgradeStatus)
  })
})

router.afterEach((to) => {
  if (isAuthWhitePath(to.path)) {
    dismissReloginPrompt()
  }
  if (isColdBootPending()) {
    scheduleColdBootComplete(220)
    return
  }
  finishRouteProgress()
})

resolveSetupStatus()
resolveUpgradeStatus()
