<template>
  <div :class="errorSlotClass">
    <img :src="placeholderSrc" :class="errorImgClass" alt="" />
  </div>
</template>

<script>
import {
  DEFAULT_IMAGE,
  IMAGE_ERROR_IMG_CLASS,
  IMAGE_ERROR_SLOT_CLASS
} from '@/utils/upload-asset'

/** 全系统 el-image #error 槽统一占位（与 upload-asset / 主题变量配套） */
export default {
  name: 'Cat2BugImageErrorSlot',
  computed: {
    placeholderSrc() {
      return DEFAULT_IMAGE
    },
    errorSlotClass() {
      return IMAGE_ERROR_SLOT_CLASS
    },
    errorImgClass() {
      return IMAGE_ERROR_IMG_CLASS
    }
  }
}
</script>
