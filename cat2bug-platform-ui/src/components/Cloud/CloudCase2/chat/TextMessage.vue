<template>
  <span class="test-message">
    {{data}}
  </span>
</template>

<script>
export default {
  name: "TextMessage",
  props: {
    data: {
      type: String,
      default: ''
    }
  },
}
</script>

<style scoped>
.test-message {
  font-weight: 500;
  color: #606266;
  margin: 20px;
}
</style>
