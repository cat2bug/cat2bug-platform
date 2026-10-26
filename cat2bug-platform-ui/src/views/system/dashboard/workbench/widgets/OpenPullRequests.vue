<template>
  <div class="wb-list" v-loading="loading">
    <div v-if="!rows.length && !loading" class="wb-empty">{{ $t('workbench.empty-list') }}</div>
    <div
      v-for="row in rows"
      :key="(row.repositoryId || '') + '-' + (row.pullRequestId || row.number)"
      class="wb-row"
    >
      <div class="wb-row__main" @click="openDetail(row)">
        <div class="wb-row__title">
          <span class="id">#{{ row.number }}</span>
          <span class="desc">{{ row.title }}</span>
        </div>
        <div class="wb-row__meta">
          <span class="chip state">{{ stateLabel(row.state) }}</span>
          <span v-if="row.repoName">{{ row.repoName }}</span>
          <span>{{ row.sourceBranch }} → {{ row.targetBranch }}</span>
          <span v-if="!isMine && row.createBy">{{ row.createBy }}</span>
        </div>
      </div>
      <div class="wb-row__actions">
        <button type="button" class="wb-op muted" @click.stop="openDetail(row)">{{ $t('detail') }}</button>
      </div>
    </div>
    <div class="wb-more">
      <el-button link type="primary" @click="goCode">{{ $t('workbench.view-more') }}</el-button>
    </div>
  </div>
</template>

<script>
import { listGitRepo } from '@/api/system/gitRepo'
import { listGitPullRequests } from '@/api/system/gitPullRequest'
import { getCurrentProjectId } from '@/utils/store-context'
import { buildProjectModulePath } from '@/utils/project-module-path'
import { buildGitRepoSpaPath } from '@/utils/git-repo-routes'
import { useUserStore } from '@/stores/user'

const MAX_REPOS = 5
const MAX_ROWS = 8

export default {
  name: 'OpenPullRequests',
  props: {
    /** me = 仅我创建的 PR；all = 仓库开放 PR */
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
    stateLabel(state) {
      const key = 'git.repo.pr.state.' + (state || 'open')
      const t = this.$t(key)
      return t === key ? state : t
    },
    matchMine(row) {
      const u = useUserStore()
      if (u.id != null && row.authorId != null && String(row.authorId) === String(u.id)) {
        return true
      }
      const hints = [u.name, u.nickName, u.email].filter(Boolean).map(s => String(s).toLowerCase())
      if (!hints.length) return u.id == null
      const fields = [row.createBy, row.author, row.authorName, row.userName]
        .filter(Boolean)
        .map(s => String(s).toLowerCase())
      return fields.some(f => hints.some(h => f === h || f.includes(h)))
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
        const chunks = await Promise.all(
          repos.map(repo =>
            listGitPullRequests(repo.repositoryId, 'open')
              .then(res => {
                const list = res.data || res.rows || []
                return list.map(pr => ({
                  ...pr,
                  repositoryId: repo.repositoryId,
                  repoName: repo.name || repo.slug,
                  repoSlug: repo.slug || repo.name
                }))
              })
              .catch(() => [])
          )
        )
        let merged = chunks.flat()
        if (this.isMine) {
          merged = merged.filter(r => this.matchMine(r))
        }
        merged.sort((a, b) => {
          const ta = new Date(a.createTime || 0).getTime()
          const tb = new Date(b.createTime || 0).getTime()
          return tb - ta
        })
        this.rows = merged.slice(0, MAX_ROWS)
      } catch (e) {
        this.rows = []
      } finally {
        this.loading = false
      }
    },
    openDetail(row) {
      const teamKey = this.$route.params.teamKey
      const projectKey = this.$route.params.projectKey
      const slug = row.repoSlug
      if (!slug || row.number == null) {
        this.goCode()
        return
      }
      const path = buildGitRepoSpaPath(teamKey, projectKey, slug, 'pulls', String(row.number))
      if (path) this.$router.push(path)
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
.wb-op {
  border: 0;
  background: transparent;
  padding: 0;
  font-size: 13px;
  cursor: pointer;
  color: var(--text-color-regular);
}
.wb-op.muted:hover { color: var(--cat2bug-primary); }
.chip.state {
  background: transparent;
  border: 1px solid var(--border-color-light, #414243);
  color: var(--text-color-secondary);
}
</style>
