<template>
  <div class="cat2bug-preview-image" :class="{ 'is-cover': fit === 'cover' }" @click.stop>
    <div v-if="imageCount==1">
      <cat2-bug-image
        v-for="(img,index) in images"
        :key="index"
        @close="handleViewClose"
        :style="`width: 60px;height: 60px;`"
        :src="resolveImage(img, index)"
        class="click"
        :preview-src-list="resolvedPreviewList"
        :initial-index="index"
        :fit="fit"
        @error="onImageError(index)"
      ></cat2-bug-image>
    </div>
    <el-popover
      v-else-if="imageCount>1"
      placement="left"
      trigger="hover"
      width="auto"
      popper-class="cat2bug-preview-image-popover"
      >
      <div class="row-image">
        <cat2-bug-image
          v-for="(img,index) in images"
          :key="index"
          @close="handleViewClose"
          :style="`width: ${width}px;height: ${height}px;`"
          :src="resolveImage(img, index)"
          class="click"
          :preview-src-list="resolvedPreviewList"
          :initial-index="index"
          :fit="fit"
          @error="onImageError(index)"
        ></cat2-bug-image>
      </div>
      <template #reference>
        <div class="preview" :style="`width: 60px;height: 60px;`">
          <cat2-bug-image
            class="button"
            style="width: 100%; height: 100%;"
            :src="previewUrl"
            :preview-src-list="resolvedPreviewList"
            :initial-index="0"
            :fit="fit"
          ></cat2-bug-image>
          <span v-if="imageCount>1" class="number">+{{ imageCount }}</span>
        </div>
      </template>
    </el-popover>
  </div>
</template>

<script>
import Cat2BugImage from '@/components/Cat2BugImage'
import { DEFAULT_IMAGE } from '@/utils/upload-asset'

export default {
  name: "Cat2BugPreviewImage",
  components: { Cat2BugImage },
  data() {
    return {
      isPreview: true,
      isView: false,
      failedIndexes: {}
    }
  },
  props: {
    images: {
      type: Array,
      default: ()=>[]
    },
    width: {
      type: Number,
      default: 52
    },
    height: {
      type: Number,
      default: 52
    },
    fit: {
      type: String,
      default: 'cover'
    }
  },
  watch: {
    images: {
      handler() {
        this.failedIndexes = {}
      },
      deep: true
    }
  },
  computed: {
    previewUrl: function () {
      if (!this.images.length) {
        return DEFAULT_IMAGE
      }
      return this.resolveImage(this.images[0], 0)
    },
    imageCount: function () {
      return this.images.length;
    },
    resolvedPreviewList() {
      return this.images.map((img, index) => this.resolveImage(img, index))
    }
  },
  methods: {
    resolveImage(url, index) {
      if (this.failedIndexes[index]) {
        return DEFAULT_IMAGE
      }
      return url
    },
    onImageError(index) {
      this.failedIndexes = { ...this.failedIndexes, [index]: true }
    },
    handleViewClose() {
      this.isView = false;
      this.isPreview = true;
    },
  }
}
</script>

<style lang="scss" scoped>
.cat2bug-preview-image {
  :deep(.el-image) {
    background-color: var(--cat2bug-image-placeholder-bg, #f2f6fc);
    padding: 5px;
    border-radius: var(--cat2bug-border-radius, 4px);
    box-sizing: border-box;
  }
  &.is-cover :deep(.el-image) {
    padding: 0;
  }
  :deep(.el-popover__reference-wrapper) {
    display: inline-flex;
  }
}
.el-image:hover, .button:hover {
  background-color: var(--table-row-hover-bg, #e8f4ff);
}
.row-image {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  line-height: 0;
  font-size: 0;
  .el-image:hover {
    z-index: 999;
    border: 1px solid #DCDFE6;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.18);
  }
}
.preview {
  position: relative;
  overflow: visible;
  .number {
    position: absolute;
    right: 4px;
    bottom: 4px;
    z-index: 2;
    min-width: 18px;
    height: 18px;
    padding: 0 5px;
    border-radius: 3px;
    font-size: 12px;
    font-weight: 600;
    line-height: 18px;
    text-align: center;
    color: #fff;
    background-color: rgba(0, 0, 0, 0.62);
    pointer-events: none;
    box-sizing: border-box;
  }
}
.click, button {
  cursor: pointer;
}
.click:hover {
  /* 用 transform，避免 scale 属性撑大布局导致底部空隙偏大 */
  transform: scale(1.08);
  transform-origin: center center;
}
</style>
