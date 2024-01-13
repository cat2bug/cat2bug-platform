import request from '@/utils/request'

// 查询缺陷列表
export function configDefect() {
  return request({
    url: '/system/defect/config',
    method: 'get'
  })
}


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

// 指派
export function assign(defectId, data) {
  return request({
    url: '/system/defect/'+defectId+'/assign',
    method: 'post',
    data: data
  })
}

// 拒绝
export function reject(defectId, data) {
  return request({
    url: '/system/defect/'+defectId+'/reject',
    method: 'post',
    data: data
  })
}

// 修复
export function repair(defectId, data) {
  return request({
    url: '/system/defect/'+defectId+'/repair',
    method: 'post',
    data: data
  })
}

// 修复
export function pass(defectId, data) {
  return request({
    url: '/system/defect/'+defectId+'/pass',
    method: 'post',
    data: data
  })
}

// 开启
export function open(defectId, data) {
  return request({
    url: '/system/defect/'+defectId+'/open',
    method: 'post',
    data: data
  })
}

// 关闭
export function close(defectId, data) {
  return request({
    url: '/system/defect/'+defectId+'/close',
    method: 'post',
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

// 修改用户缺陷
export function updateUserDefect(defectId, data) {
  return request({
    url: '/system/defect/'+defectId,
    method: 'put',
    data: data
  })
}
