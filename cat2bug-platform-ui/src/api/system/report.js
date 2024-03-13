import request from '@/utils/request'

// 查询报告列表
export function listReport(query) {
  return request({
    url: '/system/report/list',
    method: 'get',
    params: query
  })
}

// 查询报告详细
export function getReport(reportId) {
  return request({
    url: '/system/report/' + reportId,
    method: 'get'
  })
}

// 新增报告
export function addReport(data) {
  return request({
    url: '/system/report',
    method: 'post',
    data: data
  })
}

// 修改报告
export function updateReport(data) {
  return request({
    url: '/system/report',
    method: 'put',
    data: data
  })
}

// 删除报告
export function delReport(reportId) {
  return request({
    url: '/system/report/' + reportId,
    method: 'delete'
  })
}
