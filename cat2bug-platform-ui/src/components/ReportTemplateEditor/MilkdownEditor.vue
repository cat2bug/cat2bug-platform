<template>
  <MilkdownProvider>
    <milkdown-editor-inner
      :model-value="modelValue"
      @update:model-value="$emit('update:modelValue', $event)"
      @change="$emit('change', $event)"
    />
  </MilkdownProvider>
</template>

<script>
import { MilkdownProvider } from '@milkdown/vue'
import MilkdownEditorInner from './MilkdownEditorInner.vue'

export default {
  name: 'MilkdownEditor',
  components: { MilkdownProvider, MilkdownEditorInner },
  props: {
    modelValue: {
      type: String,
      default: ''
    }
  },
  emits: ['update:modelValue', 'change'],
  methods: {
    focus() {
      this.$refs.inner?.focus?.()
    }
  }
}
</script>
