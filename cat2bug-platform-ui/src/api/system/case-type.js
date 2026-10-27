import request from '@/utils/request'

/** 业务读：项目下启用的用例类型（表单/筛选等） */
export function listCaseTypes(projectId, params) {
  return request({
    url: '/system/project/' + projectId + '/case-types',
    method: 'get',
    params: params || {}
  })
}

/** 团队管理：用例类型列表（含停用） */
export function listTeamCaseTypes(teamId, params) {
  return request({
    url: '/system/team/' + teamId + '/case-types',
    method: 'get',
    params: params || {}
  })
}

export function addCaseType(teamId, typeName) {
  return request({
    url: '/system/team/' + teamId + '/case-types',
    method: 'post',
    data: { typeName }
  })
}

export function updateCaseType(teamId, typeId, typeName) {
  return request({
    url: '/system/team/' + teamId + '/case-types/' + typeId,
    method: 'put',
    data: { typeName }
  })
}

export function updateCaseTypeEnabled(teamId, typeId, enabled) {
  return request({
    url: '/system/team/' + teamId + '/case-types/' + typeId,
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

export function deleteCaseType(teamId, typeId) {
  return request({
    url: '/system/team/' + teamId + '/case-types/' + typeId,
    method: 'delete'
  })
}

export function getCaseReviewTemplate(teamId) {
  return request({
    url: '/system/team/' + teamId + '/case-review-template',
    method: 'get'
  })
}

export function saveCaseReviewTemplate(teamId, template) {
  return request({
    url: '/system/team/' + teamId + '/case-review-template',
    method: 'put',
    data: { template }
  })
}
