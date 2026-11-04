/**
 * v-hasPermi 操作权限处理
 * Copyright (c) 2023 cat2bug
 *
 * Vue 3 钩子必须用 mounted / updated / unmounted（Vue 2 的 inserted / update 不会被调用）。
 */

import { watch } from 'vue'
import { pinia } from '@/store/pinia.js'
import { useUserStore } from '@/stores/user'

function applyHasPermi(el, binding) {
  const { value } = binding
  const all_permission = '*:*:*'
  const permissions = useUserStore(pinia).permissions || []

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
  mounted(el, binding) {
    applyHasPermi(el, binding)
    const userStore = useUserStore(pinia)
    el._vHasPermiStop = watch(
      () => userStore.permissions,
      () => applyHasPermi(el, binding),
      { deep: true }
    )
  },
  updated(el, binding) {
    applyHasPermi(el, binding)
  },
  unmounted(el) {
    if (typeof el._vHasPermiStop === 'function') {
      el._vHasPermiStop()
      delete el._vHasPermiStop
    }
  }
}
