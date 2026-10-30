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

/**
 * 解析当前有效团队：优先 config/context 中的 teamId（项目列表页常见无 currentProject）。
 * 仅当尚无团队且仍有 currentProjectId 时，再按项目反查团队（避免无谓项目 getProject 403 干扰列表）。
 */
export function resolveEffectiveTeamId() {
  const userStore = useUserStore(pinia)
  return resolveTeamIdFromConfigOrContext(userStore).then((fromConfig) => {
    if (fromConfig) {
      return fromConfig
    }
    const projectId = Number((userStore.config || {}).currentProjectId)
    if (!(Number.isFinite(projectId) && projectId > 0)) {
      return null
    }
    return getProject(projectId).then((res) => {
      const tid = res && res.data && Number(res.data.teamId)
      return (Number.isFinite(tid) && tid > 0) ? tid : null
    }).catch(() => null)
  })
}
