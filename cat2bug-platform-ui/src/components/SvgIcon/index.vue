<template>
  <div
    v-if="isExternal"
    :style="styleExternalIcon"
    class="svg-external-icon svg-icon"
    v-bind="nonClickAttrs"
    @click="forwardClick"
  />
  <svg
    v-else
    :class="svgClass"
    aria-hidden="true"
    v-bind="nonClickAttrs"
    @click="forwardClick"
  >
    <use :href="iconName" />
  </svg>
</template>

<script>
import { isExternal } from '@/utils/validate'

export default {
  name: 'SvgIcon',
  inheritAttrs: false,
  props: {
    iconClass: {
      type: String,
      required: true
    },
    className: {
      type: String,
      default: ''
    }
  },
  computed: {
    isExternal() {
      return isExternal(this.iconClass)
    },
    iconName() {
      return `#icon-${this.iconClass}`
    },
    svgClass() {
      if (this.className) {
        return 'svg-icon ' + this.className
      }
      return 'svg-icon'
    },
    styleExternalIcon() {
      return {
        mask: `url(${this.iconClass}) no-repeat 50% 50%`,
        '-webkit-mask': `url(${this.iconClass}) no-repeat 50% 50%`
      }
    },
    nonClickAttrs() {
      const attrs = { ...this.$attrs }
      delete attrs.onClick
      return attrs
    }
  },
  methods: {
    forwardClick(event) {
      const handler = this.$attrs.onClick
      if (!handler) return
      if (Array.isArray(handler)) {
        handler.forEach((fn) => fn(event))
        return
      }
      handler(event)
    }
  }
}
</script>

<style scoped>
.svg-icon {
  width: 1em;
  height: 1em;
  vertical-align: -0.15em;
  fill: currentColor;
  overflow: hidden;
}

.svg-external-icon {
  background-color: currentColor;
  mask-size: cover!important;
  display: inline-block;
}
</style>
