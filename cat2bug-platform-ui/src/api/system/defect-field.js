import request from '@/utils/request'

// 查询团队缺陷自定义字段列表（含停用，管理页）
export function listFields(teamId) {
  return request({
    url: '/system/team/' + teamId + '/defect-fields',
    method: 'get'
  })
}

// 查询项目启用的缺陷自定义字段（业务读）
export function listEnabledFields(projectId) {
  return request({
    url: '/system/project/' + projectId + '/defect-fields/enabled',
    method: 'get'
  })
}

// 列表/表单列布局：启用的内置字段键 + 启用的自定义字段（业务读）
export function listColumnLayout(projectId) {
  return request({
    url: '/system/project/' + projectId + '/defect-fields/column-layout',
    method: 'get'
  })
}

// 新增缺陷自定义字段
export function addField(teamId, data) {
  return request({
    url: '/system/team/' + teamId + '/defect-fields',
    method: 'post',
    data: data
  })
}

// 修改缺陷自定义字段
export function updateField(teamId, fieldId, data) {
  return request({
    url: '/system/team/' + teamId + '/defect-fields/' + fieldId,
    method: 'put',
    data: data
  })
}

// 更新缺陷内置属性启用与排序
export function updateBuiltinFieldLayout(teamId, data) {
  return request({
    url: '/system/team/' + teamId + '/defect-fields/builtin-layout',
    method: 'put',
    data: data
  })
}

// 删除（软删）缺陷自定义字段
export function deleteField(teamId, fieldId) {
  return request({
    url: '/system/team/' + teamId + '/defect-fields/' + fieldId,
    method: 'delete'
  })
}
