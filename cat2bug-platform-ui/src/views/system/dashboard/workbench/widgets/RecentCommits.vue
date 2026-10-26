<template>
  <div class="wb-list" v-loading="loading">
    <div v-if="!rows.length && !loading" class="wb-empty">{{ $t('workbench.empty-list') }}</div>
    <div v-for="row in rows" :key="row.commitId || row.id || row.sha" class="wb-row">
      <div class="wb-row__main">
        <div class="wb-row__title">
          <span class="name">{{ row.shortMessage || row.message || row.sha || row.commitId }}</span>
        </div>
        <div class="wb-row__meta">
          <span v-if="!isMine">{{ row.authorName || row.author || '' }} · </span>
          <span v-if="row.repoName">{{ row.repoName }} · </span>
          {{ row.commitTime || row.createTime || '' }}
        </div>
      </div>
    </div>
    <div class="wb-more">
      <el-button link type="primary" @click="goCode">{{ $t('workbench.go-code') }}</el-button>
    </div>
  </div>
</template>

<script>
import { getCurrentProjectId } from '@/utils/store-context'
import { buildProjectModulePath } from '@/utils/project-module-path'
import { listGitRepo, listGitRepoCommits } from '@/api/system/gitRepo'
import { useUserStore } from '@/stores/user'

const MAX_REPOS = 5
const MAX_ROWS = 8

export default {
  name: 'RecentCommits',
  props: {
    /** me = 仅当前用户提交；all = 仓库最近提交 */
    scope: { type: String, default: 'all' }
  },
  data() {
    return { loading: false, rows: [] }
  },
  computed: {
    isMine() {
      return this.scope === 'me'
    },
    authorHint() {
      const u = useUserStore()
      return [u.email, u.nickName, u.name].filter(Boolean)
    }
  },
  mounted() {
    this.load()
  },
  methods: {
    matchMine(row) {
      const hints = this.authorHint.map(s => String(s).toLowerCase())
      if (!hints.length) return true
      const fields = [
        row.authorName,
        row.author,
        row.authorEmail,
        row.committerName,
        row.committerEmail
      ].filter(Boolean).map(s => String(s).toLowerCase())
      return fields.some(f => hints.some(h => f.includes(h) || h.includes(f)))
    },
    async load() {
      const projectId = getCurrentProjectId()
      if (!projectId) return
      this.loading = true
      try {
        const reposRes = await listGitRepo({ projectId, pageNum: 1, pageSize: MAX_REPOS })
        const repos = reposRes.rows || []
        if (!repos.length) {
          this.rows = []
          return
        }
        const author = this.isMine ? (useUserStore().email || useUserStore().name || '') : ''
        const chunks = await Promise.all(
          repos.map(repo => {
            const params = { pageNum: 1, pageSize: this.isMine ? 20 : 5 }
            if (author) params.author = author
            return listGitRepoCommits(repo.repositoryId, params)
              .then(res => {
                const list = res.rows || res.data || []
                return list.map(c => ({
                  ...c,
                  repoName: repo.name || repo.slug
                }))
              })
              .catch(() => [])
          })
        )
        let merged = chunks.flat()
        if (this.isMine) {
          merged = merged.filter(r => this.matchMine(r))
        }
        merged.sort((a, b) => {
          const ta = new Date(a.commitTime || a.createTime || 0).getTime()
          const tb = new Date(b.commitTime || b.createTime || 0).getTime()
          return tb - ta
        })
        this.rows = merged.slice(0, MAX_ROWS)
      } catch (e) {
        this.rows = []
      } finally {
        this.loading = false
      }
    },
    goCode() {
      const path = buildProjectModulePath(this.$route.params.teamKey, this.$route.params.projectKey, 'code')
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
</style>
