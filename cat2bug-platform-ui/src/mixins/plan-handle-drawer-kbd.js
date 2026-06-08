/**
 * 测试计划执行抽屉键盘集成（首期）：
 * Esc 关闭、Cmd/Ctrl+Enter 在有提交动作时触发（抽屉层无表单则 no-op）。
 */
import dialogFormShortcuts from '@/mixins/dialog-form-shortcuts'
import planHandleDrawerClose from '@/mixins/plan-handle-drawer-close'
import { getLogicalTabStopsInScope } from '@/utils/focus-tab-common'

export default {
  mixins: [dialogFormShortcuts, planHandleDrawerClose],
  methods: {
    $_focusHandlePlanFirstControl() {
      const list = this.$refs.list
      const root = (list && list.$el) || this.$el
      if (!root) return
      const first = getLogicalTabStopsInScope(root)[0]
      if (!first) return
      if (typeof first.focus === 'function') {
        try {
          first.focus({ preventScroll: false })
        } catch (err) {
          first.focus()
        }
      }
    },
    onHandlePlanDrawerOpenedKbd() {
      this.$nextTick(() => {
        requestAnimationFrame(() => {
          this.$_focusHandlePlanFirstControl()
        })
      })
    },
    shortcutClose(e) {
      if (e) {
        e.preventDefault()
        e.stopPropagation()
        if (typeof e.stopImmediatePropagation === 'function') e.stopImmediatePropagation()
      }
      this.requestClosePlanHandleDrawer()
    },
    shortcutSave() {
      if (typeof this.submitForm === 'function') {
        this.submitForm()
      }
    }
  }
}
