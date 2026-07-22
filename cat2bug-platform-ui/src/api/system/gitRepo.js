import request, { setHeader } from '@/utils/request'
import { getToken } from '@/utils/auth'
import { saveAs } from 'file-saver'
import { blobValidate } from '@/utils/ruoyi'
import { gitRepoCommitDetailRoute as namespacedCommitDetailRoute } from '@/utils/git-repo-routes'

/** projectId:slug → repo (in-memory for tab navigations within the same repo) */
const repoByProjectSlugCache = new Map()

/**
 * Resolve repository metadata by project + slug (REST still uses repositoryId afterward).
 * @param {number|string} projectId
 * @param {string} slug
 * @returns {Promise<object|null>}
 */
export function resolveGitRepoByProjectAndSlug(projectId, slug) {
  const pid = Number(projectId)
  const s = slug != null ? String(slug).trim() : ''
  if (!Number.isFinite(pid) || pid <= 0 || !s) {
    return Promise.resolve(null)
  }
  const cacheKey = `${pid}:${s}`
  if (repoByProjectSlugCache.has(cacheKey)) {
    return Promise.resolve(repoByProjectSlugCache.get(cacheKey))
  }
  return listGitRepo({ projectId: pid, slug: s }).then(res => {
    const list = (res && res.data) || []
    const repo = Array.isArray(list)
      ? (list.find(item => item && item.slug === s) || list[0] || null)
      : null
    if (repo && repo.repositoryId != null) {
      repoByProjectSlugCache.set(cacheKey, repo)
    }
    return repo
  })
}

/**
 * Commit detail route under namespaced `code/{slug}`.
 * @param {object} routeOrCtx route or { teamKey, projectKey, slug }
 * @param {string} commitId
 */
export function gitRepoCommitDetailRoute(routeOrCtx, commitId) {
  return namespacedCommitDetailRoute(routeOrCtx, commitId)
}

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

/** 从远端 URL 一次性导入为本地 bare 仓（manage）；大仓可能较久，超时略大于服务端默认 120s */
export function importGitRepo(data) {
  return request({
    url: '/system/git/repo/import',
    method: 'post',
    data,
    timeout: 130000
  })
}

/**
 * 用会话 PAT 列远端仓库（不落库）
 * @param {{ provider: 'github'|'gitlab'|'gitee', token: string, host?: string }} data
 */
export function listRemoteGitRepos(data) {
  return request({
    url: '/system/git/repo/remote/list',
    method: 'post',
    data
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
export function listGitRepoBranches(repositoryId, name) {
  return request({
    url: '/system/git/repo/' + repositoryId + '/branches',
    method: 'get',
    params: { name }
  })
}

// 创建仓库分支（manage 权限）
export function createGitRepoBranch(repositoryId, data) {
  return request({
    url: '/system/git/repo/' + repositoryId + '/branches',
    method: 'post',
    data
  })
}

// 删除仓库分支（manage 权限）
export function delGitRepoBranch(repositoryId, name) {
  return request({
    url: '/system/git/repo/' + repositoryId + '/branches/' + encodeURIComponent(name),
    method: 'delete'
  })
}

// 设置默认分支（manage 权限）
export function setGitRepoDefaultBranch(repositoryId, name) {
  return request({
    url: '/system/git/repo/' + repositoryId + '/default-branch',
    method: 'put',
    data: { name }
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

// 提交详情（meta + 变更文件与 patch，pull 权限）
export function getGitRepoCommit(repositoryId, commitId) {
  return request({
    url: '/system/git/repo/' + repositoryId + '/commits/' + encodeURIComponent(commitId),
    method: 'get'
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
 * 大仓可达百兆：优先 File System Access 流式落盘；否则用 ?token= 触发浏览器原生下载，
 * 避免 fetch→blob 把整包读进内存导致 net::ERR_FAILED 200。
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
  const ext = format === 'tar.gz' || format === 'tgz' ? 'tar.gz' : 'zip'
  const name = fileName || ('source.' + ext)
  const base = process.env.VUE_APP_BASE_API || ''
  const url = base + path + (qs.toString() ? ('?' + qs.toString()) : '')

  if (typeof window !== 'undefined' && typeof window.showSaveFilePicker === 'function') {
    const headers = {}
    setHeader(path, headers)
    return fetch(url, { method: 'GET', headers }).then(async (res) => {
      const ct = (res.headers.get('content-type') || '').toLowerCase()
      if (!res.ok || ct.includes('application/json')) {
        let msg = 'Download failed'
        try {
          const obj = await res.json()
          msg = obj.msg || msg
        } catch (e) { /* ignore */ }
        return Promise.reject(new Error(msg))
      }
      if (!res.body) {
        return Promise.reject(new Error('Download failed'))
      }
      try {
        const handle = await window.showSaveFilePicker({
          suggestedName: name,
          types: ext === 'tar.gz'
            ? [{ description: 'tar.gz', accept: { 'application/gzip': ['.tar.gz', '.tgz', '.gz'] } }]
            : [{ description: 'ZIP', accept: { 'application/zip': ['.zip'] } }]
        })
        const writable = await handle.createWritable()
        await res.body.pipeTo(writable)
      } catch (err) {
        if (err && err.name === 'AbortError') {
          return
        }
        throw err
      }
    })
  }

  // 无 showSaveFilePicker：带 token 走原生下载（不经 JS 堆）
  const token = getToken()
  if (token) {
    qs.set('token', token)
  }
  const navUrl = base + path + '?' + qs.toString()
  const a = document.createElement('a')
  a.href = navUrl
  a.rel = 'noopener'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  return Promise.resolve()
}
