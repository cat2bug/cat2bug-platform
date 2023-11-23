import request from '@/utils/request'

// 查询缺陷列表
export function listDefect(query) {
  return request({
    url: '/system/defect/list',
    method: 'get',
    params: query
  })
}

// 查询缺陷详细
export function getDefect(defectId) {
  return request({
    url: '/system/defect/' + defectId,
    method: 'get'
  })
}

// 新增缺陷
export function addDefect(data) {
  return request({
    url: '/system/defect',
    method: 'post',
    data: data
  })
}

// 修改缺陷
export function updateDefect(data) {
  return request({
    url: '/system/defect',
    method: 'put',
    data: data
  })
}

// 删除缺陷
export function delDefect(defectId) {
  return request({
    url: '/system/defect/' + defectId,
    method: 'delete'
  })
}
