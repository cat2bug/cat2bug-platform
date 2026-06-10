<template>
  <div class="app-container team-option-page" ref="optionMain">
    <el-row :gutter="20">
      <el-col :span="6">
        <team-base-info-card />
      </el-col>
      <el-col :span="6">
        <team-member-card />
      </el-col>
    </el-row>
  </div>
</template>

<script>
import TeamBaseInfoCard from './item/base'
import TeamMemberCard from './item/member'
import pageActionHints from '@/mixins/page-action-hints'
import { shortcutStore } from '@/plugins/shortcut/shortcut-store'
import {
  buildOptionCardActionHints,
  buildOptionCardRegisterActions
} from '@/utils/option-card-kbd-hints'

const TEAM_OPTION_KBD_SCOPE = 'team-option'

export default {
  name: "Team",
  mixins: [pageActionHints],
  components: { TeamBaseInfoCard, TeamMemberCard },
  mounted() {
    this.registerTeamOptionShortcuts();
  },
  activated() {
    this.registerTeamOptionShortcuts();
  },
  deactivated() {
    if (this.$shortcut) this.$shortcut.unregisterPage(TEAM_OPTION_KBD_SCOPE);
  },
  beforeDestroy() {
    if (this.$shortcut) this.$shortcut.unregisterPage(TEAM_OPTION_KBD_SCOPE);
  },
  methods: {
    registerTeamOptionShortcuts() {
      if (!this.$shortcut) return
      const container = this.getPageActionHintContainer()
      const L = (key, def) => shortcutStore.getLetter(`action.${TEAM_OPTION_KBD_SCOPE}.${key}`, def)
      const actions = buildOptionCardRegisterActions({
        container,
        letterForKey: L,
        scopeKey: TEAM_OPTION_KBD_SCOPE
      })
      this.$shortcut.registerPage(TEAM_OPTION_KBD_SCOPE, actions)
    },
    getPageActionHintContainer() {
      return this.$refs.optionMain || this.$el
    },
    getPageActionHints() {
      return []
    },
    getPageDynamicActionHints(ctx) {
      const used = (ctx && ctx.usedLetters) ? new Set(ctx.usedLetters) : new Set()
      const container = this.getPageActionHintContainer()
      const L = (key, def) => shortcutStore.getLetter(`action.${TEAM_OPTION_KBD_SCOPE}.${key}`, def)
      return buildOptionCardActionHints({
        container,
        letterForKey: L,
        usedLetters: used,
        scopeKey: TEAM_OPTION_KBD_SCOPE
      })
    },
  }
};
</script>
<style lang="scss" scoped>
  ::v-deep .el-card__header {
    font-weight: 600;
    div:first-child {
      display: flex;
      flex-direction: row;
      width: calc(100% - 30px);
      div {
        display: flex;
        flex-direction: column;
        width: 100%;
        span {
          margin-bottom: 5px;
        }
        span:first-child, span:last-child {
          text-overflow: ellipsis;
          overflow: hidden;
          white-space: nowrap;
        }
        span:last-child {
          line-height: 12px;
          font-size: 12px;
          color: #C0C4CC;
        }
      }
    }
    i, .svg-icon {
      color: var(--cat2bug-option-card-icon-color, #409EFF);
      fill: currentColor;
      margin-right: 10px;
      margin-top: 3px;
    }
  }
  ::v-deep .el-card__body {
    min-height: 150px;
    display: flex;
    align-items: flex-start;
    flex-direction: column;
    > * {
      margin-left: 20px;
      margin-bottom: 15px;
    }
  }
  ::v-deep .el-card__header {
    position: relative;
    overflow: visible !important;
  }
</style>
