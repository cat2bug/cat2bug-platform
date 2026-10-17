<template>
  <el-menu
    :default-active="activeToolsIndex"
    class="tools-menu"
    mode="horizontal"
    :ellipsis="ellipsis"
    @select="toolsHandle"
  >
    <template v-for="(tool, index) in tools" :key="toolMenuKey(tool, index)">
      <li v-if="tool.type=='siding'" class="siding" />
      <el-menu-item v-else-if="!tool.children && tool.icon" :index="toolMenuIndex(tool, index)">
        <template #title>
          <div class="row">
            <el-tooltip class="item" effect="dark" :content="menuName(tool.name)" placement="bottom">
              <div v-if="tool.type=='check'">
                <svg-icon class="icon" v-show="tool.check && tool.activeIcon" :icon-class="tool.activeIcon" />
                <svg-icon class="icon" v-show="!tool.check" :icon-class="tool.icon" />
              </div>
              <svg-icon v-else class="icon" :icon-class="tool.icon" />
            </el-tooltip>
          </div>
        </template>
      </el-menu-item>
      <el-sub-menu v-else :index="toolMenuIndex(tool, index)">
        <template #title>
          <svg-icon v-if="tool.icon" class="icon" :icon-class="tool.icon" />
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
      let tool = this.getNode(this.tools, keyPath);
      if(tool) {
        if(tool.type=='check') {
          if(tool.check) {
            tool.check = false;
          } else {
            tool.check = true;
          }
        }
        this.$emit('update:tools', this.tools, tool); this.$emit('select', this.tools, tool)
      }
    },
    getNode(tools, path) {
      if (!path || !path.length) return null
      let findTools = tools;
      let endToolName = path[path.length-1];
      for(let k in path) {
        let name = path[k];
        for(let i in findTools) {
          let tool = findTools[i];
          if(k==path.length-1 && tool.name==endToolName) return tool;
          if(tool.name==name) {
            findTools = tool.children;
            break;
          }
        }
      }
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
  row-gap: 4px;
  width: max-content;
  height: auto;
  .el-menu-item,
  :deep(.el-sub-menu),
  :deep(.el-submenu) {
    height: 30px;
    width: 30px;
    min-width: 30px;
    max-width: 30px;
    flex: 0 0 30px;
    line-height: 28px;
    padding: 0;
    border-radius: 3px;
    border: none;
    justify-content: center;
    > * {
      padding: 0px;
    }
  }
  :deep(.el-sub-menu__title),
  :deep(.el-submenu__title),
  :deep(.el-tooltip__trigger) {
    height: 30px;
    width: 30px;
    min-width: 30px;
    padding: 0;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-bottom: none;
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
.row {
  display: inline-flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
}
.siding {
  width: 1px;
  height: 15px;
  flex: 0 0 1px;
  background-color: #dcdcdc;
  margin: 0px 5px;
  float: none;
  align-self: center;
}
.siding:hover {
  background-color: #ffffff;
}
.icon {
  color: #606266;
  margin-left: 0px !important;
}
</style>
