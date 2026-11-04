import request from '@/utils/request'

export function getTeamDefectPageSettings(teamId) {
  return request({
    url: '/system/team/' + teamId + '/defect-page-settings',
    method: 'get'
  })
}

export function saveTeamDefectPageSettings(teamId, data) {
  return request({
    url: '/system/team/' + teamId + '/defect-page-settings',
    method: 'put',
    data
  })
}

export function getProjectDefectPageSettings(projectId) {
  return request({
    url: '/system/project/' + projectId + '/defect-page-settings',
    method: 'get'
  })
}
