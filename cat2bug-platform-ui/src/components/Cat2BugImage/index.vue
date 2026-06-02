<template>
  <el-image
    v-bind="$attrs"
    v-on="imageListeners"
    :src="displaySrc"
    :preview-src-list="displayPreviewList"
  >
    <image-error-slot slot="error" />
    <slot slot="placeholder" name="placeholder"></slot>
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
    imageListeners() {
      const listeners = { ...this.$listeners }
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
        this.$set(this.failedPreviewIndexes, index, true)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
::v-deep.el-image {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: var(--cat2bug-border-radius, 4px);
  background-color: var(--cat2bug-image-placeholder-bg, #ebeef5);
  box-sizing: border-box;
}
::v-deep .el-image__inner {
  display: block;
  max-width: 100%;
  max-height: 100%;
  width: auto !important;
  height: auto !important;
  object-fit: contain;
  object-position: center center;
  vertical-align: middle;
}
</style>
