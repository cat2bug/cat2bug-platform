<template>
  <el-menu :default-active="activeToolsIndex" class="tools-menu" mode="horizontal" @select="toolsHandle">
    <template v-for="(tool,index) in tools">
      <li v-if="tool.type=='siding'" class="siding" />
      <el-menu-item v-else-if="!tool.children" :index="tool.name">
        <template slot="title">
          <div class="row">
            <svg-icon class="icon" v-if="tool.icon" :icon-class="tool.icon" />
          </div>
        </template>
      </el-menu-item>
      <el-submenu v-else :index="tool.name">
        <template slot="title">
          <svg-icon v-if="tool.icon" class="icon" :icon-class="tool.icon" />
        </template>
        <tools-menu-item :tools="tool.children" />
      </el-submenu>
    </template>
  </el-menu>
</template>

<script>
import ToolsMenuItem from "@/components/Cat2BugMarkdown/ToolsMenuItem";
export default {
  name: "ToolsMenu",
  components: { ToolsMenuItem },
  data() {
    return {
      activeToolsIndex: null,

    }
  },
  props:{
    tools: {
      type: Array,
      default: () => []
    }
  },
  methods: {
    toolsHandle(key, keyPath) {
      let tool = this.getNode(this.tools, keyPath);
      if(tool) {
        this.$emit('select', tool)
      }
    },
    getNode(tools, path) {
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
  align-items: center;
  .el-menu-item, ::v-deep .el-submenu {
    height: 30px;
    line-height: 28px;
    padding: 0px 8px;
    border-radius: 3px;
  }
  .el-menu-item:hover, ::v-deep .el-submenu:hover {
    color: #1890ff;
    border-color: #badeff;
    background-color: #e8f4ff;
  }
  .el-submenu ::v-deep .el-submenu__title {
    height: 30px;
    line-height: 28px;
  }
  .el-submenu ::v-deep .el-submenu__title:hover {
    background-color: #00000000;
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
  background-color: #dcdcdc;
  margin: 0px 5px;
  float: left;
}
.siding:hover {
  background-color: #ffffff;
}
.icon {
  color: #606266;
  margin-left: 0px !important;
}
</style>
