<template>
  <div v-if="!item.hidden">
    <template v-if="hasOneShowingChild(item.children,item) && (!onlyOneChild.children||onlyOneChild.noShowingChildren)&&!item.alwaysShow">
      <app-link
        v-if="onlyOneChild.meta"
        :to="resolvePath(onlyOneChild.path, onlyOneChild.query)"
        v-slot="{ navigate }"
      >
        <el-menu-item
          :index="menuItemIndex(onlyOneChild)"
          :class="{'submenu-title-noDropdown':!isNest}"
          @click="navigate"
        >
          <item :icon="onlyOneChild.meta.icon||(item.meta&&item.meta.icon)" :title="titleName(onlyOneChild)" />
        </el-menu-item>
      </app-link>
    </template>

    <el-sub-menu v-else ref="subMenu" :index="resolvePath(item.path)" teleported>
      <template #title>
        <item v-if="item.meta" :icon="item.meta && item.meta.icon" :title="titleName(item)" />
      </template>
      <sidebar-item
        v-for="child in item.children"
        :key="child.path"
        :is-nest="true"
        :item="child"
        :base-path="resolvePath(child.path)"
        class="nest-menu"
      />
    </el-sub-menu>
  </div>
</template>

<script>
import path from 'path'
import { isExternal } from '@/utils/validate'
import Item from './Item'
import AppLink from './Link'
import FixiOSBug from './FixiOSBug'

export default {
  name: 'SidebarItem',
  components: { Item, AppLink },
  mixins: [FixiOSBug],
  props: {
    // route object
    item: {
      type: Object,
      required: true
    },
    isNest: {
      type: Boolean,
      default: false
    },
    basePath: {
      type: String,
      default: ''
    }
  },
  computed: {
    titleName: function (){
      return function (menu) {
        if(menu.meta.titleI18nKey) {
          return this.$i18n.t(menu.meta.titleI18nKey);
        } else {
          return menu.meta.title;
        }
      }
    }
  },
  data() {
    this.onlyOneChild = null
    return {}
  },
  methods: {
    hasOneShowingChild(children = [], parent) {
      if (!children) {
        children = [];
      }
      const showingChildren = children.filter(item => {
        if (item.hidden) {
          return false
        } else {
          // Temp set(will be used if only has one showing child)
          this.onlyOneChild = item
          return true
        }
      })

      // When there is only one child router, the child router is displayed by default
      if (showingChildren.length === 1) {
        return true
      }

      // Show parent if there are no child router to display
      if (showingChildren.length === 0) {
        this.onlyOneChild = { ... parent, path: '', noShowingChildren: true }
        return true
      }

      return false
    },
    resolvePath(routePath, routeQuery) {
      if (isExternal(routePath)) {
        return routePath
      }
      if (isExternal(this.basePath)) {
        return this.basePath
      }
      // 已是绝对命名空间菜单 path（/{teamKey}/{projectKey}/module），勿再与 basePath 拼接
      const raw = routePath != null ? String(routePath) : ''
      if (raw.startsWith('/')) {
        if (routeQuery) {
          let query = JSON.parse(routeQuery)
          return { path: raw, query }
        }
        return raw
      }
      if (routeQuery) {
        let query = JSON.parse(routeQuery);
        return { path: path.resolve(this.basePath, routePath), query: query }
      }
      return path.resolve(this.basePath, routePath)
    },
    menuItemIndex(menuItem) {
      const resolved = this.resolvePath(menuItem.path, menuItem.query)
      return typeof resolved === 'string' ? resolved : resolved.path
    }
  }
}
</script>
