import { getToken } from '@/utils/auth'
import { pinia } from '@/store/pinia'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import { isRelogin } from '@/utils/request'

/**
 * 在 app.mount 之前注入动态菜单路由，避免首屏直达 /project/* 时
 * vue-router 4 报 No match found（动态路由尚未 addRoute）。
 *
 * 冷启动若 Cookie 中残留过期 token：静默清会话，禁止弹出「登录状态已过期」
 * （否则拦截器弹窗会盖在随后进入的登录页上）。
 */
export async function bootstrapAuthenticatedRoutes() {
  if (!getToken()) return

  const userStore = useUserStore(pinia)
  const permissionStore = usePermissionStore(pinia)
  if (permissionStore.addRoutes.length > 0) return

  isRelogin.show = true
  try {
    if (userStore.roles.length === 0) {
      await userStore.GetInfo()
    }
    await permissionStore.GenerateRoutes()
  } catch (_err) {
    userStore.FedLogOut()
  } finally {
    isRelogin.show = false
  }
}
