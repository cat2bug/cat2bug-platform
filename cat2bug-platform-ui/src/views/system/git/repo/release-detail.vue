<template>
  <div
    v-loading="loading"
    class="git-release-detail"
    :class="{ 'is-highlighted': highlighted }"
  >
    <div v-if="release" class="git-release-detail__layout">
      <aside class="git-release-detail__side">
        <div class="git-release-detail__side-top">
          <div class="git-release-detail__side-left">
            <div class="git-release-detail__tag-row">
              <git-tag-icon class="git-release-detail__tag-icon" />
              <span class="git-release-detail__tag">{{ release.tagName }}</span>
            </div>
            <div class="git-release-detail__side-time">{{ absoluteTime(releaseTime) }}</div>
            <el-tag
              v-if="release.draft === 1"
              size="small"
              type="info"
              effect="plain"
            >{{ $t('git.repo.release.form.draft') }}</el-tag>
          </div>
          <div class="git-release-detail__side-right">
            <el-tag
              v-if="isLatest"
              size="small"
              type="success"
              effect="dark"
              class="git-release-detail__latest"
            >{{ $t('git.repo.release.latest') }}</el-tag>
            <a
              v-if="release.targetCommitId"
              href="javascript:;"
              class="git-release-detail__commit"
              :title="release.targetCommitId"
              @click.prevent="openCommitDetail"
            >
              <el-icon><Share /></el-icon>
              <span>{{ shortCommit(release.targetCommitId) }}</span>
            </a>
          </div>
        </div>
      </aside>

      <main class="git-release-detail__main">
        <header class="git-release-detail__header">
          <div class="git-release-detail__header-text">
            <h1 class="git-release-detail__title">
              <git-release-icon class="git-release-detail__title-icon" />
              <span>{{ release.title || release.tagName }}</span>
            </h1>
            <div class="git-release-detail__publisher">
              <cat2-bug-avatar :member="publisherMember" :size="32" />
              <span>{{ publisherDisplayName }}</span>
              <span class="git-release-detail__rel">{{ relativeTime(releaseTime) }}</span>
            </div>
          </div>
          <div v-if="canManage" class="git-release-detail__actions">
            <el-button
              link
              :icon="Edit"
              :title="$t('modify')"
              @click="$emit('edit', release)"
            />
            <el-button
              link
              type="danger"
              :icon="Delete"
              :title="$t('delete')"
              @click="handleDelete"
            />
          </div>
        </header>

        <section class="git-release-detail__notes">
          <h3>{{ $t('git.repo.release.update-content') }}</h3>
          <div class="git-release-detail__body markdown-body">
            <markdown-renderer v-if="release.body" :content="release.body" />
            <div v-else class="git-release-detail__empty-body">{{ $t('git.repo.release.no-body') }}</div>
            <div
              v-if="release.targetCommitMessage"
              class="git-release-detail__last-commit"
            >{{ $t('git.repo.release.last-commit-prefix') }}{{ release.targetCommitMessage }}</div>
          </div>
        </section>

        <section class="git-release-detail__assets">
          <h3>{{ $t('git.repo.release.downloads') }}</h3>
          <ul class="git-release-detail__asset-list">
            <li v-for="a in assets" :key="a.assetId">
              <a
                href="javascript:;"
                class="git-release-detail__asset-link"
                @click.prevent="downloadAsset(a)"
              >
                <el-icon class="git-release-detail__asset-icon"><Document /></el-icon>
                <span class="git-release-detail__asset-name">{{ a.fileName }}</span>
              </a>
              <span class="git-release-detail__asset-size">{{ formatSize(a.fileSize) }}</span>
              <el-button
                v-if="canManage"
                link
                type="danger"
                size="small"
                :icon="Delete"
                :title="$t('delete')"
                @click="deleteAsset(a)"
              />
            </li>
            <li v-if="sourceRef">
              <a
                href="javascript:;"
                class="git-release-detail__asset-link"
                @click.prevent="downloadSource('zip')"
              >
                <el-icon class="git-release-detail__asset-icon"><Document /></el-icon>
                <span class="git-release-detail__asset-name">{{ $t('git.repo.release.source-zip') }}</span>
              </a>
            </li>
            <li v-if="sourceRef">
              <a
                href="javascript:;"
                class="git-release-detail__asset-link"
                @click.prevent="downloadSource('tar.gz')"
              >
                <el-icon class="git-release-detail__asset-icon"><Document /></el-icon>
                <span class="git-release-detail__asset-name">{{ $t('git.repo.release.source-targz') }}</span>
              </a>
            </li>
          </ul>
          <div v-if="!assets.length && !sourceRef" class="git-aside-empty">{{ $t('git.repo.release.no-assets') }}</div>
        </section>
      </main>
    </div>
  </div>
</template>

<script>
import { markRaw } from 'vue'
import { Share, Document, Delete, Edit } from '@element-plus/icons-vue'
import GitTagIcon from './GitTagIcon.vue'
import GitReleaseIcon from './GitReleaseIcon.vue'
import {
  getGitRepoRelease,
  delGitRepoRelease,
  delGitRepoReleaseAsset,
  downloadGitRepoReleaseAsset,
  downloadGitRepoArchive
} from '@/api/system/gitRepo'
import Cat2BugAvatar from '@/components/Cat2BugAvatar'
import { MarkdownRenderer } from '@/components/Markdown'
import { checkPermi } from '@/utils/permission'
import { formatTime } from '@/utils/index'
import { parseTime } from '@/utils/ruoyi'
import { gitRepoCommitDetailRoute } from '@/utils/git-repo-routes'

export default {
  name: 'GitRepoReleaseDetailPanel',
  components: {
    Cat2BugAvatar,
    MarkdownRenderer,
    GitTagIcon,
    GitReleaseIcon,
    Share,
    Document
  },
  props: {
    repositoryId: {
      type: [String, Number],
      required: true
    },
    releaseId: {
      type: [String, Number],
      required: true
    },
    isLatest: {
      type: Boolean,
      default: false
    },
    highlighted: {
      type: Boolean,
      default: false
    },
    projectMembers: {
      type: Array,
      default: () => []
    }
  },
  emits: ['deleted', 'edit'],
  data() {
    return {
      Delete: markRaw(Delete),
      Edit: markRaw(Edit),
      loading: false,
      release: null,
      assets: []
    }
  },
  computed: {
    canManage() {
      return checkPermi(['system:git:repo:manage'])
    },
    releaseTime() {
      if (!this.release) return null
      return this.release.publishedAt || this.release.createTime
    },
    publisherMember() {
      if (!this.release) return { nickName: '-' }
      const publisherId = this.release.publisherId
      const publisherName = (this.release.publisherName || '').trim()
      const nameKey = publisherName.toLowerCase()
      const members = Array.isArray(this.projectMembers) ? this.projectMembers : []

      if (publisherId != null) {
        const byId = members.find(m => m && Number(m.userId) === Number(publisherId))
        if (byId) return byId
      }
      if (nameKey) {
        const byName = members.find(m => {
          if (!m) return false
          const userName = String(m.userName || '').trim().toLowerCase()
          const nickName = String(m.nickName || '').trim().toLowerCase()
          return nameKey === userName || nameKey === nickName
        })
        if (byName) return byName
      }
      return {
        userId: publisherId || undefined,
        nickName: publisherName || '-',
        userName: publisherName || '-'
      }
    },
    publisherDisplayName() {
      const m = this.publisherMember
      if (m && (m.nickName || m.userName || m.name)) {
        return m.nickName || m.userName || m.name
      }
      return (this.release && this.release.publisherName) || '—'
    },
    sourceRef() {
      if (!this.release) return ''
      return this.release.targetCommitId || this.release.tagName || ''
    }
  },
  watch: {
    repositoryId() {
      this.loadAll()
    },
    releaseId() {
      this.loadAll()
    }
  },
  created() {
    this.loadAll()
  },
  methods: {
    loadAll() {
      if (!this.repositoryId || !this.releaseId) return
      this.loading = true
      getGitRepoRelease(this.repositoryId, this.releaseId).then(releaseRes => {
        this.release = releaseRes.data || null
        this.assets = (this.release && this.release.assets) || []
      }).catch(() => {
        this.release = null
        this.assets = []
      }).finally(() => {
        this.loading = false
      })
    },
    shortCommit(id) {
      if (!id) return '—'
      return String(id).slice(0, 7)
    },
    openCommitDetail() {
      const id = this.release && this.release.targetCommitId
      if (!id) return
      this.$router.push(gitRepoCommitDetailRoute(this.$route, id)).catch(() => {})
    },
    absoluteTime(t) {
      if (!t) return '—'
      return parseTime(t) || '—'
    },
    relativeTime(t) {
      if (!t) return '—'
      const ms = new Date(t).getTime()
      if (Number.isNaN(ms)) return parseTime(t) || '—'
      return formatTime(ms)
    },
    formatSize(bytes) {
      const n = Number(bytes)
      if (!Number.isFinite(n) || n < 0) return ''
      if (n < 1024) return n + ' B'
      if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB'
      return (n / (1024 * 1024)).toFixed(1) + ' MB'
    },
    downloadAsset(asset) {
      if (!asset) return
      downloadGitRepoReleaseAsset(
        this.repositoryId,
        this.releaseId,
        asset.assetId,
        asset.fileName
      ).catch(() => {
        this.$modal.msgError(this.$t('git.repo.release.download-failed'))
      })
    },
    downloadSource(format) {
      if (!this.sourceRef) return
      const label = this.release.tagName || String(this.sourceRef).slice(0, 7)
      const ext = format === 'tar.gz' ? 'tar.gz' : 'zip'
      const fileName = 'source-' + label + '.' + ext
      downloadGitRepoArchive(this.repositoryId, this.sourceRef, format, fileName).catch(() => {
        this.$modal.msgError(this.$t('git.repo.release.download-failed'))
      })
    },
    deleteAsset(asset) {
      this.$modal.confirm(this.$t('git.repo.release.delete-asset-confirm')).then(() => {
        return delGitRepoReleaseAsset(this.repositoryId, this.releaseId, asset.assetId)
      }).then(() => {
        this.$modal.msgSuccess(this.$t('delete-success'))
        this.assets = this.assets.filter(a => a.assetId !== asset.assetId)
      }).catch(() => {})
    },
    handleDelete() {
      this.$modal.confirm(this.$t('git.repo.release.delete-confirm')).then(() => {
        return delGitRepoRelease(this.repositoryId, this.releaseId)
      }).then(() => {
        this.$modal.msgSuccess(this.$t('delete-success'))
        this.$emit('deleted', this.releaseId)
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.git-release-detail {
  width: 100%;
  max-width: none;
  /* 默认与列表页节奏一致；列表内由 browse timeline 再微调首条 */
  padding: 12px 0;

  &.is-highlighted {
    background: var(--bg-color-block, rgba(127, 127, 127, 0.06));
    border-radius: 6px;
    margin: 0 -8px;
    padding-left: 8px;
    padding-right: 8px;
  }
}

.git-release-detail__layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 0;
  align-items: stretch;
}

.git-release-detail__main {
  min-width: 0;
  min-height: 100%;
  padding-left: 28px;
}

.git-release-detail__side {
  /* 覆盖全局 aside { background:#eef1f6; padding/line-height… } */
  display: block;
  width: 100%;
  height: 100%;
  min-height: 100%;
  box-sizing: border-box;
  margin: 0;
  padding: 0 20px 0 0;
  border-radius: 0;
  border-right: none;
  background: transparent;
  line-height: 1.4;
  font-size: inherit;
  font-family: inherit;
  color: var(--text-color-regular);
}

.git-release-detail__side-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.git-release-detail__side-left {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  min-width: 0;
}

.git-release-detail__side-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
}

.git-release-detail__tag-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: var(--text-color-primary);
}

.git-release-detail__tag-icon {
  font-size: 16px;
  color: var(--el-color-primary, #ffc107);
}

.git-release-detail__side-time {
  font-size: 13px;
  color: var(--text-color-secondary);
}

.git-release-detail__commit {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  border: none;
  background: transparent;
  font-family: Menlo, Consolas, monospace;
  font-size: 12px;
  font-weight: 500;
  color: var(--el-color-primary);
  text-decoration: none;
  cursor: pointer;
  box-sizing: border-box;

  &:hover {
    color: var(--el-color-primary-light-3);
    text-decoration: underline;
  }
}

.git-release-detail__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.git-release-detail__title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 10px;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.25;
  color: var(--text-color-primary);
}

.git-release-detail__title-icon {
  flex-shrink: 0;
  font-size: 24px;
  color: var(--text-color-secondary);
}

.git-release-detail__actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.git-release-detail__publisher {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-color-regular);
}

.git-release-detail__rel {
  color: var(--text-color-secondary);
}

.git-release-detail__notes h3,
.git-release-detail__assets h3 {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-color-primary);
}

.git-release-detail__body {
  min-height: 40px;
  margin-bottom: 28px;
  padding: 4px 0 0;
  border-bottom: none;
  color: var(--text-color-primary);

  :deep(.markdown-body),
  :deep(.markdown-it-vue) {
    color: var(--text-color-primary);
    background: transparent;

    h1, h2, h3, h4, h5, h6 {
      color: var(--text-color-primary);
    }

    h1, h2 {
      border-bottom: 1px solid var(--divider-color, var(--border-color-light));
    }

    h1 { font-size: 1.2em; }
    h2 { font-size: 1.08em; }
    h3 { font-size: 1.02em; }

    a {
      color: var(--el-color-primary, #ffc107);
    }

    code {
      background: var(--bg-color-block, rgba(127, 127, 127, 0.12));
      color: var(--text-color-regular);
    }

    pre {
      background: var(--bg-color-base, rgba(127, 127, 127, 0.1));
    }

    table tr,
    table th {
      background-color: transparent;
      border-color: var(--border-color-light);
    }

    li,
    p {
      color: var(--text-color-primary);
    }
  }
}

.git-release-detail__empty-body {
  color: var(--text-color-secondary);
  font-size: 14px;
}

.git-release-detail__last-commit {
  margin-top: 16px;
  font-size: 13px;
  line-height: 1.5;
  color: var(--text-color-secondary);
  word-break: break-word;
}

.git-release-detail__asset-list {
  list-style: none;
  margin: 0;
  padding: 0;

  li {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 0;
    border-bottom: none;
    font-size: 14px;
  }
}

.git-release-detail__asset-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  color: var(--el-color-primary, #ffc107);
  text-decoration: none;

  &:hover {
    color: var(--el-color-primary, #ffc107);
    text-decoration: underline;
  }
}

.git-release-detail__asset-icon {
  flex-shrink: 0;
  font-size: 16px;
  color: inherit;
}

.git-release-detail__asset-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: inherit;
}

.git-release-detail__asset-size {
  margin-left: auto;
  font-size: 12px;
  color: var(--text-color-secondary);
}

.git-aside-empty {
  font-size: 13px;
  color: var(--text-color-secondary);
}

@media (max-width: 800px) {
  .git-release-detail__layout {
    grid-template-columns: 1fr;
  }

  .git-release-detail__side {
    padding: 0 0 16px;
    border-right: none;
    border-bottom: 1px solid var(--border-color-light);
  }

  .git-release-detail__main {
    padding-left: 0;
    padding-top: 20px;
  }
}
</style>
