<template>
  <MdEditor
    ref="editorRef"
    class="cat2bug-md-editor-pane"
    :model-value="modelValue"
    :preview="false"
    :toolbars="[]"
    :footers="[]"
    :show-code-row-number="false"
    language="zh-CN"
    @update:model-value="$emit('update:modelValue', $event)"
    @on-change="onChange"
  />
</template>

<script>
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'

/**
 * Edit pane for Cat2BugMarkdown — md-editor-v3 codemirror without built-in toolbar/preview.
 * Preview stays in the sibling markdown-it pane (custom plugins).
 */
export default {
  name: 'MdEditorPane',
  components: { MdEditor },
  props: {
    modelValue: {
      type: String,
      default: ''
    }
  },
  emits: ['update:modelValue', 'change'],
  methods: {
    onChange(value) {
      this.$emit('change', value)
    },
    focus() {
      const editor = this.$refs.editorRef
      if (editor && typeof editor.focus === 'function') {
        editor.focus()
      }
    },
    insertAtCursor(text) {
      if (!text) return
      const editor = this.$refs.editorRef
      if (editor && typeof editor.insert === 'function') {
        const payload = `\n${text}\n`
        editor.insert(() => ({
          targetValue: payload,
          select: false,
          deviationStart: 0,
          deviationEnd: 0
        }))
        return
      }
      this.$emit('update:modelValue', `${this.modelValue || ''}\n${text}\n`)
    },
    insertFormatted(generator) {
      const editor = this.$refs.editorRef
      if (editor && typeof editor.insert === 'function') {
        editor.insert(generator)
        return true
      }
      return false
    }
  }
}
</script>

<style scoped>
.cat2bug-md-editor-pane {
  height: 100%;
  border: none;
  box-shadow: none;
}
.cat2bug-md-editor-pane :deep(.md-editor) {
  height: 100%;
  border: none;
  box-shadow: none;
  background: transparent;
}
.cat2bug-md-editor-pane :deep(.md-editor-toolbar-wrapper),
.cat2bug-md-editor-pane :deep(.md-editor-footer) {
  display: none;
}
.cat2bug-md-editor-pane :deep(.md-editor-content) {
  height: 100%;
}
.cat2bug-md-editor-pane :deep(.cm-editor) {
  height: 100%;
  font-size: 16px;
  letter-spacing: 1px;
}
</style>
