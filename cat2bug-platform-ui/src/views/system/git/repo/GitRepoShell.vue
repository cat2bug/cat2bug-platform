<template>
  <div class="git-repo-shell">
    <div class="git-repo-shell__top">
      <el-page-header class="git-repo-shell__back" @back="goRepoList">
        <template #content>
          <div class="git-repo-shell__title">
            <el-icon class="git-repo-shell__title-icon"><FolderOpened /></el-icon>
            <span class="git-repo-shell__repo-name">{{ displayName }}</span>
            <el-tag size="small" type="info" effect="plain">{{ $t('git.repo.private') }}</el-tag>
            <slot name="title-extra" />
          </div>
        </template>
        <template v-if="$slots.extra" #extra>
          <slot name="extra" />
        </template>
      </el-page-header>
    </div>

    <el-tabs
      :model-value="activeTab"
      class="git-repo-shell__tabs"
      @tab-change="onTabChange"
    >
      <el-tab-pane name="code">
        <template #label>
          <span class="git-repo-shell__tab-label">
            <el-icon><Document /></el-icon>
            {{ $t('git.repo.tab.code') }}
          </span>
        </template>
      </el-tab-pane>
      <el-tab-pane name="pulls">
        <template #label>
          <span class="git-repo-shell__tab-label">
            <el-icon><Connection /></el-icon>
            {{ $t('git.repo.pr.title') }}
            <span v-if="openPullCount > 0" class="git-repo-shell__tab-count">{{ openPullCountLabel }}</span>
          </span>
        </template>
      </el-tab-pane>
      <el-tab-pane name="commits">
        <template #label>
          <span class="git-repo-shell__tab-label">
            <el-icon><List /></el-icon>
            {{ $t('git.repo.tab.commits') }}
          </span>
        </template>
      </el-tab-pane>
      <el-tab-pane name="releases">
        <template #label>
          <span class="git-repo-shell__tab-label">
            <git-tag-icon class="git-repo-shell__tab-tag" />
            {{ $t('git.repo.aside.releases') }}
          </span>
        </template>
      </el-tab-pane>
    </el-tabs>

    <div class="git-repo-shell__body">
      <slot />
    </div>
  </div>
</template>

<script>
import { Connection, Document, FolderOpened, List } from '@element-plus/icons-vue'
import { listGitPullRequests } from '@/api/system/gitPullRequest'
import {
  gitRepoBrowseRoute,
  gitRepoPullsRoute
} from '@/utils/git-repo-routes'
import GitTagIcon from './GitTagIcon.vue'

export default {
  name: 'GitRepoShell',
  components: { Connection, Document, FolderOpened, List, GitTagIcon },
  props: {
    repositoryId: {
      type: [Number, String],
      required: true
    },
    repoName: {
      type: String,
      default: ''
    },
    activeTab: {
      type: String,
      default: 'code'
    }
  },
  data() {
    return {
      openPullCount: 0
    }
  },
  computed: {
    displayName() {
      return this.repoName || '-'
    },
    openPullCountLabel() {
      return this.openPullCount > 99 ? '99+' : String(this.openPullCount)
    }
  },
  watch: {
    repositoryId: {
      immediate: true,
      handler() {
        this.loadOpenPullCount()
      }
    }
  },
  activated() {
    this.loadOpenPullCount()
  },
  methods: {
    loadOpenPullCount() {
      if (!this.repositoryId) {
        this.openPullCount = 0
        return
      }
      listGitPullRequests(this.repositoryId, 'open').then(res => {
        const list = res.data || []
        this.openPullCount = Array.isArray(list) ? list.length : 0
      }).catch(() => {
        this.openPullCount = 0
      })
    },
    goRepoList() {
      const { teamKey, projectKey } = this.$route.params
      if (teamKey && projectKey) {
        this.$router.push({
          name: 'GitRepoList',
          params: { teamKey, projectKey }
        }).catch(() => {})
        return
      }
      this.$router.push({ path: '/project/git-repo' }).catch(() => {})
    },
    onTabChange(tab) {
      if (!this.repositoryId || tab === this.activeTab) return
      if (tab === 'pulls') {
        this.$router.push(gitRepoPullsRoute(this.$route)).catch(() => {})
        return
      }
      if (tab === 'commits' || tab === 'releases') {
        this.$router.push(gitRepoBrowseRoute(this.$route, { tab })).catch(() => {})
        return
      }
      this.$router.push(gitRepoBrowseRoute(this.$route)).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.git-repo-shell__top {
  margin-bottom: 4px;
}

.git-repo-shell__title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.git-repo-shell__title-icon {
  color: var(--text-color-secondary);
}

.git-repo-shell__repo-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-color-primary);
}

.git-repo-shell__tabs {
  margin-bottom: 12px;

  :deep(.el-tabs__header) {
    margin-bottom: 0;
  }

  :deep(.el-tabs__item.is-active) {
    color: var(--el-color-primary);
    font-weight: 600;
  }

  :deep(.el-tabs__active-bar) {
    background-color: var(--el-color-primary);
  }
}

.git-repo-shell__tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.git-repo-shell__tab-tag {
  font-size: 14px;
}

.git-repo-shell__tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  color: var(--text-color-primary);
  background: rgba(144, 147, 153, 0.45);
}

.git-repo-shell__tabs :deep(.el-tabs__item.is-active) .git-repo-shell__tab-count {
  color: #141414;
  background: var(--el-color-primary);
}
</style>
