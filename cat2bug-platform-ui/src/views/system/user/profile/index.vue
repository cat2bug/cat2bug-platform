<template>
  <div class="app-container">
    <el-row>
      <el-page-header @back="goBack" :content="$t('personal-center')">
      </el-page-header>
    </el-row>
    <el-row :gutter="20">
      <el-col :span="6" :xs="24">
        <el-card class="box-card">
          <div slot="header" class="clearfix">
            <span>{{ $t('member.personal-info') }}</span>
          </div>
          <div>
            <div class="text-center">
              <userAvatar />
            </div>
            <ul class="list-group list-group-striped">
              <li class="list-group-item">
                <svg-icon icon-class="user" />
                {{ $t('account') }}
                <div class="pull-right">{{ user.userName }}</div>
              </li>
              <li class="list-group-item">
                <svg-icon icon-class="phone" />{{ $t('phone-number') }}
                <div class="pull-right">{{ user.phoneNumber }}</div>
              </li>
              <li class="list-group-item">
                <svg-icon icon-class="email" />{{ $t('email') }}
                <div class="pull-right">{{ user.email }}</div>
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
        <el-card>
          <div slot="header" class="clearfix">
            <span>{{ $t('member.basic-info') }}</span>
          </div>
          <el-tabs v-model="activeTab">
            <el-tab-pane :label="$t('member.basic-info')" name="userinfo">
              <userInfo :user="user" />
            </el-tab-pane>
            <el-tab-pane :label="$t('modify-password')" name="resetPwd">
              <resetPwd />
            </el-tab-pane>
          </el-tabs>
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

export default {
  name: "Profile",
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
    this.getUser();
  },
  mounted() {
    this.initFloatMenu();
  },
  // 移除滚动条监听
  destroyed() {
    this.$floatMenu.windowsDestory();
  },
  methods: {
    /** 初始化浮动菜单 */
    initFloatMenu() {
      this.$floatMenu.windowsInit(document.querySelector('.main-container'));
      this.$floatMenu.resetMenus([]);
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
</style>
