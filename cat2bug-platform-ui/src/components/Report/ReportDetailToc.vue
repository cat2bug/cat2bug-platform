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
      <p v-if="!localHeadings.length" class="report-detail-toc-empty">{{ $t('report.toc-empty') }}</p>
      <draggable
        v-else
        v-model="localHeadings"
        class="report-detail-toc-list"
        item-key="id"
        tag="ul"
        :disabled="!reorderable"
        :animation="180"
        ghost-class="is-ghost"
        chosen-class="is-chosen"
        drag-class="is-drag"
        @change="onDragChange"
      >
        <template #item="{ element: item }">
          <li
            class="report-detail-toc-item"
            :class="[
              'is-level-' + Math.min(6, Math.max(1, item.level || 1)),
              {
                'is-active': item.id === activeId,
                'is-draggable': reorderable
              }
            ]"
            :style="{ paddingLeft: (Math.max(1, item.level || 1) - 1) * 12 + 8 + 'px' }"
            :title="item.text"
            @click="onSelect(item)"
          >{{ item.text || $t('report.toc') }}</li>
        </template>
      </draggable>
    </div>
  </aside>
</template>

<script>
import draggable from '@/adapters/vuedraggable'

export default {
  name: 'ReportDetailToc',
  components: { draggable },
  props: {
    headings: {
      type: Array,
      default: () => []
    },
    activeId: {
      type: [String, Number],
      default: null
    },
    /** 编辑态允许拖拽重排，并同步正文 */
    reorderable: {
      type: Boolean,
      default: false
    }
  },
  emits: ['select', 'toggle-collapse', 'reorder'],
  data() {
    return {
      localHeadings: [],
      suppressClick: false
    }
  },
  watch: {
    headings: {
      immediate: true,
      handler(val) {
        this.localHeadings = Array.isArray(val) ? val.slice() : []
      }
    }
  },
  methods: {
    onSelect(item) {
      if (this.suppressClick) {
        return
      }
      this.$emit('select', item.id)
    },
    onDragChange(evt) {
      if (!this.reorderable || !evt || !evt.moved) {
        return
      }
      const { oldIndex, newIndex } = evt.moved
      if (oldIndex === newIndex) {
        return
      }
      // 拖拽结束勿触发点击跳转
      this.suppressClick = true
      setTimeout(() => { this.suppressClick = false }, 0)
      this.$emit('reorder', {
        oldIndex,
        newIndex,
        headings: this.localHeadings.slice()
      })
    }
  }
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
  min-height: 8px;
}
.report-detail-toc-item {
  padding: 6px 8px;
  line-height: 1.4;
  color: var(--el-text-color-regular, #606266);
  border-radius: 4px;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 13px;
  font-weight: 400;

  &.is-draggable {
    cursor: grab;
  }
  &.is-draggable:active {
    cursor: grabbing;
  }

  /* 层级字号：越高层越大越醒目 */
  &.is-level-1 {
    font-size: 15px;
    font-weight: 700;
  }
  &.is-level-2 {
    font-size: 14px;
    font-weight: 600;
  }
  &.is-level-3 {
    font-size: 13px;
    font-weight: 500;
  }
  &.is-level-4 {
    font-size: 12px;
    font-weight: 400;
  }
  &.is-level-5,
  &.is-level-6 {
    font-size: 12px;
    font-weight: 400;
    opacity: 0.92;
  }

  &:hover {
    background-color: var(--el-fill-color-light, #f5f7fa);
    color: var(--el-color-primary, #409eff);
  }
  &.is-active {
    background-color: var(--el-color-primary-light-9, #ecf5ff);
    color: var(--el-color-primary, #409eff);
  }
  &.is-ghost {
    opacity: 0.45;
    background-color: var(--el-color-primary-light-9, #ecf5ff);
  }
  &.is-chosen {
    background-color: var(--el-fill-color, #f0f2f5);
  }
  &.is-drag {
    opacity: 0.95;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
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
    &.is-ghost {
      background-color: #333336;
    }
    &.is-chosen {
      background-color: #2a2a2c;
    }
  }
}
</style>
