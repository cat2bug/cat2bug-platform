<template>
  <div class="navbar">
    <hamburger id="hamburger-container" :is-active="sidebar.opened" class="hamburger-container" @toggleClick="toggleSideBar" />

    <breadcrumb id="breadcrumb-container" class="breadcrumb-container" v-if="!topNav"/>
    <top-nav id="topmenu-container" class="topmenu-container" v-if="topNav"/>

    <div class="right-menu">
      <template v-if="device!=='mobile'">
        <el-tooltip :content="floatMenuContent" effect="dark" placement="bottom">
          <el-switch
            class="right-menu-item"
            @change="handleFloatMenuVisible"
            v-model="floatMenuVisible"
            active-color="#13ce66"
            inactive-color="#ff4949">
          </el-switch>
        </el-tooltip>
        <el-tooltip :content="$t('website')" effect="dark" placement="bottom">
          <cat2-bug-site class="right-menu-item hover-effect" />
        </el-tooltip>
        <el-tooltip :content="$t('source-code-address')" effect="dark" placement="bottom">
          <cat2-bug-git id="cat2bug-git" class="right-menu-item hover-effect" />
        </el-tooltip>
        <el-tooltip :content="$t('notice')" effect="dark" placement="bottom">
          <router-link to="/notice/index" class="right-menu-item hover-effect">
            <el-badge :hidden="noticeCount==0" is-dot class="item"><svg-icon icon-class="notice"></svg-icon></el-badge>
          </router-link>
        </el-tooltip>
<!--        <el-tooltip :content="$t('md-address')" effect="dark" placement="bottom">-->
<!--          <cat2-bug-md id="cat2bug-md" class="right-menu-item hover-effect" />-->
<!--        </el-tooltip>-->
<!--        <el-link href="/doc">doc</el-link>-->
        <screenfull id="screenfull" class="right-menu-item hover-effect" />
      </template>
      <lang-select />
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
    <audio hidden :src="noticeSound" ref="audio" @ended="handleAudioEnded"></audio>
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
import LangSelect from "@/components/LangSelect";
import {groupStatisticsNotice} from "@/api/system/notice";

export default {
  data() {
    return {
      floatMenuVisible: false,
      audio: null,
      noticeCount: 0,
      topicId: null,
      panelTopicId: null,
      langIcon: 'lang-zh-CN',
      langName: '简体中文',
      noticeSound: null,
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
    Cat2BugAvatar,
    LangSelect
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
      },
    },
    floatMenuContent: {
      get() {
        return this.$i18n.t(this.$floatMenu.getVisible()?'close':'open')+this.$i18n.t('char-span')+this.$i18n.t('float-menu')
      }
    },
    topNav: {
      get() {
        return this.$store.state.settings.topNav
      }
    }
  },
  created() {
    this.floatMenuVisible = this.$floatMenu.getVisible();
    this.guoNoticeCount();
  },
  mounted() {
    // 订阅WebSocket下载模型消息
    this.topicId = this.$topic.subscribe(this.$topic.NOTICE_TOPIC, (name, data) => {
      this.guoNoticeCount(data);
    });
    this.panelTopicId = this.$topic.subscribe(this.$topic.PANEL_NOTICE_TOPIC, (name, data) => {
      if(data && data.data) {
        const msg = data.data;
        let host = `${window.location.protocol}//${window.location.host}`;
        if(msg.panel) {
          this.$notify({
            title: this.$i18n.t('notice'),
            dangerouslyUseHTMLString: true,
            type: 'success',
            offset: 50,
            message: `<a target="_blank" style="color: #409eff;" href="${host}/#/notice/index?noticeId=${msg.noticeId}">${msg.title}<\a>`
          });
        }
      }
    });
  },
  beforeDestroy() {
    // 取消下载模型的WebSocket订阅
    this.$topic.unsubscribe(this.topicId);
    this.$topic.unsubscribe(this.panelTopicId);
    this.topicId = null;
    this.panelTopicId = null;
  },
  methods: {
    /** 播放音效 */
    playMusic(soundName) {
      if(soundName) {
        this.noticeSound = require('@/assets/sound/'+soundName)
      } else {
        this.noticeSound = require('@/assets/sound/default.mp3')
      }
      // 播放
      this.$nextTick(()=>{
        this.$refs.audio.play();
      });
    },
    /** 声音播放完成 */
    handleAudioEnded() {
      this.$refs.audio.pause(); // 停止
      this.$refs.audio.load();
    },
    /** 获取通知数量 */
    guoNoticeCount(data) {
      groupStatisticsNotice().then(res=>{
        let count = 0;
        res.rows.forEach(g=>{
          count += g.notReadCount;
        });
        this.noticeCount = count;
        if(data && data.data) {
          const msg = data.data;
          if(msg.backgroundMusic) {
            this.playMusic(msg.backgroundMusicUrl);
          }
        }
      })
    },
    toggleSideBar() {
      this.$store.dispatch('app/toggleSideBar')
    },
    async logout() {
      // 推出确认框
      this.$confirm(this.$i18n.t('sure-logout-system').toString(), this.$i18n.t('prompted').toString(), {
        confirmButtonText: this.$i18n.t('ok'),
        cancelButtonText: this.$i18n.t('cancel'),
        type: 'warning'
      }).then(() => {
        // 用户登出
        this.$store.dispatch('LogOut').then(() => {
          location.href = '/index';
        })
      }).catch(() => {});
    },
    handleFloatMenuVisible(visible) {
      this.$floatMenu.setVisible(visible);
    }
  }
}
</script>

<style lang="scss" scoped>

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
    .el-badge {
      line-height: 20px;
    }
    .right-menu-item {
      display: inline-flex;
      justify-content: center;
      align-items: center;
      padding: 0 8px;
      height: 100%;
      font-size: 18px;
      color: #5a5e66;
      vertical-align: text-bottom;

      &.hover-effect {
        cursor: pointer;
        transition: background .3s;

        &:hover {
          background: rgba(0, 0, 0, .025);
          border-bottom: 0px;
          :after {
            border-bottom: 0px;
          }
        }
      }
    }
  }
}
</style>
