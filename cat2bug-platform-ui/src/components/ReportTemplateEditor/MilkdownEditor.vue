<template>
  <div class="report-milkdown-host">
    <MilkdownProvider>
      <milkdown-editor-inner
        ref="innerRef"
        :model-value="modelValue"
        :project-id="projectId"
        :enable-dsl="enableDsl"
        :enable-selection-comment="enableSelectionComment"
        @update:model-value="$emit('update:modelValue', $event)"
        @change="$emit('change', $event)"
        @selection-comment="$emit('selection-comment', $event)"
      />
    </MilkdownProvider>
  </div>
</template>

<script>
import { MilkdownProvider } from '@milkdown/vue'
import MilkdownEditorInner from './MilkdownEditorInner.vue'

export default {
  name: 'MilkdownEditor',
  components: { MilkdownProvider, MilkdownEditorInner },
  inheritAttrs: false,
  props: {
    modelValue: {
      type: String,
      default: ''
    },
    projectId: {
      type: [Number, String],
      default: null
    },
    /** When false, skip remark-cat2bug-dsl / dynamic variable NodeViews (report body). */
    enableDsl: {
      type: Boolean,
      default: true
    },
    /** Report body: add「批注」on Crepe selection toolbar. */
    enableSelectionComment: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue', 'change', 'selection-comment'],
  methods: {
    insertMarkdown(text) {
      this.$refs.innerRef?.insertMarkdown?.(text)
    },
    focus() {
      this.$refs.innerRef?.focus?.()
    },
    refreshPreviewViews() {
      this.$refs.innerRef?.refreshPreviewViews?.()
    },
    syncFromModel() {
      this.$refs.innerRef?.syncFromModel?.()
    },
    hasFocus() {
      return !!this.$refs.innerRef?.hasFocus?.()
    }
  }
}
</script>

<style scoped>
.report-milkdown-host {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.report-milkdown-host :deep(> *) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
</style>
