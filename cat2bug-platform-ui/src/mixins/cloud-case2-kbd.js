/**
 * CloudCase2：AI 对话创建用例抽屉键盘集成。
 */
import dialogFormShortcuts from '@/mixins/dialog-form-shortcuts'
import formFieldHints from '@/mixins/form-field-hints'
import { getLogicalTabStopsInScope } from '@/utils/focus-tab-common'

export default {
  mixins: [dialogFormShortcuts, formFieldHints],
  data() {
    return {
      cloudCase2CloseBaseline: null
    }
  },
  methods: {
    captureCloudCase2CloseBaseline() {
      this.cloudCase2CloseBaseline = JSON.stringify({
        query: this.query.query || '',
        chatCount: this.chatList.length
      })
    },
    isCloudCase2CloseDirty() {
      if (!this.cloudCase2CloseBaseline) return false
      return JSON.stringify({
        query: this.query.query || '',
        chatCount: this.chatList.length
      }) !== this.cloudCase2CloseBaseline
    },
    confirmCloseCloudCase2Drawer() {
      if (!this.isCloudCase2CloseDirty()) {
        return Promise.resolve(true)
      }
      return this.$modal.confirm(
        this.$i18n.t('defect.unsaved-close-confirm').toString(),
        this.$i18n.t('prompted').toString(),
        {
          confirmButtonText: this.$i18n.t('ok').toString(),
          cancelButtonText: this.$i18n.t('cancel').toString(),
          type: 'warning'
        }
      ).then(() => true).catch(() => false)
    },
    async requestCloseCloudCase2Drawer(options = {}) {
      const { done } = options
      const ok = await this.confirmCloseCloudCase2Drawer()
      if (!ok) return false
      this.cloudCase2CloseBaseline = null
      this.close()
      if (typeof done === 'function') done()
      return true
    },
    shortcutClose(e) {
      if (e) {
        e.preventDefault()
        e.stopPropagation()
        if (typeof e.stopImmediatePropagation === 'function') e.stopImmediatePropagation()
      }
      this.requestCloseCloudCase2Drawer()
    },
    shortcutSave() {
      this.handleCreateCaseDescribe()
    },
    getFieldHintContainer() {
      const send = this.$el && this.$el.querySelector('.send')
      return send || this.$el
    },
    getFieldHintScrollContainer() {
      const chat = this.$refs.chat
      return chat || this.getFieldHintContainer()
    },
    $_focusCloudCase2FirstField() {
      const container = this.getFieldHintContainer()
      if (!container) return
      const first = getLogicalTabStopsInScope(container)[0]
      if (!first) return
      if (typeof this.$_focusControl === 'function') {
        this.$_focusControl(first)
      } else if (typeof first.focus === 'function') {
        try {
          first.focus({ preventScroll: false })
        } catch (e) {
          first.focus()
        }
      }
    }
  }
}
