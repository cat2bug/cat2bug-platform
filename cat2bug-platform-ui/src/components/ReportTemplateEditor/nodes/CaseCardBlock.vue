<template>
  <div class="cat2bug-case-card-block">
    <div class="cat2bug-case-card-block__header">
      <svg-icon icon-class="mk-card" />
      <span>{{ $t('case.card') }}</span>
      <el-button size="small" text type="danger" @click="$emit('remove')">{{ $t('delete') }}</el-button>
    </div>
    <p class="cat2bug-case-card-block__hint">{{ $t('case.card-preview-hint') || '预览区将渲染用例卡片列表' }}</p>
    <code class="cat2bug-case-card-block__token">{{ dataSource }}</code>
  </div>
</template>

<script>
export default {
  name: 'CaseCardBlock',
  props: {
    dataSource: {
      type: String,
      default: 'api.case.list'
    }
  },
  emits: ['remove']
}
</script>

<style scoped>
.cat2bug-case-card-block {
  border: 1px dashed var(--el-color-success-light-5);
  border-radius: 6px;
  padding: 10px 12px;
  margin: 8px 0;
  background: var(--el-fill-color-lighter);
}

.cat2bug-case-card-block__header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.cat2bug-case-card-block__hint {
  margin: 0 0 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.cat2bug-case-card-block__token {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
