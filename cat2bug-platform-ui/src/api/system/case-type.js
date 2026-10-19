import request from '@/utils/request'

export function listCaseTypes(projectId) {
  return request({
    url: '/system/project/' + projectId + '/case-types',
    method: 'get'
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
