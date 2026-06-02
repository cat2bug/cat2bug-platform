<template>
  <div @click="handleClickPreview" class="cat2bug-preview-image">
    <div v-if="imageCount==1">
      <cat2-bug-image
        v-for="(img,index) in images"
        :key="index"
        @click="handleClickPreview"
        @close="handleViewClose"
        :style="`width: 60px;height: 60px;`"
        :src="resolveImage(img, index)"
        class="click"
        :preview-src-list="resolvedPreviewList"
        fit="contain"
        @error="onImageError(index)"
      ></cat2-bug-image>
    </div>
    <el-popover
      v-else-if="imageCount>1"
      placement="left"
      trigger="hover"
      popper-class="cat2bug-preview-image-popover"
      >
      <div class="row-image">
        <cat2-bug-image
          v-for="(img,index) in images"
          :key="index"
          @click="handleClickPreview"
          @close="handleViewClose"
          :style="`width: ${width}px;height: ${height}px;`"
          :src="resolveImage(img, index)"
          class="click"
          :preview-src-list="resolvedPreviewList"
          fit="contain"
          @error="onImageError(index)"
        ></cat2-bug-image>
      </div>
      <div slot="reference" class="preview" :style="`width: 60px;height: 60px;`">
        <cat2-bug-image
          class="button"
          @click="handleClickPreview"
          style="width: 100%; height: 100%;"
          :src="previewUrl"
          :preview-src-list="resolvedPreviewList"
          fit="contain"
        ></cat2-bug-image>
        <span v-if="imageCount>1" class="number">+{{ imageCount }}</span>
      </div>
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
      default: 100
    },
    height: {
      type: Number,
      default: 100
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
      this.$set(this.failedIndexes, index, true)
    },
    handleClickPreview(event){
      this.isView = true;
      event.stopPropagation();
    },
    handleViewClose() {
      this.isView = false;
      this.isPreview = true;
    },
  }
}
</script>

<style lang="scss" scoped>
::v-deep .el-image {
  background-color: var(--cat2bug-image-placeholder-bg, #f2f6fc);
  padding: 5px;
  border-radius: var(--cat2bug-border-radius, 4px);
  box-sizing: border-box;
}
.el-image:hover, .button:hover {
  background-color: var(--table-row-hover-bg, #e8f4ff);
}
.row-image {
  display: inline-flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 5px;
  .el-image:hover {
    z-index: 999;
    border: 1px solid #DCDFE6;
    box-shadow: 0 10px 20px rgba(0, 0, 0, 0.3); /* 鼠标滑过时显示阴影 */
  }
}
.preview {
  position: relative;
  .number {
    position: absolute;
    right: 5px;
    bottom: 5px;
    background-color: rgba(255,255,255,0.8);
    border-radius: 3px;
    padding: 3px;
    line-height: 15px;
  }
}
.click, button {
  cursor: pointer;
}
.click:hover {
  scale: 1.2;
}
.cat2bug-preview-image {
  ::v-deep .el-popover__reference-wrapper {
    display: inline-flex;
  }
}
</style>
