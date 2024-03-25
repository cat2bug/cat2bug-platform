<template>
  <div class="navbar">
    <hamburger id="hamburger-container" :is-active="sidebar.opened" class="hamburger-container" @toggleClick="toggleSideBar" />

    <breadcrumb id="breadcrumb-container" class="breadcrumb-container" v-if="!topNav"/>
    <top-nav id="topmenu-container" class="topmenu-container" v-if="topNav"/>

    <div class="right-menu">
      <template v-if="device!=='mobile'">
        <el-tooltip :content="$t('website')" effect="dark" placement="bottom">
          <cat2-bug-site class="right-menu-item hover-effect" />
        </el-tooltip>
        <el-tooltip :content="$t('source-code-address')" effect="dark" placement="bottom">
          <cat2-bug-git id="cat2bug-git" class="right-menu-item hover-effect" />
        </el-tooltip>

<!--        <el-tooltip :content="$t('md-address')" effect="dark" placement="bottom">-->
<!--          <cat2-bug-md id="cat2bug-md" class="right-menu-item hover-effect" />-->
<!--        </el-tooltip>-->
<!--        <el-link href="/doc">doc</el-link>-->
        <screenfull id="screenfull" class="right-menu-item hover-effect" />
      </template>
      <el-dropdown class="lang avatar-container right-menu-item hover-effect" @command="handleLanguageCommand">
        <span class="dropdown-title">
          <svg-icon :icon-class="langIcon" />{{ langName }}
          <i class="el-icon-caret-bottom"></i>
        </span>
        <el-dropdown-menu class="dropdown-menu" slot="dropdown">
          <el-dropdown-item command="zh_CN"><svg-icon icon-class="lang_zh_CN" />简体中文</el-dropdown-item>
          <el-dropdown-item command="en_US"><svg-icon icon-class="lang_en_US" />English</el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
      <el-dropdown class="avatar-container right-menu-item hover-effect" trigger="click">
        <span class="dropdown-title">
          <cat2-bug-avatar :member="member" />
          <i class="el-icon-caret-bottom" />
        </span>
        <el-dropdown-menu slot="dropdown">
          <router-link to="/member/profile">
            <el-dropdown-item>{{$t('my-center')}}</el-dropdown-item>
          </router-link>
          <el-dropdown-item divided @click.native="logout">
            <span>{{$t('logout')}}</span>
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import Breadcrumb from '@/components/Breadcrumb'
import TopNav from '@/components/TopNav'
import Hamburger from '@/components/Hamburger'
import Screenfull from '@/components/Screenfull'
import SizeSelect from '@/components/SizeSelect'
import Search from '@/components/HeaderSearch'
import Cat2BugSite from '@/components/Cat2Bug/Site'
import Cat2BugGit from '@/components/Cat2Bug/Git'
import Cat2BugDoc from '@/components/Cat2Bug/Doc'
import Cat2BugAvatar from "@/components/Cat2BugAvatar";
const I18N_LOCALE_KEY='i18n-locale'
export default {
  data() {
    return {
      langIcon: 'lang-zh-CN',
      langName: '简体中文'
    }
  },
  components: {
    Breadcrumb,
    TopNav,
    Hamburger,
    Screenfull,
    SizeSelect,
    Search,
    Cat2BugSite,
    Cat2BugGit,
    Cat2BugDoc,
    Cat2BugAvatar
  },
  computed: {
    ...mapGetters([
      'sidebar',
      'avatar',
      'device',
      'name',
    ]),
    member: {
      get() {
        return {
          avatar: this.avatar,
          name: this.name
        }
      }
    },
    setting: {
      get() {
        return this.$store.state.settings.showSettings
      },
      set(val) {
        this.$store.dispatch('settings/changeSetting', {
          key: 'showSettings',
          value: val
        })
      }
    },
    topNav: {
      get() {
        return this.$store.state.settings.topNav
      }
    }
  },
  created() {
    const lang = this.$cache.local.get(I18N_LOCALE_KEY)
    this.handleLanguageCommand(lang||'zh_CN');
  },
  methods: {
    toggleSideBar() {
      this.$store.dispatch('app/toggleSideBar')
    },
    async logout() {
      this.$confirm(this.$i18n.t('sure-logout-system').toString(), this.$i18n.t('prompted').toString(), {
        confirmButtonText: this.$i18n.t('ok'),
        cancelButtonText: this.$i18n.t('cancel'),
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('LogOut').then(() => {
          location.href = '/index';
        })
      }).catch(() => {});
    },
    handleLanguageCommand(lang) {
      this.$i18n.locale = lang;
      this.$cache.local.set(I18N_LOCALE_KEY,lang);
      this.langIcon = 'lang_'+lang;
      switch (lang){
        case 'zh_CN':
          this.langName = '简体中文';
          break;
        case 'en_US':
          this.langName = 'English';
          break;
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.lang {
  margin-right: 0px !important;
}
.dropdown-title{
  height: 100%;
  font-size: 16px;
  display: inline-flex;
  flex-direction: row;
  gap: 5px;
  align-items: center;
  .svg-icon {
    font-size: 22px;
  }
  i {
    font-size: 12px;
  }
}
.dropdown-menu {
  ::v-deep .el-dropdown-menu__item {
    display: inline-flex;
    flex-direction: row;
    align-items: center;
    gap: 5px;
    .svg-icon {
      font-size: 20px;
    }
  }
}
.navbar {
  height: 50px;
  overflow: hidden;
  position: relative;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);

  .hamburger-container {
    line-height: 46px;
    height: 100%;
    float: left;
    cursor: pointer;
    transition: background .3s;
    -webkit-tap-highlight-color:transparent;

    &:hover {
      background: rgba(0, 0, 0, .025)
    }
  }

  .breadcrumb-container {
    float: left;
  }

  .topmenu-container {
    position: absolute;
    left: 50px;
  }

  .errLog-container {
    display: inline-block;
    vertical-align: top;
  }

  .right-menu {
    float: right;
    height: 100%;
    line-height: 50px;

    &:focus {
      outline: none;
    }

    .right-menu-item {
      display: inline-block;
      padding: 0 8px;
      height: 100%;
      font-size: 18px;
      color: #5a5e66;
      vertical-align: text-bottom;

      &.hover-effect {
        cursor: pointer;
        transition: background .3s;

        &:hover {
          background: rgba(0, 0, 0, .025)
        }
      }
    }
  }
}
</style>
