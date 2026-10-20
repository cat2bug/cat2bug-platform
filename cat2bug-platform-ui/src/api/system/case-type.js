import request from '@/utils/request'

export function listCaseTypes(projectId, params) {
  return request({
    url: '/system/project/' + projectId + '/case-types',
    method: 'get',
    params: params || {}
  })
}

export function addCaseType(projectId, typeName) {
  return request({
    url: '/system/project/' + projectId + '/case-types',
    method: 'post',
    data: { typeName }
  })
}

export function updateCaseType(projectId, typeId, typeName) {
  return request({
    url: '/system/project/' + projectId + '/case-types/' + typeId,
    method: 'put',
    data: { typeName }
  })
}

export function updateCaseTypeEnabled(projectId, typeId, enabled) {
  return request({
    url: '/system/project/' + projectId + '/case-types/' + typeId,
    method: 'put',
    data: { enabled: enabled ? 1 : 0 }
  })
}

/** 表单/筛选用：仅启用；编辑时保留当前已选（即使已停用） */
export function filterSelectableCaseTypes(types, currentTypeId) {
  return (types || []).filter((t) => {
    if (t && Number(t.enabled) === 1) return true
    return currentTypeId != null && t && t.typeId === currentTypeId
  })
}

export function deleteCaseType(projectId, typeId) {
  return request({
    url: '/system/project/' + projectId + '/case-types/' + typeId,
    method: 'delete'
  })
}

export function getCaseReviewTemplate(projectId) {
  return request({
    url: '/system/project/' + projectId + '/case-review-template',
    method: 'get'
  })
}

export function saveCaseReviewTemplate(projectId, template) {
  return request({
    url: '/system/project/' + projectId + '/case-review-template',
    method: 'put',
    data: { template }
  })
}
