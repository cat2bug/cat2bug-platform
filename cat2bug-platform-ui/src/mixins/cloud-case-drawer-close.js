/**
 * AI 创建用例抽屉：Esc 关闭前若有提示词或用例列表则确认。
 */
import { serializeCloudCaseDrawerCloseState } from '@/utils/cloud-case-close-state'

export default {
  data() {
    return {
      cloudCaseCloseBaseline: null
    }
  },
  methods: {
    captureCloudCaseCloseBaseline() {
      this.cloudCaseCloseBaseline = serializeCloudCaseDrawerCloseState(this.prompt, this.caseList)
    },
    isCloudCaseCloseDirty() {
      if (!this.cloudCaseCloseBaseline) return false
      return serializeCloudCaseDrawerCloseState(this.prompt, this.caseList) !== this.cloudCaseCloseBaseline
    },
    confirmCloseCloudCaseDrawer() {
      if (!this.isCloudCaseCloseDirty()) {
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
    async requestCloseCloudCaseDrawer(options = {}) {
      const { done } = options
      const ok = await this.confirmCloseCloudCaseDrawer()
      if (!ok) return false
      this.doCloseCloudCaseDrawer()
      if (typeof done === 'function') done()
      return true
    },
    doCloseCloudCaseDrawer() {
      this.cloudCaseCloseBaseline = null
      this.close()
    }
  }
}
