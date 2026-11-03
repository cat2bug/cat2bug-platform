/** 与 GET /system/project/{id}/push/estimate 对齐的默认合计上限（500MiB）。 */
export const DEFAULT_MAX_TOTAL_BYTES = 524288000

export const REQUIRED_KEYS = ['defect', 'case', 'plan', 'module']

const REQUIRED_ALIASES = {
  defect: 'defect',
  defects: 'defect',
  case: 'case',
  cases: 'case',
  plan: 'plan',
  plans: 'plan',
  module: 'module',
  modules: 'module',
  deliverable: 'module',
  deliverables: 'module'
}

const DONE_PART_STATES = ['OK', 'SUCCESS', 'SUCCEEDED']
const FAILED_PART_STATES = ['FAILED', 'ERROR']
const RUNNING_JOB_STATES = ['RUNNING', 'PENDING', 'PACKING', 'UPLOADING', 'RETRYING']
const FINISHED_JOB_STATES = ['SUCCESS', 'SUCCEEDED', 'DONE', 'COMPLETED']

export function bytesOf(node) {
  if (!node || typeof node !== 'object') return 0
  const raw = node.bytes ?? node.estimatedBytes ?? node.size ?? node.sizeBytes ?? 0
  const n = Number(raw)
  return Number.isFinite(n) && n > 0 ? n : 0
}

export function formatByteSize(bytes) {
  if (bytes == null || bytes === '') return '—'
  const n = Number(bytes)
  if (!Number.isFinite(n) || n < 0) return '—'
  if (n === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.min(units.length - 1, Math.floor(Math.log(n) / Math.log(1024)))
  const value = n / Math.pow(1024, i)
  const text = i === 0 ? String(Math.round(value)) : (value >= 100 ? value.toFixed(0) : value.toFixed(1))
  return text + ' ' + units[i]
}

/** 兼容 AjaxResult 单层 / 双层 data。 */
export function unwrapAjax(res) {
  let body = res
  if (body && typeof body === 'object' && body.data !== undefined && (body.code !== undefined || body.msg !== undefined)) {
    body = body.data
  }
  if (body && typeof body === 'object' && body.data && typeof body.data === 'object' && !Array.isArray(body.data)) {
    const inner = body.data
    if (inner.jobId || inner.maxTotalBytes != null || inner.required || inner.parts || inner.repositories || inner.documents) {
      body = inner
    }
  }
  return body && typeof body === 'object' ? body : null
}

export function normalizeEstimate(raw) {
  const body = unwrapAjax(raw) || {}
  const maxRaw = body.maxTotalBytes ?? body.maxBytes ?? body.limitBytes
  const maxTotalBytes = Number(maxRaw)
  const requiredList = Array.isArray(body.required) ? body.required : []
  const requiredMap = {}
  requiredList.forEach((item) => {
    const keyRaw = String(item.key || item.id || item.code || item.name || '').toLowerCase()
    const key = REQUIRED_ALIASES[keyRaw]
    if (key) requiredMap[key] = bytesOf(item)
  })
  const silentRaw = Number(body.silentBytes)
  const silentBytes = Number.isFinite(silentRaw) && silentRaw > 0 ? silentRaw : 0
  const gitRaw = body.repositories || body.git || body.repos || []
  const repositories = (Array.isArray(gitRaw) ? gitRaw : []).map((repo) => ({
    slug: String(repo.slug || repo.gitSlug || repo.repoSlug || ''),
    name: repo.name || repo.repoName || repo.slug || repo.gitSlug || '',
    bytes: bytesOf(repo)
  })).filter((repo) => repo.slug)
  const docRaw = body.documents || body.documentTree || body.docs || []
  return {
    maxTotalBytes: Number.isFinite(maxTotalBytes) && maxTotalBytes > 0 ? maxTotalBytes : DEFAULT_MAX_TOTAL_BYTES,
    required: REQUIRED_KEYS.map((key) => ({ key, bytes: requiredMap[key] || 0 })),
    silentBytes,
    repositories,
    documents: normalizeDocs(Array.isArray(docRaw) ? docRaw : [])
  }
}

function normalizeDocs(nodes) {
  return (nodes || []).map((node, index) => {
    const children = normalizeDocs(node.children || node.nodes || [])
    const explicitFile = node.type === 'file' || node.folder === false || node.directory === false
    const folder = !explicitFile && (
      node.folder === true
      || node.directory === true
      || node.type === 'folder'
      || node.type === 'dir'
      || children.length > 0
    )
    const id = String(node.id ?? node.docId ?? node.documentId ?? ('node-' + index))
    const localMissing = !folder && (
      node.localMissing === true
      || node.missing === true
      || node.readable === false
      || node.localReadable === false
    )
    const files = collectFiles(children)
    const disabled = folder
      ? files.length === 0 || files.every((file) => file.localMissing || file.disabled)
      : localMissing
    const bytes = folder
      ? children.reduce((sum, child) => sum + (child.localMissing ? 0 : (child.bytes || 0)), 0)
      : bytesOf(node)
    return {
      id,
      name: node.name || node.docName || node.title || id,
      folder,
      bytes,
      localMissing,
      disabled,
      children
    }
  })
}

export function collectFiles(nodes, acc = []) {
  ;(nodes || []).forEach((node) => {
    if (node.folder) collectFiles(node.children, acc)
    else acc.push(node)
  })
  return acc
}

export function selectableCheckedDocIds(documents, checkedIds) {
  const allowed = new Set(
    collectFiles(documents)
      .filter((file) => !file.localMissing && !file.disabled)
      .map((file) => String(file.id))
  )
  return (checkedIds || []).map(String).filter((id) => allowed.has(id))
}

export function selectedTotal({ required, repositories, documents, checkedGitSlugs, checkedDocIds, silentBytes }) {
  const requiredBytes = (required || []).reduce((sum, item) => sum + (Number(item.bytes) || 0), 0)
    + (Number(silentBytes) || 0)
  const gitSet = new Set(checkedGitSlugs || [])
  const gitBytes = (repositories || [])
    .filter((repo) => gitSet.has(repo.slug))
    .reduce((sum, repo) => sum + (repo.bytes || 0), 0)
  const docSet = new Set(selectableCheckedDocIds(documents, checkedDocIds))
  const docBytes = collectFiles(documents)
    .filter((file) => docSet.has(String(file.id)))
    .reduce((sum, file) => sum + (file.bytes || 0), 0)
  return requiredBytes + gitBytes + docBytes
}

/**
 * 去掉当前勾选里估算体积最大的一个可选项（Git 或文档文件）。
 * 体积相同优先去掉 Git。
 */
export function removeLargestOptional({ repositories, documents, checkedGitSlugs, checkedDocIds }) {
  const git = (repositories || [])
    .filter((repo) => (checkedGitSlugs || []).includes(repo.slug))
    .map((repo) => ({ kind: 'git', id: repo.slug, bytes: repo.bytes || 0 }))
  const docSet = new Set(selectableCheckedDocIds(documents, checkedDocIds))
  const docs = collectFiles(documents)
    .filter((file) => docSet.has(String(file.id)))
    .map((file) => ({ kind: 'doc', id: String(file.id), bytes: file.bytes || 0 }))
  const all = git.concat(docs)
  if (!all.length) {
    return {
      checkedGitSlugs: [...(checkedGitSlugs || [])],
      checkedDocIds: [...(checkedDocIds || [])],
      removed: null
    }
  }
  all.sort((a, b) => (b.bytes - a.bytes) || (a.kind === 'git' ? -1 : 1))
  const target = all[0]
  if (target.kind === 'git') {
    return {
      checkedGitSlugs: (checkedGitSlugs || []).filter((slug) => slug !== target.id),
      checkedDocIds: [...(checkedDocIds || [])],
      removed: target
    }
  }
  return {
    checkedGitSlugs: [...(checkedGitSlugs || [])],
    checkedDocIds: (checkedDocIds || []).filter((id) => String(id) !== target.id),
    removed: target
  }
}

export function toApiId(id) {
  const text = String(id)
  return /^\d+$/.test(text) ? Number(text) : text
}

export function buildPushJobBody({ pullKey, gitSlugs, documentIds }) {
  return {
    pullKey: String(pullKey || '').trim(),
    gitSlugs: (gitSlugs || []).map(String),
    documentIds: (documentIds || []).map(toApiId)
  }
}

export function isRunningJob(job) {
  return RUNNING_JOB_STATES.includes(String(job && job.state || '').toUpperCase())
}

export function isUnfinishedJob(job) {
  if (!job || !job.jobId) return false
  const state = String(job.state || '').toUpperCase()
  if (!state) return false
  return !FINISHED_JOB_STATES.includes(state)
}

export function isFailedPart(part) {
  return FAILED_PART_STATES.includes(String(part && part.state || '').toUpperCase())
}

export function jobHasFailedParts(job) {
  return ((job && job.parts) || []).some(isFailedPart)
}

export function progressPercent(job) {
  if (!job) return 0
  if (job.percent != null && job.percent !== '' && Number.isFinite(Number(job.percent))) {
    return Math.max(0, Math.min(100, Math.round(Number(job.percent))))
  }
  const parts = job.parts || []
  if (!parts.length) return 0
  const done = parts.filter((part) => DONE_PART_STATES.includes(String(part.state || '').toUpperCase())).length
  return Math.round(done * 100 / parts.length)
}

export function normalizeJob(raw) {
  const body = unwrapAjax(raw)
  if (!body) return null
  const job = body.job && typeof body.job === 'object' ? body.job : body
  if (!job.jobId) return null
  const selection = job.selection || job.include || null
  return {
    jobId: String(job.jobId),
    state: job.state != null ? String(job.state) : '',
    percent: job.percent != null ? Number(job.percent) : null,
    phase: job.phase || '',
    logs: Array.isArray(job.logs) ? job.logs : [],
    error: job.error || '',
    message: job.message || '',
    parts: Array.isArray(job.parts) ? job.parts.map(normalizePart) : null,
    selection: selection ? {
      gitSlugs: (selection.gitSlugs || selection.gits || selection.repositories || []).map(String),
      documentIds: (selection.documentIds || selection.docIds || selection.documents || []).map((id) => {
        return String(id && typeof id === 'object' ? (id.id || id.docId || '') : id)
      }).filter(Boolean)
    } : null
  }
}

function normalizePart(part) {
  return {
    partId: String(part.partId || part.id || ''),
    type: String(part.type || part.kind || '').toLowerCase(),
    name: part.name || part.label || part.partId || part.id || '',
    state: part.state != null ? String(part.state) : '',
    bytes: bytesOf(part),
    error: part.error || ''
  }
}
