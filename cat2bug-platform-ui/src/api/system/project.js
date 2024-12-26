import request from '@/utils/request'

// 查询项目列表
export function listProject(query) {
  return request({
    url: '/system/project/list',
    method: 'get',
    params: query
  })
}

// 查询项目角色列表
export function listProjectRole(projectId) {
  return request({
    url: '/system/project/'+projectId+'/role',
    method: 'get'
  })
}

// 查询项目详细
export function getProject(projectId) {
  return request({
    url: '/system/project/' + projectId,
    method: 'get'
  })
}

// 新增项目
export function addProject(data) {
  return request({
    url: '/system/project',
    method: 'post',
    data: data
  })
}

// 新增项目
export function pullProject(projectId, data) {
  return request({
    url: '/system/project/'+projectId+'/pull',
    method: 'post',
    data: data
  })
}

export function addProjectMembers(projectId, data) {
  return request({
    url: '/system/project/'+projectId+'/member',
    method: 'post',
    data: data
  })
}

// 收藏
export function collectProject(projectId,collect) {
  return request({
    url: '/system/project/'+projectId+'/collect',
    method: 'post',
    data: collect
  })
}

// 修改项目
export function updateProject(data) {
  return request({
    url: '/system/project',
    method: 'put',
    data: data
  })
}

// 删除项目
export function delProject(projectId, password) {
  return request({
    url: '/system/project/' + projectId,
    method: 'delete',
    data: password
  })
}

// 查询项目成员列表
export function listMemberOfProject(projectId,query) {
  return request({
    url: '/system/project/'+projectId+'/member',
    method: 'get',
    params: query
  })
}

// 查询非项目成员列表
export function listNotMemberOfProject(projectId,query) {
  return request({
    url: '/system/project/'+projectId+'/not-member',
    method: 'get',
    params: query
  })
}

// 更新项目成员角色
export function updateMemberRoleOfProject(projectId, memberId, roleIds) {
  return request({
    url: '/system/project/'+projectId+'/member/'+memberId+'/role',
    method: 'put',
    data: roleIds
  })
}

// 移除项目成员
export function delMemberOfProject(projectId, memberId) {
  return request({
    url: '/system/project/'+projectId+'/member/'+memberId,
    method: 'delete'
  })
}
