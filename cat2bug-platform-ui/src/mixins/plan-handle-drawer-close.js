/**
 * 测试计划执行抽屉：Esc / 关闭前若有嵌套用例抽屉则先关闭；遮罩关闭走同一确认链。
 */
export default {
  methods: {
    $_getHandleCaseDialogRef() {
      const list = this.$refs.list
      return list && list.$refs && list.$refs.handleCaseDialog
    },
    $_closeNestedHandleCaseIfOpen() {
      const nested = this.$_getHandleCaseDialogRef()
      if (nested && nested.visible) {
        nested.cancel()
        return true
      }
      return false
    },
    async confirmClosePlanHandleDrawer() {
      if (this.$_closeNestedHandleCaseIfOpen()) {
        return false
      }
      return true
    },
    async requestClosePlanHandleDrawer(options = {}) {
      const { done } = options
      const ok = await this.confirmClosePlanHandleDrawer()
      if (!ok) return false
      this.doClosePlanHandleDrawer()
      if (typeof done === 'function') done()
      return true
    },
    doClosePlanHandleDrawer() {
      this.visible = false
      this.reset()
      this.$emit('change')
      this.$emit('close')
    },
    closePlanDrawer(done) {
      this.requestClosePlanHandleDrawer({ done })
    }
  }
}
