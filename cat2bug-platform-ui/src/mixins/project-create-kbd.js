/**
 * 创建项目页：B 返回、⌘ 徽标、空格动作面板。
 * 表单子页请改用 project-create-form-kbd（含 Esc 未保存确认与 Cmd+Enter 提交）。
 */
import pageActionHints from '@/mixins/page-action-hints'
import { shortcutStore } from '@/plugins/shortcut/shortcut-store'
import { hasBlockingUiLayer } from '@/plugins/shortcut/service'
import { isEscapeCloseKey } from '@/utils/defect-drawer-shortcuts'

export const PROJECT_CREATE_KBD_SCOPE = 'project-create'
export const PROJECT_CREATE_HINT_BACK_CLASS = 'project-create-hint-back'

export default {
  mixins: [pageActionHints],
  mounted() {
    this.registerProjectCreateShortcuts()
    this.$_attachProjectCreateEscListener()
  },
  activated() {
    this.registerProjectCreateShortcuts()
    this.$_attachProjectCreateEscListener()
  },
  deactivated() {
    this.$_detachProjectCreateEscListener()
    if (this.$shortcut) this.$shortcut.unregisterPage(PROJECT_CREATE_KBD_SCOPE)
  },
  beforeDestroy() {
    this.$_detachProjectCreateEscListener()
    if (this.$shortcut) this.$shortcut.unregisterPage(PROJECT_CREATE_KBD_SCOPE)
  },
  methods: {
    registerProjectCreateShortcuts() {
      if (!this.$shortcut) return
      const extra = typeof this.getProjectCreateRegisterActions === 'function'
        ? this.getProjectCreateRegisterActions()
        : []
      this.$shortcut.registerPage(PROJECT_CREATE_KBD_SCOPE, [
        { key: 'back', defaultLetter: 'B', titleKey: 'keyboard.act.project-create-back', run: () => this.goBack() },
        ...extra
      ])
    },
    getPageActionHintContainer() {
      if (this.$refs.projectCreateMain) return this.$refs.projectCreateMain
      return this.$el
    },
    getProjectCreateBackHints() {
      const L = (key, def) => shortcutStore.getLetter(`action.${PROJECT_CREATE_KBD_SCOPE}.${key}`, def)
      return [{
        key: 'back',
        letter: L('back', 'B'),
        badgeSelector: `.${PROJECT_CREATE_HINT_BACK_CLASS} .el-page-header__left`,
        floatOffset: { placement: 'bottom-right-outset', outset: 2 },
        run: () => this.goBack()
      }]
    },
    getPageActionHints() {
      const extra = typeof this.getProjectCreateActionHints === 'function'
        ? this.getProjectCreateActionHints()
        : []
      return [
        ...this.getProjectCreateBackHints(),
        ...extra
      ]
    },
    goBack() {
      this.$router.back()
    },
    useProjectCreateEscShortcut() {
      return true
    },
    $_attachProjectCreateEscListener() {
      if (typeof this.useProjectCreateEscShortcut === 'function' &&
        !this.useProjectCreateEscShortcut()) {
        return
      }
      if (this.$_projectCreateEscBound) return
      this.$_projectCreateEscBound = true
      this.$_onProjectCreateEsc = (e) => {
        if (this._inactive) return
        if (!isEscapeCloseKey(e)) return
        if (hasBlockingUiLayer()) return
        if (typeof this.shouldProjectCreateEscGoBack === 'function' &&
          !this.shouldProjectCreateEscGoBack()) {
          return
        }
        e.preventDefault()
        e.stopPropagation()
        this.goBack()
      }
      document.addEventListener('keydown', this.$_onProjectCreateEsc, true)
    },
    $_detachProjectCreateEscListener() {
      if (!this.$_projectCreateEscBound) return
      this.$_projectCreateEscBound = false
      document.removeEventListener('keydown', this.$_onProjectCreateEsc, true)
    }
  }
}
