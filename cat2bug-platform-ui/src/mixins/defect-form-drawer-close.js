/**
 * 缺陷新建/编辑抽屉：Esc 关闭前若有未保存修改则确认。
 */
import { serializeDefectFormCloseState } from '@/utils/defect-form-close-state'

export default {
  data() {
    return {
      defectFormCloseBaseline: null
    }
  },
  methods: {
    captureDefectFormCloseBaseline() {
      this.defectFormCloseBaseline = serializeDefectFormCloseState(this.form, this.planTimeRange)
    },
    isDefectFormCloseDirty() {
      if (!this.defectFormCloseBaseline) return false
      return serializeDefectFormCloseState(this.form, this.planTimeRange) !== this.defectFormCloseBaseline
    },
    confirmCloseDefectFormDrawer() {
      if (!this.isDefectFormCloseDirty()) {
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
    async requestCloseDefectFormDrawer(options = {}) {
      const { done, isReset } = options
      const ok = await this.confirmCloseDefectFormDrawer()
      if (!ok) return false
      this.doCloseDefectFormDrawer(isReset)
      if (typeof done === 'function') done()
      return true
    },
    shortcutClose(e) {
      if (e) {
        e.preventDefault()
        e.stopPropagation()
        if (typeof e.stopImmediatePropagation === 'function') e.stopImmediatePropagation()
      }
      this.requestCloseDefectFormDrawer()
    },
    shortcutSave() {
      if (typeof this.submitForm === 'function') this.submitForm()
    }
  }
}
