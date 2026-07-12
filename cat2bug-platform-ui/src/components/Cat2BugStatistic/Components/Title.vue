<template>
  <h4 class="cat2-bug-title">{{ title }}</h4>
</template>

<script>
export default {
  emits: ['update:title'],
  name: "Title",
  props: {
    title: {
      type: String,
      default: null
    }
  }
}
</script>

<style scoped>
  .cat2-bug-title {
    margin: 0;
    font-size: 12px;
    font-weight: 600;
    line-height: 1.3;
    color: var(--text-color-secondary, #909399);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
</style>
