import { pinia } from '@/store/pinia.js'
import { useUserStore } from '@/stores/user'

/** 当前用户是否为当前上下文团队的正式成员 */
export function isCurrentTeamMember() {
  return useUserStore(pinia).teamMember !== false
}

/** 直链团队管理/设置类路径（仅项目成员应被拦截） */
export function isTeamManagementPath(path) {
  if (!path) {
    return false
  }
  if (path.startsWith('/team-options')) {
    return true
  }
  if (path.startsWith('/team/')) {
    return true
  }
  if (path.startsWith('/system/team/')) {
    return true
  }
  return false
}
