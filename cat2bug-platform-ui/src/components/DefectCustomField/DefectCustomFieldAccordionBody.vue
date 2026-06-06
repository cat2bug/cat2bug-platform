<template>
  <div class="defect-custom-field-accordion-body">
    <div v-if="row.fieldType === 'image' && row.urls.length" class="defect-image-tiles">
      <cat2-bug-image
        v-for="(img, index) in row.urls"
        :key="index"
        class="defect-image-tile"
        :src="img"
        :preview-src-list="row.urls"
        fit="cover"
      />
    </div>
    <defect-custom-field-empty v-else-if="row.fieldType === 'image'" />
    <div v-else-if="row.fieldType === 'object' && !isObjectEmpty(row)" class="defect-custom-field-object">
      <el-tooltip
        v-if="row.displayAsDeliverablePath && row.fullPath"
        :content="row.fullPath"
        placement="top"
        :disabled="row.fullPath === row.display"
      >
        <span class="deliverable-path-text">{{ row.display }}</span>
      </el-tooltip>
      <pre v-else class="defect-custom-field-json">{{ row.display }}</pre>
    </div>
    <defect-custom-field-empty v-else-if="row.fieldType === 'object'" />
    <div v-else-if="row.fieldType === 'file' && row.urls.length" class="annex-list">
      <cat2-bug-text
        v-for="(file, index) in row.urls"
        :key="index"
        class="annex-list-item"
        :content="file"
        type="down"
        :tooltip="file"
      />
    </div>
    <defect-custom-field-empty v-else-if="row.fieldType === 'file'" />
  </div>
</template>

<script>
import Cat2BugImage from '@/components/Cat2BugImage'
import Cat2BugText from '@/components/Cat2BugText'
import DefectCustomFieldEmpty, { isDefectFieldDisplayEmpty } from '@/components/DefectCustomField/DefectCustomFieldEmpty'

export default {
  name: 'DefectCustomFieldAccordionBody',
  components: { Cat2BugImage, Cat2BugText, DefectCustomFieldEmpty },
  props: {
    row: { type: Object, required: true }
  },
  methods: {
    isObjectEmpty(row) {
      return isDefectFieldDisplayEmpty(row && row.display)
    }
  }
}
</script>

<style lang="scss" scoped>
.defect-custom-field-accordion-body {
  padding-bottom: 16px;
}

.deliverable-path-text {
  cursor: default;
  word-break: break-all;
}
.defect-custom-field-json {
  margin: 0;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
.annex-list {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;

  ::v-deep .annex-list-item {
    display: block;
    width: 100%;
    border-bottom: none !important;
  }

  ::v-deep .annex-list-item:last-child {
    border-bottom: none !important;
    margin-bottom: 0;
    padding-bottom: 0;
  }
}

.defect-image-tiles {
  display: inline-flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 10px;
}

.defect-image-tile {
  display: block;
  width: 150px;
  height: 150px;
  border-radius: var(--cat2bug-border-radius, 4px);
  overflow: hidden;
  border: 1px solid var(--border-color-light, #ebeef5);
  box-sizing: border-box;
  background-color: var(--cat2bug-image-placeholder-bg, #ebeef5);

  ::v-deep .el-image__inner,
  ::v-deep .el-image__placeholder,
  ::v-deep .el-image__error {
    width: 100% !important;
    height: 100% !important;
    border-radius: inherit;
    object-fit: cover !important;
    object-position: center center;
  }

  ::v-deep .cat2bug-image-error {
    border-radius: inherit;
    overflow: hidden;
  }
}
</style>
