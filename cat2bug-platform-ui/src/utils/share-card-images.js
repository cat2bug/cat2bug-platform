import { listColumnLayout } from '@/api/system/defect-field'
import { urlListFromCustomFieldValue } from '@/components/DefectCustomField/format'
import { resolveCommaSeparatedUrls } from '@/utils/upload-asset'

/** @type {Map<number, { fieldKey: string }[]>} */
const imageFieldsCache = new Map()

/**
 * 按缺陷属性管理统一 sortOrder（orderedEnabledFieldKeys）取已启用的 image 字段。
 * @param {number|string} projectId
 * @returns {Promise<{ fieldKey: string }[]>}
 */
export async function loadEnabledImageFields(projectId) {
  const pid = Number(projectId)
  if (!pid) return []
  if (imageFieldsCache.has(pid)) {
    return imageFieldsCache.get(pid)
  }
  try {
    const res = await listColumnLayout(pid)
    const layout = res.data || {}
    const ordered = Array.isArray(layout.orderedEnabledFieldKeys)
      ? layout.orderedEnabledFieldKeys
      : []
    const enabledBuiltin = new Set(layout.enabledBuiltinFieldKeys || [])
    const customByKey = new Map()
    ;(layout.customFields || []).forEach((f) => {
      if (f && f.fieldKey) customByKey.set(f.fieldKey, f)
    })

    const imageFields = []
    ordered.forEach((key) => {
      if (!key) return
      if (key === 'imgUrls') {
        if (enabledBuiltin.has('imgUrls')) {
          imageFields.push({ fieldKey: 'imgUrls' })
        }
        return
      }
      const cf = customByKey.get(key)
      if (cf && cf.fieldType === 'image') {
        imageFields.push({ fieldKey: key })
      }
    })

    // ordered 为空时的兜底：内置图片 + 自定义图片（按 customFields 自身顺序）
    if (!imageFields.length) {
      if (enabledBuiltin.has('imgUrls')) {
        imageFields.push({ fieldKey: 'imgUrls' })
      }
      ;(layout.customFields || [])
        .filter((f) => f && f.fieldType === 'image' && f.fieldKey)
        .slice()
        .sort((a, b) => Number(a.sortOrder || 0) - Number(b.sortOrder || 0))
        .forEach((f) => imageFields.push({ fieldKey: f.fieldKey }))
    }

    imageFieldsCache.set(pid, imageFields)
    return imageFields
  } catch (_e) {
    imageFieldsCache.set(pid, [])
    return []
  }
}

export function clearShareCardImageFieldsCache(projectId) {
  if (projectId == null) {
    imageFieldsCache.clear()
    return
  }
  imageFieldsCache.delete(Number(projectId))
}

/**
 * @param {string} url
 * @returns {Promise<string|null>}
 */
/** 单图探测上限；过长会拖慢分享弹层，宁可少图也不要卡住 */
const PROBE_TIMEOUT_MS = 800

function probeImageUrl(url) {
  if (!url) return Promise.resolve(null)
  return new Promise((resolve) => {
    const img = new Image()
    let settled = false
    const done = (ok) => {
      if (settled) return
      settled = true
      resolve(ok ? url : null)
    }
    img.onload = () => done(true)
    img.onerror = () => done(false)
    // 404/挂起不要拖垮分享弹层；超时视为无图
    setTimeout(() => done(false), PROBE_TIMEOUT_MS)
    img.src = url
  })
}

/**
 * 取某字段上的候选绝对 URL（已滤脏数据）。
 * @param {object} row
 * @param {string} fieldKey
 * @returns {string[]}
 */
export function candidateUrlsForImageField(row, fieldKey) {
  if (!row || !fieldKey) return []
  if (fieldKey === 'imgUrls') {
    return resolveCommaSeparatedUrls(row.imgUrls)
  }
  const cf = row.customFields && typeof row.customFields === 'object' ? row.customFields : null
  const raw = cf ? cf[fieldKey] : undefined
  return resolveCommaSeparatedUrls(urlListFromCustomFieldValue(raw))
}

/**
 * 按 image 字段 sortOrder：字段间串行、字段内并行探测；返回第一个有可加载图的字段的成功 URL。
 * @param {object} row 缺陷行（含 imgUrls / customFields / projectId）
 * @param {{ fieldKey: string }[]} [imageFields] 可选；缺省则按 projectId 拉取
 * @returns {Promise<string[]>}
 */
export async function resolveShareCardImageUrls(row, imageFields) {
  if (!row) return []
  const fields =
    imageFields && imageFields.length
      ? imageFields
      : await loadEnabledImageFields(row.projectId)
  for (const field of fields) {
    const candidates = candidateUrlsForImageField(row, field.fieldKey)
    if (!candidates.length) continue
    const probed = await Promise.all(candidates.map(probeImageUrl))
    const ok = probed.filter(Boolean)
    if (ok.length) return ok
  }
  return []
}
