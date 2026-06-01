/**
 * 解析当前项目 ID（store 优先且须为有效正整数，路由 query 兜底）
 * @returns {number|null}
 */
export function resolveCurrentProjectId(vm) {
  const storeId = vm.$store.state.user.config && vm.$store.state.user.config.currentProjectId
  const routeId = vm.$route && vm.$route.query && vm.$route.query.projectId
  for (const raw of [storeId, routeId]) {
    if (raw == null || raw === '') continue
    const id = parseInt(raw, 10)
    if (Number.isFinite(id) && id > 0) return id
  }
  return null
}
