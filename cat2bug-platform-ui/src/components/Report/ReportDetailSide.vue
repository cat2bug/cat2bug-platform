<template>
  <aside class="report-detail-side">
    <div class="report-detail-side-header">
      <el-tabs v-model="activeTab" class="report-detail-side-tabs" @tab-change="onTabChange">
        <el-tab-pane :label="$t('report.comments')" name="comments" />
        <el-tab-pane :label="$t('report.history')" name="history" />
      </el-tabs>
      <el-button
        class="report-detail-side-toggle"
        link
        size="small"
        :icon="$legacyIcon('el-icon-d-arrow-right')"
        :title="$t('report.collapse-comments')"
        @click="$emit('toggle-collapse')"
      ></el-button>
    </div>

    <report-detail-comments
      ref="commentsPanel"
      v-show="activeTab === 'comments'"
      embedded
      :threads="threads"
      :active-thread-id="activeThreadId"
      :draft="draft"
      :draft-top="draftTop"
      :thread-tops="threadTops"
      :canvas-min-height="commentCanvasHeight"
      :current-user-id="currentUserId"
      :can-manage="canManage"
      :loading="commentsLoading"
      @focus-thread="$emit('focus-thread', $event)"
      @submit-draft="$emit('submit-draft', $event)"
      @cancel-draft="$emit('cancel-draft')"
      @reply="$emit('reply', $event)"
      @delete="$emit('delete', $event)"
      @scroll="$emit('comments-scroll', $event)"
      @layout-request="$emit('layout-request')"
    />

    <report-detail-history
      v-show="activeTab === 'history'"
      :items="historyItems"
      :can-operate="canEditHistory"
      :loading="historyLoading"
      @restore="$emit('history-restore', $event)"
      @delete="$emit('history-delete', $event)"
    />
  </aside>
</template>

<script>
import ReportDetailComments from '@/components/Report/ReportDetailComments.vue'
import ReportDetailHistory from '@/components/Report/ReportDetailHistory.vue'

export default {
  name: 'ReportDetailSide',
  components: { ReportDetailComments, ReportDetailHistory },
  props: {
    threads: { type: Array, default: () => [] },
    activeThreadId: { type: [String, Number], default: null },
    draft: { type: Object, default: null },
    currentUserId: { type: [String, Number], default: null },
    canManage: { type: Boolean, default: false },
    canEditHistory: { type: Boolean, default: false },
    commentsLoading: { type: Boolean, default: false },
    threadTops: { type: Object, default: () => ({}) },
    commentCanvasHeight: { type: Number, default: 0 },
    draftTop: { type: Number, default: null },
    historyItems: { type: Array, default: () => [] },
    historyLoading: { type: Boolean, default: false },
    sideTab: { type: String, default: 'comments' }
  },
  emits: [
    'toggle-collapse',
    'focus-thread',
    'submit-draft',
    'cancel-draft',
    'reply',
    'delete',
    'history-restore',
    'history-delete',
    'update:sideTab',
    'comments-scroll',
    'layout-request'
  ],
  computed: {
    activeTab: {
      get() {
        return this.sideTab || 'comments'
      },
      set(val) {
        this.$emit('update:sideTab', val)
      }
    }
  },
  methods: {
    onTabChange(name) {
      this.$emit('update:sideTab', name)
    },
    getCommentsScrollElement() {
      const panel = this.$refs.commentsPanel
      return panel && typeof panel.getScrollElement === 'function' ? panel.getScrollElement() : null
    }
  }
}
</script>

<style lang="scss" scoped>
.report-detail-side {
  /* 覆盖全局 aside { margin-bottom/padding/radius }，否则侧栏无法贴底 */
  width: 320px;
  min-width: 0;
  flex-shrink: 0;
  align-self: stretch;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  margin: 0;
  padding: 0;
  border-radius: 0;
  line-height: normal;
  font-size: inherit;
  font-family: inherit;
  border-left: 1px solid var(--el-border-color, #dcdfe6);
  background-color: var(--el-fill-color-blank, #fafafa);
  color: var(--el-text-color-primary, #303133);
}
.report-detail-side-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 8px 10px 12px;
  position: sticky;
  top: 0;
  background-color: inherit;
  z-index: 1;
  flex-shrink: 0;
}
.report-detail-side-tabs {
  flex: 1;
  min-width: 0;

  :deep(.el-tabs__header) {
    margin: 0;
  }
  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }
  :deep(.el-tabs__item) {
    height: 36px;
    line-height: 36px;
    font-size: 13px;
    padding: 0 12px;
  }
}
.report-detail-side-toggle {
  flex-shrink: 0;
  margin-left: 4px;
}

html.dark .report-detail-side {
  background-color: #1e1e20;
  border-left-color: #3a3a3e;
  color: #e5eaf3;
}
</style>
