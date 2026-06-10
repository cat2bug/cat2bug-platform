/**
 * 通知设置弹框键盘集成：Tab 切换、Cmd/Ctrl+Enter 保存、Esc 关闭、Cmd/Ctrl 字段字母聚焦。
 */
import dialogFormShortcuts from '@/mixins/dialog-form-shortcuts'
import noticeOptionDialogClose from '@/mixins/notice-option-dialog-close'
import formFieldHints from '@/mixins/form-field-hints'
import { getLogicalTabStopsInScope } from '@/utils/focus-tab-common'

export default {
  mixins: [dialogFormShortcuts, noticeOptionDialogClose, formFieldHints],
  watch: {
    activeTabName() {
      this.$_refreshNoticeOptionDialogHints()
    },
    activePlatformName() {
      this.$_refreshNoticeOptionDialogHints()
    }
  },
  methods: {
    onToolDialogOpened() {
      this.$nextTick(() => {
        requestAnimationFrame(() => {
          this.captureToolDialogCloseBaseline()
          this.$_focusToolDialogFirstField()
        })
      })
    },
    $_refreshNoticeOptionDialogHints() {
      if (!this.$_modifierHeld && !this.fieldHintsActive) return
      this.$_fieldHintMap = null
      this.$nextTick(() => {
        this.$_refreshFieldHintsForViewport(this.fieldHintsActive)
      })
    },
    $_focusToolDialogFirstField() {
      const container = typeof this.getFieldHintContainer === 'function'
        ? this.getFieldHintContainer()
        : this.$el
      if (!container) return
      const first = getLogicalTabStopsInScope(container)[0]
      if (!first) return
      if (typeof this.$_focusControl === 'function') {
        this.$_focusControl(first)
      } else if (typeof first.focus === 'function') {
        try {
          first.focus({ preventScroll: false })
        } catch (e) {
          first.focus()
        }
      }
    },
    shortcutSave() {
      if (typeof this.handleSaveOption === 'function') this.handleSaveOption()
    },
    getFieldHintContainer() {
      const body = this.$el && this.$el.querySelector('.el-dialog__body')
      return body || this.$el
    },
    getFieldHintScrollContainer() {
      const body = this.$el && this.$el.querySelector('.el-dialog__body')
      return body || this.getFieldHintContainer()
    },
    getFieldHintAssignMode() {
      return 'checkbox-first'
    },
    getNoticeOptionMainTabs() {
      return [
        { name: 'type', letter: 'G' },
        { name: 'platform', letter: 'P' }
      ]
    },
    getNoticeOptionPlatformTabs() {
      const tabs = [
        { name: 'asystem', letter: '1' },
        { name: 'bmail', letter: '2' }
      ]
      let idx = 3
      ;(this.externalPlatformList || []).forEach((p) => {
        tabs.push({ name: p.name, letter: String(idx++) })
      })
      return tabs
    },
    getFixedFieldHints() {
      const hints = []
      this.getNoticeOptionMainTabs().forEach((tab) => {
        hints.push({
          letter: tab.letter,
          badgeSelector: `.notice-option-main-tab-${tab.name}`,
          onActivate: () => {
            this.activeTabName = tab.name
          }
        })
      })
      if (this.activeTabName === 'platform') {
        this.getNoticeOptionPlatformTabs().forEach((tab) => {
          hints.push({
            letter: tab.letter,
            badgeSelector: `.notice-option-platform-tab-${tab.name}`,
            onActivate: () => {
              this.activePlatformName = tab.name
            }
          })
        })
        if (this.activePlatformName === 'asystem') {
          hints.push({
            letter: 'D',
            badgeSelector: '.notice-option-asystem-switch',
            onActivate: () => this.$_toggleAsystemSwitch()
          })
          hints.push({
            letter: 'E',
            badgeSelector: '.notice-option-asystem-bgm-cb',
            onActivate: () => this.$_toggleAsystemField('backgroundMusic')
          })
          hints.push({
            letter: 'F',
            badgeSelector: '.notice-option-asystem-bgm-select',
            onActivate: () => this.$_focusAsystemBgmSelect()
          })
          hints.push({
            letter: 'S',
            badgeSelector: '.notice-option-asystem-panel-cb',
            onActivate: () => this.$_toggleAsystemField('panel')
          })
        }
      }
      return hints
    },
    $_toggleAsystemSwitch() {
      const com = this.$refs['platform-asystem']
      if (!com || !com.form) return
      com.form.switch = !com.form.switch
      if (typeof com.handleSwitchChange === 'function') com.handleSwitchChange()
    },
    $_toggleAsystemField(field) {
      const com = this.$refs['platform-asystem']
      if (!com || !com.form || !com.form.switch) return
      com.form[field] = !com.form[field]
      if (typeof com.handleChange === 'function') com.handleChange()
    },
    $_focusAsystemBgmSelect() {
      const com = this.$refs['platform-asystem']
      if (!com || !com.form || !com.form.switch || !com.form.backgroundMusic) return
      const container = typeof this.getFieldHintContainer === 'function'
        ? this.getFieldHintContainer()
        : this.$el
      const input = container && container.querySelector('.notice-option-asystem-bgm-select input')
      if (input && !input.disabled && typeof this.$_focusControl === 'function') {
        this.$_focusControl(input)
      }
    }
  }
}
