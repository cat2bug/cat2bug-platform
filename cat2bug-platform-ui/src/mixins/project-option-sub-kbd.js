/**
 * 项目设置子页：返回快捷键（B / 空格面板）、⌘ 徽标、Esc 返回上一级。
 * 表单子页请改用 project-option-sub-form-kbd（含 Esc 未保存确认与 Cmd+Enter 保存）。
 */
import pageActionHints from '@/mixins/page-action-hints'
import { shortcutStore } from '@/plugins/shortcut/shortcut-store'
import { hasBlockingUiLayer } from '@/plugins/shortcut/service'
import { isEscapeCloseKey } from '@/utils/defect-drawer-shortcuts'
import {
  buildProjectOptionSubPaginationShortcuts,
  clickProjectOptionSubPagination
} from '@/utils/project-option-sub-list-kbd'

export const PROJECT_OPTION_KBD_SCOPE = 'project-option'
export const PROJECT_OPTION_SUB_HINT_BACK_CLASS = 'project-option-sub-hint-back'

export default {
  mixins: [pageActionHints],
  mounted() {
    this.registerProjectOptionSubShortcuts()
    this.$_attachProjectOptionSubEscListener()
  },
  activated() {
    this.registerProjectOptionSubShortcuts()
    this.$_attachProjectOptionSubEscListener()
  },
  deactivated() {
    this.$_detachProjectOptionSubEscListener()
    if (this.$shortcut) this.$shortcut.unregisterPage(PROJECT_OPTION_KBD_SCOPE)
  },
  beforeDestroy() {
    this.$_detachProjectOptionSubEscListener()
    if (this.$shortcut) this.$shortcut.unregisterPage(PROJECT_OPTION_KBD_SCOPE)
  },
  methods: {
    registerProjectOptionSubShortcuts() {
      if (!this.$shortcut) return
      const extra = typeof this.getProjectOptionSubRegisterActions === 'function'
        ? this.getProjectOptionSubRegisterActions()
        : []
      const pagination = buildProjectOptionSubPaginationShortcuts(this)
      this.$shortcut.registerPage(PROJECT_OPTION_KBD_SCOPE, [
        { key: 'back', defaultLetter: 'B', titleKey: 'keyboard.act.project-option-back', run: () => this.goBack() },
        ...extra,
        ...pagination.register
      ])
    },
    getPageActionHintContainer() {
      if (this.$refs.projectOptionSubMain) return this.$refs.projectOptionSubMain
      return this.$el
    },
    getProjectOptionSubBackHints() {
      const L = (key, def) => shortcutStore.getLetter(`action.${PROJECT_OPTION_KBD_SCOPE}.${key}`, def)
      return [{
        key: 'back',
        letter: L('back', 'B'),
        badgeSelector: `.${PROJECT_OPTION_SUB_HINT_BACK_CLASS} .el-page-header__left`,
        floatOffset: { placement: 'bottom-right-outset', outset: 2 },
        run: () => this.goBack()
      }]
    },
    getPageActionHints() {
      const extra = typeof this.getProjectOptionSubActionHints === 'function'
        ? this.getProjectOptionSubActionHints()
        : []
      const pagination = buildProjectOptionSubPaginationShortcuts(this)
      return [
        ...this.getProjectOptionSubBackHints(),
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
    useProjectOptionSubEscShortcut() {
      return true
    },
    $_attachProjectOptionSubEscListener() {
      if (typeof this.useProjectOptionSubEscShortcut === 'function' &&
        !this.useProjectOptionSubEscShortcut()) {
        return
      }
      if (this.$_projectOptionSubEscBound) return
      this.$_projectOptionSubEscBound = true
      this.$_onProjectOptionSubEsc = (e) => {
        if (this._inactive) return
        if (!isEscapeCloseKey(e)) return
        if (hasBlockingUiLayer()) return
        if (typeof this.shouldProjectOptionSubEscGoBack === 'function' &&
          !this.shouldProjectOptionSubEscGoBack()) {
          return
        }
        e.preventDefault()
        e.stopPropagation()
        this.goBack()
      }
      document.addEventListener('keydown', this.$_onProjectOptionSubEsc, true)
    },
    $_detachProjectOptionSubEscListener() {
      if (!this.$_projectOptionSubEscBound) return
      this.$_projectOptionSubEscBound = false
      document.removeEventListener('keydown', this.$_onProjectOptionSubEsc, true)
    }
  }
}
