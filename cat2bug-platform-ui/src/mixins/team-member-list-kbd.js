/**
 * 团队成员列表子页：Esc 返回团队设置（弹框打开时不拦截，交给 dialog-form-shortcuts）。
 */
import { isEscapeCloseKey } from '@/utils/defect-drawer-shortcuts'
import { hasBlockingUiLayer } from '@/plugins/shortcut/service'

export default {
  mounted() {
    this.$_attachTeamMemberEscListener()
  },
  activated() {
    this.$_attachTeamMemberEscListener()
  },
  deactivated() {
    this.$_detachTeamMemberEscListener()
  },
  beforeDestroy() {
    this.$_detachTeamMemberEscListener()
  },
  methods: {
    isTeamMemberDialogOpen() {
      const create = this.$refs.createTeamMemberDialog
      const invite = this.$refs.inviteTeamMemberDialog
      return !!((create && create.dialogVisible) || (invite && invite.dialogVisible))
    },
    shouldTeamMemberEscGoBack() {
      return !this.isTeamMemberDialogOpen()
    },
    $_attachTeamMemberEscListener() {
      if (this.$_teamMemberEscBound) return
      this.$_teamMemberEscBound = true
      this.$_onTeamMemberEsc = (e) => {
        if (this._inactive) return
        if (!isEscapeCloseKey(e)) return
        if (hasBlockingUiLayer()) return
        if (typeof this.shouldTeamMemberEscGoBack === 'function' &&
          !this.shouldTeamMemberEscGoBack()) {
          return
        }
        e.preventDefault()
        e.stopPropagation()
        this.goBack()
      }
      document.addEventListener('keydown', this.$_onTeamMemberEsc, true)
    },
    $_detachTeamMemberEscListener() {
      if (!this.$_teamMemberEscBound) return
      this.$_teamMemberEscBound = false
      document.removeEventListener('keydown', this.$_onTeamMemberEsc, true)
    }
  }
}
