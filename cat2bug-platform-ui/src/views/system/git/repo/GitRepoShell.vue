<template>
  <div class="git-repo-shell">
    <div class="git-repo-shell__top">
      <el-page-header class="git-repo-shell__back" @back="goRepoList">
        <template #content>
          <div class="git-repo-shell__title">
            <git-repo-select
              :repository-id="repositoryId"
              :project-id="projectId"
              :repo-name="repoName"
              :visibility="visibility"
            />
            <slot name="title-extra" />
          </div>
        </template>
        <template v-if="$slots.extra" #extra>
          <slot name="extra" />
        </template>
      </el-page-header>
    </div>

    <el-tabs
      :model-value="shellTab"
      class="git-repo-shell__tabs"
      @tab-click="onTabClick"
      @tab-change="onTabChange"
    >
      <el-tab-pane name="code">
        <template #label>
          <span class="git-repo-shell__tab-label">
            <svg-icon icon-class="code" class="git-repo-shell__tab-icon" />
            {{ $t('git.repo.tab.code') }}
          </span>
        </template>
      </el-tab-pane>
      <el-tab-pane name="pulls">
        <template #label>
          <span class="git-repo-shell__tab-label">
            <git-branch-icon class="git-repo-shell__tab-icon" />
            {{ $t('git.repo.pr.title') }}
            <span v-if="openPullCount > 0" class="git-repo-shell__tab-count">{{ openPullCountLabel }}</span>
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
      <el-tab-pane v-if="settingsTabVisible" name="settings">
        <template #label>
          <span class="git-repo-shell__tab-label">
            <el-icon class="git-repo-shell__tab-icon"><Setting /></el-icon>
            {{ $t('git.repo.tab.settings') }}
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
import { h } from 'vue'
import { Setting } from '@element-plus/icons-vue'
import { listGitPullRequests } from '@/api/system/gitPullRequest'
import { getGitRepoMyAccess } from '@/api/system/gitRepo'
import {
  gitRepoBrowseRoute,
  gitRepoPullsRoute,
  gitRepoSettingsGeneralRoute
} from '@/utils/git-repo-routes'
import { checkPermi } from '@/utils/permission'
import GitTagIcon from './GitTagIcon.vue'
import GitRepoSelect from '@/components/Git/GitRepoSelect'

/** 与 browse 页分支按钮同源图标 */
const GitBranchIcon = {
  name: 'GitBranchIcon',
  render() {
    return h(
      'svg',
      {
        viewBox: '0 0 24 24',
        width: '1em',
        height: '1em',
        fill: 'none',
        stroke: 'currentColor',
        'stroke-width': '2',
        'stroke-linecap': 'round',
        'stroke-linejoin': 'round',
        'aria-hidden': 'true',
        class: this.$attrs.class
      },
      [
        h('circle', { cx: '6', cy: '5', r: '2.5' }),
        h('circle', { cx: '6', cy: '19', r: '2.5' }),
        h('circle', { cx: '18', cy: '7', r: '2.5' }),
        h('path', { d: 'M6 7.5v9' }),
        h('path', { d: 'M8.5 5.5h2a5 5 0 0 1 5 5' })
      ]
    )
  }
}

const SHELL_TABS = new Set(['code', 'pulls', 'releases', 'settings'])

export default {
  name: 'GitRepoShell',
  components: { GitBranchIcon, GitTagIcon, GitRepoSelect, Setting },
  emits: ['tab-click'],
  props: {
    repositoryId: {
      type: [Number, String],
      default: null
    },
    projectId: {
      type: [Number, String],
      default: null
    },
    repoName: {
      type: String,
      default: ''
    },
    visibility: {
      type: String,
      default: ''
    },
    activeTab: {
      type: String,
      default: 'code'
    },
    /** When unset, visibility follows my-access.canManage (fallback: checkPermi). */
    showSettings: {
      type: Boolean,
      default: undefined
    }
  },
  data() {
    return {
      openPullCount: 0,
      /** null while unknown; boolean after my-access resolves or fails */
      canManageFromAccess: null,
      accessRequestId: 0
    }
  },
  computed: {
    settingsTabVisible() {
      if (typeof this.showSettings === 'boolean') return this.showSettings
      if (this.canManageFromAccess === true) return true
      if (this.canManageFromAccess === false) return false
      // API still loading or failed before resolve: keep permission-word compat
      return checkPermi(['system:git:repo:manage'])
    },
    /** Map non-shell pages (branches / commits) onto the nearest primary tab */
    shellTab() {
      return SHELL_TABS.has(this.activeTab) ? this.activeTab : 'code'
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
        this.loadMyAccess()
      }
    }
  },
  activated() {
    this.loadOpenPullCount()
    this.loadMyAccess()
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
    loadMyAccess() {
      if (typeof this.showSettings === 'boolean') {
        return
      }
      const id = Number(this.repositoryId)
      if (!Number.isFinite(id) || id <= 0) {
        this.canManageFromAccess = null
        return
      }
      const reqId = ++this.accessRequestId
      getGitRepoMyAccess(id).then(res => {
        if (reqId !== this.accessRequestId) return
        const access = (res && res.data) || {}
        this.canManageFromAccess = !!access.canManage
      }).catch(() => {
        if (reqId !== this.accessRequestId) return
        // Fallback: permission word only (compat when my-access not yet available)
        this.canManageFromAccess = checkPermi(['system:git:repo:manage'])
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
    tabNameFromPane(pane) {
      if (!pane) return ''
      if (pane.paneName != null) return String(pane.paneName)
      if (pane.props && pane.props.name != null) return String(pane.props.name)
      return ''
    },
    onTabClick(pane) {
      const name = this.tabNameFromPane(pane)
      if (name) this.$emit('tab-click', name)
      // 已在「代码」高亮时（分支页 / 目录内）再点不会触发 tab-change，须在 click 时回库首页
      if (name === 'code') {
        this.goCodeHome()
      }
    },
    isPlainCodeBrowse() {
      const route = this.$route
      return route.name === 'GitRepoBrowse'
        && !route.query.tab
        && !route.params.releaseId
        && !route.params.commitId
    },
    isReleasesBrowse() {
      return this.$route.name === 'GitRepoBrowse' && this.$route.query.tab === 'releases'
    },
    isSettingsRoute() {
      const name = this.$route && this.$route.name != null ? String(this.$route.name) : ''
      return name.startsWith('GitRepoSettings')
    },
    /** 仓库代码首页：`/{team}/{project}/code/{slug}`，无 tab/路径子页 */
    goCodeHome() {
      if (!this.repositoryId) return
      if (this.isPlainCodeBrowse()) return
      this.$router.push(gitRepoBrowseRoute(this.$route)).catch(() => {})
    },
    onTabChange(tab) {
      if (!this.repositoryId || !tab) return
      if (tab === 'pulls') {
        if (this.shellTab === 'pulls') return
        this.$router.push(gitRepoPullsRoute(this.$route)).catch(() => {})
        return
      }
      if (tab === 'releases') {
        if (this.isReleasesBrowse()) return
        this.$router.push(gitRepoBrowseRoute(this.$route, { tab: 'releases' })).catch(() => {})
        return
      }
      if (tab === 'settings') {
        if (this.isSettingsRoute()) return
        this.$router.push(gitRepoSettingsGeneralRoute(this.$route)).catch(() => {})
        return
      }
      // code：由 onTabClick → goCodeHome 处理（含重复点击）
      if (tab === 'code') {
        this.goCodeHome()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.git-repo-shell {
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
}

.git-repo-shell__top {
  flex: 0 0 auto;
  margin-bottom: 4px;
}

.git-repo-shell__title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  max-width: 100%;
}

.git-repo-shell__tabs {
  flex: 0 0 auto;
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

.git-repo-shell__body {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.git-repo-shell__tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.git-repo-shell__tab-icon {
  font-size: 14px;
  width: 1em;
  height: 1em;
  flex-shrink: 0;
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
