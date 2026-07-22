<template>
  <div class="app-container git-pr-page" v-loading="loading">
    <git-repo-shell :repository-id="repositoryId" :repo-name="repoName" active-tab="pulls">
      <div class="git-pr-page__toolbar">
        <el-input
          v-model="keyword"
          class="git-pr-page__search"
          clearable
          size="small"
          :placeholder="$t('git.repo.pr.search-placeholder')"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button
          v-if="canPush"
          type="primary"
          plain
          size="small"
          @click="openNew"
        >
          <el-icon class="el-icon--left"><Plus /></el-icon>
          {{ $t('git.repo.pr.new') }}
        </el-button>
      </div>

      <el-tabs v-model="state" class="git-pr-page__state-tabs" @tab-change="loadPullRequests">
        <el-tab-pane name="open" :label="$t('git.repo.pr.state.open')" />
        <el-tab-pane name="merged" :label="$t('git.repo.pr.state.merged')" />
        <el-tab-pane name="closed" :label="$t('git.repo.pr.state.closed')" />
      </el-tabs>

      <div class="git-pr-list">
        <button
          v-for="pull in filteredPulls"
          :key="pull.pullRequestId || pull.number"
          type="button"
          class="git-pr-list__item"
          @click="openDetail(pull.number)"
        >
          <span class="git-pr-list__state" :class="'is-' + pull.state">{{ stateLabel(pull.state) }}</span>
          <span class="git-pr-list__body">
            <span class="git-pr-list__title">{{ pull.title }}</span>
            <span class="git-pr-list__meta">
              #{{ pull.number }} · {{ pull.sourceBranch }} → {{ pull.targetBranch }} ·
              {{ pull.createBy || $t('unknown') }} · {{ relativeTime(pull.createTime) }}
            </span>
          </span>
          <el-icon class="git-pr-list__arrow"><ArrowRight /></el-icon>
        </button>
        <el-empty
          v-if="!loading && !filteredPulls.length"
          :description="$t('git.repo.pr.empty')"
          :image-size="96"
        />
      </div>
    </git-repo-shell>
  </div>
</template>

<script>
import { ArrowRight, Plus, Search } from '@element-plus/icons-vue'
import { listGitPullRequests } from '@/api/system/gitPullRequest'
import { getGitRepo } from '@/api/system/gitRepo'
import { gitRepoPullDetailRoute, gitRepoPullNewRoute } from '@/utils/git-repo-routes'
import { checkPermi } from '@/utils/permission'
import { formatTime } from '@/utils/index'
import { parseTime } from '@/utils/ruoyi'
import GitRepoShell from './GitRepoShell.vue'

export default {
  name: 'GitRepoPullRequests',
  components: { ArrowRight, Plus, Search, GitRepoShell },
  data() {
    return {
      repositoryId: Number(this.$route.params.repositoryId),
      repoName: String(this.$route.params.slug || ''),
      state: String(this.$route.query.state || 'open'),
      keyword: '',
      pulls: [],
      loading: false
    }
  },
  computed: {
    canPush() {
      return checkPermi(['system:git:repo:push'])
    },
    filteredPulls() {
      const q = String(this.keyword || '').trim().toLowerCase()
      if (!q) return this.pulls
      return this.pulls.filter(pull => {
        const haystack = [
          pull.title,
          pull.number,
          pull.sourceBranch,
          pull.targetBranch,
          pull.createBy
        ].map(v => String(v == null ? '' : v).toLowerCase()).join(' ')
        return haystack.includes(q)
      })
    }
  },
  created() {
    if (!['open', 'merged', 'closed'].includes(this.state)) this.state = 'open'
    this.loadRepo()
    this.loadPullRequests()
  },
  methods: {
    loadRepo() {
      getGitRepo(this.repositoryId).then(res => {
        this.repoName = (res.data && res.data.name) || this.repoName
      }).catch(() => {})
    },
    loadPullRequests() {
      this.loading = true
      listGitPullRequests(this.repositoryId, this.state).then(res => {
        this.pulls = res.data || []
      }).catch(() => {
        this.pulls = []
      }).finally(() => {
        this.loading = false
      })
    },
    openNew() {
      this.$router.push(gitRepoPullNewRoute(this.$route)).catch(() => {})
    },
    openDetail(number) {
      this.$router.push(gitRepoPullDetailRoute(this.$route, number)).catch(() => {})
    },
    stateLabel(state) {
      return this.$t('git.repo.pr.state.' + state)
    },
    relativeTime(value) {
      if (!value) return '—'
      const time = new Date(value).getTime()
      return Number.isNaN(time) ? (parseTime(value) || '—') : formatTime(time)
    }
  }
}
</script>

<style lang="scss" scoped>
.git-pr-page {
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 20px 20px 16px;
  box-sizing: border-box;
  color: var(--text-color-regular);
}

.git-pr-page__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cat2bug-toolbar-section-gap, 10px);
  /* 与上方 GitRepoShell tabs 分割线到底部间距（12px）对称 */
  margin-bottom: 12px;
}

.git-pr-page__search {
  width: 320px;
  max-width: 100%;
}

.git-pr-page__state-tabs {
  margin-bottom: 12px;

  :deep(.el-tabs__header) {
    margin-bottom: 12px;
  }
}

.git-pr-list {
  border: 1px solid var(--border-color-light);
  border-radius: 6px;
  overflow: hidden;
  background: var(--card-bg);
}

.git-pr-list__item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: 0;
  border-bottom: 1px solid var(--border-color-light);
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.git-pr-list__item:last-child {
  border-bottom: 0;
}

.git-pr-list__item:hover {
  background: var(--table-row-hover-bg);
}

.git-pr-list__state {
  flex: 0 0 auto;
  padding: 3px 7px;
  border-radius: 12px;
  font-size: 12px;
  color: var(--text-color-secondary);
  background: var(--bg-color-block);
}

.git-pr-list__state.is-open {
  color: var(--el-color-success);
}

.git-pr-list__state.is-merged {
  color: var(--el-color-primary);
}

.git-pr-list__state.is-closed {
  color: var(--el-color-info);
}

.git-pr-list__body {
  flex: 1;
  min-width: 0;
  display: grid;
  gap: 5px;
}

.git-pr-list__title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 600;
  color: var(--text-color-primary);
}

.git-pr-list__meta {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: var(--text-color-secondary);
}

.git-pr-list__arrow {
  flex: 0 0 auto;
  color: var(--text-color-secondary);
}

@media (max-width: 680px) {
  .git-pr-page__toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .git-pr-page__search {
    width: 100%;
  }
}
</style>
