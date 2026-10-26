<template>
  <div class="wb-list" v-loading="loading">
    <div v-if="!rows.length && !loading" class="wb-empty">{{ $t('workbench.empty-list') }}</div>
    <div v-for="row in rows" :key="row.defectId" class="wb-row">
      <div class="wb-row__main" @click="openDefect(row)">
        <div class="wb-row__title">
          <span class="id">#{{ row.projectNum || row.defectId }}</span>
          <span class="desc">{{ row.defectName }}</span>
        </div>
        <div class="wb-row__meta">
          <defect-state-flag :defect="stateDefect(row)" />
          <span class="chip level" v-if="isHigh(row)">{{ levelLabel(row) }}</span>
          <span v-if="scope === 'team' && typeLabel(row)" class="chip type">{{ typeLabel(row) }}</span>
          <span v-else-if="Number(row.defectState) === 1">{{ $t('workbench.meta.await-verify') }}</span>
          <span v-else-if="Number(row.defectState) === 0">{{ $t('workbench.meta.assigned-me') }}</span>
        </div>
      </div>
      <div class="wb-row__actions">
        <template v-if="Number(row.defectState) === 1 && canPass">
          <button type="button" class="wb-op green" @click.stop="openDefect(row)">{{ $t('workbench.pass') }}</button>
          <button type="button" class="wb-op orange" @click.stop="openDefect(row)">{{ $t('workbench.reject') }}</button>
          <button type="button" class="wb-op red" v-if="canClose" @click.stop="openDefect(row)">{{ $t('workbench.close') }}</button>
        </template>
        <template v-else-if="canAssign">
          <button type="button" class="wb-op orange" @click.stop="openDefect(row)">{{ $t('workbench.assign') }}</button>
          <button type="button" class="wb-op muted" @click.stop="openDefect(row)">{{ $t('detail') }}</button>
        </template>
        <button v-else type="button" class="wb-op muted" @click.stop="openDefect(row)">{{ $t('detail') }}</button>
      </div>
    </div>
    <div class="wb-more" v-if="total > rows.length">
      <el-button link type="primary" @click="goDefectList">{{ $t('workbench.view-more') }}</el-button>
    </div>
    <handle-defect ref="handleDefect" :project-id="projectId" @change="load" @delete="load" />
  </div>
</template>

<script>
import { listDefect } from '@/api/system/defect'
import { getCurrentProjectId, getCurrentUserId } from '@/utils/store-context'
import { buildProjectModulePath } from '@/utils/project-module-path'
import { checkPermi } from '@/utils/permission'
import HandleDefect from '@/components/Defect/HandleDefect'
import DefectStateFlag from '@/components/Defect/DefectStateFlag'

const STATE_NAME = { 0: 'PROCESSING', 1: 'AUDIT', 2: 'CLOSED', 3: 'REJECTED' }

export default {
  name: 'MyDefects',
  components: { HandleDefect, DefectStateFlag },
  props: {
    /** me = 指派给我；team = 项目未关闭 */
    scope: { type: String, default: 'me' }
  },
  data() {
    return { loading: false, rows: [], total: 0, projectId: null }
  },
  computed: {
    canAssign() { return checkPermi(['system:defect:assign']) },
    canPass() { return checkPermi(['system:defect:pass', 'system:defect:reject']) },
    canClose() { return checkPermi(['system:defect:close']) }
  },
  mounted() {
    this.projectId = getCurrentProjectId()
    this.load()
  },
  methods: {
    stateDefect(row) {
      const name = row.defectStateName || STATE_NAME[Number(row.defectState)] || 'PROCESSING'
      return { ...row, defectStateName: name }
    },
    isHigh(row) {
      const lv = row.defectLevel
      return lv === 'high' || lv === 'urgent' || lv === 2 || lv === 3 || lv === '2' || lv === '3'
    },
    levelLabel(row) {
      const lv = row.defectLevel
      if (lv === 'urgent' || lv === 3 || lv === '3') return this.$t('workbench.level-urgent')
      return this.$t('workbench.level-high')
    },
    typeLabel(row) {
      const t = row.defectTypeName || row.defectType
      if (t === 'TASK' || t === 1 || t === '1') return this.$t('TASK')
      if (t === 'DEMAND' || t === 2 || t === '2') return this.$t('DEMAND')
      if (t === 'BUG' || t === 0 || t === '0') return this.$t('BUG')
      if (typeof t === 'string' && t) return this.$t(t) !== t ? this.$t(t) : t
      return ''
    },
    async load() {
      const projectId = getCurrentProjectId()
      const userId = getCurrentUserId()
      this.projectId = projectId
      if (!projectId) return
      if (this.scope !== 'team' && !userId) return
      this.loading = true
      try {
        const query = {
          projectId,
          pageNum: 1,
          pageSize: 8,
          params: { defectStates: [0, 1, 3], delFlag: '0' }
        }
        if (this.scope !== 'team') query.handleBy = [userId]
        const res = await listDefect(query)
        this.rows = res.rows || []
        this.total = Number(res.total || 0)
      } finally {
        this.loading = false
      }
    },
    openDefect(row) {
      if (this.$refs.handleDefect && row?.defectId) {
        this.$refs.handleDefect.open(row.defectId)
        return
      }
      const path = buildProjectModulePath(this.$route.params.teamKey, this.$route.params.projectKey, 'defect')
      if (path) this.$router.push({ path, query: { defectId: row.defectId } })
    },
    goDefectList() {
      const path = buildProjectModulePath(this.$route.params.teamKey, this.$route.params.projectKey, 'defect')
      if (path) this.$router.push(path)
    },
    refresh() { return this.load() }
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
.wb-op.green { color: var(--cat2bug-operate-text-success, #67c23a); }
.wb-op.red { color: var(--cat2bug-operate-text-danger, #f56c6c); }
.wb-op.orange { color: #e6a23c; }
.wb-op.muted { color: var(--text-color-regular); }
.wb-op.muted:hover { color: var(--cat2bug-primary); }
.chip.level,
.chip.type {
  background: transparent;
  border: 1px solid var(--border-color-light, #414243);
  color: var(--text-color-secondary);
}
</style>
