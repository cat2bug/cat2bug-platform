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

