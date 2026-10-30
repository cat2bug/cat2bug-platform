import request from '@/utils/request'

export function getProjectWorkCalendar(projectId) {
  return request({
    url: '/system/project/' + projectId + '/work-calendar',
    method: 'get'
  })
}

export function updateProjectWorkCalendar(projectId, data) {
  return request({
    url: '/system/project/' + projectId + '/work-calendar',
    method: 'put',
    data
  })
}
