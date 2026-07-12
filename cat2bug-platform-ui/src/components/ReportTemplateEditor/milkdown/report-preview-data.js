/** Shared preview dataset for Milkdown DSL node views (mirrors Cat2BugMarkdown markdownData + getProject). */

import request from '@/utils/request'

const listeners = new Set()

let state = {
  projectId: null,
  data: null,
  loading: false,
  error: null
}

let inflight = null
let inflightProjectId = null
let loadSeq = 0

export function resolveMarkdownPayload(res) {
  if (!res || typeof res !== 'object') {
    return {}
  }
  if (res.defect || res.case || res.plan || res.project || res.member) {
    return res
  }
  if (res.data && typeof res.data === 'object') {
    return res.data
  }
  return {}
}

function notify() {
  listeners.forEach((listener) => {
    try {
      listener(state)
    } catch (_e) {
      // ignore subscriber errors
    }
  })
}

export function setReportPreviewData(projectId, data, loading = false, error = null) {
  state = {
    projectId: projectId ?? null,
    data: data ?? null,
    loading: Boolean(loading),
    error: error ?? null
  }
  notify()
}

export function getReportPreviewState() {
  return state
}

export function getPreviewRows(dataSource) {
  const { data } = state
  if (!data) {
    return []
  }
  if (dataSource === 'api.defect.list') {
    return data.defect?.list || []
  }
  if (dataSource === 'api.case.list') {
    return data.case?.list || []
  }
  return []
}

/**
 * Resolve ${api.project.projectIcon} style paths against preview payload.
 * @param {string} path e.g. api.project.projectIcon
 */
export function resolvePreviewPath(path) {
  const { data } = state
  if (!data || !path) {
    return null
  }
  const parts = String(path).split('.').filter(Boolean)
  if (parts[0] === 'api') {
    parts.shift()
  }
  let current = data
  for (const part of parts) {
    if (current == null) {
      return null
    }
    current = current[part]
  }
  return current
}

export function subscribeReportPreviewData(listener) {
  listeners.add(listener)
  try {
    listener(state)
  } catch (_e) {
    // ignore
  }
  return () => listeners.delete(listener)
}

function fetchPreviewMarkdown(projectId) {
  return request({
    url: `/markdown/${projectId}`,
    method: 'get',
    silentError: true
  })
}

function fetchPreviewProject(projectId) {
  return request({
    url: `/system/project/${projectId}`,
    method: 'get',
    silentError: true
  }).catch(() => null)
}

/**
 * Load / reuse markdown preview payload for a project (defect/case/plan + project icon).
 * @param {number|string} projectId
 */
export function getActivePreviewProjectId() {
  return state.projectId || inflightProjectId
}

export function ensureReportPreviewData(projectId) {
  try {
    return ensureReportPreviewDataInner(projectId)
  } catch (error) {
    const id = projectId != null && projectId !== '' ? String(projectId) : ''
    setReportPreviewData(id || null, null, false, error)
    return Promise.resolve(state)
  }
}

function ensureReportPreviewDataInner(projectId) {
  const id = projectId != null && projectId !== '' ? String(projectId) : ''
  if (!id) {
    setReportPreviewData(null, null, false)
    return Promise.resolve(state)
  }

  if (String(state.projectId) === id && state.data && !state.loading) {
    return Promise.resolve(state)
  }

  if (inflight && inflightProjectId === id) {
    return inflight
  }

  const seq = ++loadSeq
  inflightProjectId = id
  setReportPreviewData(id, null, true)

  inflight = Promise.all([
    fetchPreviewMarkdown(id),
    fetchPreviewProject(id)
  ])
    .then(([mdRes, projectRes]) => {
      if (seq !== loadSeq) {
        return state
      }
      const payload = {
        ...resolveMarkdownPayload(mdRes),
        project: resolveMarkdownPayload(projectRes)
      }
      setReportPreviewData(id, payload, false)
      return state
    })
    .catch((error) => {
      if (seq !== loadSeq) {
        return state
      }
      setReportPreviewData(id, {}, false, error)
      return state
    })
    .finally(() => {
      if (inflightProjectId === id) {
        inflight = null
        inflightProjectId = null
      }
    })

  return inflight
}
