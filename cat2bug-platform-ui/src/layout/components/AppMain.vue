<template>
  <section class="app-main">
    <transition name="fade-transform" mode="out-in">
      <keep-alive :include="cachedViews">
        <router-view v-if="!$route.meta.link" :key="key" />
      </keep-alive>
    </transition>
    <iframe-toggle />
  </section>
</template>

<script>
import iframeToggle from "./IframeToggle/index"

export default {
  name: 'AppMain',
  components: { iframeToggle },
  computed: {
    cachedViews() {
      return this.$store.state.tagsView.cachedViews
    },
    key() {
      return this.$route.path
    }
  }
}
</script>

<style lang="scss" scoped>
.app-main {
  /* 50= navbar  50  */
  min-height: calc(100vh - 50px);
  width: 100%;
  position: relative;
  /* hidden 会在个别布局下裁掉主区域底部内容；整页滚动由 main-container 承担 */
  overflow-x: hidden;
  overflow-y: visible;
  /* 路由根节点（transition/keep-alive 为抽象组件，不占 DOM）作为首子元素铺满，避免仅 min-height 时底部留白 */
  display: flex;
  flex-direction: column;
}

.app-main > *:first-child {
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
}

.fixed-header + .app-main {
  padding-top: 50px;
}

.hasTagsView {
  .app-main {
    /* 84 = navbar + tags-view = 50 + 34 */
    min-height: calc(100vh - 84px);
  }

  .fixed-header + .app-main {
    padding-top: 84px;
  }
}
</style>

<style lang="scss">
// fix css style bug in open el-dialog
.el-popup-parent--hidden {
  .fixed-header {
    padding-right: 6px;
  }
}

/* 勿用全局 ::-webkit-scrollbar，会压扁 / 干扰表格区滚动条（见 el-table-scrollbar.scss） */
.main-container::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.main-container::-webkit-scrollbar-track {
  background-color: #e8e8e8;
}

.main-container::-webkit-scrollbar-thumb {
  background-color: #a8a8a8;
  border-radius: 4px;
}
</style>
