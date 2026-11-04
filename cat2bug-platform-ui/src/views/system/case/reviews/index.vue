<template>
  <div class="app-container case-review-history-page project-list-page-host">
    <project-label />
    <el-row class="case-review-history-header">
      <el-page-header :title="$t('back')" @back="goBackCase" :content="$t('case.review.history')" />
    </el-row>

    <div class="list-page-toolbar">
      <div class="list-page-toolbar__filters">
        <el-select
          v-model="queryParams.status"
          class="case-review-filter-status"
          size="small"
          clearable
          :placeholder="$t('case.review.filter-status')"
          style="width: 160px"
          @change="handleQuery"
        >
          <template #prefix>
            <legacy-icon icon="el-icon-finished" class="select-header-icon" />
          </template>
          <el-option :label="$t('all')" value="" />
          <el-option :label="$t('case.review.status-open')" value="open" />
          <el-option :label="$t('case.review.status-completed')" value="completed" />
        </el-select>
        <el-date-picker
          v-model="createTimeRange"
          type="daterange"
          size="small"
          value-format="YYYY-MM-DD"
          :start-placeholder="$t('case.review.create-time-begin')"
          :end-placeholder="$t('case.review.create-time-end')"
          style="width: 260px"
          @change="onCreateTimeRangeChange"
        />
        <el-input
          v-model="queryParams.createBy"
          size="small"
          clearable
          :placeholder="$t('case.review.filter-creator')"
          style="width: 180px"
          @input="handleQuery"
        >
          <template #prefix>
            <svg-icon icon-class="user" />
          </template>
        </el-input>
      </div>
    </div>

    <cat2-bug-table
      ref="table"
      :data="reviewList"
      :loading="loading"
      :columns="tableColumns"
      cache-key="case-review-history-table"
    >
      <template #columns="{ scope, column }">
        <span v-if="column.prop === 'status'">
          <el-tag :type="statusTagType(scope.row.status)" size="small">{{ statusLabel(scope.row.status) }}</el-tag>
        </span>
        <span v-else-if="column.prop === 'summary'">{{ formatSummary(scope.row) }}</span>
        <span v-else-if="column.prop === 'createTime'">{{ parseTime(scope.row.createTime) }}</span>
        <span v-else-if="column.prop === 'completeTime'">{{ parseTime(scope.row.completeTime) }}</span>
        <span v-else-if="column.prop === 'actions'">
          <el-button size="small" link type="primary" @click="goDetail(scope.row)">{{ $t('case.review.view') }}</el-button>
          <el-dropdown
            trigger="click"
            @command="cmd => handleExport(cmd, scope.row)"
          >
            <el-button
              size="small"
              link
              :disabled="!canExportRow(scope.row)"
            >{{ $t('case.review.export') }}</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="md" :disabled="!canExportRow(scope.row)">{{ $t('case.review.export-md') }}</el-dropdown-item>
                <el-dropdown-item command="doc" :disabled="!canExportRow(scope.row)">{{ $t('case.review.export-doc') }}</el-dropdown-item>
                <el-dropdown-item command="pdf" :disabled="!canExportRow(scope.row)">{{ $t('case.review.export-pdf') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </span>
        <span v-else>{{ scope.row[column.prop] }}</span>
      </template>
    </cat2-bug-table>

    <cat2-bug-empty v-if="!loading && !reviewList.length" :description="$t('case.review.history-empty')" />

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script>
import ProjectLabel from '@/components/Project/ProjectLabel'
import Cat2BugTable from '@/components/Cat2BugTable'
import Cat2BugEmpty from '@/components/Cat2BugEmpty'
import { extractCaseReviewList, getCaseReview, listCaseReviews } from '@/api/system/case-review'
import { buildProjectModulePath } from '@/utils/project-module-path'
import {
  canExportCaseReview,
  exportCaseReviewDoc,
  exportCaseReviewMarkdown,
  exportCaseReviewPdf
} from '@/utils/case-review-export'
import { appMessageError } from '@/utils/app-message'

const TABLE_COLUMNS = [
  { prop: 'reviewId', label: 'id', width: 90, show: true },
  { prop: 'status', label: 'case.review.filter-status', width: 110, show: true },
  { prop: 'summary', label: 'case.review.summary-col', minWidth: 220, show: true },
  { prop: 'createBy', label: 'createBy', width: 120, show: true },
  { prop: 'createTime', label: 'case.review.create-time', width: 170, show: true },
  { prop: 'completeBy', label: 'case.review.complete-by', width: 120, show: true },
  { prop: 'completeTime', label: 'case.review.complete-time', width: 170, show: true },
  { prop: 'actions', label: 'operate', width: 160, fixed: 'right', show: true, disableSort: true }
]

export default {
  name: 'CaseReviewHistory',
  components: { ProjectLabel, Cat2BugTable, Cat2BugEmpty },
  data() {
    return {
      loading: false,
      reviewList: [],
      total: 0,
      createTimeRange: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        status: '',
        createBy: '',
        createTimeBegin: '',
        createTimeEnd: ''
      },
      tableColumns: TABLE_COLUMNS
    }
  },
  computed: {
    projectId() {
      return parseInt((this.userStore.config || {}).currentProjectId)
    }
  },
  watch: {
    projectId: {
      handler(val) {
        if (val) {
          this.handleQuery()
        }
      },
      immediate: true
    }
  },
  methods: {
    goBackCase() {
      const { teamKey, projectKey } = this.$route.params
      const path = buildProjectModulePath(teamKey, projectKey, 'case')
      if (path) this.$router.push(path)
    },
    onCreateTimeRangeChange(val) {
      if (val && val.length === 2) {
        this.queryParams.createTimeBegin = val[0]
        this.queryParams.createTimeEnd = val[1]
      } else {
        this.queryParams.createTimeBegin = ''
        this.queryParams.createTimeEnd = ''
      }
      this.handleQuery()
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    getList() {
      if (!this.projectId) return
      this.loading = true
      const params = {
        pageNum: this.queryParams.pageNum,
        pageSize: this.queryParams.pageSize
      }
      if (this.queryParams.status) params.status = this.queryParams.status
      if (this.queryParams.createBy) params.createBy = this.queryParams.createBy
      if (this.queryParams.createTimeBegin) params.createTimeBegin = this.queryParams.createTimeBegin
      if (this.queryParams.createTimeEnd) params.createTimeEnd = this.queryParams.createTimeEnd

      listCaseReviews(this.projectId, params)
        .then(res => {
          const { rows, total } = extractCaseReviewList(res)
          this.reviewList = rows
          this.total = total
        })
        .catch(() => {
          this.reviewList = []
          this.total = 0
        })
        .finally(() => {
          this.loading = false
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
    formatSummary(row) {
      return this.$t('case.review.summary-stats', {
        total: row.itemCount ?? 0,
        passed: row.passedCount ?? 0,
        changes: row.changesCount ?? 0,
        pending: row.pendingCount ?? 0
      })
    },
    canExportRow(row) {
      return canExportCaseReview(row)
    },
    goDetail(row) {
      if (!row || row.reviewId == null) return
      const { teamKey, projectKey } = this.$route.params
      const path = buildProjectModulePath(teamKey, projectKey, 'case', 'reviews', String(row.reviewId))
      if (path) this.$router.push(path)
    },
    async handleExport(format, row) {
      let payload = row
      if (row && row.status === 'completed' && !canExportCaseReview(row) && row.reviewId != null) {
        try {
          const res = await getCaseReview(row.reviewId)
          payload = res.data || row
        } catch (_) {
          /* keep row */
        }
      }
      if (!canExportCaseReview(payload)) return
      try {
        if (format === 'md') exportCaseReviewMarkdown(payload)
        else if (format === 'doc') await exportCaseReviewDoc(payload)
        else if (format === 'pdf') exportCaseReviewPdf(payload)
      } catch (e) {
        appMessageError(this.$t('case.review.export-failed'))
      }
    }
  }
}
</script>

<style scoped lang="scss">
.case-review-history-header {
  margin-bottom: var(--project-list-section-v-gap, 10px);
}
.case-review-history-page {
  :deep(.list-page-toolbar) {
    margin-bottom: var(--project-list-section-v-gap, 10px);
  }
}
</style>
