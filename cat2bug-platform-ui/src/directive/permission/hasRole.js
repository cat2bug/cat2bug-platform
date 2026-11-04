/**
 * v-hasRole 角色权限处理
 * Copyright (c) 2023 cat2bug
 *
 * Vue 3 钩子必须用 mounted / updated / unmounted（Vue 2 的 inserted 不会被调用）。
 */

import { watch } from 'vue'
import { pinia } from '@/store/pinia.js'
import { useUserStore } from '@/stores/user'

function applyHasRole(el, binding) {
  const { value } = binding
  const super_admin = 'admin'
  const roles = useUserStore(pinia).roles || []

  if (value && value instanceof Array && value.length > 0) {
    const roleFlag = value
    const hasRole = roles.some(role => {
      return super_admin === role || roleFlag.includes(role)
    })
    if (el._vHasRoleOriginalDisplay === undefined) {
      el._vHasRoleOriginalDisplay = el.style.display || ''
    }
    el.style.display = hasRole ? el._vHasRoleOriginalDisplay : 'none'
  } else {
    throw new Error('请设置角色权限标签值')
  }
}

export default {
  mounted(el, binding) {
    applyHasRole(el, binding)
    const userStore = useUserStore(pinia)
    el._vHasRoleStop = watch(
      () => userStore.roles,
      () => applyHasRole(el, binding),
      { deep: true }
    )
  },
  updated(el, binding) {
    applyHasRole(el, binding)
  },
  unmounted(el) {
    if (typeof el._vHasRoleStop === 'function') {
      el._vHasRoleStop()
      delete el._vHasRoleStop
    }
  }
}
