<template>
  <cat2-bug-empty
    class="defect-custom-field-empty"
    :description="$t('no-data')"
    :image-size="imageSize"
  />
</template>

<script>
import Cat2BugEmpty from '@/components/Cat2BugEmpty'

/** 缺陷属性「暂无数据」统一空状态图标尺寸（图片/附件风箱内要够醒目） */
export const DEFECT_CUSTOM_FIELD_EMPTY_IMAGE_SIZE = 148

export function isDefectFieldDisplayEmpty(display) {
  return display == null || display === '' || (typeof display === 'string' && !String(display).trim())
}

export default {
  name: 'DefectCustomFieldEmpty',
  components: { Cat2BugEmpty },
  props: {
    imageSize: {
      type: Number,
      default: DEFECT_CUSTOM_FIELD_EMPTY_IMAGE_SIZE
    }
  }
}
</script>

<style lang="scss" scoped>
.defect-custom-field-empty {
  padding: 12px 0 8px;
}
</style>
