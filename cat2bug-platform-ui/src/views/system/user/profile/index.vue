<template>
  <div class="app-container profile-page" ref="profileMain">
    <el-row class="profile-hint-back">
      <el-page-header @back="goBack" :content="$t('personal-center')">
      </el-page-header>
    </el-row>
    <el-row :gutter="20">
      <el-col :span="6" :xs="24">
        <el-card class="box-card">
          <template #header><div class="clearfix">
            <span>{{ $t('member.personal-info') }}</span>
          </div></template>
          <div>
            <div class="text-center">
              <userAvatar :profile-user="user" @updated="onAvatarUpdated" />
            </div>
            <ul class="list-group list-group-striped">
              <li class="list-group-item">
                <svg-icon icon-class="user" />
                {{ $t('account') }}
                <div class="pull-right">{{ user.userName }}</div>
              </li>
              <li class="list-group-item">
                <svg-icon icon-class="phone" />{{ $t('phone-number') }}
                <div class="pull-right contact-display" :class="{ 'is-empty': !hasContact(user.phoneNumber) }">{{ formatContact(user.phoneNumber) }}</div>
              </li>
              <li class="list-group-item">
                <svg-icon icon-class="email" />{{ $t('email') }}
                <div class="pull-right contact-display" :class="{ 'is-empty': !hasContact(user.email) }">{{ formatContact(user.email) }}</div>
              </li>
              <li class="list-group-item">
                <svg-icon icon-class="date" />{{ $t('create-time') }}
                <div class="pull-right">{{ user.createTime }}</div>
              </li>
            </ul>
          </div>
        </el-card>
      </el-col>
      <el-col :span="18" :xs="24">
        <el-card class="profile-basic-card">
          <template #header><div class="profile-basic-card__header">
            <el-tabs v-model="activeTab" class="profile-hint-tabs">
              <el-tab-pane name="userinfo">
                <template #label><span class="profile-tab-label">{{ $t('member.basic-info') }}</span></template>
              </el-tab-pane>
              <el-tab-pane name="resetPwd">
                <template #label><span class="profile-tab-label">{{ $t('modify-password') }}</span></template>
              </el-tab-pane>
            </el-tabs>
          </div></template>
          <userInfo v-show="activeTab === 'userinfo'" :user="user" :form-active="activeTab === 'userinfo'" />
          <resetPwd v-show="activeTab === 'resetPwd'" :form-active="activeTab === 'resetPwd'" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import userAvatar from "./userAvatar";
import userInfo from "./userInfo";
import resetPwd from "./resetPwd";
import { getUserProfile } from "@/api/system/user";
import { formatContactDisplay } from "@/utils/user-contact-rules";
import pageActionHints from '@/mixins/page-action-hints'
import { shortcutStore } from '@/plugins/shortcut/shortcut-store'
import { hasBlockingUiLayer } from '@/plugins/shortcut/service'

const PROFILE_KBD_SCOPE = 'profile'
const PROFILE_TABS = ['userinfo', 'resetPwd']

export default {
  name: "Profile",
  mixins: [pageActionHints],
  components: { userAvatar, userInfo, resetPwd },
  data() {
    return {
      user: {},
      roleGroup: {},
      postGroup: {},
      activeTab: "userinfo"
    };
  },
  created() {
    const tab = this.$route.query && this.$route.query.tab
    if (PROFILE_TABS.includes(tab)) this.activeTab = tab
    this.getUser();
  },
  mounted() {
    this.registerProfileShortcuts();
  },
  activated() {
    this.registerProfileShortcuts();
  },
  deactivated() {
    if (this.$shortcut) this.$shortcut.unregisterPage(PROFILE_KBD_SCOPE);
  },
  beforeUnmount() {
    if (this.$shortcut) this.$shortcut.unregisterPage(PROFILE_KBD_SCOPE);
  },
  methods: {
    registerProfileShortcuts() {
      if (!this.$shortcut) return
      this.$shortcut.registerPage(PROFILE_KBD_SCOPE, [
        { key: 'switchTab', defaultLetter: 'J', run: () => this.shortcutSwitchTab() },
        { key: 'back', defaultLetter: 'B', run: () => this.goBack() }
      ])
    },
    getPageActionHintContainer() {
      return this.$refs.profileMain || this.$el
    },
    getPageActionHints() {
      const L = (key, def) => shortcutStore.getLetter(`action.${PROFILE_KBD_SCOPE}.${key}`, def)
      return [
        {
          key: 'switchTab',
          letter: L('switchTab', 'J'),
          badgeSelector: '.profile-hint-tabs .el-tabs__item.is-active .profile-tab-label',
          floatOffset: { placement: 'bottom-right-outset', outset: 2 },
          run: () => this.shortcutSwitchTab()
        },
        {
          key: 'back',
          letter: L('back', 'B'),
          badgeSelector: '.profile-hint-back .el-page-header__left',
          floatOffset: { placement: 'bottom-right-outset', outset: 2 },
          run: () => this.goBack()
        }
      ]
    },
    shortcutSwitchTab() {
      const idx = PROFILE_TABS.indexOf(this.activeTab)
      const next = PROFILE_TABS[(idx < 0 ? 0 : idx + 1) % PROFILE_TABS.length]
      this.activeTab = next
    },
    /** 子表单激活时仍显示 Tab 切换徽标（J 保留给页级动作，不交给字段分配） */
    shouldDeferPageActionHints() {
      return hasBlockingUiLayer()
    },
    /** 返回 */
    goBack() {
      this.$router.back();
    },
    getUser() {
      getUserProfile().then(response => {
        this.user = response.data;
        this.roleGroup = response.roleGroup;
        this.postGroup = response.postGroup;
      });
    },
    formatContact(value) {
      return formatContactDisplay(value, this.$t('member.not-set'));
    },
    hasContact(value) {
      return !!(value || '').trim();
    },
    onAvatarUpdated(imgUrl) {
      if (this.user) {
        this.$set(this.user, 'avatar', imgUrl)
      }
    }
  }
};
</script>
<style lang="scss" scoped>
.list-group-item {
  .svg-icon {
    margin-right: 5px;
  }
}
.contact-display.is-empty {
  color: var(--text-color-secondary);
}
.profile-basic-card :deep(.el-card__header){
  padding: 0 20px;
  border-bottom: none;
}
.profile-basic-card__header {
  margin: 0 -5px;
}
.profile-hint-tabs :deep(.el-tabs__header){
  margin: 0;
}
.profile-hint-tabs :deep(.el-tabs__content){
  display: none;
}
.profile-hint-tabs :deep(.el-tabs__header),
.profile-hint-tabs :deep(.el-tabs__nav-wrap),
.profile-hint-tabs :deep(.el-tabs__nav-scroll),
.profile-hint-tabs :deep(.el-tabs__nav){
  overflow: visible !important;
}
.profile-hint-tabs :deep(.el-tabs__item.is-active){
  position: relative;
  overflow: visible !important;
}
.profile-hint-tabs :deep(.profile-tab-label){
  position: relative;
  display: inline-block;
}
</style>
