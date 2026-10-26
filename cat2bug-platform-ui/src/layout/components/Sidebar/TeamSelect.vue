<template>
  <div class="team-select-root layout-nav-hint-anchor" :class="{ 'team-select-root--collapse': collapse, 'team-select-root--readonly': !teamMember, 'team-select-root--dark': dark }" data-layout-nav="team">
    <div v-if="!teamMember" class="team-select-readonly">
      <el-avatar class="team-select-avatar" :size="avatarSize" shape="square" v-if="displayTeam && displayTeam.teamIcon" :src="teamAvatarSrc(displayTeam)" fit="cover" @error="onTeamAvatarError(displayTeam)"></el-avatar>
      <el-avatar class="team-select-avatar" :size="avatarSize" shape="square" v-else-if="displayTeam">{{ displayTeam.teamName }}</el-avatar>
      <el-avatar class="team-select-avatar" :size="avatarSize" shape="square" v-else :src="imgUrl('add.svg')" fit="cover"></el-avatar>
      <p class="prefix-team-name" v-if="!collapse">{{ displayTeam ? displayTeam.teamName : '' }}</p>
    </div>
    <el-select
      v-else
      class="team-select"
      :data-collapse="collapse ? 'true' : 'false'"
      v-model="currentTeamId"
      :effect="dark ? 'dark' : 'light'"
      :popper-class="teamSelectPopperClass"
      @change="selectTeamChangedHandle"
    >
      <template #prefix>
        <el-avatar class="team-select-avatar" :size="avatarSize" shape="square" v-if="!currentTeam" :src="imgUrl('add.svg')" fit="cover"></el-avatar>
        <el-avatar class="team-select-avatar" :size="avatarSize" shape="square" v-else-if="currentTeam.teamIcon" :src="teamAvatarSrc(currentTeam)" fit="cover" @error="onTeamAvatarError(currentTeam)"></el-avatar>
        <el-avatar class="team-select-avatar" :size="avatarSize" shape="square" v-else>{{ currentTeam.teamName }}</el-avatar>
        <p class="prefix-team-name" v-if="!collapse">{{ currentTeam ? currentTeam.teamName : $t('team.create') }}</p>
      </template>
      <el-option
        style="height:80px;"
        v-for="team in teamList"
        :key="team.teamId"
        :label="team.teamName"
        :value="team.teamId">
        <el-avatar class="team-select-avatar" shape="square" v-if="team.teamIcon" :src="teamAvatarSrc(team)" fit="cover" @error="onTeamAvatarError(team)"></el-avatar>
        <el-avatar class="team-select-avatar" shape="square" v-else>{{team.teamName}}</el-avatar>
        <div class="team-select-option-text">
          <p class="team-select-option-name">{{ team.teamName }}</p>
          <span
            class="team-select-option-intro"
            :class="{ 'is-empty': !team.introduce }"
            :title="team.introduce || $t('team.introduction-not-filled')"
          >{{ team.introduce || $t('team.introduction-not-filled') }}</span>
        </div>
      </el-option>
      <el-option class="team-select-footer"
                 :line="teamList.length>0?'true':'false'"
                 key="team-select-footer-create-team"
                 value="team-select-footer-create-team"
      >
        <el-button class="team-select-footer-btn" plain @click="addTeamHandle">
          <el-icon><Plus /></el-icon>
          {{ $t('team.create') }}
        </el-button>
      </el-option>
    </el-select>
  </div>
</template>

<script>
import { storeToRefs } from 'pinia'
import addSvgUrl from '@/assets/images/add.svg?url'
import { Plus } from '@element-plus/icons-vue'
import { myListTeam } from '@/api/system/team'
import { updateConfig } from "@/api/system/user-config";
import { useUserStore } from '@/stores/user'
import { resolveTeamIconUrl } from '@/utils/upload-asset'

export default {
  name: "TeamSelect",
  components: { Plus },
  setup() {
    const userStore = useUserStore()
    const { config, teamMember, contextTeam } = storeToRefs(userStore)
    return {
      userStore,
      userConfig: config,
      userTeamMember: teamMember,
      userContextTeam: contextTeam
    }
  },
  data() {
    return {
      teamList:[],
      currentTeamId:null,
      failedTeamIcons: {}
    }
  },
  props:{
    collapse: {
      type: Boolean,
      default: false
    },
    dark: {
      type: Boolean,
      default: false
    }
  },
  watch: {
    teamId: {
      immediate: true,
      handler(val) {
        this.currentTeamId = val
        if (this.teamMember) {
          this.getTeamList()
        }
      }
    },
    userContextTeam: function() {
      if (!this.teamMember) {
        this.syncReadonlyTeam()
      }
    }
  },
  computed:{
    teamMember() {
      return this.userTeamMember !== false
    },
    /** 折叠/展开均略留白，比上一档再小一点 */
    avatarSize() {
      return this.collapse ? 32 : 36
    },
    teamId: function (){
      return this.userConfig && this.userConfig.currentTeamId
    },
    displayTeam() {
      if (this.teamMember) {
        return this.currentTeam
      }
      return this.userContextTeam
    },
    currentTeam: function() {
      for(let i in this.teamList){
        if(this.teamList[i].teamId==this.teamId) {
          return this.teamList[i];
        }
      }
      return null;
    },
    imgUrl() {
      return () => addSvgUrl
    },
    teamSelectPopperClass() {
      return this.dark
        ? 'team-select-kbd-popper team-select-kbd-popper--dark'
        : 'team-select-kbd-popper'
    }
  },
  mounted() {
    if (this.teamMember) {
      this.getTeamList();
    } else {
      this.syncReadonlyTeam();
    }
  },
  methods:{
    syncReadonlyTeam() {
      this.currentTeamId = this.teamId;
    },
    teamAvatarSrc(team) {
      if (!team || !team.teamIcon) {
        return ''
      }
      if (this.failedTeamIcons[team.teamId]) {
        return resolveTeamIconUrl(null)
      }
      return resolveTeamIconUrl(team.teamIcon)
    },
    onTeamAvatarError(team) {
      if (team && team.teamId != null) {
        this.failedTeamIcons[team.teamId] = true
      }
    },
    /** 获取当前人所能访问的团队列表 */
    getTeamList() {
      myListTeam().then(res => {
        this.teamList = res.rows
        if (!this.teamId && this.teamList.length > 0) {
          this.selectTeam(this.teamList[0].teamId, true)
        } else {
          this.currentTeamId = this.teamId
        }
      })
    },
    addTeamHandle(e) {
      this.$router.push({name:'TeamAdd'});
    },
    /** 选择团队变化的处理 */
    selectTeamChangedHandle(currentTeamId,isRefresh){
      switch (currentTeamId){
        case 'team-select-footer-create-team':
          break;
        default:
          this.selectTeam(currentTeamId,true);
      }
    },
    /** 选择团队 */
    selectTeam(currentTeamId,isRefresh){
      if(currentTeamId == this.teamId) return;
      let _this=this;
      if(isRefresh) {
        updateConfig({
          currentTeamId: currentTeamId,
          currentProjectId: 0
        }).then(res => {
          this.userStore.GetInfo().then(() => {
            let path = '/team/project-list';
            if (_this.$route.path == path) {
              location.reload();
            } else {
              _this.$router.push({path: path});
            }
          });
        });
      }
    }
  }
}
</script>

<style lang="scss" scoped>
  @mixin team-select-avatar-surface {
    --el-avatar-bg-color: var(--team-select-avatar-bg, #2a2a2c);
    --el-avatar-text-color: var(--team-select-avatar-text, #e5eaf3);
    background-color: var(--team-select-avatar-bg, #2a2a2c) !important;
    color: var(--team-select-avatar-text, #e5eaf3) !important;
    border: 1px solid var(--team-select-avatar-border, #363637);
    border-radius: 8px;
    overflow: hidden;

    > img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      object-position: center;
      background-color: var(--team-select-avatar-bg, #2a2a2c);
    }
  }

  :deep(.team-select-avatar.el-avatar) {
    border-radius: 8px;
    overflow: hidden;

    > img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      object-position: center;
    }
  }

  &.team-select-root--dark {
    :deep(.team-select-avatar.el-avatar) {
      @include team-select-avatar-surface;
    }
  }

  &:not(.team-select-root--dark) {
    .prefix-team-name {
      color: #303133;
    }
  }

  .team-select-readonly {
    display: flex;
    justify-content: flex-start;
    align-items: center;
    column-gap: 8px;
    margin: 8px;
    width: calc(100% - 16px);
    min-height: 56px;
    height: 56px;
    padding: 8px;
    box-sizing: border-box;
    cursor: default;

    .prefix-team-name {
      text-overflow: ellipsis;
      overflow: hidden;
      white-space: nowrap;
      max-width: 112px;
      width: auto;
      font-size: 16px;
      text-align: left;
      margin: 0;
    }

    .el-avatar {
      display: flex;
      justify-content: center;
      align-items: center;
      flex-shrink: 0;
      width: 36px !important;
      height: 36px !important;
      border-radius: 8px;
      overflow: hidden;
    }
  }

  &.team-select-root--dark .team-select-readonly {
    .prefix-team-name {
      color: var(--sidebar-text, #b0b3b8);
    }

    .el-avatar {
      @include team-select-avatar-surface;
    }
  }

  .team-select-root--collapse {
    display: flex;
    justify-content: center;
    padding-top: 4px;
    padding-bottom: 2px;

    :deep(.team-select) {
      width: 40px;
    }

    :deep(.team-select .el-input__suffix),
    :deep(.team-select .el-select__suffix) {
      display: none !important;
    }

    /* 含 is-hovering/is-focused：防止展开态非对称 padding 在 hover 时盖过来，把头像挤向左侧 */
    :deep(.team-select .el-select__wrapper),
    :deep(.team-select .el-select__wrapper.is-hovering),
    :deep(.team-select .el-select__wrapper.is-focused),
    :deep(.team-select .el-input--prefix) {
      height: 40px;
      width: 40px !important;
      min-height: 40px;
      margin: 0 !important;
      padding: 2px !important;
      gap: 0 !important;
      justify-content: center;
      box-shadow: none !important;
      border-radius: 8px;
      background-color: var(--sidebar-tile-bg, #2a2a2c) !important;
    }

    :deep(.team-select-avatar.el-avatar) {
      width: 32px !important;
      height: 32px !important;
    }

    :deep(.team-select .el-input__inner),
    :deep(.team-select .el-select__selection) {
      height: 40px;
      width: 0 !important;
      min-width: 0 !important;
      padding: 0 !important;
      margin: 0 !important;
      flex: 0 0 0 !important;
      overflow: hidden;
    }

    :deep(.team-select .el-input__prefix),
    :deep(.team-select .el-select__prefix) {
      position: static !important;
      left: auto !important;
      right: auto !important;
      width: 100% !important;
      margin: 0 !important;
      padding: 0 !important;
      justify-content: center !important;
    }
  }

  .team-select-root--readonly.team-select-root--collapse .team-select-readonly {
    height: 40px;
    width: 40px;
    margin: 4px auto 2px;
    min-height: 40px;
    padding: 2px;
    box-sizing: border-box;
    justify-content: center;
    border-radius: 8px;
    background-color: var(--sidebar-tile-bg, #2a2a2c);

    .el-avatar {
      width: 32px !important;
      height: 32px !important;
    }
  }

  :deep(.team-select) {
    cursor: pointer;
    width: 100%;

    .el-select__wrapper,
    .el-input--prefix {
      margin: 8px;
      width: calc(100% - 16px);
      border-radius: 8px;
      min-height: 56px;
      height: 56px;
      box-shadow: none !important;
      background-color: transparent;
      box-sizing: border-box;
    }

    /* 仅展开态；折叠态由 .team-select-root--collapse 单独锁 padding，避免 hover 挤偏 */
    .el-select__wrapper,
    .el-select__wrapper.is-focused,
    .el-select__wrapper.is-hovering {
      box-shadow: none !important;
    }

    .el-select__suffix,
    .el-input__suffix {
      flex-shrink: 0;
      margin-left: 2px;
      margin-right: 0;
    }

    .el-select__caret {
      margin: 0;
    }

    .el-input__inner,
    .el-select__selection {
      background-color: transparent;
      border-width: 0;
      height: 36px;
      min-width: 0 !important;
      flex: 0 0 0 !important;
      width: 0 !important;
      overflow: hidden;
      visibility: hidden;
      padding: 0 !important;
    }

    .el-input__prefix,
    .el-select__prefix {
      display: flex;
      justify-content: flex-start;
      align-items: center;
      column-gap: 8px;
      flex: 1 1 auto;
      min-width: 0;
      max-width: none;
    }

    .prefix-team-name {
      text-overflow: ellipsis;
      overflow: hidden;
      white-space: nowrap;
      max-width: 118px;
      width: auto;
      flex: 1 1 auto;
      min-width: 0;
      color: #303133;
      font-size: 16px;
      text-align: left;
      margin: 0;
      /* 与右侧箭头约 1 字符间距 */
      padding-right: 0.6em;
    }

    .team-select-avatar.el-avatar,
    .el-avatar {
      display: flex;
      justify-content: center;
      align-items: center;
      flex-shrink: 0;
      width: 36px !important;
      height: 36px !important;
      border-radius: 8px;
      overflow: hidden;
    }

    .el-select__wrapper:hover,
    .el-input--prefix:hover {
      background-color: rgba(0, 0, 0, 0.04);
    }
  }

  /* 展开态左右内边距（不写在通用 .team-select 里，以免盖掉折叠 hover） */
  &:not(.team-select-root--collapse) :deep(.team-select) {
    .el-select__wrapper,
    .el-select__wrapper.is-focused,
    .el-select__wrapper.is-hovering {
      padding: 8px 16px 8px 8px !important;
      gap: 6px;
    }
  }

  &.team-select-root--dark :deep(.team-select) {
    .prefix-team-name {
      color: var(--sidebar-text, #b0b3b8);
    }

    .el-avatar {
      @include team-select-avatar-surface;
    }

    .el-select__wrapper:hover,
    .el-input--prefix:hover {
      background-color: var(--sidebar-item-hover-bg, rgba(255, 255, 255, 0.06));
    }
  }

  .el-select-dropdown__item:not(.team-select-footer) {
    height: 80px;
    display: flex;
    justify-content: flex-start;
    align-items: center;
    column-gap: 10px;
  }
  .team-select-footer {
    height: auto !important;
    min-height: 64px;
    padding: 10px 12px 12px !important;
    display: flex !important;
    align-items: stretch !important;
    background-color: transparent;
  }
  .team-select-footer[line="true"] {
    border-top: 1px solid var(--team-select-popper-border, #363637);
  }
  :deep(.el-select-dropdown__wrap){
    max-height: 100%;
  }
</style>

<!-- append-to-body 后下拉在 body，样式需挂 popper-class -->
<style lang="scss">
@mixin team-select-footer-btn-base {
  width: 100% !important;
  min-height: 44px;
  height: auto !important;
  padding: 10px 16px !important;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.4;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  box-sizing: border-box;

  .el-icon {
    font-size: 16px;
  }
}

.team-select-kbd-popper:not(.team-select-kbd-popper--dark).el-select__popper.el-popper,
.team-select-kbd-popper:not(.team-select-kbd-popper--dark).el-popper {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12) !important;
}

.team-select-kbd-popper:not(.team-select-kbd-popper--dark) .el-select-dropdown__item {
  &.is-selected {
    background-color: rgba(64, 158, 255, 0.12) !important;
    color: var(--el-color-primary, #1890ff) !important;
    font-weight: 600;
  }

  &.is-selected.is-hovering,
  &.is-selected:hover {
    background-color: #ecf5ff !important;
    color: var(--el-color-primary, #1890ff) !important;
  }
}

.team-select-kbd-popper:not(.team-select-kbd-popper--dark) .team-select-footer .team-select-footer-btn.el-button {
  @include team-select-footer-btn-base;
  border-color: var(--el-color-primary, #1890ff) !important;
  color: var(--el-color-primary, #1890ff) !important;

  &:hover,
  &:focus {
    background-color: #ecf5ff !important;
    border-color: #66b1ff !important;
    color: #66b1ff !important;
  }
}

.team-select-kbd-popper.el-select__popper.el-popper,
.team-select-kbd-popper.el-popper {
  min-width: 180px !important;
  z-index: 10000 !important;
  background: var(--team-select-popper-bg, #252529) !important;
  border: 1px solid var(--team-select-popper-border, #363637) !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.35) !important;
}

.team-select-kbd-popper.el-popper > .el-popper__arrow::before {
  background: var(--team-select-popper-bg, #252529) !important;
  border: 1px solid var(--team-select-popper-border, #363637) !important;
}

.team-select-kbd-popper.el-popper[data-popper-placement^='bottom'] > .el-popper__arrow::before {
  border-bottom-color: transparent !important;
  border-right-color: transparent !important;
}

.team-select-kbd-popper.el-popper[data-popper-placement^='top'] > .el-popper__arrow::before {
  border-top-color: transparent !important;
  border-left-color: transparent !important;
}

.team-select-kbd-popper .el-select-dropdown__item:not(.team-select-footer) {
  height: 80px;
  display: flex;
  justify-content: flex-start;
  align-items: center;
  column-gap: 10px;
  color: var(--team-select-popper-text, #e5eaf3) !important;
  background-color: transparent !important;

  .team-select-option-text {
    display: flex;
    flex-direction: column;
    justify-content: center;
    min-width: 0;
    flex: 1 1 auto;
    gap: 4px;
  }

  .team-select-option-name {
    color: inherit;
    margin: 0;
    font-size: 15px;
    line-height: 1.3;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .team-select-option-intro {
    display: block;
    margin: 0;
    font-size: 12px;
    line-height: 1.3;
    color: var(--text-color-secondary, #909399);
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;

    &.is-empty {
      color: var(--text-color-placeholder, #c0c4cc);
    }
  }

  .team-select-avatar.el-avatar,
  .el-avatar {
    --el-avatar-bg-color: var(--team-select-avatar-bg, #2a2a2c);
    --el-avatar-text-color: var(--team-select-avatar-text, #e5eaf3);
    overflow: hidden;
    flex-shrink: 0;
    background-color: var(--team-select-avatar-bg, #2a2a2c) !important;
    color: var(--team-select-avatar-text, #e5eaf3) !important;
    border: 1px solid var(--team-select-avatar-border, #363637);
    border-radius: 8px;

    > img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      object-position: center;
      background-color: var(--team-select-avatar-bg, #2a2a2c);
    }
  }

  &.is-hovering,
  &:hover {
    background-color: var(--team-select-popper-hover, #2d2d31) !important;
    color: var(--team-select-popper-text, #e5eaf3) !important;
  }

  &.is-selected {
    background-color: rgba(105, 177, 255, 0.16) !important;
    color: var(--sidebar-active-text, #69b1ff) !important;
    font-weight: 600;
  }

  &.is-selected.is-hovering,
  &.is-selected:hover {
    background-color: var(--team-select-popper-hover, #2d2d31) !important;
    color: var(--sidebar-active-text, #69b1ff) !important;
  }
}

.team-select-kbd-popper .team-select-footer {
  height: auto !important;
  min-height: 64px;
  padding: 10px 12px 12px !important;
  display: flex !important;
  align-items: stretch !important;
  background-color: var(--team-select-popper-bg, #252529) !important;
  color: var(--team-select-popper-text, #e5eaf3) !important;

  &.is-hovering,
  &:hover {
    background-color: var(--team-select-popper-bg, #252529) !important;
  }

  .team-select-footer-btn.el-button {
    @include team-select-footer-btn-base;
    background-color: transparent !important;
    border-color: var(--sidebar-active-text, #69b1ff) !important;
    color: var(--sidebar-active-text, #69b1ff) !important;

    &:hover,
    &:focus {
      background-color: rgba(105, 177, 255, 0.12) !important;
      border-color: var(--cat2bug-accent, #69b1ff) !important;
      color: var(--cat2bug-accent, #69b1ff) !important;
    }
  }
}

.team-select-kbd-popper .team-select-footer[line='true'] {
  border-top: 1px solid var(--team-select-popper-border, #363637) !important;
}

.team-select-kbd-popper .el-select-dropdown__wrap {
  max-height: 360px;
}
</style>
