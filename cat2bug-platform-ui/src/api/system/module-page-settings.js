import request from '@/utils/request'

export function getTeamModulePageSettings(teamId) {
  return request({
    url: '/system/team/' + teamId + '/module-page-settings',
    method: 'get'
  })
}

export function saveTeamModulePageSettings(teamId, data) {
  return request({
    url: '/system/team/' + teamId + '/module-page-settings',
    method: 'put',
    data
  })
}

export function getProjectModulePageSettings(projectId) {
  return request({
    url: '/system/project/' + projectId + '/module-page-settings',
    method: 'get'
  })
}
