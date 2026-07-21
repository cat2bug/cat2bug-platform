import request from '@/utils/request'

const baseUrl = (repositoryId) => '/system/git/repo/' + repositoryId + '/pulls'

/** 查询仓库 Pull Request 列表（pull 权限） */
export function listGitPullRequests(repositoryId, state) {
  return request({
    url: baseUrl(repositoryId),
    method: 'get',
    params: { state }
  })
}

/** 创建 Pull Request（push 权限） */
export function createGitPullRequest(repositoryId, data) {
  return request({
    url: baseUrl(repositoryId),
    method: 'post',
    data
  })
}

/** 查询 Pull Request 详情（pull 权限） */
export function getGitPullRequest(repositoryId, number) {
  return request({
    url: baseUrl(repositoryId) + '/' + number,
    method: 'get'
  })
}

/** 关闭 Pull Request（作者或 manage 权限） */
export function closeGitPullRequest(repositoryId, number) {
  return request({
    url: baseUrl(repositoryId) + '/' + number,
    method: 'patch'
  })
}

/** 查询 Pull Request 提交（pull 权限） */
export function listGitPullRequestCommits(repositoryId, number) {
  return request({
    url: baseUrl(repositoryId) + '/' + number + '/commits',
    method: 'get'
  })
}

/** 查询 Pull Request 文件与 diff（pull 权限） */
export function listGitPullRequestFiles(repositoryId, number) {
  return request({
    url: baseUrl(repositoryId) + '/' + number + '/files',
    method: 'get'
  })
}

/** 查询 Pull Request 讨论（pull 权限） */
export function listGitPullRequestComments(repositoryId, number) {
  return request({
    url: baseUrl(repositoryId) + '/' + number + '/comments',
    method: 'get'
  })
}

/** 发布 Pull Request 评论（push 权限） */
export function createGitPullRequestComment(repositoryId, number, data) {
  return request({
    url: baseUrl(repositoryId) + '/' + number + '/comments',
    method: 'post',
    data
  })
}

/** 合并 Pull Request（manage 权限） */
export function mergeGitPullRequest(repositoryId, number) {
  return request({
    url: baseUrl(repositoryId) + '/' + number + '/merge',
    method: 'post'
  })
}
