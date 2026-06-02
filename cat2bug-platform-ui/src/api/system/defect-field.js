import request from '@/utils/request'

// 查询项目缺陷自定义字段列表（含停用）
export function listFields(projectId) {
  return request({
    url: '/system/project/' + projectId + '/defect-fields',
    method: 'get'
  })
}

// 查询项目启用的缺陷自定义字段
export function listEnabledFields(projectId) {
  return request({
    url: '/system/project/' + projectId + '/defect-fields/enabled',
    method: 'get'
  })
}

// 列表/表单列布局：启用的内置字段键 + 启用的自定义字段
export function listColumnLayout(projectId) {
  return request({
    url: '/system/project/' + projectId + '/defect-fields/column-layout',
    method: 'get'
  })
}

// 新增缺陷自定义字段
export function addField(projectId, data) {
  return request({
    url: '/system/project/' + projectId + '/defect-fields',
    method: 'post',
    data: data
  })
}

// 修改缺陷自定义字段
export function updateField(projectId, fieldId, data) {
  return request({
    url: '/system/project/' + projectId + '/defect-fields/' + fieldId,
    method: 'put',
    data: data
  })
}

// 更新缺陷内置属性启用与排序
export function updateBuiltinFieldLayout(projectId, data) {
  return request({
    url: '/system/project/' + projectId + '/defect-fields/builtin-layout',
    method: 'put',
    data: data
  })
}

// 删除（软删）缺陷自定义字段
export function deleteField(projectId, fieldId) {
  return request({
    url: '/system/project/' + projectId + '/defect-fields/' + fieldId,
    method: 'delete'
  })
}
