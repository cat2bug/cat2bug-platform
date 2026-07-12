<template>
  <div class="cat2bug-raw-block">
    <div class="cat2bug-raw-block__label">{{ $t('mk.raw-dsl') || 'DSL' }}</div>
    <pre class="cat2bug-raw-block__content">{{ value }}</pre>
  </div>
</template>

<script>
export default {
  name: 'RawBlock',
  props: {
    value: {
      type: String,
      default: ''
    }
  }
}
</script>

<style scoped>
.cat2bug-raw-block {
  border: 1px dashed var(--el-border-color);
  border-radius: 4px;
  padding: 8px;
  margin: 8px 0;
  background: var(--el-fill-color-blank);
}

.cat2bug-raw-block__label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
}

.cat2bug-raw-block__content {
  margin: 0;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
