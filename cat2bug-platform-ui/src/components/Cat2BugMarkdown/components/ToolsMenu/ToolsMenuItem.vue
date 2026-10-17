<template>
  <!-- 勿包一层 div：EP 弹层要求 ul > li.el-menu-item，否则高度塌缩、点击易失效 -->
  <template v-for="(tool, index) in tools" :key="tool.name || tool.icon || `child-${index}`">
    <li v-if="tool.type=='siding'" class="line" />
    <el-menu-item v-else-if="!tool.children" :index="tool.name || `child-${index}`">
      <div class="row">
        <svg-icon v-if="tool.icon" :icon-class="tool.icon" />
        <span>{{ menuName(tool.name) }}</span>
      </div>
    </el-menu-item>
    <el-sub-menu
      v-else
      :index="tool.name || `child-${index}`"
      popper-class="cat2bug-tools-menu-popper"
    >
      <template #title>
        <div class="row">
          <svg-icon v-if="tool.icon" :icon-class="tool.icon" />
          <span>{{ menuName(tool.name) }}</span>
        </div>
      </template>
      <tools-menu-item :tools="tool.children" />
    </el-sub-menu>
  </template>
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
.line {
  border-bottom: 1px solid #EBEEF5;
  margin: 0 10px;
  list-style: none;
}
.row {
  display: inline-flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
  color: inherit;
  width: 100%;

  > * {
    margin-right: 5px;
  }

  span {
    color: inherit;
  }
}
</style>
