<template>
  <div @click="handleClickPreview">
    <div v-if="imageCount==1">
      <el-image
        v-for="(img,index) in images"
        :key="index"
        @click="handleClickPreview"
        @close="handleViewClose"
        :style="`width: ${width}px;height: ${height}px;`"
        :src="img"
        class="click"
        :preview-src-list="images"
        fit="contain"
      ></el-image>
    </div>
    <el-popover
      v-else-if="imageCount>1"
      placement="left"
      trigger="hover"
      >
      <div class="row-image">
        <el-image
          v-for="(img,index) in images"
          :key="index"
          @click="handleClickPreview"
          @close="handleViewClose"
          :style="`width: ${width}px;height: ${height}px;`"
          :src="img"
          class="click"
          :preview-src-list="images"
          fit="contain"
        ></el-image>
      </div>
      <div slot="reference" class="preview click" :style="`width: ${width}px;height: ${height}px;`">
        <el-image
          @click="handleClickPreview"
          style="width: 100%; height: 100%"
          :src="previewUrl"
          :preview-src-list="images"
          fit="contain"
        ></el-image>
        <span v-if="imageCount>1" class="number">+{{ imageCount }}</span>
      </div>
    </el-popover>
  </div>
</template>

<script>
export default {
  name: "Cat2BugPreviewImage",
  data() {
    return {
      isPreview: true,
      isView: false,
    }
  },
  props: {
    images: {
      type: Array,
      default: ()=>[]
    },
    width: {
      type: Number,
      default: 60
    },
    height: {
      type: Number,
      default: 60
    }
  },
  computed: {
    previewUrl: function () {
      return  this.images.length>0?this.images[0]:'';
    },
    imageCount: function () {
      return this.images.length;
    }
  },
  methods: {
    handleClickPreview(event){
      this.isView = true;
      event.stopPropagation();
    },
    handleViewClose() {
      this.isView = false;
      this.isPreview = true;
      alert('1')
    },
  }
}
</script>

<style lang="scss" scoped>
.row-image {
  display: inline-flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 5px;
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
.click {
  cursor: pointer;
}
.click:hover {
  scale: 1.2;
}
</style>
