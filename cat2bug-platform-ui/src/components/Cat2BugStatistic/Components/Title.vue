<template>
  <h4>{{title}}</h4>
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
  h4 {
    margin-bottom: 10px;
    margin-top: 5px;
  }
</style>
