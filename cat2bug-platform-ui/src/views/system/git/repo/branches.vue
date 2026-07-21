<template>
  <div class="app-container git-branches-page" v-loading="pageLoading">
    <git-repo-shell :repository-id="repositoryId" :repo-name="repoName" active-tab="branches">
      <template #title-extra>
        <span class="git-branches-title-separator">/</span>
        <span class="git-branches-title-sub">{{ $t('git.repo.branches.title') }}</span>
      </template>

      <div class="git-branches-toolbar">
        <el-input
          v-model="branchName"
          class="git-branches-search"
          clearable
          size="small"
          :placeholder="$t('git.repo.branches.search-placeholder')"
          @input="scheduleSearch"
          @clear="loadBranches"
          @keyup.enter="loadBranches"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button
          v-if="canManageGitRepo"
          type="primary"
          plain
          size="small"
          @click="openCreateDialog"
        >
          <el-icon class="el-icon--left"><Plus /></el-icon>
          {{ $t('git.repo.branches.create') }}
        </el-button>
      </div>

      <section class="git-branches-section">
        <h2>{{ $t('git.repo.branches.default-section') }}</h2>
        <div v-if="defaultBranchInfo" class="git-branch-card git-branch-card--default">
          <div class="git-branch-card__main">
            <div class="git-branch-card__name">
              <el-icon><ForkSpoon /></el-icon>
              <a
                href="javascript:;"
                class="git-branch-card__link"
                :title="defaultBranchInfo.name"
                @click.prevent="openBranchBrowse(defaultBranchInfo)"
              >{{ defaultBranchInfo.name }}</a>
              <el-tag size="small" effect="plain">{{ $t('git.repo.branches.default-tag') }}</el-tag>
            </div>
            <div class="git-branch-card__update" :title="lastUpdateTitle(defaultBranchInfo)">
              {{ lastUpdate(defaultBranchInfo) }}
            </div>
          </div>
        </div>
        <div v-else-if="!branchesLoading" class="git-branches-empty">
          {{ $t('git.repo.branches.no-default') }}
        </div>
      </section>

      <section class="git-branches-section">
        <div class="git-branches-section__header">
          <h2>{{ $t('git.repo.branches.all-section') }}</h2>
          <span>{{ $t('git.repo.branches.count', { n: branches.length }) }}</span>
        </div>
        <div v-loading="branchesLoading" class="git-branch-list">
          <div v-for="branch in branches" :key="branch.name" class="git-branch-card">
            <div class="git-branch-card__main">
              <div class="git-branch-card__name">
                <el-icon><ForkSpoon /></el-icon>
                <a
                  href="javascript:;"
                  class="git-branch-card__link"
                  :title="branch.name"
                  @click.prevent="openBranchBrowse(branch)"
                >{{ branch.name }}</a>
                <el-tag v-if="branch.default" size="small" effect="plain">
                  {{ $t('git.repo.branches.default-tag') }}
                </el-tag>
              </div>
              <div class="git-branch-card__update" :title="lastUpdateTitle(branch)">
                {{ lastUpdate(branch) }}
                <span v-if="branch.shortId" class="git-branch-card__sha">{{ branch.shortId }}</span>
              </div>
            </div>
            <div class="git-branch-card__meta">
              <span v-if="!branch.default" class="git-branch-card__compare">
                <span class="is-ahead">{{ $t('git.repo.branches.ahead', { n: branch.ahead || 0 }) }}</span>
                <span class="is-behind">{{ $t('git.repo.branches.behind', { n: branch.behind || 0 }) }}</span>
              </span>
              <div class="git-branch-card__actions">
                <el-button
                  v-if="canCreatePullRequest && !branch.default && defaultBranch"
                  link
                  type="primary"
                  @click="createPullRequest(branch)"
                >{{ $t('git.repo.branches.create-pr') }}</el-button>
                <el-dropdown v-if="canManageGitRepo" trigger="click" @command="command => handleManageCommand(command, branch)">
                  <el-button link :icon="MoreFilled" :aria-label="$t('git.repo.branches.manage-actions')" />
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item v-if="!branch.default" command="default">
                        {{ $t('git.repo.branches.set-default') }}
                      </el-dropdown-item>
                      <el-dropdown-item v-if="!branch.default" command="delete" divided>
                        <span class="git-branches-delete-action">{{ $t('delete') }}</span>
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>
          </div>
          <el-empty
            v-if="!branchesLoading && !branches.length"
            :description="$t('git.repo.branches.empty')"
            :image-size="80"
          />
        </div>
      </section>
    </git-repo-shell>

    <el-dialog
      v-model="createDialogVisible"
      :title="$t('git.repo.branches.create')"
      width="480px"
      append-to-body
      destroy-on-close
      @closed="resetCreateForm"
    >
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="96px">
        <el-form-item :label="$t('git.repo.branches.name')" prop="name">
          <el-input
            v-model="createForm.name"
            :placeholder="$t('git.repo.branches.name-placeholder')"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-form-item :label="$t('git.repo.branches.from-ref')" prop="fromRef">
          <el-select
            v-model="createForm.fromRef"
            clearable
            filterable
            style="width: 100%"
            :placeholder="$t('git.repo.branches.from-ref-placeholder')"
          >
            <el-option v-for="branch in allBranches" :key="branch.name" :label="branch.name" :value="branch.name" />
          </el-select>
          <div class="git-branches-form-tip">{{ $t('git.repo.branches.from-ref-tip') }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">{{ $t('cancel') }}</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="submitCreate">
          {{ $t('ok') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ForkSpoon, MoreFilled, Plus, Search } from '@element-plus/icons-vue'
import {
  createGitRepoBranch,
  delGitRepoBranch,
  getGitRepo,
  listGitRepoBranches,
  setGitRepoDefaultBranch
} from '@/api/system/gitRepo'
import { checkPermi } from '@/utils/permission'
import { formatTime } from '@/utils/index'
import { parseTime } from '@/utils/ruoyi'
import GitRepoShell from './GitRepoShell.vue'

export default {
  name: 'GitRepoBranches',
  components: { ForkSpoon, MoreFilled, Plus, Search, GitRepoShell },
  data() {
    return {
      repositoryId: null,
      repoName: '',
      defaultBranch: '',
      branches: [],
      allBranches: [],
      pageLoading: false,
      branchesLoading: false,
      branchName: '',
      searchTimer: null,
      createDialogVisible: false,
      createSubmitting: false,
      createForm: { name: '', fromRef: '' },
      createRules: {
        name: [
          { required: true, message: this.$t('git.repo.branches.name-required'), trigger: 'blur' },
          { max: 100, message: this.$t('git.repo.branches.name-max-length'), trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    canManageGitRepo() {
      return checkPermi(['system:git:repo:manage'])
    },
    canCreatePullRequest() {
      return checkPermi(['system:git:repo:push'])
    },
    defaultBranchInfo() {
      return this.branches.find(branch => branch.default) || this.allBranches.find(branch => branch.default) || null
    }
  },
  created() {
    this.repositoryId = Number(this.$route.params.repositoryId)
    this.repoName = String(this.$route.query.name || '')
    this.init()
  },
  beforeUnmount() {
    window.clearTimeout(this.searchTimer)
  },
  methods: {
    init() {
      this.pageLoading = true
      getGitRepo(this.repositoryId).then(response => {
        const repo = response.data || {}
        this.repoName = repo.name || this.repoName
        this.defaultBranch = repo.defaultBranch || ''
      }).finally(() => {
        this.pageLoading = false
      })
      this.loadBranches()
      this.loadAllBranches()
    },
    openBranchBrowse(branch) {
      if (!branch || !branch.name || !this.repositoryId) return
      this.$router.push({
        name: 'GitRepoBrowse',
        params: { repositoryId: String(this.repositoryId) },
        query: {
          ref: branch.name,
          ...(this.repoName ? { name: this.repoName } : {})
        }
      }).catch(() => {})
    },
    scheduleSearch() {
      window.clearTimeout(this.searchTimer)
      this.searchTimer = window.setTimeout(() => this.loadBranches(), 300)
    },
    loadBranches() {
      this.branchesLoading = true
      listGitRepoBranches(this.repositoryId, (this.branchName || '').trim() || undefined).then(response => {
        this.branches = response.data || []
        const currentDefault = this.branches.find(branch => branch.default)
        if (currentDefault) this.defaultBranch = currentDefault.name
      }).catch(() => {
        this.branches = []
      }).finally(() => {
        this.branchesLoading = false
      })
    },
    loadAllBranches() {
      listGitRepoBranches(this.repositoryId).then(response => {
        this.allBranches = response.data || []
        const currentDefault = this.allBranches.find(branch => branch.default)
        if (currentDefault) this.defaultBranch = currentDefault.name
      }).catch(() => {
        this.allBranches = []
      })
    },
    timeValue(time) {
      const value = new Date(time).getTime()
      return Number.isNaN(value) ? 0 : value
    },
    relativeTime(time) {
      if (!time) return '—'
      const value = this.timeValue(time)
      return value ? formatTime(value) : (parseTime(time) || '—')
    },
    lastUpdate(branch) {
      if (!branch || !branch.commitTime) return this.$t('git.repo.branches.no-commit')
      return this.$t('git.repo.branches.last-update', {
        author: branch.authorName || branch.authorEmail || '—',
        time: this.relativeTime(branch.commitTime)
      })
    },
    lastUpdateTitle(branch) {
      if (!branch || !branch.commitTime) return this.$t('git.repo.branches.no-commit')
      return `${branch.authorName || branch.authorEmail || '—'} · ${parseTime(branch.commitTime) || branch.commitTime}`
    },
    openCreateDialog() {
      this.createForm.fromRef = this.defaultBranch || ''
      this.createDialogVisible = true
    },
    resetCreateForm() {
      this.createForm = { name: '', fromRef: '' }
      if (this.$refs.createFormRef) this.$refs.createFormRef.clearValidate()
    },
    submitCreate() {
      this.$refs.createFormRef.validate(valid => {
        if (!valid) return
        this.createSubmitting = true
        const data = {
          name: this.createForm.name.trim(),
          ...(this.createForm.fromRef ? { fromRef: this.createForm.fromRef } : {})
        }
        createGitRepoBranch(this.repositoryId, data).then(() => {
          this.$modal.msgSuccess(this.$t('git.repo.branches.create-success'))
          this.createDialogVisible = false
          this.loadBranches()
          this.loadAllBranches()
        }).finally(() => {
          this.createSubmitting = false
        })
      })
    },
    handleManageCommand(command, branch) {
      if (command === 'default') this.setDefaultBranch(branch)
      if (command === 'delete') this.deleteBranch(branch)
    },
    setDefaultBranch(branch) {
      this.$modal.confirm(
        this.$t('git.repo.branches.set-default-confirm', { name: branch.name }),
        this.$t('git.repo.branches.set-default')
      ).then(() => setGitRepoDefaultBranch(this.repositoryId, branch.name))
        .then(() => {
          this.defaultBranch = branch.name
          this.$modal.msgSuccess(this.$t('git.repo.branches.set-default-success'))
          this.loadBranches()
          this.loadAllBranches()
        }).catch(() => {})
    },
    deleteBranch(branch) {
      this.$modal.confirm(
        this.$t('git.repo.branches.delete-confirm', { name: branch.name }),
        this.$t('delete'),
        { type: 'warning' }
      ).then(() => delGitRepoBranch(this.repositoryId, branch.name))
        .then(() => {
          this.$modal.msgSuccess(this.$t('delete-success'))
          this.loadBranches()
          this.loadAllBranches()
        }).catch(() => {})
    },
    createPullRequest(branch) {
      if (!branch || !branch.name || !this.defaultBranch) return
      this.$router.push({
        path: `/project/git-repo/browse/${this.repositoryId}/pulls/new`,
        query: {
          source: branch.name,
          target: this.defaultBranch
        }
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.git-branches-page {
  min-height: 0;
  max-height: calc(100vh - 84px);
  overflow-y: auto;
  padding: 16px 20px 24px;
  color: var(--text-color-regular);
}

.git-branch-card__name,
.git-branch-card__meta,
.git-branch-card__actions,
.git-branches-section__header {
  display: flex;
  align-items: center;
}

.git-branches-title-separator {
  color: var(--text-color-secondary);
  font-weight: 400;
}

.git-branches-title-sub {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-color-regular);
}

.git-branches-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cat2bug-toolbar-section-gap, 10px);
  margin-bottom: 24px;
}

.git-branches-search {
  width: 320px;
  max-width: 100%;
}

.git-branches-section {
  margin-bottom: 28px;

  h2 {
    margin: 0 0 10px;
    font-size: 16px;
    color: var(--text-color-primary);
  }
}

.git-branches-section__header {
  justify-content: space-between;
  gap: 12px;

  > span {
    font-size: 13px;
    color: var(--text-color-secondary);
  }
}

.git-branch-list,
.git-branch-card {
  border: 1px solid var(--border-color-light);
  background: var(--card-bg);
}

.git-branch-list {
  overflow: hidden;
  border-radius: 6px;
}

.git-branch-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-width: 0 0 1px;

  &:last-child {
    border-bottom: 0;
  }

  &--default {
    border-width: 1px;
    border-radius: 6px;
  }
}

.git-branch-card__main {
  min-width: 0;
}

.git-branch-card__name {
  flex-wrap: wrap;
  gap: 7px;
  color: var(--text-color-primary);
  font-weight: 600;

  .el-icon {
    color: var(--text-color-secondary);
  }
}

.git-branch-card__link {
  color: inherit;
  text-decoration: none;
  cursor: pointer;

  &:hover {
    color: var(--el-color-primary);
    text-decoration: underline;
  }
}

.git-branch-card__update {
  margin-top: 5px;
  overflow: hidden;
  font-size: 12px;
  color: var(--text-color-secondary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.git-branch-card__sha {
  margin-left: 8px;
  font-family: Menlo, Consolas, monospace;
  color: var(--text-color-regular);
}

.git-branch-card__meta {
  justify-content: flex-end;
  gap: 18px;
  flex-shrink: 0;
}

.git-branch-card__compare {
  display: inline-flex;
  gap: 10px;
  font-size: 12px;

  .is-ahead {
    color: var(--el-color-success);
  }

  .is-behind {
    color: var(--el-color-warning);
  }
}

.git-branch-card__actions {
  min-width: 112px;
  justify-content: flex-end;
  gap: 4px;
}

.git-branches-empty {
  padding: 24px 0;
  color: var(--text-color-placeholder);
  font-size: 13px;
  text-align: center;
}

.git-branches-delete-action {
  color: var(--cat2bug-delete-color, #e64545);
}

.git-branches-form-tip {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--text-color-secondary);
}

@media (max-width: 680px) {
  .git-branches-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .git-branches-search {
    width: 100%;
  }

  .git-branch-card,
  .git-branch-card__meta {
    align-items: flex-start;
  }

  .git-branch-card {
    flex-direction: column;
  }

  .git-branch-card__meta {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
