<template>
  <div
    class="git-commit-detail"
    :class="{ 'is-embedded': embedded, 'app-container': !embedded }"
    v-loading="loading"
  >
    <div v-if="!embedded" class="git-commit-detail__top">
      <el-page-header @back="goBackToCommits" class="git-commit-detail__back">
        <template #content>
          <div class="git-commit-detail__title-row">
            <el-icon class="git-commit-detail__title-icon"><Share /></el-icon>
            <span class="git-commit-detail__repo-name">{{ displayRepoName || '-' }}</span>
            <code v-if="commit" class="git-commit-detail__short-id">{{ commit.shortId || shortId(commit.id) }}</code>
          </div>
        </template>
      </el-page-header>
    </div>

    <div v-else class="git-commit-detail__embed-bar">
      <el-button
        link
        class="git-commit-detail__back-list"
        :title="$t('git.repo.commit.back-to-commits')"
        @click="goBackToCommits"
      >
        <el-icon :size="18"><RefreshLeft /></el-icon>
      </el-button>
      <code v-if="commit" class="git-commit-detail__short-id">{{ commit.shortId || shortId(commit.id) }}</code>
      <span class="git-commit-detail__embed-hint">{{ $t('git.repo.commit.detail-title') }}</span>
    </div>

    <el-alert
      v-if="loadError"
      type="error"
      :title="loadError"
      show-icon
      class="git-commit-detail__error"
    />

    <template v-else-if="commit">
      <header class="git-commit-detail__header">
        <div class="git-commit-detail__header-main">
          <h1 class="git-commit-detail__subject">{{ commitSubject }}</h1>
          <pre v-if="commitBody" class="git-commit-detail__body">{{ commitBody }}</pre>
          <div class="git-commit-detail__meta">
            <cat2-bug-avatar :member="authorMember" :size="32" />
            <span class="git-commit-detail__author">{{ commit.authorName || '-' }}</span>
            <span v-if="commit.authorEmail" class="git-commit-detail__email">&lt;{{ commit.authorEmail }}&gt;</span>
            <span class="git-commit-detail__time">{{ absoluteTime(commit.commitTime) }}</span>
            <span class="git-commit-detail__rel">{{ relativeTime(commit.commitTime) }}</span>
          </div>
          <div
            v-if="showCommitter"
            class="git-commit-detail__committer"
          >
            {{ $t('git.repo.commit.committer') }}:
            {{ commit.committerName }}
            <span v-if="commit.committerEmail">&lt;{{ commit.committerEmail }}&gt;</span>
          </div>
        </div>
        <div class="git-commit-detail__actions">
          <el-button
            type="primary"
            plain
            size="small"
            class="git-commit-detail__browse-btn"
            @click="browseFilesAtCommit"
          >
            {{ $t('git.repo.commit.browse-files') }}
          </el-button>
        </div>
      </header>

      <section class="git-commit-detail__parents">
        <span class="git-commit-detail__section-label">{{ $t('git.repo.commit.parents') }}</span>
        <template v-if="commit.parents && commit.parents.length">
          <router-link
            v-for="p in commit.parents"
            :key="p.id"
            class="git-commit-detail__parent-link"
            :to="parentRoute(p.id)"
          >
            {{ p.shortId || shortId(p.id) }}
          </router-link>
        </template>
        <span v-else class="git-commit-detail__muted">{{ $t('git.repo.commit.no-parents') }}</span>
        <p
          v-if="commit.parents && commit.parents.length > 1"
          class="git-commit-detail__parent-note"
        >
          {{ $t('git.repo.commit.compare-first-parent') }}
        </p>
      </section>

      <section v-if="commit.stats" class="git-commit-detail__stats">
        <span>{{ $t('git.repo.commit.stats-files', { n: commit.stats.files || 0 }) }}</span>
        <span class="git-commit-detail__stat-add">{{ $t('git.repo.commit.stats-additions', { n: commit.stats.additions || 0 }) }}</span>
        <span class="git-commit-detail__stat-del">{{ $t('git.repo.commit.stats-deletions', { n: commit.stats.deletions || 0 }) }}</span>
      </section>

      <section class="git-commit-detail__files">
        <h2 class="git-commit-detail__files-title">{{ $t('git.repo.commit.files-changed') }}</h2>
        <div
          v-for="(file, idx) in commit.files || []"
          :key="file.path + '-' + idx"
          class="git-commit-detail__file"
        >
          <div class="git-commit-detail__file-head" @click="toggleFile(idx)">
            <el-icon class="git-commit-detail__file-chevron">
              <ArrowRight v-if="!expandedFiles[idx]" />
              <ArrowDown v-else />
            </el-icon>
            <span class="git-commit-detail__file-type">{{ file.changeType }}</span>
            <span class="git-commit-detail__file-path" :title="file.path">{{ file.path }}</span>
            <span v-if="file.oldPath" class="git-commit-detail__file-old">{{ file.oldPath }}</span>
            <span class="git-commit-detail__file-stats">
              <span v-if="file.additions" class="git-commit-detail__stat-add">+{{ file.additions }}</span>
              <span v-if="file.deletions" class="git-commit-detail__stat-del">-{{ file.deletions }}</span>
            </span>
          </div>
          <div v-show="expandedFiles[idx]" class="git-commit-detail__file-body">
            <div v-if="file.binary" class="git-commit-detail__placeholder">
              {{ $t('git.repo.commit.binary-placeholder') }}
            </div>
            <div v-else-if="file.truncated && !file.patch" class="git-commit-detail__placeholder">
              {{ $t('git.repo.commit.truncated-placeholder') }}
            </div>
            <div v-else-if="file.patch" class="git-commit-detail__patch" role="table">
              <div
                v-for="(row, lineIdx) in (patchRowsByIndex[idx] || [])"
                :key="lineIdx"
                class="git-commit-detail__patch-row"
                :class="row.cls"
              >
                <span class="git-commit-detail__ln git-commit-detail__ln--old">{{ row.oldNo || '' }}</span>
                <span class="git-commit-detail__ln git-commit-detail__ln--new">{{ row.newNo || '' }}</span>
                <code class="git-commit-detail__patch-code">{{ row.text }}</code>
              </div>
            </div>
            <div v-else class="git-commit-detail__placeholder">
              {{ file.truncated ? $t('git.repo.commit.truncated-placeholder') : $t('git.repo.commit.no-patch') }}
            </div>
          </div>
        </div>
        <div v-if="!(commit.files && commit.files.length)" class="git-commit-detail__muted git-commit-detail__empty-files">
          —
        </div>
      </section>
    </template>
  </div>
</template>

<script>
import { Share, ArrowDown, ArrowRight, RefreshLeft } from '@element-plus/icons-vue'
import Cat2BugAvatar from '@/components/Cat2BugAvatar'
import { getGitRepo, getGitRepoCommit, gitRepoCommitDetailRoute } from '@/api/system/gitRepo'
import { listMemberOfProject } from '@/api/system/project'
import { useUserStore } from '@/stores/user'
import { formatTime } from '@/utils/index'
import { parseTime } from '@/utils/ruoyi'

export default {
  name: 'GitRepoCommitDetailPanel',
  components: {
    Cat2BugAvatar,
    Share,
    ArrowDown,
    ArrowRight,
    RefreshLeft
  },
  emits: ['back'],
  props: {
    embedded: {
      type: Boolean,
      default: false
    },
    repositoryId: {
      type: [Number, String],
      default: null
    },
    commitId: {
      type: String,
      default: ''
    },
    repoName: {
      type: String,
      default: ''
    },
    projectMembers: {
      type: Array,
      default: null
    }
  },
  data() {
    return {
      loading: false,
      loadError: '',
      localRepoName: '',
      projectId: null,
      localProjectMembers: [],
      commit: null,
      expandedFiles: {},
      patchRowsByIndex: {}
    }
  },
  computed: {
    resolvedRepositoryId() {
      if (this.repositoryId != null && this.repositoryId !== '') {
        return this.repositoryId
      }
      return this.$route.params.repositoryId
    },
    resolvedCommitId() {
      if (this.commitId) return this.commitId
      return this.$route.params.commitId
    },
    displayRepoName() {
      return this.repoName || this.localRepoName
    },
    members() {
      return Array.isArray(this.projectMembers) ? this.projectMembers : this.localProjectMembers
    },
    commitSubject() {
      const msg = (this.commit && this.commit.fullMessage) || ''
      const idx = msg.indexOf('\n')
      return idx === -1 ? msg : msg.slice(0, idx)
    },
    commitBody() {
      const msg = (this.commit && this.commit.fullMessage) || ''
      const idx = msg.indexOf('\n')
      if (idx === -1) return ''
      return msg.slice(idx + 1).replace(/^\n+/, '')
    },
    authorMember() {
      return this.memberForAuthor(this.commit)
    },
    showCommitter() {
      const c = this.commit
      if (!c || !c.committerName) return false
      return c.committerName !== c.authorName || (c.committerEmail && c.committerEmail !== c.authorEmail)
    }
  },
  watch: {
    resolvedCommitId() {
      this.loadCommit()
    }
  },
  created() {
    if (this.$route.query.name) {
      this.localRepoName = String(this.$route.query.name)
    }
    if (this.repoName) {
      this.localRepoName = this.repoName
    }
    if (!Array.isArray(this.projectMembers)) {
      this.loadRepo()
    } else if (!this.displayRepoName) {
      this.loadRepo()
    }
    this.loadCommit()
  },
  methods: {
    shortId(id) {
      if (!id) return ''
      const s = String(id)
      return s.length > 7 ? s.slice(0, 7) : s
    },
    timeValue(t) {
      if (!t) return 0
      const n = new Date(t).getTime()
      return Number.isNaN(n) ? 0 : n
    },
    absoluteTime(t) {
      if (!t) return '-'
      return parseTime(t, '{y}-{m}-{d} {h}:{i}:{s}')
    },
    relativeTime(t) {
      if (!t) return ''
      const ms = this.timeValue(t)
      if (!ms) return parseTime(t) || ''
      return formatTime(ms)
    },
    /** 按 git author 匹配项目成员，与 browse 提交列表一致 */
    memberForAuthor(commit) {
      const name = (commit && commit.authorName ? String(commit.authorName) : '').trim()
      const email = (commit && commit.authorEmail ? String(commit.authorEmail) : '').trim()
      const nameKey = name.toLowerCase()
      const emailKey = email.toLowerCase()
      const matched = this.members.find(m => {
        if (!m) return false
        const userName = String(m.userName || '').trim().toLowerCase()
        const nickName = String(m.nickName || '').trim().toLowerCase()
        const mail = String(m.email || '').trim().toLowerCase()
        return (nameKey && (nameKey === userName || nameKey === nickName))
          || (emailKey && mail && emailKey === mail)
      })
      if (matched) return matched
      const userStore = useUserStore()
      const selfName = String(userStore.name || '').trim().toLowerCase()
      const selfNick = String(userStore.nickName || '').trim().toLowerCase()
      if (nameKey && ((selfName && nameKey === selfName) || (selfNick && nameKey === selfNick))) {
        return {
          userName: userStore.name,
          nickName: userStore.nickName || userStore.name,
          avatar: userStore.avatar
        }
      }
      return {
        nickName: name || '-',
        userName: name || '-',
        email: email || undefined
      }
    },
    parentRoute(parentId) {
      return gitRepoCommitDetailRoute(this.resolvedRepositoryId, parentId)
    },
    goBackToCommits() {
      if (this.embedded) {
        this.$emit('back')
      }
      this.$router.push({
        name: 'GitRepoBrowse',
        params: { repositoryId: String(this.resolvedRepositoryId) },
        query: { tab: 'commits', ...(this.displayRepoName ? { name: this.displayRepoName } : {}) }
      }).catch(() => {})
    },
    browseFilesAtCommit() {
      const ref = (this.commit && this.commit.id) || this.resolvedCommitId
      this.$router.push({
        name: 'GitRepoBrowse',
        params: { repositoryId: String(this.resolvedRepositoryId) },
        query: {
          ref,
          tab: 'code',
          ...(this.displayRepoName ? { name: this.displayRepoName } : {})
        }
      }).catch(() => {})
    },
    loadProjectMembers(projectId) {
      if (!projectId) {
        this.localProjectMembers = []
        return
      }
      listMemberOfProject(projectId, { pageNum: 1, pageSize: 500 }).then(res => {
        const rows = res.rows || res.data || []
        this.localProjectMembers = Array.isArray(rows) ? rows : []
      }).catch(() => {
        this.localProjectMembers = []
      })
    },
    loadRepo() {
      if (!this.resolvedRepositoryId) return
      getGitRepo(this.resolvedRepositoryId).then(response => {
        const repo = response.data || {}
        this.localRepoName = repo.name || this.localRepoName
        this.projectId = repo.projectId || null
        if (!Array.isArray(this.projectMembers)) {
          this.loadProjectMembers(repo.projectId)
        }
      }).catch(() => {})
    },
    loadCommit() {
      if (!this.resolvedRepositoryId || !this.resolvedCommitId) return
      this.loading = true
      this.loadError = ''
      this.commit = null
      this.expandedFiles = {}
      this.patchRowsByIndex = {}
      getGitRepoCommit(this.resolvedRepositoryId, this.resolvedCommitId).then(response => {
        this.commit = response.data || null
        if (!this.commit) {
          this.loadError = this.$t('git.repo.commit.load-fail')
          return
        }
        this.expandAllFiles()
        this.buildPatchRowsByIndex()
      }).catch(err => {
        this.loadError = (err && err.message) || this.$t('git.repo.commit.load-fail')
      }).finally(() => {
        this.loading = false
      })
    },
    expandAllFiles() {
      const files = (this.commit && this.commit.files) || []
      const next = {}
      for (let i = 0; i < files.length; i++) {
        next[i] = true
      }
      this.expandedFiles = next
    },
    buildPatchRowsByIndex() {
      const files = (this.commit && this.commit.files) || []
      const next = {}
      for (let i = 0; i < files.length; i++) {
        const file = files[i]
        next[i] = file && file.patch ? this.buildPatchRows(file.patch) : []
      }
      this.patchRowsByIndex = next
    },
    toggleFile(idx) {
      const next = !this.expandedFiles[idx]
      this.expandedFiles = { ...this.expandedFiles, [idx]: next }
    },
    buildPatchRows(patch) {
      if (!patch) return []
      const text = String(patch)
      const lines = text.split('\n')
      if (lines.length && lines[lines.length - 1] === '') {
        lines.pop()
      }
      const rows = []
      let oldNo = 0
      let newNo = 0
      let inHunk = false
      const hunkRe = /^@@\s+-(\d+)(?:,\d+)?\s+\+(\d+)(?:,\d+)?\s+@@/
      for (let i = 0; i < lines.length; i++) {
        const line = lines[i]
        const cls = this.patchLineClass(line)
        if (line.startsWith('@@')) {
          const m = hunkRe.exec(line)
          if (m) {
            oldNo = Number(m[1])
            newNo = Number(m[2])
            inHunk = true
          }
          rows.push({ text: line, cls, oldNo: '', newNo: '' })
          continue
        }
        if (cls === 'is-meta' || !inHunk) {
          rows.push({ text: line, cls, oldNo: '', newNo: '' })
          continue
        }
        if (cls === 'is-add') {
          rows.push({ text: line, cls, oldNo: '', newNo: String(newNo) })
          newNo += 1
          continue
        }
        if (cls === 'is-del') {
          rows.push({ text: line, cls, oldNo: String(oldNo), newNo: '' })
          oldNo += 1
          continue
        }
        rows.push({
          text: line,
          cls,
          oldNo: String(oldNo),
          newNo: String(newNo)
        })
        oldNo += 1
        newNo += 1
      }
      return rows
    },
    patchLineClass(line) {
      if (!line) return 'is-context'
      if (line.startsWith('+++') || line.startsWith('---')) return 'is-meta'
      if (line.startsWith('@@')) return 'is-hunk'
      if (line.startsWith('+')) return 'is-add'
      if (line.startsWith('-')) return 'is-del'
      return 'is-context'
    }
  }
}
</script>

<style lang="scss" scoped>
.git-commit-detail {
  padding: 16px 20px 24px;
  box-sizing: border-box;
  color: var(--text-color-regular);
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;

  &:not(.is-embedded) {
    max-height: calc(100vh - 84px);
    overflow-x: hidden;
    overflow-y: auto;
  }

  &.is-embedded {
    padding: 4px 0 24px;
    max-height: none;
    overflow: visible;
  }
}

.git-commit-detail__embed-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.git-commit-detail__back-list {
  margin: 0;
  padding: 4px;
  min-height: auto;
  color: var(--text-color-regular);

  &:hover {
    color: var(--el-color-primary);
  }
}

.git-commit-detail__embed-hint {
  font-size: 13px;
  color: var(--text-color-secondary);
}

.git-commit-detail__top {
  margin-bottom: 16px;
}

.git-commit-detail__title-row {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.git-commit-detail__title-icon {
  color: var(--text-color-secondary);
}

.git-commit-detail__repo-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-color-primary);
}

.git-commit-detail__short-id {
  font-size: 13px;
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--bg-color-block, rgba(127, 127, 127, 0.12));
  color: var(--text-color-secondary);
}

.git-commit-detail__error {
  margin-bottom: 16px;
}

.git-commit-detail__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color-light);
}

.git-commit-detail__actions {
  flex-shrink: 0;
  padding-top: 2px;
}

.git-commit-detail__browse-btn {
  height: var(--cat2bug-toolbar-control-height, 32px) !important;
  min-height: var(--cat2bug-toolbar-control-height, 32px) !important;
  padding: 0 15px !important;
  font-size: 13px;
  line-height: 1;
}

.git-commit-detail__subject {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-color-primary);
  line-height: 1.35;
}

.git-commit-detail__body {
  margin: 0 0 12px;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: 14px;
  color: var(--text-color-regular);
  background: transparent;
  border: none;
  padding: 0;
}

.git-commit-detail__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 14px;
}

.git-commit-detail__author {
  font-weight: 500;
  color: var(--text-color-primary);
}

.git-commit-detail__email,
.git-commit-detail__time,
.git-commit-detail__rel,
.git-commit-detail__committer {
  color: var(--text-color-secondary);
  font-size: 13px;
}

.git-commit-detail__committer {
  margin-top: 6px;
}

.git-commit-detail__section-label {
  font-weight: 600;
  color: var(--text-color-primary);
  margin-right: 8px;
}

.git-commit-detail__parents {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px 8px;
  margin-bottom: 12px;
  font-size: 14px;
}

.git-commit-detail__parent-link {
  color: var(--el-color-primary);
  margin-right: 10px;
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}

.git-commit-detail__parent-note {
  flex-basis: 100%;
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--text-color-secondary);
}

.git-commit-detail__stats {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  font-size: 14px;
  font-weight: 500;
}

.git-commit-detail__stat-add {
  color: var(--el-color-success, #67c23a);
}

.git-commit-detail__stat-del {
  color: var(--el-color-danger, #f56c6c);
}

.git-commit-detail__files-title {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-color-primary);
}

.git-commit-detail__file {
  border: 1px solid var(--border-color-light);
  border-radius: 6px;
  margin-bottom: 10px;
  background: var(--card-bg);
  overflow: hidden;
}

.git-commit-detail__file-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  cursor: pointer;
  user-select: none;

  &:hover {
    background: var(--table-row-hover-bg);
  }
}

.git-commit-detail__file-chevron {
  flex-shrink: 0;
  color: var(--text-color-secondary);
}

.git-commit-detail__file-type {
  flex-shrink: 0;
  font-size: 12px;
  text-transform: uppercase;
  color: var(--text-color-secondary);
}

.git-commit-detail__file-path {
  flex: 1;
  min-width: 0;
  font-family: ui-monospace, monospace;
  font-size: 13px;
  color: var(--text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.git-commit-detail__file-old {
  font-size: 12px;
  color: var(--text-color-placeholder);
}

.git-commit-detail__file-stats {
  flex-shrink: 0;
  display: flex;
  gap: 6px;
  font-size: 12px;
  font-family: ui-monospace, monospace;
}

.git-commit-detail__file-body {
  border-top: 1px solid var(--border-color-light);
}

.git-commit-detail__placeholder {
  padding: 12px 14px;
  font-size: 13px;
  color: var(--text-color-secondary);
  background: var(--bg-color-block, rgba(127, 127, 127, 0.06));
}

.git-commit-detail__patch {
  margin: 0;
  padding: 0;
  overflow-x: auto;
  font-size: 12px;
  line-height: 1.45;
  background: var(--bg-color-block, rgba(127, 127, 127, 0.08));
}

.git-commit-detail__patch-row {
  display: grid;
  grid-template-columns: 48px 48px minmax(0, 1fr);
  align-items: stretch;
  min-width: max-content;
  width: 100%;

  &.is-add {
    background: rgba(103, 194, 58, 0.32);

    .git-commit-detail__patch-code {
      color: #95d475;
    }
  }

  &.is-del {
    background: rgba(245, 108, 108, 0.28);

    .git-commit-detail__patch-code {
      color: #f09898;
    }
  }

  &.is-hunk,
  &.is-meta {
    .git-commit-detail__patch-code {
      color: var(--text-color-secondary);
    }
  }
}

.git-commit-detail__ln {
  flex-shrink: 0;
  box-sizing: border-box;
  padding: 0 6px;
  text-align: right;
  user-select: none;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  color: var(--text-color-secondary);
  opacity: 0.95;
  border-right: 1px solid var(--border-color-light);
  background: rgba(127, 127, 127, 0.06);
}

.git-commit-detail__patch-code {
  display: block;
  padding: 0 12px;
  margin: 0;
  white-space: pre;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  color: var(--text-color-regular);
  background: transparent;
}

.git-commit-detail__muted {
  color: var(--text-color-secondary);
}

.git-commit-detail__empty-files {
  padding: 12px 0;
}
</style>
