<template>
  <el-empty
    class="defect-custom-field-empty"
    :description="$t('no-data')"
    :image-size="imageSize"
  />
</template>

<script>
/** 缺陷属性「暂无数据」统一空状态图标尺寸 */
export const DEFECT_CUSTOM_FIELD_EMPTY_IMAGE_SIZE = 80

export function isDefectFieldDisplayEmpty(display) {
  return display == null || display === '' || (typeof display === 'string' && !String(display).trim())
}

export default {
  name: 'DefectCustomFieldEmpty',
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
  padding: 4px 0 0;

  ::v-deep .el-empty__image {
    margin-bottom: 10px;
  }

  ::v-deep .el-empty__description {
    margin-top: 0;
    line-height: 1.5;
  }
}
</style>
