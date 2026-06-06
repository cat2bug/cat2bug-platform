<template>
  <div ref="statisticTemplateRoot" class="app-container statistic-template-page">
    <el-row class="project-add-page-header">
      <el-page-header @back="goBack" :content="$t('defect.statistic-template')" />
    </el-row>
    <h4 class="first-title">{{ $t('defect.statistic.view') }}</h4>
    <div
      class="view-panel statistic-template-preview-panel"
      :class="{ 'statistic-template-kbd-nav': tplKbdZone === 'preview' }"
    >
      <cat2-bug-statistic
        ref="previewStatistic"
        :read="true"
        v-model="selectTemplate"
        :draggable="true"
      />
      <span class="statistic-template-hint-preview" aria-hidden="true" />
    </div>
    <h4>{{ $t('defect.statistic.select') }}</h4>
    <div class="templates-groups">
      <div class="templates-group">
        <h5 class="templates-group-title">{{ $t('defect.statistic.personal-template') }}</h5>
        <div
          class="templates-panel statistic-template-personal-panel"
          :class="{ 'statistic-template-kbd-nav': tplKbdZone === 'personal' }"
        >
          <cat2-bug-statistic
            ref="personalStatistic"
            show-type="all"
            template-group="personal"
            :read="true"
            :wrap="true"
            :statistic-tools="[]"
            @click-template-node="clickAddTemplate"
          />
          <span class="statistic-template-hint-personal" aria-hidden="true" />
        </div>
      </div>
      <div class="templates-group">
        <h5 class="templates-group-title">{{ $t('defect.statistic.team-template') }}</h5>
        <div
          class="templates-panel statistic-template-team-panel"
          :class="{ 'statistic-template-kbd-nav': tplKbdZone === 'team' }"
        >
          <cat2-bug-statistic
            ref="teamStatistic"
            show-type="all"
            template-group="team"
            :read="true"
            :wrap="true"
            :statistic-tools="[]"
            @click-template-node="clickAddTemplate"
          />
          <span class="statistic-template-hint-team" aria-hidden="true" />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import Cat2BugStatistic from '@/components/Cat2BugStatistic'
import { addStatistic, listStatistic } from '@/api/system/statistic/template'
import { resolveCurrentProjectId } from '@/components/Cat2BugStatistic/utils/project-id'
import pageActionHints from '@/mixins/page-action-hints'
import { hasBlockingUiLayer } from '@/plugins/shortcut/service'
import { shortcutStore } from '@/plugins/shortcut/shortcut-store'
import {
  findClosestIndexByX,
  getElementCenterX,
  isOnFirstRow,
  isOnLastRow,
  moveStatisticGridIndex
} from '@/utils/statistic-grid-kbd'

const TPL_KBD_SCOPE = 'statistic-template'
const TPL_KBD_ZONE_ORDER = ['preview', 'personal', 'team']

// 统计模版
export default {
  name: 'StatisticTemplate',
  components: { Cat2BugStatistic },
  mixins: [pageActionHints],
  data() {
    return {
      selectTemplate: [],
      /** 键盘导航当前区域：preview | personal | team */
      tplKbdZone: null,
      tplKbdIndex: -1,
      tplKbdSuppressBlur: false
    }
  },
  computed: {
    userId() {
      return this.$store.state.user.id
    },
    projectId() {
      return resolveCurrentProjectId(this)
    }
  },
  created() {
    this.getTemplate()
  },
  mounted() {
    this.registerStatisticTemplateShortcuts()
    this.$_attachTemplatePageEscListener()
  },
  activated() {
    this.registerStatisticTemplateShortcuts()
    this.$_attachTemplatePageEscListener()
  },
  deactivated() {
    this.exitTemplateKbdNav()
    this.$_detachTemplatePageEscListener()
    if (this.$shortcut) this.$shortcut.unregisterPage(TPL_KBD_SCOPE)
  },
  beforeDestroy() {
    this.exitTemplateKbdNav()
    this.$_detachTemplatePageEscListener()
    if (this.$shortcut) this.$shortcut.unregisterPage(TPL_KBD_SCOPE)
  },
  methods: {
    registerStatisticTemplateShortcuts() {
      if (!this.$shortcut) return
      this.$shortcut.registerPage(TPL_KBD_SCOPE, [
        { key: 'preview', defaultLetter: 'P', run: () => this.enterTemplateKbdZone('preview') },
        { key: 'personal', defaultLetter: 'G', run: () => this.enterTemplateKbdZone('personal') },
        { key: 'team', defaultLetter: 'H', run: () => this.enterTemplateKbdZone('team') }
      ])
    },
    getPageActionHintContainer() {
      return this.$refs.statisticTemplateRoot || this.$el
    },
    getPageActionHints() {
      const L = (key, def) => shortcutStore.getLetter(`action.${TPL_KBD_SCOPE}.${key}`, def)
      return [
        {
          key: 'preview',
          letter: L('preview', 'P'),
          badgeSelector: '.statistic-template-preview-panel .statistic-template-hint-preview',
          floatOffset: { placement: 'bottom-right-outset', outset: 2 },
          run: () => this.enterTemplateKbdZone('preview')
        },
        {
          key: 'personal',
          letter: L('personal', 'G'),
          badgeSelector: '.statistic-template-personal-panel .statistic-template-hint-personal',
          floatOffset: { placement: 'bottom-right-outset', outset: 2 },
          run: () => this.enterTemplateKbdZone('personal')
        },
        {
          key: 'team',
          letter: L('team', 'H'),
          badgeSelector: '.statistic-template-team-panel .statistic-template-hint-team',
          floatOffset: { placement: 'bottom-right-outset', outset: 2 },
          run: () => this.enterTemplateKbdZone('team')
        }
      ]
    },
    getTemplate() {
      const params = {
        userId: this.userId,
        projectId: this.projectId,
        moduleType: 1
      }
      listStatistic(params).then(res => {
        if (res.rows.length > 0) {
          this.selectTemplate = JSON.parse(res.rows[0].statisticTemplatConfig)
        } else {
          this.selectTemplate = []
        }
      })
    },
    addTemplateToPreview(template) {
      if (this.selectTemplate.filter(t => t.name === template.name).length === 0) {
        this.selectTemplate.push(template)
        const params = {
          userId: this.userId,
          projectId: this.projectId,
          moduleType: 1,
          statisticTemplatConfig: JSON.stringify(this.selectTemplate)
        }
        addStatistic(params).then(() => {
          this.$message.success(this.$i18n.t('defect.statistic-save-success').toString())
        })
      } else {
        this.$message.warning(this.$i18n.t('defect.statistic-exists-warning').toString())
      }
    },
    clickAddTemplate(e, template) {
      this.addTemplateToPreview(template)
      e.stopPropagation()
    },
    goBack() {
      this.exitTemplateKbdNav()
      this.$router.back()
    },
    $_attachTemplatePageEscListener() {
      if (this.$_templatePageEscBound) return
      this.$_templatePageEscBound = true
      this.$_onTemplatePageEscKeydown = (e) => {
        if (e.key !== 'Escape' && e.key !== 'Esc') return
        if (e.isComposing) return
        if (e.metaKey || e.ctrlKey || e.altKey) return
        if (this.tplKbdZone) return
        if (hasBlockingUiLayer()) return
        const el = document.activeElement
        const tag = el && el.tagName
        if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || (el && el.isContentEditable)) return
        e.preventDefault()
        e.stopPropagation()
        this.goBack()
      }
      document.addEventListener('keydown', this.$_onTemplatePageEscKeydown, true)
    },
    $_detachTemplatePageEscListener() {
      if (!this.$_templatePageEscBound) return
      this.$_templatePageEscBound = false
      document.removeEventListener('keydown', this.$_onTemplatePageEscKeydown, true)
      this.$_onTemplatePageEscKeydown = null
    },
    getTemplateKbdStatisticRef(zone) {
      if (zone === 'preview') return this.$refs.previewStatistic
      if (zone === 'personal') return this.$refs.personalStatistic
      if (zone === 'team') return this.$refs.teamStatistic
      return null
    },
    getTemplateKbdPanelEl(zone) {
      const map = {
        preview: '.statistic-template-preview-panel',
        personal: '.statistic-template-personal-panel',
        team: '.statistic-template-team-panel'
      }
      const sel = map[zone]
      return sel ? this.$el.querySelector(sel) : null
    },
    getTemplateKbdItemEls(zone) {
      const stat = this.getTemplateKbdStatisticRef(zone)
      const viewport = stat && stat.$refs && stat.$refs.viewport
      if (!viewport) return []
      return Array.from(viewport.querySelectorAll('.statistic-item'))
    },
    getTemplateKbdItems(zone) {
      const stat = this.getTemplateKbdStatisticRef(zone)
      if (!stat || !stat.list) return []
      return Array.isArray(stat.list) ? stat.list : []
    },
    clearTemplateKbdFocusMarks() {
      this.$el.querySelectorAll('.statistic-item.statistic-template-kbd-focused').forEach((el) => {
        el.classList.remove('statistic-template-kbd-focused')
        el.tabIndex = -1
      })
    },
    applyTemplateKbdFocus(index) {
      this.tplKbdSuppressBlur = true
      this.clearTemplateKbdFocusMarks()
      const els = this.getTemplateKbdItemEls(this.tplKbdZone)
      const el = els[index]
      if (el) {
        el.classList.add('statistic-template-kbd-focused')
        el.tabIndex = 0
        if (typeof el.scrollIntoView === 'function') {
          el.scrollIntoView({ block: 'nearest', inline: 'nearest' })
        }
        el.focus()
      }
      this.tplKbdIndex = index
      this.$nextTick(() => {
        this.tplKbdSuppressBlur = false
      })
    },
    enterTemplateKbdZone(zone) {
      this.exitTemplateKbdNav()
      const items = this.getTemplateKbdItems(zone)
      if (!items.length) return
      this.tplKbdZone = zone
      this.tplKbdIndex = 0
      this.$nextTick(() => {
        this.applyTemplateKbdFocus(0)
        this.$nextTick(() => {
          this.$_attachTemplateKbdListeners()
        })
      })
    },
    exitTemplateKbdNav() {
      if (!this.tplKbdZone && !this.$_templateKbdListenersBound) return
      this.tplKbdZone = null
      this.tplKbdIndex = -1
      this.clearTemplateKbdFocusMarks()
      this.$_detachTemplateKbdListeners()
    },
    /** 预览区 ↔ 个人模版 ↔ 团队模版：边界行按水平位置对齐后跨区传递焦点 */
    crossTemplateKbdZone(direction) {
      const zone = this.tplKbdZone
      const zi = TPL_KBD_ZONE_ORDER.indexOf(zone)
      if (zi < 0) return false
      const els = this.getTemplateKbdItemEls(zone)
      const cur = els[this.tplKbdIndex]
      if (!cur) return false
      const curCx = getElementCenterX(cur)

      let targetZone = null
      let rowMode = null

      if (direction === 'up') {
        if (zi <= 0) return false
        if (zone !== 'preview' && !isOnFirstRow(els, this.tplKbdIndex)) return false
        targetZone = TPL_KBD_ZONE_ORDER[zi - 1]
        rowMode = targetZone === 'preview' ? null : 'last'
      } else if (direction === 'down') {
        if (zi >= TPL_KBD_ZONE_ORDER.length - 1) return false
        if (zone !== 'preview' && !isOnLastRow(els, this.tplKbdIndex)) return false
        targetZone = TPL_KBD_ZONE_ORDER[zi + 1]
        rowMode = 'first'
      } else {
        return false
      }

      const targetEls = this.getTemplateKbdItemEls(targetZone)
      if (!targetEls.length) return false
      let targetIdx = findClosestIndexByX(targetEls, curCx, rowMode)
      if (targetIdx < 0) targetIdx = 0

      this.tplKbdZone = targetZone
      this.applyTemplateKbdFocus(targetIdx)
      return true
    },
    moveTemplateKbd(direction) {
      const els = this.getTemplateKbdItemEls(this.tplKbdZone)
      if (!els.length || this.tplKbdIndex < 0) return

      if (this.tplKbdZone === 'preview') {
        if (direction === 'left') {
          const next = Math.max(0, this.tplKbdIndex - 1)
          if (next !== this.tplKbdIndex) this.applyTemplateKbdFocus(next)
          return
        }
        if (direction === 'right') {
          const next = Math.min(els.length - 1, this.tplKbdIndex + 1)
          if (next !== this.tplKbdIndex) this.applyTemplateKbdFocus(next)
          return
        }
        if (direction === 'down') {
          this.crossTemplateKbdZone('down')
          return
        }
        return
      }

      const next = moveStatisticGridIndex(els, this.tplKbdIndex, direction)
      if (next !== this.tplKbdIndex) {
        this.applyTemplateKbdFocus(next)
        return
      }
      if (direction === 'up' || direction === 'down') {
        this.crossTemplateKbdZone(direction)
      }
    },
    confirmTemplateKbd() {
      if (!this.tplKbdZone || this.tplKbdIndex < 0) return
      if (this.tplKbdZone === 'preview') return
      const items = this.getTemplateKbdItems(this.tplKbdZone)
      const template = items[this.tplKbdIndex]
      if (template) {
        this.addTemplateToPreview(template)
      }
    },
    removeTemplateKbdPreview() {
      if (this.tplKbdZone !== 'preview' || this.tplKbdIndex < 0) return
      const items = this.getTemplateKbdItems('preview')
      const sc = items[this.tplKbdIndex]
      const stat = this.$refs.previewStatistic
      if (!sc || !stat || typeof stat.removeStatistic !== 'function') return
      const removeIndex = this.tplKbdIndex
      stat.removeStatistic(sc)
      this.$nextTick(() => {
        const nextItems = this.getTemplateKbdItems('preview')
        if (!nextItems.length) {
          this.exitTemplateKbdNav()
          return
        }
        const idx = Math.min(removeIndex, nextItems.length - 1)
        this.applyTemplateKbdFocus(idx)
      })
    },
    $_attachTemplateKbdListeners() {
      if (this.$_templateKbdListenersBound) return
      this.$_templateKbdListenersBound = true
      this.$_onTemplateKbdKeydown = (e) => {
        if (!this.tplKbdZone) return
        const key = e.key
        if (key === 'ArrowLeft' || key === 'Left') {
          e.preventDefault()
          e.stopPropagation()
          this.moveTemplateKbd('left')
          return
        }
        if (key === 'ArrowRight' || key === 'Right') {
          e.preventDefault()
          e.stopPropagation()
          this.moveTemplateKbd('right')
          return
        }
        if (key === 'ArrowUp' || key === 'Up') {
          e.preventDefault()
          e.stopPropagation()
          this.moveTemplateKbd('up')
          return
        }
        if (key === 'ArrowDown' || key === 'Down') {
          e.preventDefault()
          e.stopPropagation()
          this.moveTemplateKbd('down')
          return
        }
        if (key === 'Enter' || key === ' ' || key === 'Spacebar' || e.code === 'Space') {
          e.preventDefault()
          e.stopPropagation()
          this.confirmTemplateKbd()
          return
        }
        if (key === 'Delete' || key === 'Backspace') {
          if (this.tplKbdZone !== 'preview') return
          e.preventDefault()
          e.stopPropagation()
          this.removeTemplateKbdPreview()
          return
        }
        if (key === 'Escape' || key === 'Esc') {
          e.preventDefault()
          e.stopPropagation()
          this.exitTemplateKbdNav()
        }
      }
      this.$_onTemplateKbdFocusOut = (e) => {
        if (!this.tplKbdZone || this.tplKbdSuppressBlur) return
        const roots = TPL_KBD_ZONE_ORDER.map((z) => this.getTemplateKbdPanelEl(z)).filter(Boolean)
        if (e.relatedTarget && roots.some((r) => r.contains(e.relatedTarget))) return
        setTimeout(() => {
          if (!this.tplKbdZone || this.tplKbdSuppressBlur) return
          const active = document.activeElement
          if (!roots.some((r) => r.contains(active))) {
            this.exitTemplateKbdNav()
          }
        }, 0)
      }
      document.addEventListener('keydown', this.$_onTemplateKbdKeydown, true)
      TPL_KBD_ZONE_ORDER.forEach((zone) => {
        const root = this.getTemplateKbdPanelEl(zone)
        if (root) {
          root.addEventListener('focusout', this.$_onTemplateKbdFocusOut, true)
        }
      })
    },
    $_detachTemplateKbdListeners() {
      if (!this.$_templateKbdListenersBound) return
      this.$_templateKbdListenersBound = false
      document.removeEventListener('keydown', this.$_onTemplateKbdKeydown, true)
      const zones = ['preview', 'personal', 'team']
      zones.forEach((zone) => {
        const root = this.getTemplateKbdPanelEl(zone)
        if (root && this.$_onTemplateKbdFocusOut) {
          root.removeEventListener('focusout', this.$_onTemplateKbdFocusOut, true)
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
  .first-title {
    margin-top: 10px;
    margin-bottom: 10px;
  }
  h4:not(.first-title) {
    margin-top: 12px;
    margin-bottom: 10px;
  }
  .app-container {
    width: 100%;
    display: flex;
    flex-direction: column;
    position: absolute;
    top: 0px;
    bottom: 0px;
  }
  .view-panel, .templates-panel {
    position: relative;
    overflow: visible;
    width: 100%;
    background-color: #F2F6FC;
    border-radius: 5px;
    padding: 10px;
    box-sizing: border-box;
    ::v-deep .statistic-box:hover {
      cursor: pointer;
    }
  }
  /* 与缺陷页 .defect-list-hint-stat-nav 一致：区块右下角锚点，供 ⌘ 浮层徽标定位 */
  .statistic-template-hint-preview,
  .statistic-template-hint-personal,
  .statistic-template-hint-team {
    position: absolute;
    right: -2px;
    bottom: -4px;
    width: 16px;
    height: 16px;
    pointer-events: none;
    z-index: 5;
  }
  .view-panel {
    min-height: calc(var(--statistic-card-height, 115px) + 20px);
    display: flex;
    flex-direction: column;

    ::v-deep .statistic-panel {
      flex: 0 0 auto;
    }
  }
  .templates-groups {
    flex: 1 1 auto;
    min-height: 0;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .templates-group-title {
    margin: 0 0 8px;
    font-size: 14px;
    font-weight: 600;
    color: var(--text-color-regular, #606266);
  }
  .statistic-template-kbd-nav ::v-deep .statistic-item.statistic-template-kbd-focused {
    position: relative;
    z-index: 2;
    outline: none;
    &::after {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      top: 0;
      bottom: 0;
      box-sizing: border-box;
      border: var(--cat2bug-field-focus-ring-width, 2px) solid var(--cat2bug-field-focus-color, #409eff);
      border-radius: var(--cat2bug-border-radius, 4px);
      pointer-events: none;
    }
  }
</style>
