<template>
  <div class="report-milkdown-host">
    <MilkdownProvider>
      <milkdown-editor-inner
        ref="innerRef"
        :model-value="modelValue"
        :project-id="projectId"
        @update:model-value="$emit('update:modelValue', $event)"
        @change="$emit('change', $event)"
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
    }
  },
  emits: ['update:modelValue', 'change'],
  methods: {
    insertMarkdown(text) {
      this.$refs.innerRef?.insertMarkdown?.(text)
    },
    focus() {
      this.$refs.innerRef?.focus?.()
    },
    refreshPreviewViews() {
      this.$refs.innerRef?.refreshPreviewViews?.()
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
