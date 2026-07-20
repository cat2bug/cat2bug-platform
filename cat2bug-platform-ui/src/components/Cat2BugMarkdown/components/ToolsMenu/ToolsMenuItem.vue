<template>
  <div class="tools-row">
    <template v-for="(tool,index) in tools">
      <li v-if="tool.type=='siding'" class="line"></li>
      <el-menu-item v-else-if="!tool.children" :index="tool.name">
        <div class="row">
          <svg-icon v-if="tool.icon" :icon-class="tool.icon" />
          <span>{{menuName(tool.name)}}</span>
        </div>
      </el-menu-item>
      <el-sub-menu v-else :index="tool.name">
        <template #title>
          <div class="row">
            <svg-icon v-if="tool.icon" :icon-class="tool.icon" />
            <span>{{menuName(tool.name)}}</span>
          </div>
        </template>
        <tools-menu-item :tools="tool.children" />
      </el-sub-menu>
    </template>
  </div>
</template>

<script>
import { resolveToolbarMenuLabel } from '@/components/Cat2BugMarkdown/resolveToolbarMenuLabel'

export default {
  name: "ToolsMenuItem",
  props: {
    tools: {
      type: Array,
      default: ()=>[]
    }
  },
  computed: {
    menuName() {
      return (name) => resolveToolbarMenuLabel(this.$i18n, name)
    }
  },
}
</script>

<style lang="scss" scoped>
.tools-row {

}
.line {
  border-bottom: 1px solid #EBEEF5;
  margin: 0px 10px;
}
.row {
  display: inline-flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  color: inherit;

  > * {
    margin-right: 5px;
  }

  span {
    color: inherit;
  }

  :deep(.el-submenu__icon-arrow){
    display: none;
  }
}
</style>
