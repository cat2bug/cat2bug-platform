<template>
  <aside class="report-detail-toc">
    <div class="report-detail-toc-header">
      <span class="report-detail-toc-title">{{ $t('report.toc') }}</span>
      <el-button
        class="report-detail-toc-toggle"
        link
        size="small"
        :icon="$legacyIcon('el-icon-d-arrow-left')"
        :title="$t('report.collapse-toc')"
        @click="$emit('toggle-collapse')"
      ></el-button>
    </div>
    <div class="report-detail-toc-body">
      <p v-if="!headings.length" class="report-detail-toc-empty">{{ $t('report.toc-empty') }}</p>
      <ul v-else class="report-detail-toc-list">
        <li
          v-for="item in headings"
          :key="item.id"
          class="report-detail-toc-item"
          :class="{ 'is-active': item.id === activeId }"
          :style="{ paddingLeft: (item.level - 1) * 12 + 8 + 'px' }"
          :title="item.text"
          @click="$emit('select', item.id)"
        >{{ item.text || $t('report.toc') }}</li>
      </ul>
    </div>
  </aside>
</template>

<script>
export default {
  name: 'ReportDetailToc',
  props: {
    headings: {
      type: Array,
      default: () => []
    },
    activeId: {
      type: [String, Number],
      default: null
    }
  },
  emits: ['select', 'toggle-collapse']
}
</script>

<style lang="scss" scoped>
.report-detail-toc {
  /* 覆盖全局 aside { margin-bottom/padding/radius }，否则目录栏无法贴底 */
  width: 220px;
  min-width: 0;
  flex-shrink: 0;
  align-self: stretch;
  box-sizing: border-box;
  overflow-y: auto;
  margin: 0;
  padding: 0;
  border-radius: 0;
  line-height: normal;
  font-size: inherit;
  font-family: inherit;
  border-right: 1px solid var(--el-border-color, #dcdfe6);
  background-color: var(--el-fill-color-blank, #fafafa);
  color: var(--el-text-color-primary, #303133);
}
.report-detail-toc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  position: sticky;
  top: 0;
  background-color: inherit;
  z-index: 1;
}
.report-detail-toc-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-secondary, #909399);
}
.report-detail-toc-body {
  padding: 0 4px 12px;
}
.report-detail-toc-empty {
  padding: 8px 12px;
  font-size: 12px;
  color: var(--el-text-color-placeholder, #c0c4cc);
}
.report-detail-toc-list {
  margin: 0;
  padding: 0;
  list-style: none;
}
.report-detail-toc-item {
  padding: 6px 8px;
  font-size: 13px;
  line-height: 1.4;
  color: var(--el-text-color-regular, #606266);
  border-radius: 4px;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;

  &:hover {
    background-color: var(--el-fill-color-light, #f5f7fa);
    color: var(--el-color-primary, #409eff);
  }
  &.is-active {
    background-color: var(--el-color-primary-light-9, #ecf5ff);
    color: var(--el-color-primary, #409eff);
    font-weight: 600;
  }
}

html.dark .report-detail-toc {
  background-color: #1e1e20;
  border-right-color: #3a3a3e;
  color: #e5eaf3;

  .report-detail-toc-title {
    color: #a3a6ad;
  }
  .report-detail-toc-empty {
    color: #6b6e76;
  }
  .report-detail-toc-item {
    color: #c8ccd4;

    &:hover {
      background-color: #2a2a2c;
      color: var(--crepe-color-primary, #f4bd6f);
    }
    &.is-active {
      background-color: #333336;
      color: var(--crepe-color-primary, #f4bd6f);
    }
  }
}
</style>
