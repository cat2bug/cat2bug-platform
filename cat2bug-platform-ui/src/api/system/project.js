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

// 按 teamKey + projectKey 解析项目（分享链接）
export function getProjectByPathKeys(teamKey, projectKey) {
  return request({
    url: `/system/project/path/${encodeURIComponent(teamKey)}/${encodeURIComponent(projectKey)}`,
    method: 'get',
    silentError: true
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

/** 可作为复制源的项目（当前用户为创建人或管理员，可跨团队） */
export function listCopySources() {
  return request({
    url: '/system/project/copy-sources',
    method: 'get',
    silentError: true
  })
}

/** 源项目可勾选资源：type=git|cases|modules|documents */
export function listCopySourceItems(sourceProjectId, params) {
  return request({
    url: '/system/project/copy-sources/' + sourceProjectId + '/items',
    method: 'get',
    params,
    silentError: true
  })
}

/** 复制体积预估。body: ProjectCopySelection */
export function estimateProjectCopy(data) {
  return request({
    url: '/system/project/copy-estimate',
    method: 'post',
    data,
    timeout: 120000,
    silentError: true
  })
}

/** 复制任务进度 */
export function getProjectCopyJob(jobId) {
  return request({
    url: '/system/project/copy-job/' + jobId,
    method: 'get',
    silentError: true
  })
}

/** 旧单包出站。同步页已改走 estimatePush / createPushJob。 */
export function pullProject(projectId, data) {
  return request({
    url: '/system/project/'+projectId+'/push',
    method: 'post',
    timeout: 60000,
    data: data
  })
}

/** 旧进度查询（带 projectId）。同步页轮询 getPushJobById。 */
export function getPushJob(projectId, jobId) {
  return request({
    url: '/system/project/' + projectId + '/push/jobs/' + jobId,
    method: 'get',
    timeout: 30000
  })
}

/**
 * 出站体积预估。
 * data: { maxTotalBytes, required:[{key,bytes}], repositories:[{slug,name,bytes}],
 *         documents:[{id,name,folder,bytes,localMissing,children}] }
 */
export function estimatePush(projectId) {
  return request({
    url: '/system/project/' + projectId + '/push/estimate',
    method: 'get',
    timeout: 120000,
    silentError: true
  })
}

/** body: { pullKey, gitSlugs:[], documentIds:[] }，返回 Job（含 parts）。 */
export function createPushJob(projectId, data) {
  return request({
    url: '/system/project/' + projectId + '/push/jobs',
    method: 'post',
    timeout: 60000,
    data: data
  })
}

export function getPushJobById(jobId) {
  return request({
    url: '/system/project/push/jobs/' + jobId,
    method: 'get',
    timeout: 30000,
    silentError: true
  })
}

export function retryFailedPushJob(jobId) {
  return request({
    url: '/system/project/push/jobs/' + jobId + '/retry-failed',
    method: 'post',
    timeout: 60000
  })
}

/** 当前项目未完成 Job；没有时 data 为 null。 */
export function getActivePushJob(projectId) {
  return request({
    url: '/system/project/' + projectId + '/push/jobs/active',
    method: 'get',
    timeout: 30000,
    silentError: true
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

// 移除项目成员前预检（缺陷交接 / Git 最后 admin）
export function previewRemoveMemberOfProject(projectId, memberId) {
  return request({
    url: '/system/project/' + projectId + '/member/' + memberId + '/remove-preview',
    method: 'get'
  })
}

// 移除项目成员（requiresHandoff 时传 handoffUserId）
export function delMemberOfProject(projectId, memberId, handoffUserId) {
  return request({
    url: '/system/project/'+projectId+'/member/'+memberId,
    method: 'delete',
    params: handoffUserId != null ? { handoffUserId } : undefined
  })
}
