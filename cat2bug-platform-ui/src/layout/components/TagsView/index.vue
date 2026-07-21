<template>
  <div id="tags-view-container" class="tags-view-container">
    <scroll-pane ref="scrollPane" class="tags-view-wrapper" @scroll="handleScroll">
      <router-link
        v-for="tag in visitedViews"
        ref="tag"
        :key="tag.path"
        v-slot="{ navigate, isActive }"
        custom
        :to="{ path: tag.path, query: tag.query, fullPath: tag.fullPath }"
      >
        <span
          :class="['tags-view-item', isActive ? 'active' : '']"
          :style="isActive ? activeStyle(tag) : undefined"
          @click="navigate"
          @click.middle="!isAffix(tag) ? closeSelectedTag(tag) : undefined"
          @contextmenu.prevent="openMenu(tag, $event)"
        >
          {{ tag.title }}
          <span v-if="!isAffix(tag)" class="tags-view-close" @click.prevent.stop="closeSelectedTag(tag)">
            <el-icon><Close /></el-icon>
          </span>
        </span>
      </router-link>
    </scroll-pane>
    <ul v-show="visible" :style="{left:left+'px',top:top+'px'}" class="contextmenu">
      <li @click="refreshSelectedTag(selectedTag)"><el-icon><RefreshRight /></el-icon> 刷新页面</li>
      <li v-if="!isAffix(selectedTag)" @click="closeSelectedTag(selectedTag)"><el-icon><Close /></el-icon> 关闭当前</li>
      <li @click="closeOthersTags"><el-icon><CircleClose /></el-icon> 关闭其他</li>
      <li v-if="!isFirstView()" @click="closeLeftTags"><el-icon><Back /></el-icon> 关闭左侧</li>
      <li v-if="!isLastView()" @click="closeRightTags"><el-icon><Right /></el-icon> 关闭右侧</li>
      <li @click="closeAllTags(selectedTag)"><el-icon><CircleClose /></el-icon> 全部关闭</li>
    </ul>
  </div>
</template>

<script>
import ScrollPane from './ScrollPane'
import path from 'path'
import { Back, CircleClose, Close, RefreshRight, Right } from '@element-plus/icons-vue'
import { useTagsViewStore } from '@/stores/tagsView'
import { usePermissionStore } from '@/stores/permission'
import { useSettingsStore } from '@/stores/settings'

export default {
  components: { ScrollPane, Back, CircleClose, Close, RefreshRight, Right },
  data() {
    return {
      visible: false,
      top: 0,
      left: 0,
      selectedTag: {},
      affixTags: []
    }
  },
  computed: {
    visitedViews() {
      return useTagsViewStore().visitedViews
    },
    routes() {
      return usePermissionStore().routes
    },
    theme() {
      return useSettingsStore().theme;
    }
  },
  watch: {
    $route() {
      this.addTags()
      this.moveToCurrentTag()
    },
    visible(value) {
      if (value) {
        document.body.addEventListener('click', this.closeMenu)
      } else {
        document.body.removeEventListener('click', this.closeMenu)
      }
    }
  },
  mounted() {
    this.initTags()
    this.addTags()
  },
  methods: {
    isActive(route) {
      return route.path === this.$route.path
    },
    activeStyle(tag) {
      if (!this.isActive(tag)) return {};
      return {
        "background-color": this.theme,
        "border-color": this.theme
      };
    },
    isAffix(tag) {
      return tag.meta && tag.meta.affix
    },
    isFirstView() {
      try {
        return this.selectedTag.fullPath === '/index' || this.selectedTag.fullPath === this.visitedViews[1].fullPath
      } catch (err) {
        return false
      }
    },
    isLastView() {
      try {
        return this.selectedTag.fullPath === this.visitedViews[this.visitedViews.length - 1].fullPath
      } catch (err) {
        return false
      }
    },
    filterAffixTags(routes, basePath = '/') {
      let tags = []
      routes.forEach(route => {
        if (route.meta && route.meta.affix) {
          const tagPath = path.resolve(basePath, route.path)
          tags.push({
            fullPath: tagPath,
            path: tagPath,
            name: route.name,
            meta: { ...route.meta }
          })
        }
        if (route.children) {
          const tempTags = this.filterAffixTags(route.children, route.path)
          if (tempTags.length >= 1) {
            tags = [...tags, ...tempTags]
          }
        }
      })
      return tags
    },
    initTags() {
      const affixTags = this.affixTags = this.filterAffixTags(this.routes)
      const tagsViewStore = useTagsViewStore()
      for (const tag of affixTags) {
        if (tag.name) {
          tagsViewStore.addVisitedView(tag)
        }
      }
    },
    addTags() {
      const { name } = this.$route
      const tagsViewStore = useTagsViewStore()
      if (name) {
        tagsViewStore.addView(this.$route)
        if (this.$route.meta.link) {
          tagsViewStore.addIframeView(this.$route)
        }
      }
      return false
    },
    moveToCurrentTag() {
      const tags = this.$refs.tag
      this.$nextTick(() => {
        for (const tag of tags) {
          const targetPath = tag.to?.path || tag.$props?.to?.path
          if (targetPath === this.$route.path) {
            this.$refs.scrollPane.moveToTarget(tag)
            const fullPath = tag.to?.fullPath || tag.$props?.to?.fullPath
            if (fullPath !== this.$route.fullPath) {
              useTagsViewStore().updateVisitedView(this.$route)
            }
            break
          }
        }
      })
    },
    refreshSelectedTag(view) {
      this.$tab.refreshPage(view);
      if (this.$route.meta.link) {
        useTagsViewStore().delIframeView(this.$route)
      }
    },
    closeSelectedTag(view) {
      this.$tab.closePage(view).then(({ visitedViews }) => {
        if (this.isActive(view)) {
          this.toLastView(visitedViews, view)
        }
      })
    },
    closeRightTags() {
      this.$tab.closeRightPage(this.selectedTag).then(visitedViews => {
        if (!visitedViews.find(i => i.fullPath === this.$route.fullPath)) {
          this.toLastView(visitedViews)
        }
      })
    },
    closeLeftTags() {
      this.$tab.closeLeftPage(this.selectedTag).then(visitedViews => {
        if (!visitedViews.find(i => i.fullPath === this.$route.fullPath)) {
          this.toLastView(visitedViews)
        }
      })
    },
    closeOthersTags() {
      this.$router.push(this.selectedTag.fullPath).catch(()=>{});
      this.$tab.closeOtherPage(this.selectedTag).then(() => {
        this.moveToCurrentTag()
      })
    },
    closeAllTags(view) {
      this.$tab.closeAllPage().then(({ visitedViews }) => {
        if (this.affixTags.some(tag => tag.path === this.$route.path)) {
          return
        }
        this.toLastView(visitedViews, view)
      })
    },
    toLastView(visitedViews, view) {
      const latestView = visitedViews.slice(-1)[0]
      if (latestView) {
        this.$router.push(latestView.fullPath)
      } else {
        // now the default is to redirect to the home page if there is no tags-view,
        // you can adjust it according to your needs.
        if (view.name === 'Dashboard' || view.name === 'ProjectDashboard') {
          // to reload home page
          this.$router.replace({ path: '/redirect' + view.fullPath })
        } else {
          this.$router.push('/')
        }
      }
    },
    openMenu(tag, e) {
      const menuMinWidth = 105
      const offsetLeft = this.$el.getBoundingClientRect().left // container margin left
      const offsetWidth = this.$el.offsetWidth // container width
      const maxLeft = offsetWidth - menuMinWidth // left boundary
      const left = e.clientX - offsetLeft + 15 // 15: margin right

      if (left > maxLeft) {
        this.left = maxLeft
      } else {
        this.left = left
      }

      this.top = e.clientY
      this.visible = true
      this.selectedTag = tag
    },
    closeMenu() {
      this.visible = false
    },
    handleScroll() {
      this.closeMenu()
    }
  }
}
</script>

<style lang="scss" scoped>
.tags-view-container {
  height: 34px;
  width: 100%;
  background: var(--tags-view-bg, #fff);
  border-bottom: 1px solid var(--tags-view-border, #d8dce5);
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, .12), 0 0 3px 0 rgba(0, 0, 0, .04);
  .tags-view-wrapper {
    .tags-view-item {
      display: inline-block;
      position: relative;
      cursor: pointer;
      height: 26px;
      line-height: 26px;
      border: 1px solid #d8dce5;
      color: #495060;
      background: #fff;
      padding: 0 8px;
      font-size: 12px;
      margin-left: 5px;
      margin-top: 4px;
      &:first-of-type {
        margin-left: 15px;
      }
      &:last-of-type {
        margin-right: 15px;
      }
      &.active {
        background-color: #42b983;
        color: #fff;
        border-color: #42b983;
        &::before {
          content: '';
          background: #fff;
          display: inline-block;
          width: 8px;
          height: 8px;
          border-radius: 50%;
          position: relative;
          margin-right: 2px;
        }
      }
    }
  }
  .contextmenu {
    margin: 0;
    background: #fff;
    z-index: 3000;
    position: absolute;
    list-style-type: none;
    padding: 5px 0;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 400;
    color: #333;
    box-shadow: 2px 2px 3px 0 rgba(0, 0, 0, .3);
    li {
      margin: 0;
      padding: 7px 16px;
      cursor: pointer;
      display: flex;
      align-items: center;
      gap: 6px;
      &:hover {
        background: #eee;
      }
    }
  }
}
</style>

<style lang="scss">
.tags-view-wrapper {
  .tags-view-item {
    .tags-view-close {
      width: 16px;
      height: 16px;
      vertical-align: middle;
      border-radius: 50%;
      text-align: center;
      transition: all .3s cubic-bezier(.645, .045, .355, 1);
      transform-origin: 100% 50%;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      margin-left: 2px;
      &:hover {
        background-color: #b4bccc;
        color: #fff;
      }
    }
  }
}
</style>
