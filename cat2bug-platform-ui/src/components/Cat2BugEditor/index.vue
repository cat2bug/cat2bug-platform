<template>
  <div
    class="cat2bug-editor"
    :class="{ 'is-disabled': !editable, 'is-paper': paper, 'is-flat': flat }"
    :style="editorStyle"
  >
    <div v-if="editable" class="cat2bug-editor__toolbar" @mousedown.prevent>
      <el-tooltip v-if="showSave" :content="$t('save')" placement="bottom">
        <button
          type="button"
          class="cat2bug-editor__save"
          :class="{ 'is-disabled': saveDisabled }"
          :disabled="saveDisabled || saveLoading"
          @click="$emit('save')"
        >
          <svg-icon icon-class="mk-save" class="cat2bug-editor__save-icon" :class="{ 'is-spinning': saveLoading }" />
        </button>
      </el-tooltip>
      <span v-if="showSave" class="cat2bug-editor__toolbar-siding" />
      <tools-menu :tools="tools" :ellipsis="false" @select="onToolSelect" />
      <input ref="imageFile" type="file" accept="image/*" class="cat2bug-editor__file" @change="onImageFile" />
    </div>
    <div class="cat2bug-editor__body">
      <div class="cat2bug-editor__page">
        <editor-content class="cat2bug-editor__content" :editor="editor" />
      </div>
    </div>
    <el-dialog
      :title="$t('mk.add-link')"
      v-model="linkDialogVisible"
      append-to-body
      width="500px"
      @closed="onLinkDialogClosed"
    >
      <el-form :model="linkForm" @submit.prevent>
        <el-form-item :label="$t('mk.link-describe')" label-width="120px">
          <el-input v-model="linkForm.describe" autocomplete="off" maxlength="255" />
        </el-form-item>
        <el-form-item :label="$t('mk.link-url')" label-width="120px">
          <el-input v-model="linkForm.url" autocomplete="off" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelLinkDialog">{{ $t('cancel') }}</el-button>
        <el-button type="primary" @click="confirmLinkDialog">{{ $t('ok') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { Editor, EditorContent } from '@tiptap/vue-3'
import { NodeSelection } from '@tiptap/pm/state'
import StarterKit from '@tiptap/starter-kit'
import { Markdown } from '@tiptap/markdown'
import Image from '@tiptap/extension-image'
import TextAlign from '@tiptap/extension-text-align'
import Highlight from '@tiptap/extension-highlight'
import Superscript from '@tiptap/extension-superscript'
import Subscript from '@tiptap/extension-subscript'
import Placeholder from '@tiptap/extension-placeholder'
import { TableKit } from '@tiptap/extension-table'
import ToolsMenu from '@/components/Cat2BugMarkdown/components/ToolsMenu'
import { MarkdownTools } from '@/components/Cat2BugMarkdown/MarkdownTools'
import { upload } from '@/api/common/upload'

const BUSINESS_TOOLS = new Set(['project', 'case', 'defect', 'member', 'plan', 'save', 'clear', 'siding-9899'])

function escapeHtmlAttr(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/"/g, '&quot;')
    .replace(/</g, '&lt;')
}

/**
 * 有宽高时写成块级 HTML。图片节点是块级，行内 img 再读回来会被丢掉。
 */
const ResizableImage = Image.extend({
  renderMarkdown(node) {
    const src = node.attrs && node.attrs.src ? node.attrs.src : ''
    const alt = node.attrs && node.attrs.alt ? node.attrs.alt : ''
    const title = node.attrs && node.attrs.title ? node.attrs.title : ''
    const width = node.attrs && node.attrs.width
    const height = node.attrs && node.attrs.height
    if (width || height) {
      const parts = [`src="${escapeHtmlAttr(src)}"`, `alt="${escapeHtmlAttr(alt)}"`]
      if (title) parts.push(`title="${escapeHtmlAttr(title)}"`)
      if (width) parts.push(`width="${Math.round(Number(width))}"`)
      if (height) parts.push(`height="${Math.round(Number(height))}"`)
      return `<div><img ${parts.join(' ')}></div>`
    }
    return title ? `![${alt}](${src} "${title}")` : `![${alt}](${src})`
  }
})

function clipboardImageFiles(event) {
  const found = []
  const data = event && event.clipboardData
  if (!data) return found
  const raw = []
  if (data.files && data.files.length) {
    for (let i = 0; i < data.files.length; i++) raw.push(data.files[i])
  }
  if (!raw.length && data.items) {
    for (let i = 0; i < data.items.length; i++) {
      const item = data.items[i]
      if (item.kind === 'file' && item.type && item.type.indexOf('image/') === 0) {
        const file = item.getAsFile()
        if (file) raw.push(file)
      }
    }
  }
  raw.forEach((file) => {
    if (!file || !file.type || file.type.indexOf('image/') !== 0) return
    if (file.name) {
      found.push(file)
      return
    }
    const ext = (file.type.split('/')[1] || 'png').replace('jpeg', 'jpg')
    found.push(new File([file], 'clipboard.' + ext, { type: file.type }))
  })
  return found
}

function clipboardDataImageUrls(event) {
  const html = event && event.clipboardData && event.clipboardData.getData
    ? event.clipboardData.getData('text/html') || ''
    : ''
  if (!html) return []
  const urls = []
  const pattern = /src\s*=\s*["'](data:image\/[a-zA-Z0-9.+-]+;base64,[^"']+)["']/gi
  let match = pattern.exec(html)
  while (match) {
    urls.push(match[1])
    match = pattern.exec(html)
  }
  return urls
}

function editorToolbarTools(includeBusiness) {
  const tools = MarkdownTools().filter((tool) => {
    if (!includeBusiness && tool.name && BUSINESS_TOOLS.has(tool.name)) {
      return false
    }
    return true
  })
  while (tools.length && tools[tools.length - 1].type === 'siding') {
    tools.pop()
  }
  return tools
}

export default {
  name: 'Cat2BugEditor',
  components: { EditorContent, ToolsMenu },
  props: {
    modelValue: {
      type: String,
      default: ''
    },
    placeholder: {
      type: String,
      default: ''
    },
    editable: {
      type: Boolean,
      default: true
    },
    minHeight: {
      type: Number,
      default: 280
    },
    /** 带上项目/用例/缺陷等插入项。默认只保留排版工具条。 */
    includeBusinessTools: {
      type: Boolean,
      default: false
    },
    /**
     * 连续 A4 纸面：宽 210mm、页边距约 25/20mm，不分页。
     * 弹框等窄场景传 false，编辑区铺满容器。
     */
    paper: {
      type: Boolean,
      default: true
    },
    /** 纸面内外底色接近、无内框、仅保留左右边距（文档页等） */
    flat: {
      type: Boolean,
      default: false
    },
    /** 工具条最左侧显示保存图标 */
    showSave: {
      type: Boolean,
      default: false
    },
    saveDisabled: {
      type: Boolean,
      default: false
    },
    saveLoading: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue', 'save'],
  data() {
    return {
      editor: null,
      tools: editorToolbarTools(this.includeBusinessTools),
      linkDialogVisible: false,
      linkForm: { describe: '', url: '' }
    }
  },
  computed: {
    editorStyle() {
      return { '--cat2bug-editor-min-height': this.minHeight + 'px' }
    }
  },
  watch: {
    modelValue(next) {
      this.applyExternalMarkdown(next)
    },
    editable(next) {
      if (this.editor) {
        this.editor.setEditable(!!next)
      }
    }
  },
  mounted() {
    const placeholder = this.placeholder
    this.editor = new Editor({
      editable: this.editable,
      content: this.modelValue || '',
      contentType: 'markdown',
      extensions: [
        StarterKit.configure({
          link: { openOnClick: false, autolink: true }
        }),
        Markdown,
        ResizableImage.configure({
          inline: false,
          resize: {
            enabled: true,
            minWidth: 48,
            minHeight: 48,
            alwaysPreserveAspectRatio: true
          }
        }),
        TextAlign.configure({ types: ['heading', 'paragraph'] }),
        Highlight,
        Superscript,
        Subscript,
        TableKit.configure({ table: { resizable: false } }),
        Placeholder.configure({ placeholder })
      ],
      onUpdate: () => {
        this.emitMarkdown()
      },
      editorProps: {
        handleClickOn: (view, _pos, node, nodePos, _event, direct) => {
          if (!direct || node.type.name !== 'image') return false
          view.dispatch(view.state.tr.setSelection(NodeSelection.create(view.state.doc, nodePos)))
          return true
        },
        handlePaste: (_view, event) => {
          const files = clipboardImageFiles(event)
          const dataUrls = files.length ? [] : clipboardDataImageUrls(event)
          if (!files.length && !dataUrls.length) return false
          event.preventDefault()
          this.insertClipboardImages(files, dataUrls)
          return true
        }
      }
    })
  },
  beforeUnmount() {
    if (this.editor) {
      this.editor.destroy()
      this.editor = null
    }
  },
  methods: {
    emitMarkdown() {
      if (!this.editor) return
      const markdown = this.editor.getMarkdown()
      if (markdown !== this.modelValue) {
        this.$emit('update:modelValue', markdown)
      }
    },
    applyExternalMarkdown(next) {
      if (!this.editor) return
      const incoming = next || ''
      if (incoming === this.editor.getMarkdown()) return
      this.editor.commands.setContent(incoming, { contentType: 'markdown' })
    },
    onToolSelect(_tools, tool) {
      if (!tool || !this.editor) return
      const name = tool.name
      const chain = () => this.editor.chain().focus()
      if (name === 'font-weight') return chain().toggleBold().run()
      if (name === 'font-italic') return chain().toggleItalic().run()
      if (name === 'title1') return chain().toggleHeading({ level: 1 }).run()
      if (name === 'title2') return chain().toggleHeading({ level: 2 }).run()
      if (name === 'title3') return chain().toggleHeading({ level: 3 }).run()
      if (name === 'title4') return chain().toggleHeading({ level: 4 }).run()
      if (name === 'title5') return chain().toggleHeading({ level: 5 }).run()
      if (name === 'font-underline') return chain().toggleUnderline().run()
      if (name === 'font-strikethrough') return chain().toggleStrike().run()
      if (name === 'flag') return chain().toggleHighlight().run()
      if (name === 'font-superscript') return chain().toggleSuperscript().run()
      if (name === 'font-subscript') return chain().toggleSubscript().run()
      if (name === 'font-left') return chain().setTextAlign('left').run()
      if (name === 'font-center') return chain().setTextAlign('center').run()
      if (name === 'font-right') return chain().setTextAlign('right').run()
      if (name === 'paragraph') return chain().toggleBlockquote().run()
      if (name === 'ordered-list') return chain().toggleOrderedList().run()
      if (name === 'unordered-list') return chain().toggleBulletList().run()
      if (name === 'code') return chain().toggleCodeBlock().run()
      if (name === 'table') return chain().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()
      if (name === 'link') {
        this.openLinkDialog()
        return
      }
      if (name === 'mk.add-image-link') {
        this.promptImageUrl()
        return
      }
      if (name === 'upload-image') {
        this.$refs.imageFile && this.$refs.imageFile.click()
        return
      }
      if (typeof tool.content === 'string') {
        const { from, to } = this.editor.state.selection
        const selected = this.editor.state.doc.textBetween(from, to, '\n')
        const markdown = tool.content.replaceAll('{0}', selected)
        chain().insertContent(markdown, { contentType: 'markdown' }).run()
      }
    },
    openLinkDialog() {
      this.linkForm = { describe: '', url: '' }
      this.linkDialogVisible = true
    },
    confirmLinkDialog() {
      const href = (this.linkForm.url || '').trim()
      const label = (this.linkForm.describe || '').trim() || href
      this.linkDialogVisible = false
      if (!href || !this.editor) return
      const markdown = `[${label}](${href})`
      this.editor.chain().focus().insertContent(markdown, { contentType: 'markdown' }).run()
    },
    cancelLinkDialog() {
      this.linkDialogVisible = false
    },
    onLinkDialogClosed() {},
    promptImageUrl() {
      this.$prompt(this.$t('mk.link-url'), this.$t('mk.add-image-link'), {
        confirmButtonText: this.$t('ok'),
        cancelButtonText: this.$t('cancel'),
        inputPattern: /\S+/,
        inputErrorMessage: this.$t('mk.link-url')
      }).then(({ value }) => {
        const src = String(value || '').trim()
        if (!src || !this.editor) return
        this.editor.chain().focus().setImage({ src }).run()
      }).catch(() => {})
    },
    onImageFile(event) {
      const file = event.target.files && event.target.files[0]
      event.target.value = ''
      if (!file || !this.editor) return
      this.uploadAndInsertImage(file)
    },
    async insertClipboardImages(files, dataUrls) {
      const queue = (files || []).slice()
      for (const url of dataUrls || []) {
        try {
          const response = await fetch(url)
          const blob = await response.blob()
          const ext = ((blob.type || 'image/png').split('/')[1] || 'png').replace('jpeg', 'jpg')
          queue.push(new File([blob], 'clipboard.' + ext, { type: blob.type || 'image/png' }))
        } catch (e) { /* 剪贴板里的 data URL 读不出来就跳过 */ }
      }
      for (const file of queue) {
        await this.uploadAndInsertImage(file)
      }
    },
    uploadAndInsertImage(file) {
      if (!file || !this.editor) return Promise.resolve()
      const formData = new FormData()
      formData.append('file', file)
      return upload(formData).then((res) => {
        const src = this.uploadedImageSrc(res)
        if (!src || !this.editor) return
        this.editor.chain().focus().setImage({ src }).run()
      }).catch(() => {})
    },
    uploadedImageSrc(res) {
      const fileName = res && (res.fileName || (res.data && res.data.fileName))
      if (!fileName) return ''
      const base = process.env.VUE_APP_BASE_API || ''
      const name = String(fileName)
      if (name.indexOf(base) === 0 || /^https?:/i.test(name)) return name
      return base + name
    }
  }
}
</script>

<style scoped>
.cat2bug-editor {
  --cat2bug-editor-page-width: 210mm;
  --cat2bug-editor-page-pad-y: 25mm;
  --cat2bug-editor-page-pad-x: 20mm;
  width: 100%;
  min-width: 0;
  min-height: 0;
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.cat2bug-editor__toolbar {
  flex-shrink: 0;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px;
}
.cat2bug-editor__save {
  width: 30px;
  height: 30px;
  padding: 0;
  margin: 0;
  border: none;
  border-radius: 3px;
  background: transparent;
  color: var(--text-color-regular, #606266);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 30px;
}
.cat2bug-editor__save:hover:not(:disabled) {
  color: var(--tools-menu-hover-color, #1890ff);
  background-color: var(--tools-menu-hover-bg, #e8f4ff);
}
.cat2bug-editor__save:disabled,
.cat2bug-editor__save.is-disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.cat2bug-editor__save-icon {
  width: 16px !important;
  height: 16px !important;
  font-size: 16px;
}
.cat2bug-editor__save-icon.is-spinning {
  animation: cat2bug-editor-spin 0.8s linear infinite;
}
@keyframes cat2bug-editor-spin {
  to { transform: rotate(360deg); }
}
.cat2bug-editor__toolbar-siding {
  width: 1px;
  height: 15px;
  flex: 0 0 1px;
  background-color: var(--border-color-light, #dcdcdc);
  margin: 0 5px;
  align-self: center;
}
.cat2bug-editor__file {
  display: none;
}
.cat2bug-editor__toolbar :deep(.tools-menu) {
  border: none;
  background: transparent;
  width: auto;
  flex: 1 1 auto;
  min-width: 0;
  height: auto !important;
  min-height: 30px;
}
.cat2bug-editor__body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  border: 1px solid var(--border-color-light);
  border-radius: var(--cat2bug-border-radius, 4px);
  background: var(--bg-color-base);
  color: var(--text-color-primary);
  box-sizing: border-box;
}
.cat2bug-editor:not(.is-paper) .cat2bug-editor__page {
  min-height: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
}
.cat2bug-editor.is-paper .cat2bug-editor__body {
  background: var(--bg-color-block);
  padding: 16px 12px 24px;
}
.cat2bug-editor.is-paper .cat2bug-editor__page {
  width: min(100%, var(--cat2bug-editor-page-width));
  min-height: max(297mm, var(--cat2bug-editor-min-height, 280px));
  margin: 0 auto;
  box-sizing: border-box;
  padding: var(--cat2bug-editor-page-pad-y) var(--cat2bug-editor-page-pad-x);
  background: var(--bg-color-base);
  color: var(--text-color-primary);
  border: 1px solid var(--border-color-light);
  border-radius: 2px;
  box-shadow: 0 1px 4px color-mix(in srgb, var(--text-color-primary) 8%, transparent);
}
.cat2bug-editor.is-paper.is-flat .cat2bug-editor__body {
  background: var(--bg-color-base);
  border: none;
  border-radius: 0;
  padding: 0;
}
.cat2bug-editor.is-paper.is-flat .cat2bug-editor__page {
  width: min(100%, var(--cat2bug-editor-page-width));
  margin: 0 auto;
  padding: 0 var(--cat2bug-editor-page-pad-x);
  background: var(--bg-color-base);
  border: none;
  border-radius: 0;
  box-shadow: none;
}
.cat2bug-editor.is-paper.is-flat .cat2bug-editor__content :deep(.ProseMirror) {
  min-height: var(--cat2bug-editor-min-height, 280px);
}
.cat2bug-editor__content {
  min-width: 0;
}
.cat2bug-editor__content :deep(.ProseMirror) {
  outline: none;
  min-height: var(--cat2bug-editor-min-height, 280px);
}
.cat2bug-editor.is-paper .cat2bug-editor__content :deep(.ProseMirror) {
  min-height: calc(297mm - 2 * var(--cat2bug-editor-page-pad-y));
}
.cat2bug-editor__content :deep(.ProseMirror p) {
  margin: 0 0 0.6em;
}
.cat2bug-editor__content :deep(.ProseMirror h1),
.cat2bug-editor__content :deep(.ProseMirror h2),
.cat2bug-editor__content :deep(.ProseMirror h3),
.cat2bug-editor__content :deep(.ProseMirror h4),
.cat2bug-editor__content :deep(.ProseMirror h5) {
  margin: 0.6em 0 0.4em;
  line-height: 1.3;
}
.cat2bug-editor__content :deep(.ProseMirror ul),
.cat2bug-editor__content :deep(.ProseMirror ol) {
  margin: 0 0 0.6em;
  padding-left: 1.4em;
}
.cat2bug-editor__content :deep(.ProseMirror blockquote) {
  margin: 0 0 0.6em;
  padding-left: 0.8em;
  border-left: 3px solid var(--border-color-light);
  color: var(--text-color-regular);
}
.cat2bug-editor__content :deep(.ProseMirror pre) {
  margin: 0 0 0.6em;
  padding: 8px 10px;
  border-radius: 4px;
  background: var(--bg-color-block);
  overflow: auto;
}
.cat2bug-editor__content :deep(.ProseMirror table) {
  border-collapse: collapse;
  width: 100%;
  margin: 0 0 0.6em;
}
.cat2bug-editor__content :deep(.ProseMirror th),
.cat2bug-editor__content :deep(.ProseMirror td) {
  border: 1px solid var(--border-color-light);
  padding: 4px 8px;
  vertical-align: top;
}
.cat2bug-editor__content :deep([data-resize-container]) {
  max-width: 100%;
  vertical-align: bottom;
}
.cat2bug-editor__content :deep([data-resize-wrapper]) {
  max-width: 100%;
}
.cat2bug-editor__content :deep(.ProseMirror-selectednode[data-resize-container]) {
  outline: 1px solid var(--el-color-primary);
}
.cat2bug-editor__content :deep([data-resize-handle]) {
  width: 8px;
  height: 8px;
  margin: -4px;
  box-sizing: border-box;
  background: var(--bg-color-base);
  border: 1px solid var(--el-color-primary);
  border-radius: 1px;
  z-index: 2;
  opacity: 0;
  pointer-events: none;
}
.cat2bug-editor__content :deep([data-resize-container]:hover [data-resize-handle]),
.cat2bug-editor__content :deep(.ProseMirror-selectednode [data-resize-handle]),
.cat2bug-editor__content :deep([data-resize-state='true'] [data-resize-handle]) {
  opacity: 1;
  pointer-events: auto;
}
.cat2bug-editor__content :deep([data-resize-handle='top-left']),
.cat2bug-editor__content :deep([data-resize-handle='bottom-right']) {
  cursor: nwse-resize;
}
.cat2bug-editor__content :deep([data-resize-handle='top-right']),
.cat2bug-editor__content :deep([data-resize-handle='bottom-left']) {
  cursor: nesw-resize;
}
.cat2bug-editor__content :deep(.ProseMirror img) {
  max-width: 100%;
}
.cat2bug-editor__content :deep(.ProseMirror p.is-editor-empty:first-child::before) {
  color: var(--text-color-secondary);
  content: attr(data-placeholder);
  float: left;
  height: 0;
  pointer-events: none;
}
.cat2bug-editor.is-disabled .cat2bug-editor__body {
  opacity: 0.85;
}

@media print {
  .cat2bug-editor__toolbar {
    display: none !important;
  }
  .cat2bug-editor,
  .cat2bug-editor__body {
    overflow: visible !important;
    height: auto !important;
    max-height: none !important;
    border: none !important;
    background: transparent !important;
    padding: 0 !important;
  }
  .cat2bug-editor.is-paper .cat2bug-editor__page {
    width: 210mm;
    min-height: auto;
    margin: 0;
    padding: 0;
    border: none;
    border-radius: 0;
    box-shadow: none;
    background: transparent;
  }
  .cat2bug-editor.is-paper .cat2bug-editor__content :deep(.ProseMirror) {
    min-height: 0;
  }
  @page {
    size: A4;
    margin: 25mm 20mm;
  }
}
</style>
