import {
  getReportPreviewState,
  resolvePreviewPath
} from './report-preview-data'

/**
 * @param {unknown} value
 * @returns {string|null}
 */
export function formatPreviewScalar(value) {
  if (value == null || value === '') {
    return null
  }
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}

/**
 * @param {{ path?: string }} attrs
 * @returns {{ kind: 'loading'|'error'|'empty'|'value', text?: string }}
 */
export function resolvePreviewVariable(attrs) {
  const { loading, error } = getReportPreviewState()
  if (loading) {
    return { kind: 'loading' }
  }
  if (error) {
    return { kind: 'error' }
  }
  const text = formatPreviewScalar(resolvePreviewPath(attrs.path))
  if (!text) {
    return { kind: 'empty' }
  }
  return { kind: 'value', text }
}

/**
 * @param {{ path?: string, planIndex?: number }} attrs
 * @returns {{ kind: 'loading'|'error'|'empty'|'value', text?: string }}
 */
export function resolvePreviewPlanVariable(attrs) {
  const { loading, error, data } = getReportPreviewState()
  if (loading) {
    return { kind: 'loading' }
  }
  if (error) {
    return { kind: 'error' }
  }

  const list = data?.plan?.list || []
  const planIndex = Number(attrs.planIndex) || 1
  const plan = list.find((item) => Number(item?.planNumber) === planIndex)
  if (!plan) {
    return { kind: 'empty' }
  }

  const parts = String(attrs.path || '').split('.').filter(Boolean)
  if (parts[0] === 'api') {
    parts.shift()
  }
  if (parts[0] === 'plan') {
    parts.shift()
  }
  const fieldPath = parts.join('.')
  if (!fieldPath) {
    return { kind: 'empty' }
  }

  let current = plan
  for (const part of fieldPath.split('.')) {
    if (!part) {
      continue
    }
    current = current?.[part]
    if (current == null) {
      return { kind: 'empty' }
    }
  }

  const text = formatPreviewScalar(current)
  if (!text) {
    return { kind: 'empty' }
  }
  return { kind: 'value', text }
}
