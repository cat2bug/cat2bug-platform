<template>
  <h4 class="cat2-bug-title">{{ title }}</h4>
</template>

<script>
export default {
  name: "Title",
  model: {
    prop: 'title',
    event: 'change'
  },
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
