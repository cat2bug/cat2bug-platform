/**
 * 团队设置子页：返回快捷键（B / 空格面板）、⌘ 徽标、Esc 返回上一级。
 * 表单子页请改用 team-option-sub-form-kbd（含 Esc 未保存确认与 Cmd+Enter 保存）。
 */
import pageActionHints from '@/mixins/page-action-hints'
import { shortcutStore } from '@/plugins/shortcut/shortcut-store'
import { hasBlockingUiLayer } from '@/plugins/shortcut/service'
import { isEscapeCloseKey } from '@/utils/defect-drawer-shortcuts'
import {
  buildProjectOptionSubPaginationShortcuts,
  clickProjectOptionSubPagination
} from '@/utils/project-option-sub-list-kbd'

export const TEAM_OPTION_KBD_SCOPE = 'team-option'
export const TEAM_OPTION_SUB_HINT_BACK_CLASS = 'team-option-sub-hint-back'

export default {
  mixins: [pageActionHints],
  mounted() {
    this.registerTeamOptionSubShortcuts()
    this.$_attachTeamOptionSubEscListener()
  },
  activated() {
    this.registerTeamOptionSubShortcuts()
    this.$_attachTeamOptionSubEscListener()
  },
  deactivated() {
    this.$_detachTeamOptionSubEscListener()
    if (this.$shortcut) this.$shortcut.unregisterPage(TEAM_OPTION_KBD_SCOPE)
  },
  beforeDestroy() {
    this.$_detachTeamOptionSubEscListener()
    if (this.$shortcut) this.$shortcut.unregisterPage(TEAM_OPTION_KBD_SCOPE)
  },
  methods: {
    registerTeamOptionSubShortcuts() {
      if (!this.$shortcut) return
      const extra = typeof this.getTeamOptionSubRegisterActions === 'function'
        ? this.getTeamOptionSubRegisterActions()
        : typeof this.getProjectOptionSubRegisterActions === 'function'
          ? this.getProjectOptionSubRegisterActions()
          : []
      const pagination = buildProjectOptionSubPaginationShortcuts(this)
      this.$shortcut.registerPage(TEAM_OPTION_KBD_SCOPE, [
        { key: 'back', defaultLetter: 'B', titleKey: 'keyboard.act.team-option-back', run: () => this.goBack() },
        ...extra,
        ...pagination.register
      ])
    },
    getPageActionHintContainer() {
      if (this.$refs.teamOptionSubMain) return this.$refs.teamOptionSubMain
      if (this.$refs.projectOptionSubMain) return this.$refs.projectOptionSubMain
      return this.$el
    },
    getTeamOptionSubBackHints() {
      const L = (key, def) => shortcutStore.getLetter(`action.${TEAM_OPTION_KBD_SCOPE}.${key}`, def)
      return [{
        key: 'back',
        letter: L('back', 'B'),
        badgeSelector: `.${TEAM_OPTION_SUB_HINT_BACK_CLASS} .el-page-header__left`,
        floatOffset: { placement: 'bottom-right-outset', outset: 2 },
        run: () => this.goBack()
      }]
    },
    getPageActionHints() {
      const extra = typeof this.getTeamOptionSubActionHints === 'function'
        ? this.getTeamOptionSubActionHints()
        : typeof this.getProjectOptionSubActionHints === 'function'
          ? this.getProjectOptionSubActionHints()
          : []
      const pagination = buildProjectOptionSubPaginationShortcuts(this)
      return [
        ...this.getTeamOptionSubBackHints(),
        ...extra,
        ...pagination.hints
      ]
    },
    shortcutChangePage(delta) {
      clickProjectOptionSubPagination(this, delta)
    },
    goBack() {
      this.$router.back()
    },
    useTeamOptionSubEscShortcut() {
      return true
    },
    $_attachTeamOptionSubEscListener() {
      const enabled = typeof this.useTeamOptionSubEscShortcut === 'function'
        ? this.useTeamOptionSubEscShortcut()
        : typeof this.useProjectOptionSubEscShortcut === 'function'
          ? this.useProjectOptionSubEscShortcut()
          : true
      if (!enabled) return
      if (this.$_teamOptionSubEscBound) return
      this.$_teamOptionSubEscBound = true
      this.$_onTeamOptionSubEsc = (e) => {
        if (this._inactive) return
        if (!isEscapeCloseKey(e)) return
        if (hasBlockingUiLayer()) return
        if (typeof this.shouldTeamOptionSubEscGoBack === 'function' &&
          !this.shouldTeamOptionSubEscGoBack()) {
          return
        }
        if (typeof this.shouldProjectOptionSubEscGoBack === 'function' &&
          !this.shouldProjectOptionSubEscGoBack()) {
          return
        }
        e.preventDefault()
        e.stopPropagation()
        this.goBack()
      }
      document.addEventListener('keydown', this.$_onTeamOptionSubEsc, true)
    },
    $_detachTeamOptionSubEscListener() {
      if (!this.$_teamOptionSubEscBound) return
      this.$_teamOptionSubEscBound = false
      document.removeEventListener('keydown', this.$_onTeamOptionSubEsc, true)
    }
  }
}
