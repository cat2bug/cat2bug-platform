import { computed, getCurrentInstance, onBeforeUnmount, watch } from 'vue'
import { getLogicalTabStopsInScope } from '@/utils/focus-tab-common'
import dialogFormShortcuts from '@/mixins/dialog-form-shortcuts'
import defectToolDialogClose from '@/mixins/defect-tool-dialog-close'
import formFieldHints from '@/mixins/form-field-hints'

function serializePlanAddCloseState(vm) {
  const form = vm.form || {}
  const caseIds = vm.selectCaseIdSet ? [...vm.selectCaseIdSet].sort((a, b) => a - b) : []
  const timeRange = Array.isArray(vm.planTimeRang)
    ? vm.planTimeRang.map((d) => (d instanceof Date ? d.getTime() : d))
    : []
  return JSON.stringify({ form: { ...form }, caseIds, timeRange })
}

export function usePlanAddDialogKbd() {
  const instance = getCurrentInstance()
  const vm = () => instance && instance.proxy

  let _planAddCaseQueryKeyBound = false
  let _onPlanAddCaseQueryKey = null

  const dialogVisible = computed(() => vm()?.open === true)

  const captureToolDialogCloseBaseline = () => {
    const host = vm()
    if (!host) return
    host.toolDialogCloseBaseline = serializePlanAddCloseState(host)
  }

  const isToolDialogCloseDirty = () => {
    const host = vm()
    if (!host || !host.toolDialogCloseBaseline) return false
    return serializePlanAddCloseState(host) !== host.toolDialogCloseBaseline
  }

  const doCloseToolDialog = () => {
    const host = vm()
    if (!host) return
    host.open = false
    host.toolDialogCloseBaseline = null
    if (typeof host.reset === 'function') host.reset()
    host.$emit('close')
  }

  const $_syncPlanAddCaseQueryKey = (open) => {
    if (open) {
      if (_planAddCaseQueryKeyBound) return
      _planAddCaseQueryKeyBound = true
      _onPlanAddCaseQueryKey = (e) => $_handlePlanAddCaseQueryKey(e)
      document.addEventListener('keydown', _onPlanAddCaseQueryKey, true)
      return
    }
    $_unbindPlanAddCaseQueryKey()
  }

  const $_unbindPlanAddCaseQueryKey = () => {
    if (!_planAddCaseQueryKeyBound) return
    _planAddCaseQueryKeyBound = false
    document.removeEventListener('keydown', _onPlanAddCaseQueryKey, true)
    _onPlanAddCaseQueryKey = null
  }

  const $_handlePlanAddCaseQueryKey = (e) => {
    const host = vm()
    if (!host || !host.open || e.isComposing) return
    if (e.metaKey || e.ctrlKey || e.altKey || e.shiftKey) return
    if (e.key !== 's' && e.key !== 'S') return
    if (typeof host.$_ownsTopFormDrawer === 'function' && !host.$_ownsTopFormDrawer()) return
    const tag = e.target && e.target.tagName
    if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return
    if (e.target && e.target.isContentEditable) return
    e.preventDefault()
    e.stopPropagation()
    $_focusPlanCaseQuery()
  }

  const $_focusPlanCaseQuery = () => {
    const host = vm()
    const input = host && host.$el && host.$el.querySelector('.plan-add-hint-query input')
    if (!input) return
    if (host && typeof host.$_focusControl === 'function') {
      host.$_focusControl(input)
    } else if (typeof input.focus === 'function') {
      input.focus()
    }
  }

  const $_focusToolDialogFirstField = () => {
    const host = vm()
    if (!host) return
    const container = typeof host.getFieldHintContainer === 'function'
      ? host.getFieldHintContainer()
      : host.$el
    if (!container) return
    const first = getLogicalTabStopsInScope(container)[0]
    if (!first) return
    if (typeof host.$_focusControl === 'function') {
      host.$_focusControl(first)
    } else if (typeof first.focus === 'function') {
      try {
        first.focus({ preventScroll: false })
      } catch (err) {
        first.focus()
      }
    }
  }

  const shortcutSave = () => {
    const host = vm()
    if (host && typeof host.submitForm === 'function') host.submitForm()
  }

  const getFixedFieldHints = () => {
    return [
      {
        letter: 'S',
        badgeSelector: '.plan-add-hint-query',
        focusSelector: '.plan-add-hint-query input',
        onActivate: () => $_focusPlanCaseQuery()
      }
    ]
  }

  const getFieldHintContainer = () => {
    const host = vm()
    const form = host && host.$refs && host.$refs.form
    return (form && form.$el) || (host && host.$el)
  }

  const getFieldHintScrollContainer = () => {
    const host = vm()
    const body = host && host.$el && host.$el.closest('.el-dialog__body')
    return body || getFieldHintContainer()
  }

  const onPlanAddDialogOpenedKbd = () => {
    const host = vm()
    if (!host || typeof host.$nextTick !== 'function') return
    host.$nextTick(() => {
      requestAnimationFrame(() => {
        captureToolDialogCloseBaseline()
        $_focusToolDialogFirstField()
      })
    })
  }

  watch(() => vm()?.open, (val) => {
    $_syncPlanAddCaseQueryKey(!!val)
  })

  onBeforeUnmount(() => {
    $_unbindPlanAddCaseQueryKey()
  })

  return {
    dialogVisible,
    captureToolDialogCloseBaseline,
    isToolDialogCloseDirty,
    doCloseToolDialog,
    $_syncPlanAddCaseQueryKey,
    $_unbindPlanAddCaseQueryKey,
    $_handlePlanAddCaseQueryKey,
    $_focusPlanCaseQuery,
    $_focusToolDialogFirstField,
    shortcutSave,
    getFixedFieldHints,
    getFieldHintContainer,
    getFieldHintScrollContainer,
    onPlanAddDialogOpenedKbd
  }
}

/**
 * 新建计划弹框快捷键/关闭：须用 Options methods 覆盖 defectToolDialogClose
 *（关闭字段是 open 而非 dialogVisible；否则 setup 返回值会被 mixin 盖掉，取消按钮无反应）。
 */
export default {
  mixins: [dialogFormShortcuts, defectToolDialogClose, formFieldHints],
  setup() {
    return usePlanAddDialogKbd()
  },
  methods: {
    captureToolDialogCloseBaseline() {
      this.toolDialogCloseBaseline = serializePlanAddCloseState(this)
    },
    isToolDialogCloseDirty() {
      if (!this.toolDialogCloseBaseline) return false
      return serializePlanAddCloseState(this) !== this.toolDialogCloseBaseline
    },
    doCloseToolDialog() {
      this.open = false
      this.toolDialogCloseBaseline = null
      if (typeof this.reset === 'function') this.reset()
      this.$emit('close')
    }
  }
}
