/** 看板泳道负责人：默认角色、本机缓存、全局执行人过滤 */

export const LANE_PROCESSING = 'processing'
export const LANE_AUDIT = 'audit'

export const ROLE_DEVELOP_KEYS = ['project.develop', 'project.developer']
export const ROLE_TESTER_KEYS = ['project.tester']
export const ROLE_ADMIN_KEYS = ['project.admin', 'project.create-by']

/**
 * @param {number|string} projectId
 * @returns {string}
 */
export function laneAssigneesStorageKey(projectId) {
  return `defect.kanban.laneAssignees.${projectId}`
}

/**
 * @param {number|string} projectId
 * @returns {{ processing: number[], audit: number[] }|null}
 */
export function readLaneAssigneesCache(projectId) {
  if (projectId == null || projectId === '') return null
  try {
    const raw = localStorage.getItem(laneAssigneesStorageKey(projectId))
    if (!raw) return null
    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object') return null
    return {
      processing: normalizeIdList(parsed.processing),
      audit: normalizeIdList(parsed.audit)
    }
  } catch (e) {
    return null
  }
}

/**
 * @param {number|string} projectId
 * @param {{ processing?: number[], audit?: number[] }} data
 */
export function writeLaneAssigneesCache(projectId, data) {
  if (projectId == null || projectId === '') return
  try {
    localStorage.setItem(
      laneAssigneesStorageKey(projectId),
      JSON.stringify({
        processing: normalizeIdList(data && data.processing),
        audit: normalizeIdList(data && data.audit)
      })
    )
  } catch (e) {
    /* ignore quota */
  }
}

/**
 * @param {unknown} list
 * @returns {number[]}
 */
export function normalizeIdList(list) {
  if (!Array.isArray(list)) return []
  const out = []
  const seen = new Set()
  list.forEach((x) => {
    const id = Number(x)
    if (!Number.isFinite(id) || seen.has(id)) return
    seen.add(id)
    out.push(id)
  })
  return out
}

/**
 * @param {unknown} handleBy
 * @returns {Set<number>|null} null = 不过滤
 */
export function handleByFilterSet(handleBy) {
  if (handleBy == null || handleBy === '') return null
  const arr = Array.isArray(handleBy) ? handleBy : [handleBy]
  const ids = normalizeIdList(arr)
  return ids.length ? new Set(ids) : null
}

/**
 * @param {number[]} ids
 * @param {Set<number>|null} filterSet
 * @returns {number[]}
 */
export function filterIdsByHandleBy(ids, filterSet) {
  const list = normalizeIdList(ids)
  if (!filterSet) return list
  return list.filter((id) => filterSet.has(id))
}

/**
 * 缺陷是否命中泳道负责人筛选：assigneeIds 为空则不过滤；
 * 否则 handleBy / handleByList 与筛选人有交集即命中。
 * @param {object} defect
 * @param {unknown} assigneeIds
 * @returns {boolean}
 */
export function defectMatchesLaneAssignees(defect, assigneeIds) {
  const ids = normalizeIdList(assigneeIds)
  if (!ids.length) return true
  const set = new Set(ids)
  const handleBy = Array.isArray(defect && defect.handleBy) ? defect.handleBy : []
  if (handleBy.some((id) => set.has(Number(id)))) return true
  const list = Array.isArray(defect && defect.handleByList) ? defect.handleByList : []
  return list.some((m) => m && set.has(Number(m.userId)))
}

/**
 * @param {object} member
 * @param {string[]} roleKeys
 */
function memberHasRoleKey(member, roleKeys) {
  if (!member || !roleKeys || !roleKeys.length) return false
  const roles = Array.isArray(member.roles) ? member.roles : []
  const set = new Set(roleKeys.map(String))
  return roles.some((r) => {
    if (!r) return false
    if (roleKeys === ROLE_ADMIN_KEYS && r.projectCreateBy) return true
    return set.has(String(r.roleKey || ''))
  })
}

/**
 * @param {object[]|null|undefined} members
 * @returns {Map<number, object>}
 */
export function buildMemberMapFromList(members) {
  const map = new Map()
  ;(Array.isArray(members) ? members : []).forEach((m) => {
    if (!m || m.userId == null) return
    const id = Number(m.userId)
    if (!Number.isFinite(id)) return
    map.set(id, m)
  })
  return map
}

/**
 * @param {object|null} project
 * @returns {Map<number, object>}
 */
export function buildMemberMap(project) {
  return buildMemberMapFromList(project && project.members)
}

/**
 * 用成员列表 + 可选 createBy 构造供默认解析用的上下文（getProject 常不带 members）。
 * @param {object[]|null|undefined} members
 * @param {object|null|undefined} project
 */
export function projectContextFromMembers(members, project) {
  return {
    members: Array.isArray(members) ? members : [],
    createBy: project && project.createBy,
    createById: project && project.createById
  }
}

/**
 * @param {object|null} project
 * @param {string[]} roleKeys
 * @returns {number[]}
 */
export function memberIdsByRoleKeys(project, roleKeys) {
  const members = project && Array.isArray(project.members) ? project.members : []
  return normalizeIdList(
    members.filter((m) => memberHasRoleKey(m, roleKeys)).map((m) => m.userId)
  )
}

/**
 * @param {object|null} project
 * @returns {number[]}
 */
export function adminOrCreatorIds(project) {
  const ids = new Set()
  if (project && project.createBy != null) {
    const c = Number(project.createBy)
    if (Number.isFinite(c)) ids.add(c)
  }
  if (project && project.createById != null) {
    const c = Number(project.createById)
    if (Number.isFinite(c)) ids.add(c)
  }
  memberIdsByRoleKeys(project, ROLE_ADMIN_KEYS).forEach((id) => ids.add(id))
  return [...ids]
}

/**
 * @param {object|null} project
 * @param {string} laneKey processing | audit
 * @returns {number[]}
 */
export function defaultAssigneeIdsForLane(project, laneKey) {
  const roleKeys =
    laneKey === LANE_PROCESSING
      ? ROLE_DEVELOP_KEYS
      : laneKey === LANE_AUDIT
        ? ROLE_TESTER_KEYS
        : []
  if (!roleKeys.length) return []
  const byRole = memberIdsByRoleKeys(project, roleKeys)
  if (byRole.length) return byRole
  return adminOrCreatorIds(project)
}

/**
 * 无缓存时生成默认；有缓存则原样返回（展示层再按 handleBy 过滤）。
 * @param {object|null} project
 * @param {number|string} projectId
 * @returns {{ processing: number[], audit: number[], fromCache: boolean }}
 */
export function resolveLaneAssigneeIds(project, projectId) {
  let hasKey = false
  try {
    hasKey = localStorage.getItem(laneAssigneesStorageKey(projectId)) != null
  } catch (e) {
    hasKey = false
  }
  if (hasKey) {
    const cached = readLaneAssigneesCache(projectId) || {
      processing: [],
      audit: []
    }
    return {
      processing: cached.processing,
      audit: cached.audit,
      fromCache: true
    }
  }
  const processing = defaultAssigneeIdsForLane(project, LANE_PROCESSING)
  const audit = defaultAssigneeIdsForLane(project, LANE_AUDIT)
  writeLaneAssigneesCache(projectId, { processing, audit })
  return { processing, audit, fromCache: false }
}
