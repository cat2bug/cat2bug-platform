/**
 * 创建项目表单页：Esc 返回（未保存确认）、Cmd/Ctrl+Enter 提交、B 返回徽标。
 */
import dialogFormShortcuts from '@/mixins/dialog-form-shortcuts'
import formFieldHints from '@/mixins/form-field-hints'
import pageFormClose from '@/mixins/page-form-close'
import projectCreateKbd from '@/mixins/project-create-kbd'

export default {
  mixins: [dialogFormShortcuts, formFieldHints, pageFormClose, projectCreateKbd],
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
    useProjectCreateEscShortcut() {
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
