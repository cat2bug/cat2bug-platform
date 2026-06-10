/**
 * 通知列表页：Esc 返回上一页（弹框/抽屉打开时不拦截，查询区键盘导航时 Esc 退出导航）。
 */
import { isEscapeCloseKey } from '@/utils/defect-drawer-shortcuts'
import { hasBlockingUiLayer } from '@/plugins/shortcut/service'

export default {
  mounted() {
    this.$_attachNoticeListEscListener()
  },
  activated() {
    this.$_attachNoticeListEscListener()
  },
  deactivated() {
    this.$_detachNoticeListEscListener()
  },
  beforeDestroy() {
    this.$_detachNoticeListEscListener()
  },
  methods: {
    shouldNoticeEscGoBack() {
      if (this.listQueryNavActive) return false
      const el = document.activeElement
      const tag = el && el.tagName
      if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || (el && el.isContentEditable)) {
        return false
      }
      return true
    },
    $_attachNoticeListEscListener() {
      if (this.$_noticeListEscBound) return
      this.$_noticeListEscBound = true
      this.$_onNoticeListEsc = (e) => {
        if (this._inactive) return
        if (!isEscapeCloseKey(e)) return
        if (hasBlockingUiLayer()) return
        if (typeof this.shouldNoticeEscGoBack === 'function' &&
          !this.shouldNoticeEscGoBack()) {
          return
        }
        e.preventDefault()
        e.stopPropagation()
        this.goBack()
      }
      document.addEventListener('keydown', this.$_onNoticeListEsc, true)
    },
    $_detachNoticeListEscListener() {
      if (!this.$_noticeListEscBound) return
      this.$_noticeListEscBound = false
      document.removeEventListener('keydown', this.$_onNoticeListEsc, true)
    }
  }
}
