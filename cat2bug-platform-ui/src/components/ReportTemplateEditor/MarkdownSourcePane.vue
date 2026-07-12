<template>
  <div class="report-template-markdown-source">
    <MdEditor
      class="report-template-markdown-source__editor"
      :model-value="content"
      :theme="editorTheme"
      :preview="false"
      :toolbars="[]"
      :footers="[]"
      :show-code-row-number="true"
      :read-only="true"
      language="zh-CN"
    />
  </div>
</template>

<script>
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { useSettingsStore } from '@/stores/settings'
import { isReportEditorDarkMode } from './report-editor-theme'

export default {
  name: 'MarkdownSourcePane',
  components: { MdEditor },
  props: {
    content: {
      type: String,
      default: ''
    }
  },
  setup() {
    const settingsStore = useSettingsStore()
    const { themeMode } = storeToRefs(settingsStore)
    const editorTheme = computed(() => {
      return themeMode.value === 'dark' || isReportEditorDarkMode() ? 'dark' : 'light'
    })
    return { editorTheme }
  }
}
</script>

<style scoped>
.report-template-markdown-source {
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: transparent;
}

.report-template-markdown-source__editor {
  height: 100%;
  border: none;
  box-shadow: none;
}

.report-template-markdown-source__editor :deep(.md-editor) {
  height: 100%;
  border: none;
  box-shadow: none;
  background: transparent;
}

.report-template-markdown-source__editor :deep(.md-editor-content) {
  height: 100%;
}

.report-template-markdown-source__editor :deep(.cm-editor) {
  height: 100%;
  font-size: 14px;
  line-height: 1.6;
}
</style>
