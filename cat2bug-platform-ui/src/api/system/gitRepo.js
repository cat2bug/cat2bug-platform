import request from '@/utils/request'

// 查询代码仓库列表（按项目）
export function listGitRepo(query) {
  return request({
    url: '/system/git/repo/list',
    method: 'get',
    params: query
  })
}

// 查询代码仓库详细
export function getGitRepo(repositoryId) {
  return request({
    url: '/system/git/repo/' + repositoryId,
    method: 'get'
  })
}

// 新增代码仓库
export function addGitRepo(data) {
  return request({
    url: '/system/git/repo',
    method: 'post',
    data: data
  })
}

// 修改代码仓库（slug 变更会导致旧 clone 地址失效，响应体带 cloneUrlChanged/message）
export function updateGitRepo(data) {
  return request({
    url: '/system/git/repo',
    method: 'put',
    data: data
  })
}

// 删除代码仓库；wipeDisk 默认 true，同步清空磁盘目录
export function delGitRepo(repositoryIds, wipeDisk) {
  const ids = Array.isArray(repositoryIds) ? repositoryIds.join(',') : repositoryIds
  return request({
    url: '/system/git/repo/' + ids,
    method: 'delete',
    params: { wipeDisk: wipeDisk !== false }
  })
}

// 关注 / 取消关注代码仓库
export function collectGitRepo(repositoryId, collect) {
  return request({
    url: '/system/git/repo/' + repositoryId + '/collect',
    method: 'post',
    data: { collect: !!collect }
  })
}

// 查询仓库分支列表（pull 权限）
export function listGitRepoBranches(repositoryId) {
  return request({
    url: '/system/git/repo/' + repositoryId + '/branches',
    method: 'get'
  })
}

// 查询目录内容（pull 权限）
export function listGitRepoTree(repositoryId, ref, path) {
  return request({
    url: '/system/git/repo/' + repositoryId + '/tree',
    method: 'get',
    params: { ref, path }
  })
}

// 读取文件内容（pull 权限）
export function getGitRepoBlob(repositoryId, ref, path) {
  return request({
    url: '/system/git/repo/' + repositoryId + '/blob',
    method: 'get',
    params: { ref, path }
  })
}

// 查询提交记录（pull 权限）
export function listGitRepoCommits(repositoryId, ref, limit) {
  return request({
    url: '/system/git/repo/' + repositoryId + '/commits',
    method: 'get',
    params: { ref, limit }
  })
}
