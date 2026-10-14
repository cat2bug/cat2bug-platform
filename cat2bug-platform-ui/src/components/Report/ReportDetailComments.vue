<template>
  <component :is="embedded ? 'div' : 'aside'" :class="embedded ? 'report-detail-comments is-embedded' : 'report-detail-comments'">
    <div v-if="!embedded" class="report-detail-comments-header">
      <span class="report-detail-comments-title">{{ $t('report.comments') }}</span>
      <el-button
        class="report-detail-comments-toggle"
        link
        size="small"
        :icon="$legacyIcon('el-icon-d-arrow-right')"
        :title="$t('report.collapse-comments')"
        @click="$emit('toggle-collapse')"
      ></el-button>
    </div>

    <div
      ref="commentsBody"
      class="report-detail-comments-body"
      v-loading="loading"
      @scroll="$emit('scroll', $event)"
    >
      <p v-if="!threads.length && !draft" class="report-detail-comments-empty">
        {{ $t('report.comments-empty') }}
      </p>

      <div
        class="report-detail-comments-canvas"
        :style="{ minHeight: canvasMinHeight + 'px' }"
      >
        <div
          v-if="draft"
          class="report-detail-comments-draft"
          :style="draftStyle"
        >
          <div class="report-detail-comments-draft-quote">
            <span class="report-detail-comments-draft-label">{{ $t('report.quote-preview-prefix') }}</span>
            “{{ truncate(draft.quote) }}”
          </div>
          <comment-input
            ref="draftInput"
            show-word-limit
            show-cancel
            button-size="small"
            :max-length="500"
            @submit="onSubmitDraft"
            @cancel="$emit('cancel-draft')"
          />
        </div>

        <div
          v-for="thread in threads"
          :key="thread.root.commentId"
          class="report-thread"
          :class="{
            'is-active': isActive(thread),
            'is-orphaned': !thread.matched
          }"
          :data-report-thread-id="thread.root.commentId"
          :style="threadStyle(thread)"
          @click="$emit('focus-thread', thread.root.commentId)"
        >
          <div class="report-thread-quote" v-if="thread.root.quote">
            <span class="report-thread-quote-text">“{{ truncate(thread.root.quote) }}”</span>
            <el-tag v-if="!thread.matched" type="warning" size="small" effect="plain" class="report-thread-orphan-badge">
              {{ $t('report.orphaned') }}
            </el-tag>
          </div>

          <div class="report-thread-comment">
            <cat2-bug-avatar size="small" :member="thread.root.createMember" />
            <div class="report-thread-comment-main">
              <div class="report-thread-comment-title">
                <span class="name">{{ memberName(thread.root.createMember) }}</span>
                <span class="time">{{ parseTime(thread.root.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
                <el-button
                  v-if="canDelete(thread.root)"
                  class="report-thread-delete"
                  :icon="$legacyIcon('el-icon-delete')"
                  size="small"
                  link
                  @click.stop="$emit('delete', thread.root)"
                ></el-button>
              </div>
              <div class="report-thread-comment-content" v-html="toHtml(thread.root.commentContent)"></div>
            </div>
          </div>

          <div v-if="thread.replies.length" class="report-thread-replies">
            <div v-for="reply in thread.replies" :key="reply.commentId" class="report-thread-reply">
              <cat2-bug-avatar size="small" :member="reply.createMember" />
              <div class="report-thread-comment-main">
                <div class="report-thread-comment-title">
                  <span class="name">{{ memberName(reply.createMember) }}</span>
                  <span class="time">{{ parseTime(reply.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
                  <el-button
                    v-if="canDelete(reply)"
                    class="report-thread-delete"
                    :icon="$legacyIcon('el-icon-delete')"
                    size="small"
                    link
                    @click.stop="$emit('delete', reply)"
                  ></el-button>
                </div>
                <div class="report-thread-comment-content" v-html="toHtml(reply.commentContent)"></div>
              </div>
            </div>
          </div>

          <div class="report-thread-actions">
            <el-button
              link
              size="small"
              :icon="$legacyIcon('el-icon-chat-line-round')"
              @click.stop="toggleReply(thread)"
            >{{ $t('report.reply') }}</el-button>
          </div>

          <comment-input
            v-if="replyingId === thread.root.commentId"
            :ref="'replyInput-' + thread.root.commentId"
            show-word-limit
            @submit="onSubmitReply(thread, $event)"
            @click.stop
          />
        </div>
      </div>
    </div>
  </component>
</template>

<script>
import Cat2BugAvatar from '@/components/Cat2BugAvatar'
import CommentInput from '@/components/Comment/CommentInput'
import { defaultEmojiMap } from '@/utils/emoji-assets'
import { commentToHtml } from '@/utils/comment-html'

const imgMap = defaultEmojiMap

export default {
  name: 'ReportDetailComments',
  components: { Cat2BugAvatar, CommentInput },
  props: {
    threads: {
      type: Array,
      default: () => []
    },
    activeThreadId: {
      type: [String, Number],
      default: null
    },
    draft: {
      type: Object,
      default: null
    },
    /** 草稿卡片 top（与正文选区同高）；无则吸顶 */
    draftTop: {
      type: Number,
      default: null
    },
    currentUserId: {
      type: [String, Number],
      default: null
    },
    canManage: {
      type: Boolean,
      default: false
    },
    loading: {
      type: Boolean,
      default: false
    },
    /** 嵌入侧栏 Tab 时隐藏自身 header / 外框宽度 */
    embedded: {
      type: Boolean,
      default: false
    },
    /** commentId -> top(px)，按正文锚点 Y 排布 */
    threadTops: {
      type: Object,
      default: () => ({})
    },
    canvasMinHeight: {
      type: Number,
      default: 0
    }
  },
  emits: ['toggle-collapse', 'focus-thread', 'submit-draft', 'cancel-draft', 'reply', 'delete', 'scroll', 'layout-request'],
  data() {
    return {
      replyingId: null
    }
  },
  computed: {
    draftStyle() {
      if (this.draftTop == null || Number.isNaN(this.draftTop)) {
        return { top: '8px' }
      }
      return {
        top: `${Math.max(0, this.draftTop)}px`
      }
    }
  },
  watch: {
    draft(val) {
      if (val) {
        this.$nextTick(() => {
          if (this.$refs.draftInput) {
            this.$refs.draftInput.reset()
            this.$refs.draftInput.focus()
          }
        })
      }
    }
  },
  methods: {
    getScrollElement() {
      return this.$refs.commentsBody || null
    },
    hasTop(thread) {
      const id = thread && thread.root ? String(thread.root.commentId) : ''
      return Object.prototype.hasOwnProperty.call(this.threadTops || {}, id)
    },
    threadStyle(thread) {
      const id = thread && thread.root ? String(thread.root.commentId) : ''
      if (this.hasTop(thread)) {
        return {
          top: `${Math.max(0, this.threadTops[id])}px`,
          visibility: 'visible'
        }
      }
      // Tops 尚未算完时先相对排布，避免发表成功后列表空白
      return {
        position: 'relative',
        top: 'auto',
        visibility: 'visible',
        marginBottom: '8px'
      }
    },
    isActive(thread) {
      return this.activeThreadId != null && String(thread.root.commentId) === String(this.activeThreadId)
    },
    memberName(member) {
      return member && member.nickName ? member.nickName : ''
    },
    canDelete(comment) {
      if (!comment) {
        return false
      }
      if (this.currentUserId != null && comment.createById != null && String(this.currentUserId) === String(comment.createById)) {
        return true
      }
      return this.canManage
    },
    truncate(text, max = 60) {
      const str = (text || '').trim()
      return str.length > max ? str.slice(0, max) + '…' : str
    },
    toggleReply(thread) {
      this.replyingId = this.replyingId === thread.root.commentId ? null : thread.root.commentId
      this.$emit('layout-request')
      if (this.replyingId) {
        this.$nextTick(() => {
          const ref = this.$refs['replyInput-' + thread.root.commentId]
          const input = Array.isArray(ref) ? ref[0] : ref
          if (input) {
            input.focus()
          }
          this.$emit('layout-request')
        })
      }
    },
    onSubmitDraft(content) {
      this.$emit('submit-draft', content)
    },
    onSubmitReply(thread, content) {
      this.replyingId = null
      this.$emit('reply', { threadId: thread.root.commentId, content })
    },
    /** 与 CommentView 一致的表情/换行渲染，避免耦合 CommentView 的删除权限逻辑 */
    toHtml(str) {
      return commentToHtml(str, imgMap)
    }
  }
}
</script>

<style lang="scss" scoped>
.report-detail-comments {
  width: 320px;
  flex-shrink: 0;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-left: 1px solid var(--el-border-color-lighter, #ebeef5);
  background-color: var(--el-bg-color, #ffffff);
  color: var(--el-text-color-primary, #303133);

  &.is-embedded {
    width: auto;
    flex: 1;
    min-height: 0;
    border-left: none;
    background: transparent;
  }
}
.report-detail-comments-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  flex-shrink: 0;
  background-color: inherit;
  z-index: 1;
}
.report-detail-comments-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-secondary, #909399);
}
.report-detail-comments-body {
  position: relative;
  flex: 1;
  min-height: 0;
  min-width: 0;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 0;
}
.report-detail-comments-canvas {
  position: relative;
  width: 100%;
  box-sizing: border-box;
  padding: 0 12px 24px;
}
.report-detail-comments-empty {
  padding: 24px 16px;
  font-size: 12px;
  text-align: center;
  color: var(--el-text-color-placeholder, #c0c4cc);
}
.report-detail-comments-draft {
  /* 与 .report-thread 同宽同左右边距（画布内 absolute） */
  position: absolute;
  left: 12px;
  right: 12px;
  width: auto;
  margin: 0;
  padding: 10px 8px;
  border-radius: 8px;
  border: 1px solid var(--el-border-color-extra-light, #f2f6fc);
  background-color: var(--el-fill-color-light, #f5f7fa);
  box-sizing: border-box;
  z-index: 3;
}

/* 讨论框工具栏：表情 + 发言/取消靠右；按钮再小一号，两按钮间距 10px */
.report-detail-comments-draft,
.report-thread {
  :deep(.comment-input-tools) {
    justify-content: flex-end;
    gap: 6px;
  }
  :deep(.comment-input-tools-actions) {
    gap: 10px;
  }
  :deep(.comment-input-tools-actions .el-button) {
    height: 20px;
    min-height: 20px;
    padding: 0 6px;
    font-size: 11px;
    margin-left: 0;
  }
  :deep(.comment-input-tools-actions .el-button + .el-button) {
    margin-left: 0;
  }
  :deep(.comment-input-tools .emoji-button) {
    width: 18px;
    height: 18px;
  }
}
.report-detail-comments-draft-quote {
  font-size: 12px;
  color: var(--el-text-color-secondary, #909399);
  margin-bottom: 8px;
  word-break: break-word;
}
.report-detail-comments-draft-label {
  margin-right: 2px;
}
.report-thread {
  position: absolute;
  left: 12px;
  right: 12px;
  width: auto;
  margin: 0;
  padding: 10px 8px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid var(--el-border-color-extra-light, #f2f6fc);
  min-width: 0;
  max-width: 100%;
  box-sizing: border-box;
  overflow: hidden;
  background-color: var(--el-fill-color-lighter, #fafafa);

  &:hover {
    background-color: var(--el-fill-color-light, #f5f7fa);
  }
  &.is-active {
    border-color: var(--el-color-primary, #409eff);
    background-color: var(--el-color-primary-light-9, #ecf5ff);
    z-index: 2;
  }
  &.is-orphaned {
    opacity: 0.85;
  }
}
.report-thread-quote {
  font-size: 12px;
  color: var(--el-text-color-secondary, #909399);
  margin-bottom: 6px;
  word-break: break-word;
  display: flex;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}
.report-thread-quote-text {
  min-width: 0;
  flex: 1 1 auto;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.report-thread-orphan-badge {
  flex-shrink: 0;
}
.report-thread-comment,
.report-thread-reply {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 4px;
  min-width: 0;
  max-width: 100%;

  > .cat2bug-avatar,
  > :first-child {
    flex-shrink: 0;
  }
}
.report-thread-comment-main {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}
.report-thread-comment-title {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  max-width: 100%;
  .name {
    flex: 1 1 auto;
    min-width: 0;
    font-weight: 500;
    font-size: 13px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .time {
    flex: 0 0 auto;
    max-width: 42%;
    font-size: 12px;
    color: var(--el-text-color-secondary, #909399);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .report-thread-delete {
    flex-shrink: 0;
  }
}
.report-thread-comment-content {
  font-size: 13px;
  word-break: break-word;
  overflow-wrap: anywhere;
}
.report-thread-replies {
  margin-left: 20px;
  padding-left: 8px;
  border-left: 2px solid var(--el-border-color-lighter, #ebeef5);
}
.report-thread-actions {
  margin-top: 2px;
}

html.dark .report-detail-comments {
  background-color: #1e1e20;
  border-left-color: #3a3a3e;
  color: #e5eaf3;

  .report-detail-comments-title {
    color: #a3a6ad;
  }
  .report-detail-comments-empty {
    color: #6b6e76;
  }
  .report-detail-comments-draft {
    background-color: #252529;
    border: 1px solid #3a3a3e;
  }
  .report-detail-comments-draft-quote {
    color: #a3a6ad;
  }
  .report-thread {
    background-color: #252529;
    border-color: #3a3a3e;
    &:hover {
      background-color: #2a2a2c;
    }
    &.is-active {
      border-color: var(--crepe-color-primary, #f4bd6f);
      background-color: #333336;
    }
  }
  .report-thread-quote,
  .report-thread-comment-title .time {
    color: #a3a6ad;
  }
  .report-thread-comment-title .name,
  .report-thread-comment-content {
    color: #e5eaf3;
  }
  .report-thread-replies {
    border-left-color: #3a3a3e;
  }
}
</style>
