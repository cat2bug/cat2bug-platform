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
  data.srcHost = `${window.location.protocol}//${window.location.host}`;
  return request({
    url: '/system/report',
    method: 'post',
    data: data
  })
}

// 修改报告
export function updateReport(data) {
  data.srcHost = `${window.location.protocol}//${window.location.host}`;
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

export function closeEditWindow() {
  return request({
    url: '/system/report/close-edit-window',
    method: 'post',
  })
}
