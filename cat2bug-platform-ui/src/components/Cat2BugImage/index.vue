<template>
  <el-image
    ref="elImageRef"
    v-bind="imageAttrs"
    v-on="imageListeners"
    class="cat2bug-image-root"
    :class="{ 'is-cover': isCoverFit }"
    :src="displaySrc"
    :preview-src-list="displayPreviewList"
    :initial-index="resolvedInitialIndex"
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
      <slot name="placeholder">
        <div class="cat2bug-image-loading" aria-busy="true">
          <cat2-bug-loading
            class="cat2bug-image-loading__inner"
            :size="loadingSize"
            :text="loadingText"
          />
        </div>
      </slot>
    </template>
  </el-image>
</template>

<script>
import { DEFAULT_IMAGE } from '@/utils/upload-asset'
import Cat2BugLoading from '@/components/Cat2BugLoading'
import ImageErrorSlot from './ImageErrorSlot.vue'

export default {
  name: 'Cat2BugImage',
  components: { Cat2BugLoading, ImageErrorSlot },
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
    /** 点击预览时打开的起始下标（悬停列表点第 N 张应对应第 N 张） */
    initialIndex: {
      type: Number,
      default: undefined
    },
    previewTeleported: {
      type: Boolean,
      default: true
    },
    zIndex: {
      type: Number,
      default: 3000
    },
    /** 加载中奔跑猫高度（px）；缩略图场景宜 18～28 */
    loadingSize: {
      type: Number,
      default: 22
    },
    /** 传空字符串可隐藏文案，仅保留动画 */
    loadingText: {
      type: String,
      default: ''
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
    resolvedInitialIndex() {
      if (typeof this.initialIndex === 'number' && this.initialIndex >= 0) {
        return this.initialIndex
      }
      const fromAttr = this.$attrs['initial-index']
      if (typeof fromAttr === 'number' && fromAttr >= 0) {
        return fromAttr
      }
      const list = this.displayPreviewList
      if (list && list.length && this.displaySrc) {
        const idx = list.indexOf(this.displaySrc)
        if (idx > -1) return idx
        // 缩略图失败回退到 DEFAULT_IMAGE 时，仍按原始 src 定位
        const rawIdx = list.indexOf(this.src)
        if (rawIdx > -1) return rawIdx
      }
      return 0
    },
    isCoverFit() {
      const fit = this.$attrs.fit
      return fit === 'cover'
    },
    imageAttrs() {
      const attrs = { ...this.$attrs }
      Object.keys(attrs).forEach((key) => {
        if (key.startsWith('on') && typeof attrs[key] === 'function') {
          delete attrs[key]
        }
        // 已由 props / 显式绑定处理，避免重复
        if (key === 'initial-index' || key === 'preview-src-list' || key === 'src') {
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
.cat2bug-image-root {
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
.cat2bug-image-root.is-cover {
  :deep(.el-image__inner) {
    width: 100% !important;
    height: 100% !important;
    max-width: none;
    max-height: none;
    object-fit: cover;
  }
}
.cat2bug-image-error-trigger {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: zoom-in;
}
.cat2bug-image-loading {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
}
.cat2bug-image-loading__inner {
  gap: 0 !important;
  /* 隐藏默认「加载中」文案，缩略图只留奔跑猫 */
  :deep(.cat2bug-loading__label) {
    display: none;
  }
}
</style>
