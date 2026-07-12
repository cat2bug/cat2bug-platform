<template>
  <el-tag
    class="cat2bug-variable-chip"
    type="info"
    size="small"
    closable
    @close="$emit('remove')"
  >
    {{ displayLabel }}
  </el-tag>
</template>

<script>
export default {
  name: 'VariableChip',
  props: {
    path: {
      type: String,
      required: true
    },
    planIndex: {
      type: Number,
      default: null
    }
  },
  emits: ['remove'],
  computed: {
    displayLabel() {
      if (this.planIndex != null) {
        return `\${${this.path}[${this.planIndex}]}`
      }
      return `\${${this.path}}`
    }
  }
}
</script>

<style scoped>
.cat2bug-variable-chip {
  font-family: var(--el-font-family-monospace, monospace);
  margin: 0 2px;
  vertical-align: middle;
}
</style>
