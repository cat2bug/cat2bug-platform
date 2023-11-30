import request from '@/utils/request'

// 查询缺陷日志列表
export function listLog(query) {
  return request({
    url: '/system/log/list',
    method: 'get',
    params: query
  })
}

// 查询缺陷日志详细
export function getLog(defectLogId) {
  return request({
    url: '/system/log/' + defectLogId,
    method: 'get'
  })
}

// 新增缺陷日志
export function addLog(data) {
  return request({
    url: '/system/log',
    method: 'post',
    data: data
  })
}

// 修改缺陷日志
export function updateLog(data) {
  return request({
    url: '/system/log',
    method: 'put',
    data: data
  })
}

// 删除缺陷日志
export function delLog(defectLogId) {
  return request({
    url: '/system/log/' + defectLogId,
    method: 'delete'
  })
}
