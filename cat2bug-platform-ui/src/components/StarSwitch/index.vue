<template>
  <span
    class="start-switch"
    :collect="isCollected ? 'true' : 'false'"
    @click="clickCollectHandle"
  >
    <el-icon>
      <component :is="isCollected ? 'StarFilled' : 'Star'" />
    </el-icon>
  </span>
</template>

<script>
import { StarFilled, Star } from '@element-plus/icons-vue'

export default {
  name: 'StartSwitch',
  inheritAttrs: false,
  components: {
    StarFilled,
    Star
  },
  props: {
    /** Vue 3 v-model */
    modelValue: {
      type: Boolean,
      default: false
    },
    /** 兼容旧版 collect 传参 */
    collect: {
      type: Boolean,
      default: undefined
    }
  },
  emits: ['update:modelValue', 'change'],
  computed: {
    isCollected() {
      return this.collect !== undefined ? this.collect : this.modelValue
    }
  },
  methods: {
    clickCollectHandle(event) {
      const next = !this.isCollected
      this.$emit('update:modelValue', next)
      this.$emit('change', next)
      event.stopPropagation()
    }
  }
}
</script>

<style lang="scss" scoped>
.start-switch {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
}

.start-switch :deep(.el-icon) {
  font-size: inherit;
}

.start-switch[collect='true'] :deep(.el-icon) {
  color: var(--star-switch-on-color, rgb(247, 186, 42));
}

/* 浅色主题 off-color 为描边色（#fff），SVG 应使用 off-stroke / off-fill */
.start-switch:not([collect='true']) :deep(.el-icon) {
  color: var(--star-switch-off-stroke, #c0c4cc);
}
</style>
