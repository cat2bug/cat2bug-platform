<template>
  <div class="app-container git-tags-page" v-loading="pageLoading">
    <git-repo-shell :repository-id="repositoryId" :repo-name="repoName" active-tab="tags">
      <div class="git-tags-toolbar">
        <el-input
          v-model="tagQuery"
          class="git-tags-search"
          clearable
          size="small"
          :placeholder="$t('git.repo.tags.search-placeholder')"
          @input="scheduleSearch"
          @clear="loadTags"
          @keyup.enter="loadTags"
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
          {{ $t('git.repo.tags.create') }}
        </el-button>
      </div>

      <section class="git-tags-section">
        <div class="git-tags-section__header">
          <h2>{{ $t('git.repo.tags.all-section') }}</h2>
          <span>{{ $t('git.repo.tags.count', { n: tags.length }) }}</span>
        </div>
        <div v-loading="tagsLoading" class="git-tag-list">
          <div v-if="tags.length" class="git-tag-list__head" aria-hidden="true">
            <div class="git-tag-col git-tag-col--name">{{ $t('git.repo.tags.col-name') }}</div>
            <div class="git-tag-col git-tag-col--desc">{{ $t('git.repo.tags.col-description') }}</div>
            <div class="git-tag-col git-tag-col--commit">{{ $t('git.repo.tags.col-commit') }}</div>
            <div class="git-tag-col git-tag-col--actions" />
          </div>
          <div v-for="tag in tags" :key="tag.name" class="git-tag-card">
            <div class="git-tag-col git-tag-col--name">
              <div class="git-tag-card__name">
                <git-tag-icon class="git-tag-card__icon" />
                <span class="git-tag-card__title" :title="tag.name">{{ tag.name }}</span>
              </div>
            </div>
            <div
              class="git-tag-col git-tag-col--desc"
              :class="{ 'is-empty': !tagMessage(tag) }"
              :title="tagMessage(tag) || undefined"
            >{{ tagMessage(tag) || $t('git.repo.tags.no-description') }}</div>
            <div class="git-tag-col git-tag-col--commit" :title="commitTitle(tag)">
              <template v-if="tag.commitId || tag.shortId">
                <a
                  href="javascript:;"
                  class="git-tag-card__commit-subject is-link"
                  @click.prevent="openCommitDetail(tag)"
                >{{ tagCommitSubject(tag) || '—' }}</a>
                <div class="git-tag-card__commit-meta">
                  <span>{{ commitMetaLine(tag) }}</span>
                  <a
                    v-if="tagShortId(tag)"
                    href="javascript:;"
                    class="git-tag-card__sha is-link"
                    @click.prevent="openCommitDetail(tag)"
                  >{{ tagShortId(tag) }}</a>
                </div>
              </template>
              <div v-else class="git-tag-card__commit-subject is-empty">
                {{ $t('git.repo.tags.no-commit') }}
              </div>
            </div>
            <div class="git-tag-col git-tag-col--actions">
              <div class="git-tag-card__actions">
                <el-button link type="primary" @click="downloadTag(tag)">
                  {{ $t('git.repo.tags.download') }}
                </el-button>
                <el-button
                  v-if="tag.releaseId"
                  link
                  type="primary"
                  @click="openRelease(tag)"
                >{{ $t('git.repo.tags.open-release') }}</el-button>
                <el-button
                  v-else-if="canManageGitRepo"
                  link
                  type="primary"
                  @click="createReleaseFromTag(tag)"
                >{{ $t('git.repo.tags.create-release') }}</el-button>
                <el-button
                  v-if="canManageGitRepo"
                  link
                  type="danger"
                  @click="deleteTag(tag)"
                >{{ $t('delete') }}</el-button>
              </div>
            </div>
          </div>
          <el-empty
            v-if="!tagsLoading && !tags.length"
            :description="$t('git.repo.tags.empty')"
            :image-size="80"
          />
        </div>
      </section>
    </git-repo-shell>

    <el-dialog
      v-model="createDialogVisible"
      :title="$t('git.repo.tags.create')"
      width="520px"
      append-to-body
      destroy-on-close
      @closed="resetCreateForm"
    >
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="96px">
        <el-form-item :label="$t('git.repo.tags.name')" prop="name">
          <el-input
            v-model="createForm.name"
            :placeholder="$t('git.repo.tags.name-placeholder')"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-form-item :label="$t('git.repo.tags.message')" prop="message">
          <el-input
            v-model="createForm.message"
            type="textarea"
            :rows="3"
            :placeholder="$t('git.repo.tags.message-placeholder')"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item :label="$t('git.repo.tags.target-ref')" prop="targetRef">
          <el-select
            v-model="createForm.targetRef"
            filterable
            style="width: 100%"
            :placeholder="$t('git.repo.tags.target-ref-placeholder')"
          >
            <el-option
              v-for="branch in branches"
              :key="branch.name"
              :label="branch.name"
              :value="branch.name"
            />
          </el-select>
          <div class="git-tags-form-tip">{{ $t('git.repo.tags.target-ref-tip') }}</div>
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
import { Plus, Search } from '@element-plus/icons-vue'
import {
  createGitRepoTag,
  delGitRepoTag,
  downloadGitRepoArchive,
  getGitRepo,
  listGitRepoBranches,
  listGitRepoTags
} from '@/api/system/gitRepo'
import { checkPermi } from '@/utils/permission'
import { formatTime } from '@/utils/index'
import { parseTime } from '@/utils/ruoyi'
import { gitRepoBrowseRoute, gitRepoCommitDetailRoute, gitRepoReleaseDetailRoute } from '@/utils/git-repo-routes'
import GitRepoShell from './GitRepoShell.vue'
import GitTagIcon from './GitTagIcon.vue'

export default {
  name: 'GitRepoTags',
  components: { Plus, Search, GitRepoShell, GitTagIcon },
  data() {
    return {
      repositoryId: null,
      repoName: '',
      defaultBranch: '',
      tags: [],
      branches: [],
      pageLoading: false,
      tagsLoading: false,
      tagQuery: '',
      searchTimer: null,
      createDialogVisible: false,
      createSubmitting: false,
      createForm: {
        name: '',
        message: '',
        targetRef: ''
      },
      createRules: {
        name: [
          { required: true, message: this.$t('git.repo.tags.name-required'), trigger: 'blur' },
          { max: 100, message: this.$t('git.repo.tags.name-max-length'), trigger: 'blur' }
        ],
        targetRef: [
          { required: true, message: this.$t('git.repo.tags.target-required'), trigger: 'change' }
        ]
      }
    }
  },
  computed: {
    canManageGitRepo() {
      return checkPermi(['system:git:repo:manage'])
    }
  },
  created() {
    this.repositoryId = Number(this.$route.params.repositoryId)
    this.repoName = String(this.$route.params.slug || '')
    this.init()
  },
  beforeUnmount() {
    window.clearTimeout(this.searchTimer)
  },
  methods: {
    init() {
      this.pageLoading = true
      Promise.all([
        getGitRepo(this.repositoryId).then(response => {
          const repo = response.data || {}
          this.repoName = repo.name || this.repoName
          return repo.defaultBranch || ''
        }).catch(() => ''),
        listGitRepoBranches(this.repositoryId).then(response => {
          this.branches = response.data || []
          return this.branches
        }).catch(() => {
          this.branches = []
          return []
        })
      ]).then(([metaDefault, branches]) => {
        const list = branches || []
        const liveDefault = list.find(branch => branch && branch.default)
        // Prefer live refs; metadata may still be master while HEAD is another branch
        this.defaultBranch = (liveDefault && liveDefault.name)
          || (list.some(b => b && b.name === metaDefault) ? metaDefault : '')
          || (list[0] && list[0].name)
          || ''
      }).finally(() => {
        this.pageLoading = false
      })
      this.loadTags()
    },
    scheduleSearch() {
      window.clearTimeout(this.searchTimer)
      this.searchTimer = window.setTimeout(() => this.loadTags(), 300)
    },
    loadTags() {
      this.tagsLoading = true
      listGitRepoTags(this.repositoryId, (this.tagQuery || '').trim() || undefined).then(response => {
        this.tags = response.data || []
      }).catch(() => {
        this.tags = []
      }).finally(() => {
        this.tagsLoading = false
      })
    },
    tagMessage(tag) {
      if (!tag) return ''
      return (tag.message || '').trim()
    },
    tagShortId(tag) {
      if (!tag) return ''
      if (tag.shortId) return tag.shortId
      const id = tag.commitId || ''
      return id ? String(id).slice(0, 7) : ''
    },
    tagCommitSubject(tag) {
      if (!tag) return ''
      return tag.commitSubject || tag.subject || tag.commitMessage || tag.shortMessage || ''
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
    commitMetaLine(tag) {
      if (!tag || !(tag.commitId || tag.shortId)) return ''
      const author = tag.authorName || tag.authorEmail || '—'
      if (tag.commitTime) {
        return this.$t('git.repo.tags.commit-meta', {
          author,
          time: this.relativeTime(tag.commitTime)
        })
      }
      return author
    },
    commitTitle(tag) {
      if (!tag || !(tag.commitId || tag.shortId)) {
        return this.$t('git.repo.tags.no-commit')
      }
      const parts = [
        this.tagCommitSubject(tag),
        tag.authorName || tag.authorEmail || '',
        tag.commitTime ? (parseTime(tag.commitTime) || tag.commitTime) : '',
        tag.commitId || ''
      ].filter(Boolean)
      return parts.join(' · ')
    },
    openCreateDialog() {
      const names = this.branches.map(b => b && b.name).filter(Boolean)
      const fromRef = (this.defaultBranch && names.includes(this.defaultBranch) ? this.defaultBranch : '')
        || names[0]
        || ''
      this.createForm.targetRef = fromRef
      this.createDialogVisible = true
    },
    resetCreateForm() {
      this.createForm = {
        name: '',
        message: '',
        targetRef: ''
      }
      if (this.$refs.createFormRef) this.$refs.createFormRef.clearValidate()
    },
    submitCreate() {
      this.$refs.createFormRef.validate(valid => {
        if (!valid) return
        const targetRef = (this.createForm.targetRef || '').trim()
        if (!targetRef) {
          this.$modal.msgError(this.$t('git.repo.tags.target-required'))
          return
        }
        if (!this.branches.some(b => b && b.name === targetRef)) {
          this.$modal.msgError(this.$t('git.repo.tags.target-ref-missing'))
          return
        }
        this.createSubmitting = true
        createGitRepoTag(this.repositoryId, {
          name: this.createForm.name.trim(),
          message: (this.createForm.message || '').trim(),
          targetRef
        }).then(() => {
          this.$modal.msgSuccess(this.$t('git.repo.tags.create-success'))
          this.createDialogVisible = false
          this.loadTags()
        }).finally(() => {
          this.createSubmitting = false
        })
      })
    },
    downloadTag(tag) {
      if (!tag || !tag.name || !this.repositoryId) return
      const refLabel = String(tag.name).replace(/[\\/]+/g, '-')
      const fileName = `${this.repoName || 'repo'}-${refLabel}.zip`
      downloadGitRepoArchive(this.repositoryId, tag.name, 'zip', fileName).catch(() => {
        this.$modal.msgError(this.$t('git.repo.tags.download-failed'))
      })
    },
    openRelease(tag) {
      if (!tag || !tag.releaseId || !this.repositoryId) return
      this.$router.push(gitRepoReleaseDetailRoute(this.$route, tag.releaseId)).catch(() => {})
    },
    openCommitDetail(tag) {
      const commitId = tag && (tag.commitId || tag.shortId)
      if (!commitId || !this.repositoryId) return
      this.$router.push(gitRepoCommitDetailRoute(this.$route, commitId)).catch(() => {})
    },
    createReleaseFromTag(tag) {
      if (!tag || !tag.name || !this.repositoryId) return
      this.$router.push(gitRepoBrowseRoute(this.$route, {
        tab: 'releases',
        createRelease: '1',
        tagName: tag.name
      })).catch(() => {})
    },
    deleteTag(tag) {
      if (!tag || !tag.name) return
      const hasRelease = tag.releaseId != null && tag.releaseId !== ''
      const message = hasRelease
        ? this.$t('git.repo.tags.delete-cascade-confirm', { name: tag.name })
        : this.$t('git.repo.tags.delete-confirm', { name: tag.name })
      this.$modal.confirm(message, this.$t('delete'), { type: 'warning' })
        .then(() => delGitRepoTag(this.repositoryId, tag.name, hasRelease))
        .then(() => {
          this.$modal.msgSuccess(this.$t('delete-success'))
          this.loadTags()
        }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.git-tags-page {
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 20px 20px 16px;
  box-sizing: border-box;
  color: var(--text-color-regular);
}

.git-tag-card__name,
.git-tag-card__actions,
.git-tags-section__header {
  display: flex;
  align-items: center;
}

.git-tags-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cat2bug-toolbar-section-gap, 10px);
  margin-bottom: 24px;
}

.git-tags-search {
  width: 320px;
  max-width: 100%;
}

.git-tags-section {
  margin-bottom: 28px;

  h2 {
    margin: 0 0 10px;
    font-size: 16px;
    color: var(--text-color-primary);
  }
}

.git-tags-section__header {
  justify-content: space-between;
  gap: 12px;

  > span {
    font-size: 13px;
    color: var(--text-color-secondary);
  }
}

.git-tag-list {
  overflow: hidden;
  border: 1px solid var(--border-color-light);
  border-radius: 6px;
  background: var(--card-bg);
}

.git-tag-list__head,
.git-tag-card {
  display: grid;
  /* 操作列定宽，避免表头空列与数据行按钮列宽度不一致导致「描述/提交」错位 */
  grid-template-columns: minmax(120px, 1fr) minmax(160px, 1.3fr) minmax(180px, 1.2fr) 240px;
  gap: 16px;
  align-items: center;
  padding: 12px 16px;
}

.git-tag-list__head {
  border-bottom: 1px solid var(--border-color-light);
  background: var(--bg-color-overlay, var(--card-bg));
  font-size: 12px;
  font-weight: 600;
  color: var(--text-color-secondary);
}

.git-tag-card {
  border-bottom: 1px solid var(--border-color-light);

  &:last-child {
    border-bottom: 0;
  }
}

.git-tag-col {
  min-width: 0;
  text-align: left;
}

.git-tag-col--actions {
  text-align: right;
}

.git-tag-col--desc {
  font-size: 13px;
  line-height: 1.5;
  color: var(--text-color-regular);
  white-space: pre-wrap;
  word-break: break-word;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
  overflow: hidden;

  &.is-empty {
    color: var(--text-color-secondary);
  }
}

.git-tag-col--commit {
  font-size: 12px;
  color: var(--text-color-secondary);
  line-height: 1.45;
}

.git-tag-card__name {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
  color: var(--text-color-primary);
  font-weight: 600;
}

.git-tag-card__icon {
  flex-shrink: 0;
  color: var(--text-color-secondary);
  font-size: 16px;
}

.git-tag-card__title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.git-tag-card__commit-subject {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--el-color-primary);
  font-size: 13px;
  text-decoration: none;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }

  &.is-empty {
    color: var(--text-color-secondary);
    font-size: 12px;
    cursor: default;

    &:hover {
      text-decoration: none;
    }
  }
}

.git-tag-card__commit-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0 6px;
  margin-top: 2px;
  overflow: hidden;
  color: var(--text-color-secondary);
}

.git-tag-card__sha {
  font-family: Menlo, Consolas, monospace;
  color: var(--el-color-primary);
  white-space: nowrap;
  text-decoration: none;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
}

.git-tag-card__actions {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 4px;
}

.git-tags-form-tip {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--text-color-secondary);
}

@media (max-width: 900px) {
  .git-tag-list__head {
    display: none;
  }

  .git-tag-card {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .git-tag-col--actions {
    padding-top: 4px;
  }

  .git-tag-card__actions {
    justify-content: flex-start;
  }
}

@media (max-width: 680px) {
  .git-tags-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .git-tags-search {
    width: 100%;
  }
}
</style>
