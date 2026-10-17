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
        <el-icon :size="18"><ArrowLeft /></el-icon>
      </el-button>
      <span v-if="displayBranch" class="git-commit-detail__branch" :title="displayBranch">
        <svg
          class="git-commit-detail__branch-icon"
          viewBox="0 0 24 24"
          width="14"
          height="14"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <line x1="6" y1="3" x2="6" y2="15" />
          <circle cx="18" cy="6" r="3" />
          <circle cx="6" cy="18" r="3" />
          <path d="M18 9a9 9 0 0 1-9 9" />
        </svg>
        <span class="git-commit-detail__branch-name">{{ displayBranch }}</span>
      </span>
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
            <span
              class="git-commit-detail__time"
              :title="relativeTime(commit.commitTime) || undefined"
            >{{ absoluteTime(commit.commitTime) }}</span>
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
            <span class="git-commit-detail__file-stats">
              <span
                v-if="file.additions"
                class="git-commit-detail__stat-add"
              >+{{ file.additions }}</span>
              <span
                v-if="file.deletions"
                class="git-commit-detail__stat-del"
              >-{{ file.deletions }}</span>
              <span
                v-if="!file.additions && !file.deletions"
                class="git-commit-detail__file-type"
              >{{ file.changeType }}</span>
            </span>
            <span class="git-commit-detail__file-path" :title="fileDisplayPath(file)">
              <template v-if="file.oldPath && file.oldPath !== file.path">
                <span class="git-commit-detail__file-old">{{ file.oldPath }}</span>
                <span class="git-commit-detail__file-arrow">→</span>
              </template>
              {{ file.path }}
            </span>
            <el-button
              class="git-commit-detail__file-copy"
              size="small"
              link
              :icon="CopyDocument"
              :title="$t('git.repo.copy-path')"
              @click.stop="copyFilePath(file.path)"
            />
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
import { Share, ArrowDown, ArrowRight, ArrowLeft, CopyDocument } from '@element-plus/icons-vue'
import { markRaw } from 'vue'
import Cat2BugAvatar from '@/components/Cat2BugAvatar'
import { getGitRepo, getGitRepoCommit, gitRepoCommitDetailRoute } from '@/api/system/gitRepo'
import { gitRepoBrowseRoute } from '@/utils/git-repo-routes'
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
    ArrowLeft,
    CopyDocument
  },
  emits: ['back', 'browse-files'],
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
    },
    /** Branch context when opening from history (also read from ?branch=) */
    branch: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      CopyDocument: markRaw(CopyDocument),
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
    displayBranch() {
      const fromProp = this.branch != null ? String(this.branch).trim() : ''
      if (fromProp) return fromProp
      const q = this.$route.query && this.$route.query.branch
      return q != null && String(q).trim() !== '' ? String(q).trim() : ''
    },
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
    if (this.$route.params.slug) {
      this.localRepoName = String(this.$route.params.slug)
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
    rememberedBranch() {
      return this.displayBranch
    },
    parentRoute(parentId) {
      const branch = this.rememberedBranch()
      return gitRepoCommitDetailRoute(this.$route, parentId, branch ? { branch } : undefined)
    },
    goBackToCommits() {
      if (this.embedded) {
        this.$emit('back')
      }
      const query = { tab: 'commits' }
      const branch = this.rememberedBranch()
      if (branch) query.branch = branch
      this.$router.push(gitRepoBrowseRoute(this.$route, query)).catch(() => {})
    },
    browseFilesAtCommit() {
      const ref = (this.commit && this.commit.id) || this.resolvedCommitId
      if (!ref) return
      if (this.embedded) {
        this.$emit('browse-files', ref)
        return
      }
      const query = { ref: String(ref), tab: 'code' }
      const branch = this.rememberedBranch()
      if (branch) query.branch = branch
      this.$router.push(gitRepoBrowseRoute(this.$route, query)).catch(() => {})
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
    fileDisplayPath(file) {
      if (!file) return ''
      if (file.oldPath && file.path && file.oldPath !== file.path) {
        return `${file.oldPath} → ${file.path}`
      }
      return file.path || file.oldPath || ''
    },
    copyFilePath(path) {
      const text = path != null ? String(path).trim() : ''
      if (!text) {
        this.$modal.msgWarning(this.$t('git.repo.copy-path-empty'))
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
        const ta = document.createElement('textarea')
        ta.value = text
        ta.setAttribute('readonly', '')
        ta.style.position = 'fixed'
        ta.style.left = '-9999px'
        document.body.appendChild(ta)
        ta.select()
        const ok = document.execCommand('copy')
        document.body.removeChild(ta)
        return ok
      } catch (e) {
        return false
      }
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
        // Skip raw git headers (diff --git / index / --- +++ / mode …)
        if (cls === 'is-meta' || !inHunk) {
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
      if (line == null) return 'is-context'
      if (line.startsWith('@@')) return 'is-hunk'
      if (line.startsWith('+') && !line.startsWith('+++')) return 'is-add'
      if (line.startsWith('-') && !line.startsWith('---')) return 'is-del'
      if (
        line.startsWith('diff --git') ||
        line.startsWith('index ') ||
        line.startsWith('---') ||
        line.startsWith('+++') ||
        line.startsWith('new file mode') ||
        line.startsWith('deleted file mode') ||
        line.startsWith('old mode') ||
        line.startsWith('new mode') ||
        line.startsWith('similarity index') ||
        line.startsWith('rename from') ||
        line.startsWith('rename to') ||
        line.startsWith('copy from') ||
        line.startsWith('copy to') ||
        line.startsWith('Binary files') ||
        line.startsWith('\\')
      ) {
        return 'is-meta'
      }
      return 'is-context'
    }
  }
}
</script>

<style lang="scss" scoped>
.git-commit-detail {
  padding: 20px 20px 16px;
  box-sizing: border-box;
  color: var(--text-color-regular);
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;

  &:not(.is-embedded) {
    overflow-x: hidden;
    overflow-y: auto;
  }

  &.is-embedded {
    padding: 4px 0 0;
    overflow: visible;
  }
}

.git-commit-detail__embed-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  min-width: 0;
}

.git-commit-detail__branch {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 220px;
  min-width: 0;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 13px;
  color: var(--text-color-regular);
  background: var(--el-fill-color-light, rgba(144, 147, 153, 0.12));
  border: 1px solid var(--border-color-lighter, var(--border-color-light));
  box-sizing: border-box;
}

.git-commit-detail__branch-icon {
  flex-shrink: 0;
  color: var(--text-color-secondary);
}

.git-commit-detail__branch-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.git-commit-detail__file-stats {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 4.5em;
  font-size: 12px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-weight: 600;
}

.git-commit-detail__file-type {
  flex-shrink: 0;
  font-size: 12px;
  text-transform: uppercase;
  color: var(--text-color-secondary);
  font-weight: 500;
}

.git-commit-detail__file-path {
  flex: 1;
  min-width: 0;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  color: var(--text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.git-commit-detail__file-old {
  color: var(--text-color-placeholder);
  text-decoration: line-through;
}

.git-commit-detail__file-arrow {
  margin: 0 4px;
  color: var(--text-color-secondary);
}

.git-commit-detail__file-copy {
  flex-shrink: 0;
  margin-left: auto;
  color: var(--text-color-secondary);

  &:hover {
    color: var(--el-color-primary);
  }
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
  box-sizing: border-box;
  /* 短行铺满容器宽度；长行随内容变宽以便横向滚动 */
  min-width: 100%;
  width: max-content;

  &.is-add {
    background: rgba(103, 194, 58, 0.22);

    .git-commit-detail__ln {
      background: transparent;
    }

    .git-commit-detail__patch-code {
      /* 浅色主题：深绿，避免浅绿字看不清 */
      color: #1f6b2a;
    }
  }

  &.is-del {
    background: rgba(245, 108, 108, 0.2);

    .git-commit-detail__ln {
      background: transparent;
    }

    .git-commit-detail__patch-code {
      color: #a61d24;
    }
  }

  &.is-hunk {
    background: rgba(64, 158, 255, 0.14);

    .git-commit-detail__ln {
      background: transparent;
      border-right-color: transparent;
    }

    .git-commit-detail__patch-code {
      color: var(--el-color-primary, #1890ff);
    }
  }

  &.is-meta {
    .git-commit-detail__patch-code {
      color: var(--text-color-secondary);
    }
  }
}

html.dark .git-commit-detail__patch-row {
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
  min-width: 0;
  width: 100%;
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
