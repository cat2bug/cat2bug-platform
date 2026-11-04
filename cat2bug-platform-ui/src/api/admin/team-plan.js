import request from '@/utils/request'

export function listTeamPlan() {
  return request({
    url: '/admin/team-plan/list',
    method: 'get'
  })
}

export function getTeamPlan(planId) {
  return request({
    url: '/admin/team-plan/' + planId,
    method: 'get'
  })
}

export function addTeamPlan(data) {
  return request({
    url: '/admin/team-plan',
    method: 'post',
    data
  })
}

export function updateTeamPlan(data) {
  return request({
    url: '/admin/team-plan',
    method: 'put',
    data
  })
}

export function bindTeamPlan(teamId, planId) {
  return request({
    url: '/admin/team/' + teamId + '/plan',
    method: 'put',
    data: { planId }
  })
}
