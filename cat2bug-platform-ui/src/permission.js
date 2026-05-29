import router from './router'
import store from './store'
import { Message } from 'element-ui'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { isRelogin } from '@/utils/request'
import { isLockTeamPath } from '@/utils/team'
import { isLockProjectPath } from '@/utils/project'
import { resolveSetupStatus } from '@/utils/setup-status'
import { getCodeImg } from '@/api/login'
import i18n from '@/utils/i18n/i18n'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/register', '/tools/browser', '/shard/defect', '/setup']

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

router.beforeEach((to, from, next) => {
  NProgress.start()
  const refreshSetupStatus = (from.path === '/login' && !!getToken()) || to.path === '/login'
  resolveSetupStatus(refreshSetupStatus).then(status => {
    if (status.restartRequired) {
      if (to.path === '/setup') {
        next()
      } else {
        next('/setup')
      }
      NProgress.done()
      return
    }
    if (!status.installed) {
      if (to.path === '/setup') {
        next()
      } else {
        next('/setup')
      }
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

router.afterEach(() => {
  NProgress.done()
})
