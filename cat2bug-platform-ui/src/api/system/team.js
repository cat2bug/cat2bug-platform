import request from '@/utils/request'

// 查询团队列表
export function listTeam(query) {
  return request({
    url: '/system/team/list',
    method: 'get',
    params: query
  })
}

// 查询团队列表
export function myListTeam() {
  return request({
    url: '/system/team/my',
    method: 'get'
  })
}

// 查询团队详细
export function getMemberByTeam(teamId) {
  return request({
    url: '/system/team/' + teamId + '/member',
    method: 'get'
  })
}

// 查询团队详细
export function getTeam(teamId) {
  return request({
    url: '/system/team/' + teamId,
    method: 'get'
  })
}

// 新增团队
export function addTeam(data) {
  return request({
    url: '/system/team',
    method: 'post',
    data: data
  })
}

// 修改团队
export function updateTeam(data) {
  return request({
    url: '/system/team',
    method: 'put',
    data: data
  })
}

// 删除团队
export function delTeam(teamId) {
  return request({
    url: '/system/team/' + teamId,
    method: 'delete'
  })
}
