import request from '@/utils/request'

/** 缺陷类型统计 */
export function statisticDefectType(projectId) {
  return request({
    url: '/system/defect/statistic/type/'+projectId,
    method: 'get'
  })
}
/** 我的缺陷类型统计 */
export function statisticMyDefectType(projectId) {
  return request({
    url: '/system/defect/statistic/type/'+projectId+'/my',
    method: 'get'
  })
}
/** 缺陷状态统计 */
export function statisticDefectState(projectId) {
  return request({
    url: '/system/defect/statistic/state/'+projectId,
    method: 'get'
  })
}

/** 缺陷状态统计 */
export function statisticMyDefectState(projectId) {
  return request({
    url: '/system/defect/statistic/state/'+projectId+'/my',
    method: 'get'
  })
}

/** 缺陷模块统计 */
export function statisticModule(projectId, query) {
  return request({
    url: '/system/defect/statistic/module/'+projectId,
    method: 'get',
    params: query
  })
}

/** 团队未关闭待办工作量统计（分页） */
export function statisticOpenWorkload(projectId, query) {
  return request({
    url: '/system/defect/statistic/open-workload/' + projectId,
    method: 'get',
    params: query
  })
}

/** 我的未关闭待办工作量统计 */
export function statisticMyOpenWorkload(projectId) {
  return request({
    url: '/system/defect/statistic/open-workload/' + projectId + '/my',
    method: 'get'
  })
}

/** 我的缺陷日志参与（按日） */
export function statisticMyParticipation(projectId, days = 30) {
  return request({
    url: '/system/defect/statistic/participation/' + projectId + '/my',
    method: 'get',
    params: { days }
  })
}

/** 项目测试计划列表（轻量） */
export function statisticPlanList(projectId) {
  return request({
    url: '/system/defect/statistic/plan/list',
    method: 'get',
    params: { projectId }
  })
}

/** 测试计划燃尽 */
export function statisticPlanBurndown(planId) {
  return request({
    url: '/system/defect/statistic/plan/' + planId + '/burndown',
    method: 'get'
  })
}

/** 测试计划质量指标（雷达） */
export function statisticPlanMetrics(projectId) {
  return request({
    url: '/system/defect/statistic/plan-metrics/' + projectId,
    method: 'get'
  })
}

/** 测试计划倒计时摘要 */
export function statisticPlanCountdownSummary(planId) {
  return request({
    url: '/system/defect/statistic/plan/' + planId + '/summary',
    method: 'get'
  })
}

/** 缺陷状态走势 */
export function statisticDefectStateLine(projectId, query) {
  return request({
    url: '/system/defect/statistic/defect-state-line/' + projectId,
    method: 'get',
    params: query
  })
}

/** 成员处理缺陷走势 */
export function statisticMemberDefectLine(projectId, query) {
  return request({
    url: '/system/defect/statistic/member-defect-line/' + projectId,
    method: 'get',
    params: query
  })
}
