/**
 * Group commits by local calendar day of commitTime (newest day first).
 * @param {Array<{ commitTime?: string|number|Date, id?: string }>} commits
 * @param {string} [locale] e.g. 'zh-CN' | 'en-US'
 * @returns {Array<{ dateKey: string, label: string, items: Array }>}
 */
export function groupCommitsByDate(commits, locale) {
  if (!Array.isArray(commits) || !commits.length) {
    return []
  }
  const loc = locale || 'en-US'
  const groups = []
  const indexByKey = Object.create(null)

  for (let i = 0; i < commits.length; i++) {
    const c = commits[i]
    const d = toLocalDate(c && c.commitTime)
    if (!d) {
      const orphanKey = '__unknown__'
      if (indexByKey[orphanKey] == null) {
        indexByKey[orphanKey] = groups.length
        groups.push({ dateKey: orphanKey, label: '', items: [] })
      }
      groups[indexByKey[orphanKey]].items.push(c)
      continue
    }
    const dateKey = formatDateKey(d)
    if (indexByKey[dateKey] == null) {
      indexByKey[dateKey] = groups.length
      groups.push({
        dateKey,
        label: formatDateLabel(d, loc),
        items: []
      })
    }
    groups[indexByKey[dateKey]].items.push(c)
  }
  return groups
}

function toLocalDate(value) {
  if (value == null || value === '') return null
  const d = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(d.getTime())) return null
  return d
}

function formatDateKey(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return y + '-' + m + '-' + day
}

function formatDateLabel(d, locale) {
  try {
    return new Intl.DateTimeFormat(locale, {
      year: 'numeric',
      month: locale && String(locale).toLowerCase().startsWith('zh') ? 'numeric' : 'short',
      day: 'numeric'
    }).format(d)
  } catch (e) {
    return formatDateKey(d)
  }
}
