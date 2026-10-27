<template>
  <div :class="classObj" class="app-wrapper" :style="{'--current-color': theme}">
    <div v-if="device==='mobile'&&sidebar.opened" class="drawer-bg" @click="handleClickOutside" />
    <sidebar v-if="!sidebar.hide" class="sidebar-container" />
    <div
      :class="{
        hasTagsView: needTagsView,
        sidebarHide: sidebar.hide,
        'main-container--project-list': isProjectListPage
      }"
      class="main-container"
    >
      <div :class="{'fixed-header':fixedHeader}">
        <navbar />
        <tags-view v-if="needTagsView" />
      </div>
      <app-main />
      <right-panel>
        <settings />
      </right-panel>
    </div>
    <command-palette />
  </div>
</template>

<script>
import RightPanel from '@/components/RightPanel'
import { AppMain, Navbar, Settings, Sidebar, TagsView } from './components'
import ResizeMixin from './mixin/ResizeHandler'
import ViewBackMixin from '@/layout/mixin/ViewBackHandle'
import { useLayoutNavHints } from '@/composables/keyboard/useLayoutNavHints'
import { storeToRefs } from 'pinia'
import { useSettingsStore } from '@/stores/settings'
import { useAppStore } from '@/stores/app'
import variables from '@/assets/styles/variables.js'
import { isProjectListPageRoute } from '@/utils/project-list-page'

export default {
  name: 'Layout',
  components: {
    AppMain,
    Navbar,
    RightPanel,
    Settings,
    Sidebar,
    TagsView
  },
  mixins: [ResizeMixin, ViewBackMixin],
  setup() {
    useLayoutNavHints()
    const settingsStore = useSettingsStore()
    const appStore = useAppStore()
    const { theme, sideTheme, tagsView, fixedHeader } = storeToRefs(settingsStore)
    const { sidebar, device } = storeToRefs(appStore)
    return {
      appStore,
      theme,
      sideTheme,
      tagsView,
      fixedHeader,
      sidebar,
      device
    }
  },
  computed: {
    classObj() {
      return {
        hideSidebar: !this.sidebar.opened,
        openSidebar: this.sidebar.opened,
        withoutAnimation: this.sidebar.withoutAnimation,
        mobile: this.device === 'mobile'
      }
    },
    isProjectListPage() {
      return isProjectListPageRoute(this.$route)
    },
    needTagsView() {
      return this.tagsView
    },
    variables() {
      return variables
    }
  },
  methods: {
    handleClickOutside() {
      this.appStore.closeSideBar({ withoutAnimation: false })
    }
  }
}
</script>

<style lang="scss" scoped>
  @import "@/assets/styles/mixin.scss";
  @import "@/assets/styles/variables.scss";

  .app-wrapper {
    @include clearfix;
    position: relative;
    height: 100%;
    width: 100%;
    /* 钳住外壳高度，纵向只让 .main-container 出现滚动条 */
    overflow: hidden;

    &.mobile.openSidebar {
      position: fixed;
      top: 0;
    }
  }

  .drawer-bg {
    background: #000;
    opacity: 0.3;
    width: 100%;
    top: 0;
    height: 100%;
    position: absolute;
    z-index: 999;
  }

  .fixed-header {
    position: fixed;
    top: 0;
    right: 0;
    z-index: 9;
    width: calc(100% - #{$base-sidebar-width});
    transition: width 0.28s;
  }

  .hideSidebar .fixed-header {
    width: calc(100% - 54px);
  }

  .sidebarHide .fixed-header {
    width: 100%;
  }

  .mobile .fixed-header {
    width: 100%;
  }
  .main-container {
    overflow-x: hidden;
    overflow-y: auto;
    /* 全局不用 scrollbar-gutter：无纵向滚动时右侧仍占槽，与左侧 20px 不对称；
       且部分 Chromium 下 gutter 轨道会与真实滚动条并排，看起来像双条。 */
  }
  /*
   * 工作台 tab 切换时内容高度骤变，滚动条显隐会抢宽度。
   * 用常显纵向滚动条占位（勿用 scrollbar-gutter），打断宽度抖动且只保留一条。
   */
  .main-container:has(.workbench-page) {
    overflow-y: scroll;
  }
</style>
