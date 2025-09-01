import request from '@/utils/request'

// 查询报告模版列表
export function listReportTemplate(query) {
  return request({
    url: '/system/report/template/list',
    method: 'get',
    params: query
  })
}

// 查询报告模版详细
export function getReportTemplate(templateId) {
  return request({
    url: '/system/report/template/' + templateId,
    method: 'get'
  })
}

// 新增报告模版
export function addReportTemplate(data) {
  return request({
    url: '/system/report/template',
    method: 'post',
    data: data
  })
}

// 修改报告模版
export function updateTemplate(data) {
  return request({
    url: '/system/report/template',
    method: 'put',
    data: data
  })
}

// 删除报告模版
export function delTemplate(templateId) {
  return request({
    url: '/system/report/template/' + templateId,
    method: 'delete'
  })
}
