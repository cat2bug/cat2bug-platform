<template>
  <div class="app-container git-pr-detail" v-loading="loading">
    <git-repo-shell :repository-id="repositoryId" :repo-name="repoName" active-tab="pulls">
      <template v-if="pull">
        <section class="git-pr-detail__summary">
          <div class="git-pr-detail__summary-head">
            <div class="git-pr-detail__summary-main">
              <h1 class="git-pr-detail__title">
                <template v-for="(seg, idx) in titleSegments" :key="'t-' + idx">
                  <a
                    v-if="seg.type === 'mention'"
                    href="javascript:;"
                    class="git-pr-defect-mention"
                    @click.prevent="openDefectByProjectNum(seg.num)"
                  >{{ seg.value }}</a>
                  <span v-else>{{ seg.value }}</span>
                </template>
              </h1>
              <div class="git-pr-detail__meta">
                <el-tag
                  size="small"
                  effect="plain"
                  class="git-pr-state-tag"
                  :class="'is-' + pull.state"
                  :type="stateType(pull.state)"
                >{{ $t('git.repo.pr.state.' + pull.state) }}</el-tag>
                <span>{{ pull.sourceBranch }} → {{ pull.targetBranch }}</span>
                <span>{{ pull.createBy || $t('unknown') }} · {{ relativeTime(pull.createTime) }}</span>
              </div>
            </div>
            <div v-if="canClose || canMerge" class="git-pr-detail__actions">
              <el-button
                v-if="canClose"
                type="info"
                plain
                size="small"
                :loading="closing"
                @click="closePullRequest"
              >{{ $t('git.repo.pr.close') }}</el-button>
              <el-button
                v-if="canMerge"
                type="primary"
                size="small"
                :loading="merging"
                @click="mergePullRequest"
              >{{ $t('git.repo.pr.merge') }}</el-button>
            </div>
          </div>
          <div
            v-if="pull.body"
            class="git-pr-detail__body markdown-body"
            @click="onPrBodyClick"
          >
            <markdown-renderer :content="linkedPrBody" />
          </div>
        </section>

        <el-tabs v-model="activeTab" class="git-pr-detail__tabs">
          <el-tab-pane name="commits" :label="$t('git.repo.pr.commits', { n: commits.length })">
            <div v-loading="commitsLoading" class="git-pr-detail__card">
              <div v-for="commit in commits" :key="commit.id" class="git-pr-detail__commit">
                <div class="git-pr-detail__commit-row">
                  <cat2-bug-avatar :member="memberForAuthor(commit)" :size="28" />
                  <div class="git-pr-detail__commit-main">
                    <a
                      href="javascript:;"
                      class="git-pr-detail__commit-message is-link"
                      @click.prevent="openCommitDetail(commit.id || commit.shortId)"
                    >{{ commit.message }}</a>
                    <div class="git-pr-detail__commit-meta">
                      {{ commit.authorName || '-' }} · {{ relativeTime(commit.commitTime) }} ·
                      <a
                        href="javascript:;"
                        class="git-pr-detail__commit-hash is-link"
                        @click.prevent="openCommitDetail(commit.id || commit.shortId)"
                      >{{ commit.shortId || shortId(commit.id) }}</a>
                    </div>
                  </div>
                </div>
              </div>
              <el-empty v-if="!commitsLoading && !commits.length" :description="$t('git.repo.pr.no-commits')" :image-size="72" />
            </div>
          </el-tab-pane>
          <el-tab-pane name="files" :label="$t('git.repo.pr.files', { n: files.length })">
            <div v-loading="filesLoading">
              <div v-for="(file, index) in files" :key="file.path + '-' + index" class="git-pr-detail__file">
                <button type="button" class="git-pr-detail__file-head" @click="toggleFile(index)">
                  <el-icon><ArrowDown v-if="expandedFiles[index]" /><ArrowRight v-else /></el-icon>
                  <span>{{ file.changeType }}</span>
                  <code>{{ file.path }}</code>
                  <span v-if="file.oldPath" class="git-pr-detail__old-path">{{ file.oldPath }}</span>
                  <span class="git-pr-detail__file-stats"><b class="is-add">+{{ file.additions || 0 }}</b><b class="is-del">-{{ file.deletions || 0 }}</b></span>
                </button>
                <div v-show="expandedFiles[index]" class="git-pr-detail__patch">
                  <div v-if="file.binary || (file.truncated && !file.patch)" class="git-pr-detail__placeholder">{{ file.binary ? $t('git.repo.commit.binary-placeholder') : $t('git.repo.commit.truncated-placeholder') }}</div>
                  <pre v-else-if="file.patch"><code v-for="(line, lineIndex) in patchLines(file.patch)" :key="lineIndex" :class="patchClass(line)">{{ line }}{{ '\n' }}</code></pre>
                  <div v-else class="git-pr-detail__placeholder">{{ $t('git.repo.commit.no-patch') }}</div>
                </div>
              </div>
              <el-empty v-if="!filesLoading && !files.length" :description="$t('git.repo.pr.no-files')" :image-size="72" />
            </div>
          </el-tab-pane>
          <el-tab-pane name="discussion" :label="$t('git.repo.pr.discussion', { n: comments.length })">
            <section class="git-pr-detail__discussion">
              <div v-if="canPush" class="git-pr-detail__comment-form">
                <div class="git-pr-markdown git-pr-markdown--composer">
                  <cat2-bug-markdown
                    v-if="activeTab === 'discussion'"
                    key="pr-comment-composer"
                    v-model:content="commentBody"
                    :placeholder="$t('git.repo.pr.comment-placeholder')"
                    :exclude-tools="commentMarkdownExcludeTools"
                    :show-preview="false"
                  />
                </div>
                <div class="git-pr-detail__comment-form-actions">
                  <el-button type="primary" size="small" :loading="commenting" @click="postComment()">
                    {{ $t('git.repo.pr.comment') }}
                  </el-button>
                </div>
              </div>
              <div v-for="comment in rootComments" :key="comment.commentId" class="git-pr-detail__comment">
                <div class="git-pr-detail__comment-head">
                  <cat2-bug-avatar :member="memberForComment(comment)" :size="28" />
                  <div class="git-pr-detail__comment-meta">
                    <span class="git-pr-detail__comment-author">{{ commentAuthor(comment) }}</span>
                    <span>· {{ relativeTime(comment.createTime) }}</span>
                  </div>
                </div>
                <div class="git-pr-detail__comment-body markdown-body">
                  <markdown-renderer :content="comment.body || ''" />
                </div>
                <el-button v-if="canPush" link size="small" @click="replyTo(comment)">{{ $t('git.repo.pr.reply') }}</el-button>
                <div v-if="replyingTo === comment.commentId" class="git-pr-detail__reply-form">
                  <div class="git-pr-markdown git-pr-markdown--reply">
                    <cat2-bug-markdown
                      :key="'pr-reply-' + comment.commentId"
                      v-model:content="replyBody"
                      :placeholder="$t('git.repo.pr.reply-placeholder')"
                      :exclude-tools="commentMarkdownExcludeTools"
                      :show-preview="false"
                    />
                  </div>
                  <el-button type="primary" size="small" :loading="commenting" @click="postComment(comment.commentId)">
                    {{ $t('git.repo.pr.comment') }}
                  </el-button>
                </div>
                <div v-for="reply in repliesOf(comment.commentId)" :key="reply.commentId" class="git-pr-detail__comment git-pr-detail__comment--reply">
                  <div class="git-pr-detail__comment-head">
                    <cat2-bug-avatar :member="memberForComment(reply)" :size="24" />
                    <div class="git-pr-detail__comment-meta">
                      <span class="git-pr-detail__comment-author">{{ commentAuthor(reply) }}</span>
                      <span>· {{ relativeTime(reply.createTime) }}</span>
                    </div>
                  </div>
                  <div class="git-pr-detail__comment-body markdown-body">
                    <markdown-renderer :content="reply.body || ''" />
                  </div>
                </div>
              </div>
              <el-empty v-if="!commentsLoading && !comments.length" :description="$t('git.repo.pr.no-comments')" :image-size="72" />
            </section>
          </el-tab-pane>
        </el-tabs>
      </template>
    </git-repo-shell>
    <handle-defect
      v-if="projectId"
      ref="handleDefect"
      :project-id="projectId"
      :append-to-body="true"
    />
  </div>
</template>

<script>
import { ArrowDown, ArrowRight } from '@element-plus/icons-vue'
import { getGitRepo, gitRepoCommitDetailRoute } from '@/api/system/gitRepo'
import { listMemberOfProject } from '@/api/system/project'
import { listDefect } from '@/api/system/defect'
import {
  closeGitPullRequest, createGitPullRequestComment, getGitPullRequest, listGitPullRequestComments,
  listGitPullRequestCommits, listGitPullRequestFiles, mergeGitPullRequest
} from '@/api/system/gitPullRequest'
import { checkPermi } from '@/utils/permission'
import { useUserStore } from '@/stores/user'
import { formatTime } from '@/utils/index'
import { parseTime } from '@/utils/ruoyi'
import { memberForGitCommitAuthor } from '@/utils/git-commit-author-member'
import {
  linkDefectMentionsInMarkdown,
  parseDefectMentionHref,
  splitDefectMentions
} from '@/utils/defect-mention'
import Cat2BugAvatar from '@/components/Cat2BugAvatar'
import Cat2BugMarkdown from '@/components/Cat2BugMarkdown'
import { MarkdownRenderer } from '@/components/Markdown'
import HandleDefect from '@/components/Defect/HandleDefect'
import GitRepoShell from './GitRepoShell.vue'

export default {
  name: 'GitRepoPullRequestDetail',
  components: {
    ArrowDown, ArrowRight, Cat2BugAvatar, Cat2BugMarkdown, MarkdownRenderer, HandleDefect, GitRepoShell
  },
  data() {
    return {
      repositoryId: Number(this.$route.params.repositoryId),
      number: Number(this.$route.params.number),
      repoName: String(this.$route.params.slug || ''),
      projectId: null,
      projectMembers: [],
      pull: null, commits: [], files: [], comments: [], activeTab: 'commits',
      loading: false, commitsLoading: false, filesLoading: false, commentsLoading: false,
      merging: false, closing: false, commenting: false, openingDefect: false,
      commentBody: '', replyBody: '', replyingTo: null,
      expandedFiles: {},
      commentMarkdownExcludeTools: ['siding-9899', 'project', 'case', 'defect', 'member', 'save', 'plan']
    }
  },
  computed: {
    canPush() { return checkPermi(['system:git:repo:push']) },
    canManage() { return checkPermi(['system:git:repo:manage']) },
    canMerge() { return !!(this.pull && this.pull.state === 'open' && this.canManage) },
    canClose() {
      const user = useUserStore()
      return !!(this.pull && this.pull.state === 'open' && (this.canManage || (this.canPush && Number(this.pull.authorId) === Number(user.id))))
    },
    rootComments() { return this.comments.filter(comment => !comment.parentId) },
    titleSegments() {
      return splitDefectMentions(this.pull && this.pull.title ? this.pull.title : '')
    },
    linkedPrBody() {
      return linkDefectMentionsInMarkdown(this.pull && this.pull.body ? this.pull.body : '')
    }
  },
  created() {
    this.loadRepo()
    this.loadAll()
  },
  methods: {
    loadRepo() {
      getGitRepo(this.repositoryId).then(res => {
        const repo = res.data || {}
        this.repoName = repo.name || this.repoName
        this.projectId = repo.projectId || null
        this.loadProjectMembers(this.projectId)
      }).catch(() => {})
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
    loadAll() { this.loadDetail(); this.loadCommits(); this.loadFiles(); this.loadComments() },
    loadDetail() {
      this.loading = true
      getGitPullRequest(this.repositoryId, this.number).then(res => { this.pull = res.data || null }).finally(() => { this.loading = false })
    },
    loadCommits() {
      this.commitsLoading = true
      listGitPullRequestCommits(this.repositoryId, this.number).then(res => { this.commits = res.data || [] }).finally(() => { this.commitsLoading = false })
    },
    loadFiles() {
      this.filesLoading = true
      listGitPullRequestFiles(this.repositoryId, this.number).then(res => {
        this.files = res.data || []
        this.expandedFiles = Object.fromEntries(this.files.map((file, index) => [index, true]))
      }).catch(() => {
        this.files = []
        this.expandedFiles = {}
      }).finally(() => { this.filesLoading = false })
    },
    loadComments() {
      this.commentsLoading = true
      listGitPullRequestComments(this.repositoryId, this.number).then(res => { this.comments = res.data || [] }).finally(() => { this.commentsLoading = false })
    },
    toggleFile(index) { this.expandedFiles = { ...this.expandedFiles, [index]: !this.expandedFiles[index] } },
    patchLines(patch) { return String(patch || '').split('\n') },
    patchClass(line) { return line.startsWith('+') && !line.startsWith('+++') ? 'is-add' : (line.startsWith('-') && !line.startsWith('---') ? 'is-del' : (line.startsWith('@@') ? 'is-hunk' : '')) },
    shortId(id) { return String(id || '').slice(0, 7) },
    openCommitDetail(commitId) {
      if (!commitId || !this.repositoryId) return
      this.$router.push(gitRepoCommitDetailRoute(this.$route, commitId)).catch(() => {})
    },
    memberForAuthor(commit) {
      const userStore = useUserStore()
      return memberForGitCommitAuthor(commit, this.projectMembers, {
        id: userStore.id,
        name: userStore.name,
        nickName: userStore.nickName,
        avatar: userStore.avatar
      })
    },
    onPrBodyClick(event) {
      const a = event.target && event.target.closest ? event.target.closest('a') : null
      if (!a) return
      const num = parseDefectMentionHref(a.getAttribute('href'))
      if (num == null) return
      event.preventDefault()
      this.openDefectByProjectNum(num)
    },
    openDefectByProjectNum(projectNum) {
      const num = Number(projectNum)
      if (!Number.isFinite(num) || num <= 0) return
      if (!this.projectId) {
        this.$modal.msgWarning(this.$t('git.repo.pr.defect-project-missing'))
        return
      }
      if (this.openingDefect) return
      this.openingDefect = true
      listDefect({
        projectId: this.projectId,
        projectNum: num,
        pageNum: 1,
        pageSize: 1
      }).then((res) => {
        const rows = res.rows || []
        const defect = rows.find((r) => Number(r.projectNum) === num) || rows[0]
        if (!defect || defect.defectId == null) {
          this.$modal.msgWarning(this.$t('git.repo.pr.defect-not-found', { num }))
          return
        }
        const drawer = this.$refs.handleDefect
        if (drawer && typeof drawer.open === 'function') {
          drawer.open(defect.defectId)
        }
      }).catch(() => {
        this.$modal.msgWarning(this.$t('git.repo.pr.defect-load-failed', { num }))
      }).finally(() => {
        this.openingDefect = false
      })
    },
    relativeTime(value) { if (!value) return '—'; const time = new Date(value).getTime(); return Number.isNaN(time) ? (parseTime(value) || '—') : formatTime(time) },
    stateType(state) { return state === 'open' ? 'success' : (state === 'merged' ? 'warning' : 'info') },
    memberForComment(comment) {
      if (!comment || comment.userId == null) {
        return { nickName: '?', userName: '?' }
      }
      const uid = Number(comment.userId)
      const matched = this.projectMembers.find(m => m && Number(m.userId) === uid)
      if (matched) return matched
      const userStore = useUserStore()
      if (Number(userStore.id) === uid) {
        return {
          userId: userStore.id,
          userName: userStore.name,
          nickName: userStore.nickName || userStore.name,
          avatar: userStore.avatar
        }
      }
      return {
        userId: comment.userId,
        nickName: this.$t('git.repo.pr.user', { id: comment.userId }),
        userName: this.$t('git.repo.pr.user', { id: comment.userId })
      }
    },
    commentAuthor(comment) {
      const member = this.memberForComment(comment)
      return member.nickName || member.userName || this.$t('unknown')
    },
    repliesOf(parentId) { return this.comments.filter(comment => Number(comment.parentId) === Number(parentId)) },
    replyTo(comment) { this.replyingTo = comment.commentId; this.replyBody = '' },
    postComment(parentId) {
      const body = (parentId ? this.replyBody : this.commentBody).trim()
      if (!body) { this.$modal.msgWarning(this.$t('git.repo.pr.comment-required')); return }
      this.commenting = true
      createGitPullRequestComment(this.repositoryId, this.number, { body, ...(parentId ? { parentId } : {}) }).then(() => {
        if (parentId) { this.replyingTo = null; this.replyBody = '' } else this.commentBody = ''
        this.loadComments()
      }).finally(() => { this.commenting = false })
    },
    closePullRequest() {
      this.$modal.confirm(this.$t('git.repo.pr.close-confirm')).then(() => {
        this.closing = true
        return closeGitPullRequest(this.repositoryId, this.number).then(() => { this.$modal.msgSuccess(this.$t('git.repo.pr.close-success')); this.loadDetail() }).finally(() => { this.closing = false })
      }).catch(() => {})
    },
    mergePullRequest() {
      this.$modal.confirm(this.$t('git.repo.pr.merge-confirm')).then(() => {
        this.merging = true
        return mergeGitPullRequest(this.repositoryId, this.number).then(() => { this.$modal.msgSuccess(this.$t('git.repo.pr.merge-success')); this.loadDetail() }).finally(() => { this.merging = false })
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.git-pr-detail {
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 20px 20px 16px;
  box-sizing: border-box;
  color: var(--text-color-regular);
}
.git-pr-detail__summary {
  margin-bottom: 18px;
  padding: 18px;
  border: 1px solid var(--border-color-light);
  border-radius: 6px;
  background: var(--card-bg);
}

.git-pr-detail__summary-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.git-pr-detail__summary-main {
  min-width: 0;
  flex: 1;
}

.git-pr-detail__summary h1,
.git-pr-detail__title {
  margin: 0 0 9px;
  font-size: 20px;
  color: var(--text-color-primary);
  word-break: break-word;
}

.git-pr-defect-mention {
  color: var(--el-color-primary);
  text-decoration: none;
  cursor: pointer;
}
.git-pr-defect-mention:hover {
  text-decoration: underline;
}

.git-pr-detail__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  align-items: center;
  font-size: 13px;
  color: var(--text-color-secondary);
}

.git-pr-detail__actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 5px;
}

.git-pr-detail__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.git-pr-detail__meta :deep(.git-pr-state-tag.is-open) {
  color: var(--el-color-success) !important;
  background-color: rgba(103, 194, 58, 0.14) !important;
  border-color: rgba(103, 194, 58, 0.4) !important;
}

.git-pr-detail__meta :deep(.git-pr-state-tag.is-merged) {
  color: var(--el-color-warning) !important;
  background-color: rgba(230, 162, 60, 0.14) !important;
  border-color: rgba(230, 162, 60, 0.4) !important;
}

.git-pr-detail__meta :deep(.git-pr-state-tag.is-closed) {
  color: var(--text-color-secondary) !important;
  background-color: rgba(144, 147, 153, 0.14) !important;
  border-color: rgba(144, 147, 153, 0.35) !important;
}

.git-pr-detail__commit-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}
.git-pr-detail__commit-main {
  min-width: 0;
  flex: 1;
}
.git-pr-detail__commit-row :deep(.member-avatar) {
  flex: 0 0 auto;
  margin-top: 2px;
}
.git-pr-detail__body {
  margin: 16px 0 0;
  font: inherit;
  line-height: 1.6;
  color: var(--text-color-primary);

  /* 正文 Markdown：字色 / 标题尺度 / 分割线走主题 token（勿用 github #eaecef） */
  :deep(.markdown-body),
  :deep(.markdown-it-vue) {
    color: var(--text-color-regular);
    background: transparent;
    font-size: 14px;

    h1, h2, h3, h4, h5, h6 {
      color: var(--text-color-primary);
    }

    h1 {
      font-size: 1.2em;
      border-bottom: 1px solid var(--divider-color, var(--border-color-light));
    }

    h2 {
      font-size: 1.08em;
      border-bottom: 1px solid var(--divider-color, var(--border-color-light));
    }

    h3 {
      font-size: 1.02em;
    }

    p, li, span, strong, em, blockquote {
      color: var(--text-color-regular);
    }

    a {
      color: var(--el-color-primary);
    }

    code {
      background: var(--bg-color-block, rgba(127, 127, 127, 0.12));
      color: var(--text-color-regular);
    }

    pre {
      background: var(--bg-color-base, rgba(127, 127, 127, 0.1));
      color: var(--text-color-regular);
    }

    table tr,
    table th,
    table td {
      color: var(--text-color-primary);
      border-color: var(--border-color-light);
    }
  }

  :deep(a[href^="#cat2bug-defect-"]) {
    color: var(--el-color-primary);
    text-decoration: none;
    cursor: pointer;
  }
  :deep(a[href^="#cat2bug-defect-"]:hover) {
    text-decoration: underline;
  }
}

@media (max-width: 680px) {
  .git-pr-detail__summary-head {
    flex-direction: column;
  }

  .git-pr-detail__actions {
    width: 100%;
    justify-content: flex-end;
  }
}
.git-pr-detail__tabs :deep(.el-tabs__header) { margin-bottom: 12px; }.git-pr-detail__card { overflow: hidden; border: 1px solid var(--border-color-light); border-radius: 6px; background: var(--card-bg); }.git-pr-detail__commit { padding: 13px 15px; border-bottom: 1px solid var(--border-color-light); }.git-pr-detail__commit:last-child { border-bottom: 0; }.git-pr-detail__commit-message { display: inline-block; max-width: 100%; color: var(--text-color-primary); font-weight: 500; text-decoration: none; }.git-pr-detail__commit-message.is-link { cursor: pointer; }.git-pr-detail__commit-message.is-link:hover { color: var(--el-color-primary); text-decoration: underline; }.git-pr-detail__commit-meta { margin-top: 5px; font-size: 12px; color: var(--text-color-secondary); }.git-pr-detail__commit-hash { font-family: Menlo, Consolas, monospace; color: var(--el-color-primary); text-decoration: none; }.git-pr-detail__commit-hash.is-link { cursor: pointer; }.git-pr-detail__commit-hash.is-link:hover { text-decoration: underline; }
.git-pr-detail__file { overflow: hidden; margin-bottom: 10px; border: 1px solid var(--border-color-light); border-radius: 6px; background: var(--card-bg); }.git-pr-detail__file-head { display: flex; align-items: center; gap: 8px; width: 100%; padding: 10px 12px; border: 0; background: transparent; color: var(--text-color-regular); text-align: left; cursor: pointer; }.git-pr-detail__file-head:hover { background: var(--table-row-hover-bg); }.git-pr-detail__file-head code { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--text-color-primary); }.git-pr-detail__old-path { color: var(--text-color-placeholder); font-size: 12px; }.git-pr-detail__file-stats { display: flex; gap: 6px; font-size: 12px; }.is-add { color: var(--el-color-success); }.is-del { color: var(--el-color-danger); }
.git-pr-detail__patch { border-top: 1px solid var(--border-color-light); overflow: auto; background: var(--bg-color-block); }.git-pr-detail__patch pre { margin: 0; padding: 10px 12px; font: 12px/1.5 ui-monospace, monospace; white-space: pre; }.git-pr-detail__patch code { display: block; }.git-pr-detail__patch code.is-add { background: rgba(103,194,58,.25); }.git-pr-detail__patch code.is-del { background: rgba(245,108,108,.22); }.git-pr-detail__patch code.is-hunk { color: var(--text-color-secondary); }.git-pr-detail__placeholder { padding: 14px; color: var(--text-color-secondary); font-size: 13px; }
.git-pr-detail__discussion { width: 100%; }
.git-pr-detail__comment-form { display: grid; gap: 8px; margin-bottom: 16px; }
.git-pr-detail__comment-form-actions { text-align: right; }
.git-pr-detail__comment { padding: 14px 0; border-bottom: 1px solid var(--border-color-light); }
.git-pr-detail__comment--reply { margin: 12px 0 0 20px; padding: 12px; border: 1px solid var(--border-color-light); border-radius: 6px; background: var(--card-bg); }
.git-pr-detail__comment-head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.git-pr-detail__comment-head :deep(.member-avatar) {
  flex-shrink: 0;
}
.git-pr-detail__comment-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 6px;
  align-items: center;
  font-size: 12px;
  color: var(--text-color-secondary);
}
.git-pr-detail__comment-author {
  font-weight: 600;
  color: var(--text-color-primary);
}
.git-pr-detail__comment-body {
  margin: 7px 0 7px 38px;
  line-height: 1.55;
  color: var(--text-color-primary);

  :deep(.markdown-body),
  :deep(.markdown-it-vue) {
    color: var(--text-color-regular);
    background: transparent;
    font-size: 14px;

    h1, h2, h3, h4, h5, h6 {
      color: var(--text-color-primary);
    }

    h1, h2 {
      border-bottom: 1px solid var(--divider-color, var(--border-color-light));
    }

    h1 { font-size: 1.2em; }
    h2 { font-size: 1.08em; }
    h3 { font-size: 1.02em; }

    p, li, span, strong, em, blockquote {
      color: var(--text-color-regular);
    }

    a {
      color: var(--el-color-primary);
    }

    code {
      background: var(--bg-color-block, rgba(127, 127, 127, 0.12));
      color: var(--text-color-regular);
    }

    pre {
      background: var(--bg-color-base, rgba(127, 127, 127, 0.1));
      color: var(--text-color-regular);
    }

    table tr,
    table th,
    table td {
      background-color: transparent;
      border-color: var(--border-color-light);
      color: var(--text-color-primary);
    }

    hr {
      border-color: var(--divider-color, var(--border-color-light));
      background-color: var(--divider-color, var(--border-color-light));
    }
  }
}
.git-pr-detail__comment--reply .git-pr-detail__comment-body { margin-left: 34px; }
.git-pr-detail__reply-form {
  display: grid;
  gap: 8px;
  margin-top: 8px;
  margin-left: 38px;
  width: calc(100% - 38px);
  max-width: none;
}
.git-pr-detail__reply-form .el-button { justify-self: end; }
.git-pr-detail__comment > .el-button { margin-left: 38px; }

.git-pr-markdown {
  width: 100%;
  border: 1px solid var(--border-color-base, #dcdfe6);
  border-radius: 6px;
  overflow: hidden;
  background: var(--card-bg, #fff);

  &--composer {
    height: 200px;
  }

  &--reply {
    height: 160px;
  }

  :deep(.cat2bug-textarea) {
    width: 100%;
  }

  :deep(.markdown) {
    height: 100%;
    display: flex;
    flex-direction: column;
    box-shadow: none;
  }

  :deep(.markdown-tools) {
    flex-shrink: 0;
    padding: 2px 6px;
    min-height: 0;
    border-bottom: 1px solid var(--border-color-light, #ebeef5);

    .tools-menu.el-menu--horizontal {
      height: auto !important;
      border: none !important;
    }

    .el-menu-item,
    .el-sub-menu,
    .el-sub-menu__title {
      height: 26px !important;
      min-height: 26px !important;
      line-height: 26px !important;
      width: 26px !important;
      padding: 0 !important;
      margin: 0 1px !important;
    }

    .el-menu--horizontal > .el-menu-item,
    .el-menu--horizontal > .el-sub-menu .el-sub-menu__title {
      border-bottom: none !important;
    }
  }

  :deep(.markdown-body) {
    flex: 1;
    min-height: 0;
    background: var(--card-bg, #fff) !important;

    > .markdown-body-edit,
    > .markdown-body-view {
      padding: 8px 12px;
      background: var(--card-bg, #fff) !important;
      border-right: none !important;
    }

    .cat2bug-md-editor-pane,
    .md-editor {
      --md-bk-color: var(--card-bg, #fff);
      --md-color: var(--text-color-primary, #303133);
      background: var(--card-bg, #fff) !important;
      background-color: var(--card-bg, #fff) !important;
      border: none !important;
      box-shadow: none !important;
    }

    .md-editor .cm-editor,
    .md-editor .cm-scroller,
    .md-editor .cm-content,
    .md-editor .cm-gutters {
      background: var(--card-bg, #fff) !important;
      background-color: var(--card-bg, #fff) !important;
    }

    .md-editor .cm-gutters {
      border-right: none !important;
    }
  }
}
</style>
