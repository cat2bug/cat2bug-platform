<template>
  <el-image
    ref="elImageRef"
    v-bind="imageAttrs"
    v-on="imageListeners"
    :src="displaySrc"
    :preview-src-list="displayPreviewList"
    :preview-teleported="previewTeleported"
    :z-index="zIndex"
    @close="onPreviewClose"
  >
    <template #error>
      <div class="cat2bug-image-error-trigger" @click.stop="openPreviewFromError">
        <image-error-slot />
      </div>
    </template>
    <template #placeholder>
      <slot name="placeholder"></slot>
    </template>
  </el-image>
</template>

<script>
import { DEFAULT_IMAGE } from '@/utils/upload-asset'
import ImageErrorSlot from './ImageErrorSlot.vue'

export default {
  name: 'Cat2BugImage',
  components: { ImageErrorSlot },
  inheritAttrs: false,
  props: {
    src: {
      type: [String, Object],
      default: ''
    },
    previewSrcList: {
      type: Array,
      default: undefined
    },
    previewTeleported: {
      type: Boolean,
      default: true
    },
    zIndex: {
      type: Number,
      default: 3000
    }
  },
  data() {
    return {
      loadFailed: false,
      failedPreviewIndexes: {}
    }
  },
  computed: {
    previewListProp() {
      if (this.previewSrcList !== undefined) {
        return this.previewSrcList
      }
      return this.$attrs['preview-src-list']
    },
    imageAttrs() {
      const attrs = { ...this.$attrs }
      Object.keys(attrs).forEach((key) => {
        if (key.startsWith('on') && typeof attrs[key] === 'function') {
          delete attrs[key]
        }
      })
      return attrs
    },
    imageListeners() {
      const listeners = {}
      Object.entries(this.$attrs).forEach(([key, handler]) => {
        if (key.startsWith('on') && typeof handler === 'function') {
          const eventName = key.slice(2).replace(/^[A-Z]/, (c) => c.toLowerCase())
          listeners[eventName] = handler
        }
      })
      const userError = listeners.error
      listeners.error = (...args) => {
        this.onImageError()
        if (typeof userError === 'function') {
          userError(...args)
        }
      }
      return listeners
    },
    displaySrc() {
      if (this.loadFailed || !this.src) {
        return DEFAULT_IMAGE
      }
      return this.src
    },
    displayPreviewList() {
      const list = this.previewListProp
      if (!list || !list.length) {
        return list
      }
      return list.map((url, index) => {
        if (this.failedPreviewIndexes[index]) {
          return DEFAULT_IMAGE
        }
        return url
      })
    }
  },
  watch: {
    src() {
      this.loadFailed = false
    },
    previewListProp: {
      handler() {
        this.failedPreviewIndexes = {}
      },
      deep: true
    }
  },
  methods: {
    onImageError() {
      this.loadFailed = true
      const list = this.previewListProp
      if (!list || !list.length) {
        return
      }
      const index = list.indexOf(this.src)
      if (index > -1) {
        this.failedPreviewIndexes = { ...this.failedPreviewIndexes, [index]: true }
      }
    },
    openPreviewFromError() {
      if (!this.displayPreviewList || !this.displayPreviewList.length) return
      this.$refs.elImageRef?.showPreview?.()
    },
    onPreviewClose() {
      this.$emit('close')
    }
  }
}
</script>

<style lang="scss" scoped>
:deep(.el-image){
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: var(--cat2bug-border-radius, 4px);
  background-color: var(--cat2bug-image-placeholder-bg, #ebeef5);
  box-sizing: border-box;
}
:deep(.el-image__inner){
  display: block;
  max-width: 100%;
  max-height: 100%;
  width: auto !important;
  height: auto !important;
  object-fit: contain;
  object-position: center center;
  vertical-align: middle;
}
.cat2bug-image-error-trigger {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: zoom-in;
}
</style>
