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
