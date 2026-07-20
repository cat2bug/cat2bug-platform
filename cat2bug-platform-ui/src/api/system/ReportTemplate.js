import request from '@/utils/request'
import { resolveUploadUrl } from '@/utils/upload-asset'

/** Picker group codes in display order (custom → 29119-3 → ISO 9001). */
export const REPORT_TEMPLATE_PICKER_GROUP_ORDER = Object.freeze([
  'custom',
  'iso-29119-3',
  'iso-9001'
])

// 查询报告模版列表（管理页 / 仅 DB 自定义）
export function listReportTemplate(query) {
  return request({
    url: '/system/report/template/list',
    method: 'get',
    params: query
  })
}

/** 分组选择器：自定义分页 + classpath 预制 */
export function pickerReportTemplates(projectId, query = {}) {
  return request({
    url: '/system/report/template/picker',
    method: 'get',
    params: {
      projectId,
      ...query
    }
  })
}

/** 预览图缓存破坏参数（与后端 PREVIEW_URL_VERSION 对齐；重渲主题后 bump） */
export const PRESET_PREVIEW_CACHE_BUST = 'light-1'

/** classpath 预制预览 API 路径（相对 VUE_APP_BASE_API） */
export function presetTemplatePreviewPath(group, key, folder) {
  if (!group || !key) return ''
  const parts = splitPresetKey(key, folder)
  let url = `/system/report/template/preset/${encodeURIComponent(group)}`
  if (parts.folder) {
    url += `/${encodeURIComponent(parts.folder)}`
  }
  url += `/${encodeURIComponent(parts.key)}/preview`
  return `${url}?v=${PRESET_PREVIEW_CACHE_BUST}`
}

/** 预制预览绝对 URL（匿名可读后可直接作 img src） */
export function presetTemplatePreviewUrl(group, key, folder, baseApi = process.env.VUE_APP_BASE_API) {
  return resolveUploadUrl(presetTemplatePreviewPath(group, key, folder), baseApi)
}

/**
 * 带 Authorization 拉取预制预览 PNG（&lt;img&gt; 不会带 token；并绕过旧黑底缓存）。
 * @returns {Promise<Blob|null>}
 */
export function fetchPresetPreviewBlob(group, key, folder) {
  const url = presetTemplatePreviewPath(group, key, folder)
  if (!url) return Promise.resolve(null)
  return request({
    url,
    method: 'get',
    responseType: 'blob',
    silentError: true,
    headers: {
      'Cache-Control': 'no-cache',
      Pragma: 'no-cache'
    }
  }).then((data) => {
    if (!data || !(data instanceof Blob) || data.size < 32) return null
    // 失败时后端常返回 JSON（code:401），被 axios 当成 blob
    if (data.type && /json|text\//i.test(data.type)) return null
    return data
  }).catch(() => null)
}

/** Split `manuals/foo` style keys for path-based preset APIs. */
export function splitPresetKey(key, folder) {
  if (folder) {
    return { folder, key }
  }
  if (key && key.includes('/')) {
    const idx = key.indexOf('/')
    return { folder: key.slice(0, idx), key: key.slice(idx + 1) }
  }
  return { folder: null, key }
}

/** 复制预制为项目内自定义模版 */
export function copyPresetTemplate(group, key, projectId, folder) {
  const parts = splitPresetKey(key, folder)
  let url = `/system/report/template/preset/${encodeURIComponent(group)}`
  if (parts.folder) {
    url += `/${encodeURIComponent(parts.folder)}`
  }
  url += `/${encodeURIComponent(parts.key)}/copy`
  return request({
    url,
    method: 'post',
    params: { projectId }
  })
}

// 查询报告模版详细
export function getReportTemplate(templateId) {
  return request({
    url: '/system/report/template/' + templateId,
    method: 'get'
  })
}

// 新增报告模版
export function addReportTemplate(data) {
  return request({
    url: '/system/report/template',
    method: 'post',
    data: data
  })
}

// 修改报告模版
export function updateTemplate(data) {
  return request({
    url: '/system/report/template',
    method: 'put',
    data: data
  })
}

// 删除报告模版
export function delTemplate(templateId) {
  return request({
    url: '/system/report/template/' + templateId,
    method: 'delete'
  })
}
