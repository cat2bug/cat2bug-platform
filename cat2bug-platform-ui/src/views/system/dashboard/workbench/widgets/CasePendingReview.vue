<template>
  <div class="wb-list" v-loading="loading">
    <div v-if="!rows.length && !loading" class="wb-empty">{{ $t('workbench.empty-list') }}</div>
    <div v-for="row in rows" :key="row.reviewId || row.caseReviewId || rowTitle(row)" class="wb-row">
      <div class="wb-row__main" @click="goCase">
        <div class="wb-row__title">
          <span class="desc">{{ rowTitle(row) }}</span>
        </div>
      </div>
      <div class="wb-row__actions">
        <button type="button" class="wb-op" @click="goCase">{{ $t('workbench.go-review') }}</button>
      </div>
    </div>
  </div>
</template>

<script>
import { getCurrentProjectId } from '@/utils/store-context'
import { buildProjectModulePath } from '@/utils/project-module-path'
import { listCaseReviews } from '@/api/system/case-review'

export default {
  name: 'CasePendingReview',
  data() {
    return { loading: false, rows: [] }
  },
  mounted() {
    this.load()
  },
  methods: {
    rowTitle(row) {
      if (row.caseName) return row.caseName
      if (row.reviewTitle) return row.reviewTitle
      if (row.reviewId != null) return `Review #${row.reviewId}`
      const n = Array.isArray(row.caseIds) ? row.caseIds.length : (row.itemCount || 0)
      return n ? `${this.$t('workbench.widget.case-pending-review')} (${n})` : this.$t('workbench.widget.case-pending-review')
    },
    async load() {
      const projectId = getCurrentProjectId()
      if (!projectId) return
      this.loading = true
      try {
        const res = await listCaseReviews(projectId)
        const list = res.data || res.rows || res || []
        const arr = Array.isArray(list) ? list : []
        this.rows = arr
          .filter(r => {
            const s = String(r.state || r.reviewState || '').toLowerCase()
            return !s || s === 'pending' || s === 'in_review' || s === 'open' || s === '0'
          })
          .slice(0, 5)
      } catch (e) {
        this.rows = []
      } finally {
        this.loading = false
      }
    },
    goCase() {
      const path = buildProjectModulePath(this.$route.params.teamKey, this.$route.params.projectKey, 'case')
      if (path) this.$router.push(path)
    }
  }
}
</script>

<style scoped lang="scss">
@import './wb-list.scss';
.wb-op {
  border: 0;
  background: transparent;
  padding: 0;
  font-size: 13px;
  cursor: pointer;
  color: var(--cat2bug-operate-text-primary, var(--cat2bug-primary));
}
</style>
