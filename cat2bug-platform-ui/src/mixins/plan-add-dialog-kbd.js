/**
 * 新建/编辑测试计划弹框键盘集成：
 * Cmd/Ctrl+Enter 保存、Esc 关闭（未保存确认）、Cmd/Ctrl 字段字母聚焦、S 聚焦用例查询。
 */
import dialogFormShortcuts from '@/mixins/dialog-form-shortcuts'
import defectToolDialogClose from '@/mixins/defect-tool-dialog-close'
import formFieldHints from '@/mixins/form-field-hints'
import { getLogicalTabStopsInScope } from '@/utils/focus-tab-common'

function serializePlanAddCloseState(vm) {
  const form = vm.form || {}
  const caseIds = vm.selectCaseIdSet ? [...vm.selectCaseIdSet].sort((a, b) => a - b) : []
  const timeRange = Array.isArray(vm.planTimeRang)
    ? vm.planTimeRang.map((d) => (d instanceof Date ? d.getTime() : d))
    : []
  return JSON.stringify({ form: { ...form }, caseIds, timeRange })
}

export default {
  mixins: [dialogFormShortcuts, defectToolDialogClose, formFieldHints],
  computed: {
    dialogVisible() {
      return this.open === true
    }
  },
  watch: {
    open(val) {
      this.$_syncPlanAddCaseQueryKey(!!val)
    }
  },
  beforeDestroy() {
    this.$_unbindPlanAddCaseQueryKey()
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
    },
    $_syncPlanAddCaseQueryKey(open) {
      if (open) {
        if (this._planAddCaseQueryKeyBound) return
        this._planAddCaseQueryKeyBound = true
        this._onPlanAddCaseQueryKey = (e) => this.$_handlePlanAddCaseQueryKey(e)
        document.addEventListener('keydown', this._onPlanAddCaseQueryKey, true)
      } else {
        this.$_unbindPlanAddCaseQueryKey()
      }
    },
    $_unbindPlanAddCaseQueryKey() {
      if (!this._planAddCaseQueryKeyBound) return
      this._planAddCaseQueryKeyBound = false
      document.removeEventListener('keydown', this._onPlanAddCaseQueryKey, true)
      this._onPlanAddCaseQueryKey = null
    },
    $_handlePlanAddCaseQueryKey(e) {
      if (!this.open || e.isComposing) return
      if (e.metaKey || e.ctrlKey || e.altKey || e.shiftKey) return
      if (e.key !== 's' && e.key !== 'S') return
      if (typeof this.$_ownsTopFormDrawer === 'function' && !this.$_ownsTopFormDrawer()) return
      const tag = e.target && e.target.tagName
      if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return
      if (e.target && e.target.isContentEditable) return
      e.preventDefault()
      e.stopPropagation()
      this.$_focusPlanCaseQuery()
    },
    $_focusPlanCaseQuery() {
      const input = this.$el && this.$el.querySelector('.plan-add-hint-query input')
      if (!input) return
      if (typeof this.$_focusControl === 'function') {
        this.$_focusControl(input)
      } else if (typeof input.focus === 'function') {
        input.focus()
      }
    },
    $_focusToolDialogFirstField() {
      const container = typeof this.getFieldHintContainer === 'function'
        ? this.getFieldHintContainer()
        : this.$el
      if (!container) return
      const first = getLogicalTabStopsInScope(container)[0]
      if (!first) return
      if (typeof this.$_focusControl === 'function') {
        this.$_focusControl(first)
      } else if (typeof first.focus === 'function') {
        try {
          first.focus({ preventScroll: false })
        } catch (err) {
          first.focus()
        }
      }
    },
    shortcutSave() {
      if (typeof this.submitForm === 'function') this.submitForm()
    },
    getFixedFieldHints() {
      return [
        {
          letter: 'S',
          badgeSelector: '.plan-add-hint-query',
          focusSelector: '.plan-add-hint-query input',
          onActivate: () => this.$_focusPlanCaseQuery()
        }
      ]
    },
    getFieldHintContainer() {
      const form = this.$refs.form
      return (form && form.$el) || this.$el
    },
    getFieldHintScrollContainer() {
      const body = this.$el && this.$el.closest('.el-dialog__body')
      return body || this.getFieldHintContainer()
    },
    onPlanAddDialogOpenedKbd() {
      this.$nextTick(() => {
        requestAnimationFrame(() => {
          this.captureToolDialogCloseBaseline()
          this.$_focusToolDialogFirstField()
        })
      })
    }
  }
}
