<template>
  <div class="app-container case-review-detail-page">
    <project-label />
    <el-row class="case-review-detail-header">
      <el-page-header :title="$t('back')" @back="goBackList" :content="pageTitle" />
    </el-row>

    <el-alert v-if="loadError" type="warning" :closable="false" show-icon :title="loadError" />

    <template v-if="review && !loadError">
      <el-descriptions :column="2" border size="small" class="case-review-detail-meta">
        <el-descriptions-item :label="$t('case.review.filter-status')">
          <el-tag :type="statusTagType(review.status)" size="small">{{ statusLabel(review.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('case.review.summary-col')">{{ formatSummary(review) }}</el-descriptions-item>
        <el-descriptions-item :label="$t('createBy')">{{ review.createBy || '—' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('case.review.create-time')">{{ parseTime(review.createTime) }}</el-descriptions-item>
        <el-descriptions-item :label="$t('case.review.complete-by')">{{ review.completeBy || '—' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('case.review.complete-time')">{{ parseTime(review.completeTime) }}</el-descriptions-item>
      </el-descriptions>

      <div class="case-review-detail-actions">
        <el-button
          v-if="review.status === 'open'"
          v-hasPermi="['system:case:review']"
          type="primary"
          size="small"
          @click="handleContinue"
        >{{ $t('case.review.continue') }}</el-button>
        <el-dropdown trigger="click" @command="handleExport">
          <el-button size="small" :disabled="!canExport">{{ $t('case.review.export') }}</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="md" :disabled="!canExport">{{ $t('case.review.export-md') }}</el-dropdown-item>
              <el-dropdown-item command="doc" :disabled="!canExport">{{ $t('case.review.export-doc') }}</el-dropdown-item>
              <el-dropdown-item command="pdf" :disabled="!canExport">{{ $t('case.review.export-pdf') }}</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <h4 class="case-review-detail-section-title">{{ $t('case.review.step-cases') }}</h4>
      <el-table v-loading="loading" :data="review.items || []" size="small" border>
        <el-table-column :label="$t('id')" prop="caseNum" width="90" />
        <el-table-column :label="$t('title')" prop="caseName" min-width="160" />
        <el-table-column :label="$t('case.type')" prop="caseTypeName" width="120" />
        <el-table-column :label="$t('case.review.verdict')" width="120">
          <template #default="scope">
            {{ verdictLabel(scope.row.verdict) }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('case.review.comment')" prop="comment" min-width="180" show-overflow-tooltip />
      </el-table>

      <template v-if="review.status === 'completed' && reportMarkdown">
        <h4 class="case-review-detail-section-title">{{ $t('case.review.step-report') }}</h4>
        <div class="case-review-report-preview markdown-body">
          <markdown-content :content="reportMarkdown" />
        </div>
      </template>
    </template>

    <case-review-dialog ref="caseReviewDialog" @changed="onReviewChanged" />
  </div>
</template>

<script>
import ProjectLabel from '@/components/Project/ProjectLabel'
import CaseReviewDialog from '@/components/Case/CaseReviewDialog.vue'
import MarkdownContent from '@/components/Markdown/MarkdownContent.vue'
import { getCaseReview } from '@/api/system/case-review'
import { buildProjectModulePath } from '@/utils/project-module-path'
import {
  canExportCaseReview,
  exportCaseReviewDoc,
  exportCaseReviewMarkdown,
  exportCaseReviewPdf
} from '@/utils/case-review-export'
import { appMessageError } from '@/utils/app-message'

export default {
  name: 'CaseReviewDetail',
  components: { ProjectLabel, CaseReviewDialog, MarkdownContent },
  data() {
    return {
      loading: false,
      loadError: '',
      review: null,
      reportMarkdown: ''
    }
  },
  computed: {
    reviewId() {
      return this.$route.params.reviewId
    },
    pageTitle() {
      if (!this.reviewId) return this.$t('case.review.detail')
      return `${this.$t('case.review.detail')} #${this.reviewId}`
    },
    canExport() {
      return canExportCaseReview(this.review)
    }
  },
  watch: {
    reviewId: {
      handler() {
        this.loadDetail()
      },
      immediate: true
    }
  },
  methods: {
    goBackList() {
      const { teamKey, projectKey } = this.$route.params
      const path = buildProjectModulePath(teamKey, projectKey, 'case', 'reviews')
      if (path) this.$router.push(path)
    },
    loadDetail() {
      this.loadError = ''
      this.review = null
      if (!this.reviewId) {
        this.loadError = this.$t('case.review.not-found')
        return
      }
      this.loading = true
      getCaseReview(this.reviewId)
        .then(res => {
          const data = res.data || {}
          if (!data.reviewId) {
            this.loadError = this.$t('case.review.not-found')
            return
          }
          this.review = data
          this.reportMarkdown = data.reportMarkdown || ''
        })
        .catch(err => {
          const status = err && err.response && err.response.status
          if (status === 404 || status === 403) {
            this.loadError = this.$t('case.review.not-found')
          } else {
            this.loadError = this.$t('case.review.load-failed')
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    formatSummary(row) {
      if (!row) return '—'
      const items = Array.isArray(row.items) ? row.items : []
      const countVerdict = (v) => items.filter(i => i.verdict === v).length
      return this.$t('case.review.summary-stats', {
        total: row.itemCount ?? items.length,
        passed: row.passedCount ?? countVerdict('passed'),
        changes: row.changesCount ?? countVerdict('changes'),
        pending: row.pendingCount ?? countVerdict('pending')
      })
    },
    statusLabel(status) {
      if (status === 'completed') return this.$t('case.review.status-completed')
      if (status === 'open') return this.$t('case.review.status-open')
      return status || '—'
    },
    statusTagType(status) {
      if (status === 'completed') return 'success'
      if (status === 'open') return 'warning'
      return 'info'
    },
    verdictLabel(verdict) {
      if (verdict === 'passed') return this.$t('case.review.passed')
      if (verdict === 'changes') return this.$t('case.review.changes')
      if (verdict === 'pending') return this.$t('case.review.pending')
      return verdict || '—'
    },
    handleContinue() {
      const dialog = this.$refs.caseReviewDialog
      if (!dialog || typeof dialog.openByReviewId !== 'function') return
      dialog.openByReviewId(this.reviewId).then(opened => {
        if (!opened) {
          appMessageError(this.$t('case.review.not-found'))
        }
      })
    },
    onReviewChanged() {
      this.loadDetail()
    },
    async handleExport(format) {
      if (!canExportCaseReview(this.review)) return
      try {
        if (format === 'md') exportCaseReviewMarkdown(this.review)
        else if (format === 'doc') await exportCaseReviewDoc(this.review)
        else if (format === 'pdf') exportCaseReviewPdf(this.review)
      } catch (e) {
        appMessageError(this.$t('case.review.export-failed'))
      }
    }
  }
}
</script>

<style scoped lang="scss">
.case-review-detail-header {
  margin-bottom: var(--project-list-section-v-gap, 10px);
}
.case-review-detail-meta {
  margin-bottom: var(--project-list-section-v-gap, 10px);
}
.case-review-detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: var(--project-list-section-v-gap, 10px);
}
.case-review-detail-section-title {
  margin: 12px 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-color-primary);
}
.case-review-report-preview {
  padding: 16px;
  border: 1px solid var(--border-color-light);
  border-radius: 4px;
  background: var(--bg-color-block);
  color: var(--text-color-primary);
}
</style>
