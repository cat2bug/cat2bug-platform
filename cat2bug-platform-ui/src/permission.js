import router from './router'
import store from './store'
import { Message } from 'element-ui'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { isRelogin } from '@/utils/request'
import { isLockTeamPath } from '@/utils/team'
import { isLockProjectPath } from '@/utils/project'
import { resolveUpgradeStatus } from '@/utils/upgrade-status'
import { resolveSetupStatus } from '@/utils/setup-status'
import { isUpgradeAwaitingReady } from '@/utils/upgrade-await'
import { getCodeImg } from '@/api/login'
import i18n from '@/utils/i18n/i18n'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/register', '/tools/browser', '/shard/defect', '/setup', '/upgrade']

function handleUpgradeAwaitingReadyRoute(to, next) {
  if (!isUpgradeAwaitingReady()) {
    return false
  }
  if (to.path === '/upgrade') {
    next()
  } else {
    next('/upgrade')
  }
  NProgress.done()
  return true
}

function proceedWithAuth(to, from, next) {
  if (getToken()) {
    to.meta.title && store.dispatch('settings/setTitle', to.meta.title)
    if (to.path === '/login') {
      next({ path: '/' })
      NProgress.done()
    } else if (isLockTeamPath(to.path)) {
      next({ path: '/error/team-lock', replace: true })
    } else if (isLockProjectPath(to.path)) {
      next({ path: '/error/project-lock', replace: true })
    } else if (store.getters.roles.length === 0) {
      isRelogin.show = true
      store.dispatch('GetInfo').then(() => {
        isRelogin.show = false
        store.dispatch('GenerateRoutes').then(() => {
          next({ ...to, replace: true })
        }).catch(err => {
          isRelogin.show = false
          store.dispatch('LogOut').then(() => {
            Message.error(err && err.message ? err.message : err)
            next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
          })
        })
      }).catch(err => {
        isRelogin.show = false
        store.dispatch('LogOut').then(() => {
          Message.error(err && err.message ? err.message : err)
          next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
        })
      })
    } else {
      next()
    }
  } else if (whiteList.indexOf(to.path) !== -1) {
    next()
  } else {
    next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
    NProgress.done()
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
    NProgress.done()
    return true
  }
  if (setupStatus.installed) {
    if (to.path === '/setup' || to.path === '/login') {
      next()
    } else {
      next('/setup')
    }
    NProgress.done()
    return true
  }
  if (to.path === '/setup') {
    next()
  } else {
    next('/setup')
  }
  NProgress.done()
  return true
}

router.beforeEach((to, from, next) => {
  NProgress.start()
  if (handleUpgradeAwaitingReadyRoute(to, next)) {
    return
  }
  const refreshSetupStatus = (from.path === '/login' && !!getToken()) || to.path === '/login'

  resolveSetupStatus(refreshSetupStatus).then(setupStatus => {
    if (setupStatus.restartRequired) {
      resolveUpgradeStatus(true).then(upgradeStatus => {
        handleInstallRestartWait(to, next, upgradeStatus, setupStatus)
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
      NProgress.done()
      return
    }

    const refreshUpgradeStatus = to.path === '/upgrade' || from.path === '/upgrade'
    resolveUpgradeStatus(refreshUpgradeStatus).then(upgradeStatus => {
      const state = upgradeStatus.state || ''
      const wizardLocked = upgradeStatus.upgradeRequired
        && ['pending', 'running', 'failed'].includes(state)

      if (wizardLocked) {
        if (to.path !== '/upgrade') {
          next('/upgrade')
          NProgress.done()
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
        NProgress.done()
        return
      }

      if (state === 'restart_required' || upgradeStatus.restartRequired) {
        if (to.path === '/upgrade') {
          next()
          return
        }
        next('/upgrade')
        NProgress.done()
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
        NProgress.done()
        return
      }

      if (to.path === '/setup') {
        next({ path: '/login' })
        NProgress.done()
        return
      }

      if (to.path === '/login' && getToken()) {
        next({ path: '/' })
        NProgress.done()
        return
      }

      if (to.path === '/register') {
        getCodeImg().then(res => {
          const registerEnabled = res.registerEnabled === undefined ? true : res.registerEnabled
          if (!registerEnabled) {
            Message.warning(i18n.t('registration-closed').toString())
            next('/login')
            NProgress.done()
          } else {
            proceedWithAuth(to, from, next)
          }
        }).catch(() => {
          next('/login')
          NProgress.done()
        })
        return
      }

      proceedWithAuth(to, from, next)
    })
  })
})

router.afterEach(() => {
  NProgress.done()
})
