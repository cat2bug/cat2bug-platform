<template>
  <div class="app-container git-browse-page" v-loading="pageLoading">
    <div class="git-browse-top">
      <el-page-header @back="goBack" class="git-browse-back">
        <template #content>
          <div class="git-browse-title">
            <el-icon class="git-browse-title-icon"><FolderOpened /></el-icon>
            <span class="git-browse-repo-name">{{ repoName || '-' }}</span>
            <el-tag size="small" type="info" effect="plain">{{ $t('git.repo.private') }}</el-tag>
          </div>
        </template>
      </el-page-header>
    </div>

    <el-tabs v-model="activeTab" class="git-browse-tabs">
      <el-tab-pane :label="$t('git.repo.tab.code')" name="code" />
      <el-tab-pane :label="$t('git.repo.tab.commits')" name="commits" />
    </el-tabs>

    <div v-show="activeTab === 'code'" class="git-browse-layout">
      <div class="git-browse-main">
        <!-- 工具栏 -->
        <div class="git-browse-toolbar">
          <div class="git-browse-toolbar-left">
            <el-select
              v-model="currentRef"
              size="small"
              class="git-browse-branch-select"
              :placeholder="$t('git.repo.select-branch')"
              :loading="branchesLoading"
              @change="onRefChange"
            >
              <template #prefix>
                <el-icon><Share /></el-icon>
              </template>
              <el-option
                v-for="b in branches"
                :key="b.name"
                :label="b.name"
                :value="b.name"
              />
            </el-select>
            <span class="git-browse-meta-chip">{{ $t('git.repo.branch-count', { n: branches.length }) }}</span>
          </div>
          <div class="git-browse-toolbar-right">
            <el-dropdown trigger="click">
              <el-button type="primary" plain size="small" class="git-browse-clone-btn">
                {{ $t('git.repo.clone-download') }}
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <div class="git-browse-clone-panel" @click.stop>
                  <div class="git-browse-clone-label">HTTP</div>
                  <div class="git-browse-clone-row">
                    <el-input :model-value="fullCloneUrl" readonly size="small" />
                    <el-button size="small" type="primary" @click="copyCloneUrl">{{ $t('git.repo.copy') }}</el-button>
                  </div>
                </div>
              </template>
            </el-dropdown>
          </div>
        </div>

        <!-- 最近提交条 -->
        <div v-if="latestCommit" class="git-browse-commit-bar">
          <div class="git-browse-commit-bar-left">
            <cat2-bug-avatar :member="memberForAuthor(latestCommit)" :size="28" />
            <span class="git-browse-author">{{ latestCommit.authorName || '-' }}</span>
            <span class="git-browse-commit-msg" :title="latestCommit.message">{{ latestCommit.message }}</span>
            <span class="git-browse-commit-hash">{{ latestCommit.shortId }}</span>
            <span class="git-browse-commit-time">{{ relativeTime(latestCommit.commitTime) }}</span>
          </div>
          <a class="git-browse-commit-total" href="javascript:;" @click="activeTab = 'commits'">
            {{ $t('git.repo.commit-count', { n: commits.length }) }}
            <el-icon><ArrowRight /></el-icon>
          </a>
        </div>

        <!-- 面包屑 -->
        <div class="git-browse-breadcrumb">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>
              <a href="javascript:;" @click="navigateToPath('')">{{ repoName || $t('git.repo.path-root') }}</a>
            </el-breadcrumb-item>
            <el-breadcrumb-item v-for="(seg, idx) in breadcrumbSegments" :key="idx">
              <a
                v-if="idx < breadcrumbSegments.length - 1 || viewMode === 'blob'"
                href="javascript:;"
                @click="navigateToPath(breadcrumbPathAt(idx))"
              >{{ seg }}</a>
              <span v-else>{{ seg }}</span>
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <!-- 文件树 -->
        <div v-if="viewMode === 'tree'" v-loading="treeLoading" class="git-browse-tree-card">
          <div
            v-for="row in treeEntries"
            :key="row.path"
            class="git-browse-tree-row"
            @click="handleTreeRowClick(row)"
          >
            <div class="git-browse-tree-name">
              <el-icon class="git-browse-file-icon" :class="{ 'is-dir': row.type === 'tree' }">
                <Folder v-if="row.type === 'tree'" />
                <Document v-else />
              </el-icon>
              <span>{{ row.name }}</span>
            </div>
            <div class="git-browse-tree-msg" :title="row.lastCommitMessage">{{ row.lastCommitMessage || '—' }}</div>
            <div class="git-browse-tree-time">{{ relativeTime(row.lastCommitTime) }}</div>
          </div>
          <div v-if="!treeLoading && !treeEntries.length" class="git-browse-empty">{{ $t('git.repo.empty-repository') }}</div>
        </div>

        <!-- 文件内容 -->
        <div v-else v-loading="blobLoading" class="git-browse-blob-card">
          <template v-if="blob">
            <div v-if="blob.binary" class="git-browse-empty">{{ $t('git.repo.file-binary') }}</div>
            <div v-else-if="!blob.content" class="git-browse-empty">{{ $t('git.repo.file-empty') }}</div>
            <template v-else>
              <div v-if="blob.truncated" class="git-browse-truncated">{{ $t('git.repo.file-truncated') }}</div>
              <pre class="git-browse-code"><code>{{ blob.content }}</code></pre>
            </template>
          </template>
        </div>
      </div>

      <!-- 右侧栏 -->
      <aside class="git-browse-aside">
        <section class="git-aside-block">
          <h4 class="git-aside-title">{{ $t('git.repo.aside.intro') }}</h4>
          <p class="git-aside-desc">{{ repoRemark || $t('git.repo.no-description') }}</p>
          <ul class="git-aside-links">
            <li>
              <el-icon><Share /></el-icon>
              <span>{{ $t('git.repo.default-branch') }}: {{ defaultBranch || currentRef || '-' }}</span>
            </li>
            <li>
              <el-icon><Link /></el-icon>
              <a href="javascript:;" @click="copyCloneUrl">{{ $t('git.repo.clone-url') }}</a>
            </li>
          </ul>
        </section>

        <section class="git-aside-block">
          <h4 class="git-aside-title">{{ $t('git.repo.aside.activity') }}</h4>
          <div v-if="recentActivity.length" class="git-aside-activity">
            <div v-for="c in recentActivity" :key="c.id" class="git-aside-activity-item">
              <cat2-bug-avatar :member="memberForAuthor(c)" :size="24" />
              <div class="git-aside-activity-body">
                <div class="git-aside-activity-text">
                  <strong>{{ c.authorName || '-' }}</strong>
                  {{ $t('git.repo.aside.pushed') }}
                  <span class="git-aside-hash">{{ c.shortId }}</span>
                </div>
                <div class="git-aside-activity-msg" :title="c.message">{{ c.message }}</div>
                <div class="git-aside-activity-time">{{ relativeTime(c.commitTime) }}</div>
              </div>
            </div>
          </div>
          <div v-else class="git-aside-empty">{{ $t('git.repo.empty-repository') }}</div>
        </section>
      </aside>
    </div>

    <div v-show="activeTab === 'commits'" v-loading="commitsLoading" class="git-browse-commits">
      <div v-for="c in commits" :key="c.id" class="git-browse-commit-item">
        <cat2-bug-avatar :member="memberForAuthor(c)" :size="32" />
        <div class="git-browse-commit-item-body">
          <div class="git-browse-commit-item-msg">{{ c.message }}</div>
          <div class="git-browse-commit-item-meta">
            <span>{{ c.authorName }}</span>
            <span>{{ relativeTime(c.commitTime) }}</span>
            <span class="git-browse-commit-hash">{{ c.shortId }}</span>
          </div>
        </div>
      </div>
      <div v-if="!commitsLoading && !commits.length" class="git-browse-empty">{{ $t('git.repo.empty-repository') }}</div>
    </div>
  </div>
</template>

<script>
import { FolderOpened, Folder, Document, Share, ArrowDown, ArrowRight, Link } from '@element-plus/icons-vue'
import {
  getGitRepo,
  listGitRepoBranches,
  listGitRepoTree,
  getGitRepoBlob,
  listGitRepoCommits
} from '@/api/system/gitRepo'
import { listMemberOfProject } from '@/api/system/project'
import Cat2BugAvatar from '@/components/Cat2BugAvatar'
import { useUserStore } from '@/stores/user'
import { formatTime } from '@/utils/index'
import { parseTime } from '@/utils/ruoyi'

export default {
  name: 'GitRepoBrowse',
  components: { FolderOpened, Folder, Document, Share, ArrowDown, ArrowRight, Link, Cat2BugAvatar },
  data() {
    return {
      repositoryId: null,
      repoName: '',
      repoRemark: '',
      repoSlug: '',
      repoProjectId: null,
      cloneUrlPath: '',
      defaultBranch: '',
      branches: [],
      branchesLoading: false,
      currentRef: null,
      activeTab: 'code',
      currentPath: '',
      viewMode: 'tree',
      treeEntries: [],
      treeLoading: false,
      blob: null,
      blobLoading: false,
      commits: [],
      commitsLoading: false,
      pageLoading: false,
      projectMembers: []
    }
  },
  computed: {
    breadcrumbSegments() {
      const path = this.viewMode === 'blob' ? this.blob && this.blob.path : this.currentPath
      if (!path) return []
      return path.split('/').filter(Boolean)
    },
    latestCommit() {
      return this.commits.length ? this.commits[0] : null
    },
    recentActivity() {
      return this.commits.slice(0, 5)
    },
    fullCloneUrl() {
      const path = this.cloneUrlPath
        || (this.repoProjectId && this.repoSlug ? `/git/p/${this.repoProjectId}/${this.repoSlug}.git` : '')
      if (!path) return ''
      return window.location.origin + path
    }
  },
  created() {
    this.repositoryId = Number(this.$route.params.repositoryId)
    if (this.$route.query.name) this.repoName = String(this.$route.query.name)
    this.init()
  },
  methods: {
    goBack() {
      this.$router.push({ path: '/project/git-repo' })
    },
    timeValue(t) {
      if (!t) return 0
      const n = new Date(t).getTime()
      return Number.isNaN(n) ? 0 : n
    },
    relativeTime(t) {
      if (!t) return '—'
      const ms = this.timeValue(t)
      if (!ms) return parseTime(t) || '—'
      return formatTime(ms)
    },
    /** 按 git author 匹配项目成员，用于展示平台头像 */
    memberForAuthor(commit) {
      const name = (commit && commit.authorName ? String(commit.authorName) : '').trim()
      const email = (commit && commit.authorEmail ? String(commit.authorEmail) : '').trim()
      const nameKey = name.toLowerCase()
      const emailKey = email.toLowerCase()
      const matched = this.projectMembers.find(m => {
        if (!m) return false
        const userName = String(m.userName || '').trim().toLowerCase()
        const nickName = String(m.nickName || '').trim().toLowerCase()
        const mail = String(m.email || '').trim().toLowerCase()
        return (nameKey && (nameKey === userName || nameKey === nickName))
          || (emailKey && emailKey === mail)
      })
      if (matched) return matched
      const userStore = useUserStore()
      const selfName = String(userStore.name || '').trim().toLowerCase()
      if (nameKey && selfName && nameKey === selfName) {
        return {
          userName: userStore.name,
          nickName: userStore.name,
          avatar: userStore.avatar
        }
      }
      return {
        nickName: name || '?',
        userName: name || '?',
        email: email || undefined
      }
    },
    loadProjectMembers(projectId) {
      if (!projectId) {
        this.projectMembers = []
        return
      }
      listMemberOfProject(projectId, { pageNum: 1, pageSize: 500 }).then(res => {
        const rows = res.rows || res.data || []
        this.projectMembers = Array.isArray(rows) ? rows : []
      }).catch(() => {
        this.projectMembers = []
      })
    },
    copyText(text) {
      if (!text) {
        this.$modal.msgError(this.$t('git.repo.copy-empty'))
        return
      }
      const onOk = () => this.$modal.msgSuccess(this.$t('git.repo.copy-success'))
      const onFail = () => this.$modal.msgError(this.$t('git.repo.copy-failed'))
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(onOk).catch(() => {
          if (this.copyTextFallback(text)) onOk()
          else onFail()
        })
        return
      }
      if (this.copyTextFallback(text)) onOk()
      else onFail()
    },
    copyTextFallback(text) {
      try {
        const input = document.createElement('textarea')
        input.value = text
        input.setAttribute('readonly', 'readonly')
        input.style.position = 'fixed'
        input.style.left = '-9999px'
        document.body.appendChild(input)
        input.select()
        const ok = document.execCommand('copy')
        document.body.removeChild(input)
        return ok
      } catch (e) {
        return false
      }
    },
    copyCloneUrl() {
      this.copyText(this.fullCloneUrl)
    },
    init() {
      this.pageLoading = true
      getGitRepo(this.repositoryId).then(response => {
        const repo = response.data || {}
        this.repoName = repo.name || this.repoName
        this.repoRemark = repo.remark || ''
        this.defaultBranch = repo.defaultBranch || ''
        this.repoSlug = repo.slug || ''
        this.repoProjectId = repo.projectId
        this.cloneUrlPath = repo.cloneUrl || ''
        this.loadProjectMembers(repo.projectId)
      }).catch(() => {}).finally(() => {
        this.pageLoading = false
      })
      this.loadBranches()
    },
    loadBranches() {
      this.branchesLoading = true
      listGitRepoBranches(this.repositoryId).then(response => {
        this.branches = response.data || []
        this.branchesLoading = false
        const current = this.branches.find(b => b.default) || this.branches[0]
        this.currentRef = current ? current.name : null
        this.loadTree('')
        this.loadCommits()
      }).catch(() => {
        this.branchesLoading = false
      })
    },
    onRefChange() {
      this.currentPath = ''
      this.viewMode = 'tree'
      this.loadTree('')
      this.loadCommits()
    },
    breadcrumbPathAt(idx) {
      return this.breadcrumbSegments.slice(0, idx + 1).join('/')
    },
    navigateToPath(path) {
      this.viewMode = 'tree'
      this.loadTree(path)
    },
    handleTreeRowClick(row) {
      if (row.type === 'tree') {
        this.loadTree(row.path)
      } else {
        this.loadBlob(row.path)
      }
    },
    loadTree(path) {
      if (!this.currentRef) {
        this.treeEntries = []
        return
      }
      this.treeLoading = true
      this.viewMode = 'tree'
      this.currentPath = path || ''
      listGitRepoTree(this.repositoryId, this.currentRef, this.currentPath).then(response => {
        this.treeEntries = response.data || []
        this.treeLoading = false
      }).catch(() => {
        this.treeEntries = []
        this.treeLoading = false
      })
    },
    loadBlob(path) {
      this.blobLoading = true
      this.viewMode = 'blob'
      getGitRepoBlob(this.repositoryId, this.currentRef, path).then(response => {
        this.blob = response.data || null
        this.blobLoading = false
      }).catch(() => {
        this.blob = null
        this.blobLoading = false
      })
    },
    loadCommits() {
      if (!this.currentRef) {
        this.commits = []
        return
      }
      this.commitsLoading = true
      listGitRepoCommits(this.repositoryId, this.currentRef, 50).then(response => {
        this.commits = response.data || []
        this.commitsLoading = false
      }).catch(() => {
        this.commits = []
        this.commitsLoading = false
      })
    }
  }
}
</script>

<style lang="scss" scoped>
/* 使用 theme-dark.scss 全局语义色：--text-color-* / --card-bg / --border-color-* */
.git-browse-page {
  padding: 16px 20px 24px;
  box-sizing: border-box;
  color: var(--text-color-regular);
}

.git-browse-top {
  margin-bottom: 4px;
}

.git-browse-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.git-browse-title-icon {
  color: var(--text-color-secondary);
}

.git-browse-repo-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-color-primary);
}

.git-browse-tabs {
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

.git-browse-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 20px;
  align-items: start;
}

.git-browse-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cat2bug-toolbar-section-gap, 10px);
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.git-browse-toolbar-left,
.git-browse-toolbar-right {
  display: flex;
  align-items: center;
  gap: var(--cat2bug-toolbar-item-gap, 10px);
  flex-wrap: wrap;
}

.git-browse-branch-select {
  width: 220px;
  min-width: 180px;
}

.git-browse-meta-chip {
  font-size: 13px;
  color: var(--text-color-secondary);
}

.git-browse-clone-btn {
  font-weight: 500;
}

.git-browse-clone-panel {
  padding: 12px;
  width: 420px;
  max-width: 80vw;
  background: var(--dropdown-bg);
  border: 1px solid var(--border-color-light);
  border-radius: 4px;
  box-shadow: var(--el-box-shadow-light);
  color: var(--text-color-regular);
}

.git-browse-clone-label {
  font-size: 12px;
  color: var(--text-color-secondary);
  margin-bottom: 6px;
}

.git-browse-clone-row {
  display: flex;
  gap: var(--cat2bug-toolbar-item-gap, 10px);
  align-items: center;
  margin-bottom: 8px;

  .el-input {
    flex: 1 1 auto;
    min-width: 0;
  }
}

.git-browse-commit-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  margin-bottom: 10px;
  border: 1px solid var(--border-color-light);
  border-radius: 6px;
  background: var(--card-bg);
  flex-wrap: wrap;
}

.git-browse-commit-bar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1;
  flex-wrap: wrap;
}

.git-browse-author {
  font-weight: 600;
  font-size: 13px;
  color: var(--text-color-primary);
}

.git-browse-commit-bar-left :deep(.member-avatar),
.git-aside-activity-item :deep(.member-avatar),
.git-browse-commit-item :deep(.member-avatar) {
  flex-shrink: 0;
}

.git-browse-commit-msg {
  flex: 1;
  min-width: 80px;
  font-size: 13px;
  color: var(--text-color-regular);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.git-browse-commit-hash {
  font-family: Menlo, Consolas, monospace;
  font-size: 12px;
  color: var(--el-color-primary);
}

.git-browse-commit-time {
  font-size: 12px;
  color: var(--text-color-placeholder);
}

.git-browse-commit-total {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--el-color-primary);
  text-decoration: none;
  white-space: nowrap;

  &:hover {
    text-decoration: underline;
  }
}

.git-browse-breadcrumb {
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--text-color-secondary);

  a {
    color: var(--el-color-primary);
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }
}

.git-browse-tree-card,
.git-browse-blob-card {
  border: 1px solid var(--border-color-light);
  border-radius: 6px;
  overflow: hidden;
  background: var(--card-bg);
  min-height: 120px;
  color: var(--text-color-regular);
}

.git-browse-tree-row {
  display: grid;
  grid-template-columns: minmax(140px, 1.1fr) minmax(120px, 1.6fr) 100px;
  gap: 12px;
  align-items: center;
  padding: 10px 14px;
  border-bottom: 1px solid var(--border-color-light);
  cursor: pointer;
  font-size: 13px;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: var(--table-row-hover-bg);
  }
}

.git-browse-tree-name {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  color: var(--text-color-primary);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.git-browse-file-icon {
  color: var(--text-color-secondary);
  flex-shrink: 0;

  &.is-dir {
    color: var(--text-color-regular);
  }
}

.git-browse-tree-msg {
  color: var(--text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.git-browse-tree-time {
  text-align: right;
  color: var(--text-color-placeholder);
  font-size: 12px;
}

.git-browse-code {
  margin: 0;
  padding: 14px 16px;
  overflow: auto;
  max-height: 70vh;
  font-family: Menlo, Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre;
  color: var(--text-color-regular);
  background: var(--input-bg);
}

.git-browse-truncated {
  padding: 8px 14px;
  color: var(--el-color-warning, #e6a23c);
  font-size: 12px;
  border-bottom: 1px solid var(--border-color-light);
}

.git-browse-empty {
  padding: 40px 0;
  text-align: center;
  color: var(--text-color-placeholder);
  font-size: 13px;
}

.git-browse-aside {
  position: sticky;
  top: 12px;
  padding: 14px 16px;
  border: 1px solid var(--border-color-light);
  border-radius: 6px;
  background: var(--card-bg);
  color: var(--text-color-regular);
}

.git-aside-block {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color-light);

  &:last-child {
    border-bottom: none;
  }
}

.git-aside-title {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-color-primary);
}

.git-aside-desc {
  margin: 0 0 12px;
  font-size: 13px;
  line-height: 1.5;
  color: var(--text-color-secondary);
}

.git-aside-links {
  list-style: none;
  margin: 0;
  padding: 0;

  li {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
    font-size: 13px;
    color: var(--text-color-regular);

    a {
      color: var(--el-color-primary);
      text-decoration: none;

      &:hover {
        text-decoration: underline;
      }
    }
  }
}

.git-aside-activity-item {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.git-aside-activity-body {
  min-width: 0;
  flex: 1;
}

.git-aside-activity-text {
  font-size: 12px;
  color: var(--text-color-regular);
  line-height: 1.4;
}

.git-aside-hash {
  font-family: Menlo, Consolas, monospace;
  color: var(--el-color-primary);
}

.git-aside-activity-msg {
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.git-aside-activity-time {
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-color-placeholder);
}

.git-aside-empty {
  font-size: 12px;
  color: var(--text-color-placeholder);
}

.git-browse-commits {
  border: 1px solid var(--border-color-light);
  border-radius: 6px;
  overflow: hidden;
  background: var(--card-bg);
}

.git-browse-commit-item {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-color-light);

  &:last-child {
    border-bottom: none;
  }
}

.git-browse-commit-item-msg {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-color-primary);
  margin-bottom: 4px;
}

.git-browse-commit-item-meta {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 12px;
  color: var(--text-color-secondary);
}

@media (max-width: 960px) {
  .git-browse-layout {
    grid-template-columns: 1fr;
  }

  .git-browse-aside {
    position: static;
    order: 2;
  }

  .git-browse-tree-row {
    grid-template-columns: 1fr;
    gap: 4px;
  }

  .git-browse-tree-time {
    text-align: left;
  }
}
</style>

