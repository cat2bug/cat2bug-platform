import { pinia } from '@/store/pinia'
import { useUserStore } from '@/stores/user'
import { myListTeam } from '@/api/system/team'
import { getProject } from '@/api/system/project'

/** Pinia-backed project/user accessors (safe outside component setup). */
export function getCurrentProjectId() {
  const config = useUserStore(pinia).config || {}
  return parseInt(config.currentProjectId || 0)
}

export function getCurrentUserId() {
  const id = useUserStore(pinia).id
  return id == null || id === '' ? null : id
}

export function getCurrentTeamId() {
  const config = useUserStore(pinia).config || {}
  const tid = Number(config.currentTeamId)
  return Number.isFinite(tid) && tid > 0 ? tid : null
}

export function canUseAdminProjectList() {
  const perms = useUserStore(pinia).permissions
  return Array.isArray(perms) && (perms.includes('*:*:*') || perms.includes('admin:project:list'))
}

function resolveTeamIdFromMyList() {
  return myListTeam().then((res) => {
    const first = res && res.rows && res.rows[0]
    const tid = first && Number(first.teamId)
    return (Number.isFinite(tid) && tid > 0) ? tid : null
  }).catch(() => null)
}

function resolveTeamIdFromConfigOrContext(userStore) {
  const direct = getCurrentTeamId()
  if (direct) {
    return Promise.resolve(direct)
  }
  const ctx = userStore.contextTeam
  if (ctx && ctx.teamId) {
    const tid = Number(ctx.teamId)
    if (Number.isFinite(tid) && tid > 0) {
      return Promise.resolve(tid)
    }
  }
  return resolveTeamIdFromMyList()
}

/** 当 user.config.currentTeamId 为空或与当前项目不一致时，优先用当前项目所属团队 */
export function resolveEffectiveTeamId() {
  const userStore = useUserStore(pinia)
  const projectId = Number((userStore.config || {}).currentProjectId)
  if (Number.isFinite(projectId) && projectId > 0) {
    return getProject(projectId).then((res) => {
      const tid = res && res.data && Number(res.data.teamId)
      if (Number.isFinite(tid) && tid > 0) {
        return tid
      }
      return resolveTeamIdFromConfigOrContext(userStore)
    }).catch(() => resolveTeamIdFromConfigOrContext(userStore))
  }
  return resolveTeamIdFromConfigOrContext(userStore)
}
