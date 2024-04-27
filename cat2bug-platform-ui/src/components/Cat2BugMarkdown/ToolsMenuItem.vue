<template>
  <div class="tools-row">
    <template v-for="(tool,index) in tools">
      <el-menu-item v-if="!tool.children" :index="tool.name">
        <div class="row">
          <svg-icon v-if="tool.icon" :icon-class="tool.icon" />
          <span>{{tool.name}}</span>
        </div>
      </el-menu-item>
      <el-submenu v-else :index="tool.name">
        <template slot="title">
          <div class="row">
            <svg-icon v-if="tool.icon" :icon-class="tool.icon" />
            <span>{{tool.name}}</span>
          </div>
        </template>
        <tools-menu-item :tools="tool.children" />
      </el-submenu>
    </template>
  </div>
</template>

<script>
export default {
  name: "ToolsMenuItem",
  props: {
    tools: {
      type: Array,
      default: ()=>[]
    }
  }
}
</script>

<style lang="scss" scoped>
.tools-row {

}
.row {
  display: inline-flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  > * {
    margin-right: 3px;
  }
  ::v-deep .el-submenu__icon-arrow {
    display: none;
  }
}
</style>
