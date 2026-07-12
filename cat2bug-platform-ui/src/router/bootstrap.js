import { getToken } from '@/utils/auth'
import { pinia } from '@/store/pinia'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'

/**
 * 在 app.mount 之前注入动态菜单路由，避免首屏直达 /project/* 时
 * vue-router 4 报 No match found（动态路由尚未 addRoute）。
 */
export async function bootstrapAuthenticatedRoutes() {
  if (!getToken()) return

  const userStore = useUserStore(pinia)
  const permissionStore = usePermissionStore(pinia)
  if (permissionStore.addRoutes.length > 0) return

  if (userStore.roles.length === 0) {
    await userStore.GetInfo()
  }
  await permissionStore.GenerateRoutes()
}
