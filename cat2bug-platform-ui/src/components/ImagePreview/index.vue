<template>
  <cat2-bug-image
    :src="realSrc"
    fit="cover"
    :style="`width:${realWidth};height:${realHeight};`"
    :preview-src-list="realSrcList"
  />
</template>

<script>
import { isExternal } from "@/utils/validate";
import Cat2BugImage from '@/components/Cat2BugImage'
import { DEFAULT_IMAGE, resolveUploadUrl } from '@/utils/upload-asset'

export default {
  name: "ImagePreview",
  components: { Cat2BugImage },
  props: {
    src: {
      type: String,
      default: ""
    },
    width: {
      type: [Number, String],
      default: ""
    },
    height: {
      type: [Number, String],
      default: ""
    }
  },
  computed: {
    realSrc() {
      if (!this.src) {
        return DEFAULT_IMAGE;
      }
      let real_src = this.src.split(",")[0];
      if (isExternal(real_src)) {
        return real_src;
      }
      return resolveUploadUrl(real_src);
    },
    realSrcList() {
      if (!this.src) {
        return [DEFAULT_IMAGE];
      }
      let real_src_list = this.src.split(",");
      let srcList = [];
      real_src_list.forEach(item => {
        if (isExternal(item)) {
          return srcList.push(item);
        }
        return srcList.push(resolveUploadUrl(item));
      });
      return srcList;
    },
    realWidth() {
      return typeof this.width == "string" ? this.width : `${this.width}px`;
    },
    realHeight() {
      return typeof this.height == "string" ? this.height : `${this.height}px`;
    }
  },
};
</script>

<style lang="scss" scoped>
::v-deep .el-image {
  border-radius: 5px;
  background-color: var(--cat2bug-image-placeholder-bg, #ebeef5);
  box-shadow: 0 0 5px 1px #ccc;
  .el-image__inner {
    transition: all 0.3s;
    cursor: pointer;
    &:hover {
      transform: scale(1.2);
    }
  }
}
</style>
