<template>
  <el-dropdown trigger="click" @command="handleSetSize">
    <div>
      <svg-icon class-name="size-icon" icon-class="size" />
    </div>
    <template #dropdown>
    <el-dropdown-menu>
      <el-dropdown-item v-for="item of sizeOptions" :key="item.value" :disabled="size===item.value" :command="item.value">
        {{ item.label }}
      </el-dropdown-item>
    </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script>
import { storeToRefs } from 'pinia'
import { useAppStore } from '@/stores/app'
import { useTagsViewStore } from '@/stores/tagsView'

export default {
  setup() {
    const appStore = useAppStore()
    const tagsViewStore = useTagsViewStore()
    const { size } = storeToRefs(appStore)
    return {
      appStore,
      tagsViewStore,
      size
    }
  },
  data() {
    return {
      sizeOptions: [
        { label: 'Default', value: 'default' },
        { label: 'Medium', value: 'medium' },
        { label: 'Small', value: 'small' },
        { label: 'Mini', value: 'mini' }
      ]
    }
  },
  methods: {
    handleSetSize(size) {
      this.$ELEMENT.size = size
      this.appStore.setSize(size)
      this.refreshView()
      this.$message({
        message: 'Switch Size Success',
        type: 'success'
      })
    },
    refreshView() {
      // In order to make the cached page re-rendered
      this.tagsViewStore.delAllCachedViews(this.$route)

      const { fullPath } = this.$route

      this.$nextTick(() => {
        this.$router.replace({
          path: '/redirect' + fullPath
        })
      })
    }
  }

}
</script>
