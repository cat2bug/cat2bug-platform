<template>
  <el-icon v-if="iconComponent" v-bind="iconAttrs">
    <component :is="iconComponent" />
  </el-icon>
</template>

<script>
import { resolveLegacyIcon } from '@/utils/element-icon'

export default {
  name: 'LegacyIcon',
  inheritAttrs: false,
  props: {
    icon: {
      type: String,
      required: true
    }
  },
  computed: {
    iconComponent() {
      return resolveLegacyIcon(this.icon)
    },
    iconAttrs() {
      const { icon, ...rest } = this.$attrs
      return rest
    }
  }
}
</script>
