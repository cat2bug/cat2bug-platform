<template>
  <div class="wb-list" v-loading="loading">
    <div v-if="!rows.length && !loading" class="wb-empty">{{ $t('workbench.empty-list') }}</div>
    <div
      v-for="row in rows"
      :key="row.docId"
      class="wb-row wb-row--click"
      @click="openDoc(row)"
    >
      <div class="wb-row__main">
        <div class="wb-row__title">
          <span class="name">{{ row.docName || ('#' + row.docId) }}</span>
        </div>
        <div class="wb-row__meta">
          <span v-if="!isMine && row.updateBy">{{ row.updateBy }} · </span>
          <span v-if="row.fileExtension">.{{ row.fileExtension }} · </span>
          {{ row.updateTime || row.createTime || '' }}
        </div>
      </div>
    </div>
    <div class="wb-more">
      <el-button link type="primary" @click="goDocs">{{ $t('workbench.go-document') }}</el-button>
    </div>
  </div>
</template>

<script>
import { listDocument } from '@/api/system/document'
import { getCurrentProjectId, getCurrentUserId } from '@/utils/store-context'
import { buildProjectModulePath } from '@/utils/project-module-path'

const MAX_ROWS = 8
const PAGE = 30

export default {
  name: 'DocumentList',
  props: {
    /** me = 我创建或更新过的文档；all = 项目最近文档 */
    scope: { type: String, default: 'all' }
  },
  data() {
    return { loading: false, rows: [] }
  },
  computed: {
    isMine() {
      return this.scope === 'me'
    }
  },
  mounted() {
    this.load()
  },
  methods: {
    isFile(d) {
      return Number(d.docType) > 0
    },
    byUpdateDesc(a, b) {
      const ta = new Date(a.updateTime || a.createTime || 0).getTime()
      const tb = new Date(b.updateTime || b.createTime || 0).getTime()
      return tb - ta
    },
    async load() {
      const projectId = getCurrentProjectId()
      if (!projectId) return
      this.loading = true
      try {
        if (this.isMine) {
          await this.loadMine(projectId)
        } else {
          await this.loadAll(projectId)
        }
      } catch (e) {
        this.rows = []
      } finally {
        this.loading = false
      }
    },
    async loadAll(projectId) {
      const res = await listDocument({ projectId, pageNum: 1, pageSize: PAGE })
      this.rows = (res.rows || [])
        .filter(this.isFile)
        .sort(this.byUpdateDesc)
        .slice(0, MAX_ROWS)
    },
    async loadMine(projectId) {
      const userId = getCurrentUserId()
      if (!userId) {
        this.rows = []
        return
      }
      const [createdRes, updatedRes] = await Promise.all([
        listDocument({ projectId, createById: userId, pageNum: 1, pageSize: PAGE }),
        listDocument({ projectId, updateById: userId, pageNum: 1, pageSize: PAGE })
      ])
      const map = new Map()
      for (const row of [...(createdRes.rows || []), ...(updatedRes.rows || [])]) {
        if (!this.isFile(row) || row.docId == null) continue
        const prev = map.get(row.docId)
        if (!prev || this.byUpdateDesc(row, prev) < 0) map.set(row.docId, row)
      }
      this.rows = [...map.values()].sort(this.byUpdateDesc).slice(0, MAX_ROWS)
    },
    openDoc(row) {
      if (!row) return
      const ext = String(row.fileExtension || '').toLowerCase()
      if (ext === 'md' || ext === 'markdown') {
        this.$router.push({
          name: 'HandleMarkdownDocument',
          params: { docId: String(row.docId) }
        })
        return
      }
      this.goDocs()
    },
    goDocs() {
      const path = buildProjectModulePath(
        this.$route.params.teamKey,
        this.$route.params.projectKey,
        'document'
      )
      if (path) this.$router.push(path)
    },
    refresh() {
      return this.load()
    }
  }
}
</script>

<style scoped lang="scss">
@import './wb-list.scss';
.wb-row--click {
  cursor: pointer;
}
.wb-row--click:hover .name {
  color: var(--cat2bug-primary);
}
</style>
