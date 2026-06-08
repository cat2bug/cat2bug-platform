/**
 * 用例新建/编辑抽屉：Esc 关闭前若有未保存修改则确认。
 */
import { serializeCaseFormCloseState } from '@/utils/case-form-close-state'

export default {
  data() {
    return {
      caseFormCloseBaseline: null
    }
  },
  methods: {
    captureCaseFormCloseBaseline() {
      this.caseFormCloseBaseline = serializeCaseFormCloseState(this.form, this.isCreateNextCase)
    },
    isCaseFormCloseDirty() {
      if (!this.caseFormCloseBaseline) return false
      return serializeCaseFormCloseState(this.form, this.isCreateNextCase) !== this.caseFormCloseBaseline
    },
    confirmCloseCaseFormDrawer() {
      if (!this.isCaseFormCloseDirty()) {
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
    async requestCloseCaseFormDrawer(options = {}) {
      const { done, isReset } = options
      const ok = await this.confirmCloseCaseFormDrawer()
      if (!ok) return false
      this.doCloseCaseFormDrawer(isReset)
      if (typeof done === 'function') done()
      return true
    },
    shortcutClose(e) {
      if (e) {
        e.preventDefault()
        e.stopPropagation()
        if (typeof e.stopImmediatePropagation === 'function') e.stopImmediatePropagation()
      }
      this.requestCloseCaseFormDrawer()
    },
    shortcutSave() {
      if (typeof this.submitForm === 'function') this.submitForm()
    }
  }
}
