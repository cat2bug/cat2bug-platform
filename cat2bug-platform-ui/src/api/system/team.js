import request from '@/utils/request'

// 查询团队列表
export function listTeam(query) {
  return request({
    url: '/system/team/list',
    method: 'get',
    params: query
  })
}

// 查询团队成员
export function listMember(teamId,query) {
  return request({
    url: '/system/team/'+teamId+'/member',
    method: 'get',
    params: query
  })
}

// 查询非团队成员
export function listNotMember(teamId,query) {
  return request({
    url: '/system/team/'+teamId+'/not-member',
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

// 查询团队成员详细
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

// 新增团队成员
export function addMember(teamId,data) {
  return request({
    url: '/system/team/'+teamId+'/member',
    method: 'post',
    data: data
  })
}

// 邀请团队成员
export function inviteMember(teamId,data) {
  return request({
    url: '/system/team/'+teamId+'/invite',
    method: 'post',
    data: data
  })
}

// 禁用或解锁团队
export function lockTeam(teamId, data) {
  return request({
    url: '/system/team/'+teamId+'/lock',
    method: 'put',
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

export function updateMemberTeamRole(teamId, memberId, data) {
  return request({
    url: '/system/team/'+teamId+'/member/'+memberId+'/role',
    method: 'put',
    data: data
  })
}


export function listTeamRole(teamId) {
  return request({
    url: '/system/team/'+teamId+'/role',
    method: 'get',
  })
}


export function updateMemberTeamRoleIds(teamId, memberId, roleIds) {
  return request({
    url: '/system/team/'+teamId+'/member/'+memberId+'/roles',
    method: 'put',
    data: roleIds
  })
}

// 删除团队
export function delTeam(teamId) {
  return request({
    url: '/system/team/' + teamId,
    method: 'delete'
  })
}
