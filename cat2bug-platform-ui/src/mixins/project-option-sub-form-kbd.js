/**
 * 项目设置表单子页：Esc 返回（未保存确认）、Cmd/Ctrl+Enter 保存、B 返回徽标。
 */
import dialogFormShortcuts from '@/mixins/dialog-form-shortcuts'
import formFieldHints from '@/mixins/form-field-hints'
import pageFormClose from '@/mixins/page-form-close'
import projectOptionSubKbd from '@/mixins/project-option-sub-kbd'

export default {
  mixins: [dialogFormShortcuts, formFieldHints, pageFormClose, projectOptionSubKbd],
  data() {
    return {
      visible: true
    }
  },
  mounted() {
    this.$nextTick(() => {
      if (typeof this.$_syncFieldHintListeners === 'function') {
        this.$_syncFieldHintListeners(true)
      }
    })
  },
  methods: {
    useProjectOptionSubEscShortcut() {
      return false
    },
    goBack() {
      this.onPageFormShortcutClose()
    },
    onPageFormShortcutClose() {
      this.requestClosePageForm({ onClose: () => this.$router.back() })
    }
  }
}
