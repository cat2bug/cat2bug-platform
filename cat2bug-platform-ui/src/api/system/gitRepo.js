import request, { setHeader } from '@/utils/request'
import { saveAs } from 'file-saver'
import { blobValidate } from '@/utils/ruoyi'

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

// 查询贡献者（pull 权限）
export function listGitRepoContributors(repositoryId, ref, limit) {
  return request({
    url: '/system/git/repo/' + repositoryId + '/contributors',
    method: 'get',
    params: { ref, limit }
  })
}

// 发行版列表（pull 权限）
export function listGitRepoReleases(repositoryId) {
  return request({
    url: '/system/git/repo/' + repositoryId + '/releases',
    method: 'get'
  })
}

// 发行版详情（pull 权限）
export function getGitRepoRelease(repositoryId, releaseId) {
  return request({
    url: '/system/git/repo/' + repositoryId + '/releases/' + releaseId,
    method: 'get'
  })
}

// 创建发行版（manage 权限）
export function createGitRepoRelease(repositoryId, data) {
  return request({
    url: '/system/git/repo/' + repositoryId + '/releases',
    method: 'post',
    data
  })
}

// 更新发行版（manage 权限）
export function updateGitRepoRelease(repositoryId, releaseId, data) {
  return request({
    url: '/system/git/repo/' + repositoryId + '/releases/' + releaseId,
    method: 'put',
    data
  })
}

// 删除发行版（manage 权限）
export function delGitRepoRelease(repositoryId, releaseId) {
  return request({
    url: '/system/git/repo/' + repositoryId + '/releases/' + releaseId,
    method: 'delete'
  })
}

// 发行版附件列表（pull 权限）
export function listGitRepoReleaseAssets(repositoryId, releaseId) {
  return request({
    url: '/system/git/repo/' + repositoryId + '/releases/' + releaseId + '/assets',
    method: 'get'
  })
}

// 上传发行版附件（manage 权限）
export function uploadGitRepoReleaseAsset(repositoryId, releaseId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/system/git/repo/' + repositoryId + '/releases/' + releaseId + '/assets',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 删除发行版附件（manage 权限）
export function delGitRepoReleaseAsset(repositoryId, releaseId, assetId) {
  return request({
    url: '/system/git/repo/' + repositoryId + '/releases/' + releaseId + '/assets/' + assetId,
    method: 'delete'
  })
}

/** 发行版附件下载 URL（需携带 Authorization） */
export function gitRepoReleaseAssetDownloadUrl(repositoryId, releaseId, assetId) {
  const base = process.env.VUE_APP_BASE_API || ''
  return `${base}/system/git/repo/${repositoryId}/releases/${releaseId}/assets/${assetId}/download`
}

/** 下载发行版附件（带鉴权） */
export function downloadGitRepoReleaseAsset(repositoryId, releaseId, assetId, fileName) {
  return request({
    url: '/system/git/repo/' + repositoryId + '/releases/' + releaseId + '/assets/' + assetId + '/download',
    method: 'get',
    responseType: 'blob'
  }).then(async (data) => {
    if (!blobValidate(data)) {
      const text = await data.text()
      let msg = 'Download failed'
      try {
        const obj = JSON.parse(text)
        msg = obj.msg || msg
      } catch (e) { /* ignore */ }
      return Promise.reject(new Error(msg))
    }
    saveAs(new Blob([data]), fileName || 'download')
  })
}

/** 下载仓库指定 ref 的源码包（zip / tar.gz）
 * 大仓归档可达百兆级，走原生 fetch 且不设短超时，避免 axios 60s 在浏览器侧掐成 net::ERR_FAILED 200。
 */
export function downloadGitRepoArchive(repositoryId, ref, format, fileName) {
  const path = '/system/git/repo/' + repositoryId + '/archive'
  const qs = new URLSearchParams()
  if (ref != null && ref !== '') {
    qs.set('ref', String(ref))
  }
  if (format != null && format !== '') {
    qs.set('format', String(format))
  }
  const url = (process.env.VUE_APP_BASE_API || '') + path + (qs.toString() ? ('?' + qs.toString()) : '')
  const headers = {}
  setHeader(path, headers)
  return fetch(url, { method: 'GET', headers }).then(async (res) => {
    const blob = await res.blob()
    if (!res.ok || !blobValidate(blob)) {
      let msg = 'Download failed'
      try {
        const obj = JSON.parse(await blob.text())
        msg = obj.msg || msg
      } catch (e) { /* ignore */ }
      return Promise.reject(new Error(msg))
    }
    const ext = format === 'tar.gz' || format === 'tgz' ? 'tar.gz' : 'zip'
    saveAs(blob, fileName || ('source.' + ext))
  })
}
