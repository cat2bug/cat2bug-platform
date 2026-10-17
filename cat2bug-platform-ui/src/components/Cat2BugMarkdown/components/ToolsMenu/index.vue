<template>
  <el-menu
    :default-active="activeToolsIndex"
    class="tools-menu"
    mode="horizontal"
    menu-trigger="click"
    :ellipsis="ellipsis"
    :hide-timeout="300"
    @select="toolsHandle"
  >
    <template v-for="(tool, index) in tools" :key="toolMenuKey(tool, index)">
      <li v-if="tool.type=='siding'" class="siding" />
      <el-menu-item v-else-if="!tool.children && tool.icon" :index="toolMenuIndex(tool, index)">
        <template #title>
          <el-tooltip class="item" effect="dark" :content="menuName(tool.name)" placement="bottom">
            <!-- 必须用块级包裹：否则 EP 会把 svg 标成 tooltip trigger，被 30px 规则撑满导致图标叠在一起 -->
            <span class="tool-btn">
              <template v-if="tool.type=='check'">
                <svg-icon class="icon" v-show="tool.check && tool.activeIcon" :icon-class="tool.activeIcon" />
                <svg-icon class="icon" v-show="!tool.check" :icon-class="tool.icon" />
              </template>
              <svg-icon v-else class="icon" :icon-class="tool.icon" />
            </span>
          </el-tooltip>
        </template>
      </el-menu-item>
      <el-sub-menu
        v-else
        :index="toolMenuIndex(tool, index)"
        popper-class="cat2bug-tools-menu-popper"
      >
        <template #title>
          <span class="tool-btn">
            <svg-icon v-if="tool.icon" class="icon" :icon-class="tool.icon" />
          </span>
        </template>
        <tools-menu-item :tools="tool.children" />
      </el-sub-menu>
    </template>
  </el-menu>
</template>

<script>
import ToolsMenuItem from "./ToolsMenuItem";
import { resolveToolbarMenuLabel } from "@/components/Cat2BugMarkdown/resolveToolbarMenuLabel";
export default {
  emits: ['update:tools', 'select'],
  name: "ToolsMenu",
  components: { ToolsMenuItem },
  data() {
    return {
      activeToolsIndex: null,

    }
  },
  computed: {
    menuName() {
      return (name) => resolveToolbarMenuLabel(this.$i18n, name)
    }
  },
  props:{
    tools: {
      type: Array,
      default: () => []
    },
    /** false = show all toolbar icons (no "..." overflow menu; avoids EP resize duplicate-key warnings) */
    ellipsis: {
      type: Boolean,
      default: false
    }
  },
  methods: {
    toolMenuKey(tool, index) {
      return tool.name || tool.icon || `tool-${index}`
    },
    toolMenuIndex(tool, index) {
      // el-menu 用 index 做内部 key；无 name 的分隔项/异常项补唯一后缀
      return tool.name || `tool-${index}`
    },
    toolsHandle(key, keyPath) {
      const tool = this.getNode(this.tools, keyPath) || this.findToolByName(this.tools, key)
      if (!tool) return
      if (tool.type === 'check') {
        tool.check = !tool.check
      }
      this.$emit('update:tools', this.tools, tool)
      this.$emit('select', this.tools, tool)
    },
    getNode(tools, path) {
      if (!path || !path.length || !tools) return null
      let findTools = tools
      let found = null
      for (let i = 0; i < path.length; i++) {
        const name = path[i]
        if (!findTools || !findTools.length) return null
        const tool = findTools.find((t) => t && t.name === name)
        if (!tool) return null
        found = tool
        findTools = tool.children
      }
      return found
    },
    findToolByName(tools, name) {
      if (!tools || !name) return null
      for (const tool of tools) {
        if (!tool) continue
        if (tool.name === name) return tool
        const child = this.findToolByName(tool.children, name)
        if (child) return child
      }
      return null
    }
  }
}
</script>

<style lang="scss" scoped>
.tools-menu {
  border: none;
  display: inline-flex;
  flex-direction: row;
  flex-wrap: wrap;
  align-items: center;
  gap: 2px;
  row-gap: 4px;
  width: max-content;
  height: auto;

  /* 仅约束顶栏按钮；勿用 :deep(.el-menu-item) 以免压到弹层内菜单项 */
  > .el-menu-item,
  > :deep(.el-sub-menu),
  > :deep(.el-submenu) {
    height: 30px;
    width: 30px;
    min-width: 30px;
    max-width: 30px;
    flex: 0 0 30px;
    line-height: 30px;
    padding: 0;
    margin: 0;
    border-radius: 3px;
    border: none;
    overflow: hidden;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    > * {
      padding: 0;
    }
  }

  :deep(.el-sub-menu__title),
  :deep(.el-submenu__title) {
    height: 30px;
    width: 30px;
    min-width: 30px;
    max-width: 30px;
    padding: 0;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-bottom: none;
    overflow: hidden;
  }

  .el-menu-item:hover,
  :deep(.el-sub-menu:hover),
  :deep(.el-submenu:hover) {
    color: #1890ff;
    border-color: #badeff;
    background-color: #e8f4ff;
  }

  :deep(.el-sub-menu__title:hover),
  :deep(.el-submenu__title:hover) {
    background-color: #00000000;
  }

  /* 图标按钮不展示下拉三角（如标题 H），悬停/点击仍可展开子菜单 */
  :deep(.el-sub-menu__icon-arrow),
  :deep(.el-submenu__icon-arrow) {
    display: none !important;
  }
}

.tool-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  line-height: 1;
}

.siding {
  width: 1px;
  height: 15px;
  flex: 0 0 1px;
  background-color: #dcdcdc;
  margin: 0 5px;
  float: none;
  align-self: center;
}

.siding:hover {
  background-color: #ffffff;
}

.icon {
  color: #606266;
  width: 16px !important;
  height: 16px !important;
  font-size: 16px;
  margin-left: 0 !important;
  flex: 0 0 16px;
}
</style>

<!-- 弹层挂到 body，需非 scoped；去掉 scaleY 动画，避免菜单项高度为 0 点不中 -->
<style lang="scss">
.cat2bug-tools-menu-popper.el-popper {
  transform: none !important;
  transition: opacity 0.1s ease !important;
}
.cat2bug-tools-menu-popper .el-menu--popup {
  min-width: 160px;
}
.cat2bug-tools-menu-popper .el-menu-item {
  height: 36px !important;
  line-height: 36px !important;
  width: auto !important;
  max-width: none !important;
  min-width: 0 !important;
  padding: 0 12px !important;
  overflow: visible !important;
}
</style>
