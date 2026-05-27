 /**
 * v-hasPermi 操作权限处理
 * Copyright (c) 2023 cat2bug
 */

import store from '@/store'

function applyHasPermi(el, binding) {
  const { value } = binding
  const all_permission = '*:*:*'
  const permissions = (store.getters && store.getters.permissions) || []

  if (value && value instanceof Array && value.length > 0) {
    const permissionFlag = value
    const hasPermissions = permissions.some(permission => {
      return all_permission === permission || permissionFlag.includes(permission)
    })
    if (el._vHasPermiOriginalDisplay === undefined) {
      el._vHasPermiOriginalDisplay = el.style.display || ''
    }
    el.style.display = hasPermissions ? el._vHasPermiOriginalDisplay : 'none'
  } else {
    throw new Error('请设置操作权限标签值')
  }
}

export default {
  inserted(el, binding) {
    applyHasPermi(el, binding)
  },
  update(el, binding) {
    applyHasPermi(el, binding)
  }
}
