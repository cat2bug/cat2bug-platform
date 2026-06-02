/**
 * 缺陷列表「扩展查询参数」注册表（与通用 Tab/工具栏条件分离）。
 * 新增扩展域：在 DEFECT_QUERY_EXTENSION_KEYS 追加一组键名，并实现对应 Mapper 条件。
 */

/** @type {Record<string, string[]>} */
export const DEFECT_QUERY_EXTENSION_KEYS = {
  participation: ['participationLogDate', 'participationLogMonth', 'participationUserId'],
  customFields: ['customFieldFilters']
}

function getAllExtensionKeys() {
  return Object.values(DEFECT_QUERY_EXTENSION_KEYS).flat()
}

/**
 * 从 queryParams.params 移除所有已注册的扩展键。
 * @param {Object} queryParams
 * @param {Object} [vm] Vue 组件实例，传入时使用 $delete 以保持响应式
 */
export function clearExtensionParams(queryParams, vm) {
  const params = queryParams && queryParams.params
  if (!params) {
    return
  }
  getAllExtensionKeys().forEach(key => {
    if (Object.prototype.hasOwnProperty.call(params, key)) {
      if (vm && typeof vm.$delete === 'function') {
        vm.$delete(params, key)
      } else {
        delete params[key]
      }
    }
  })
}

/**
 * @param {Object} [extension]
 * @returns {boolean}
 */
export function hasParticipationExtension(extension) {
  if (!extension) {
    return false
  }
  if (extension.participationUserId == null || extension.participationUserId === '') {
    return false
  }
  return (extension.participationLogDate != null && extension.participationLogDate !== '')
    || (extension.participationLogMonth != null && extension.participationLogMonth !== '')
}
