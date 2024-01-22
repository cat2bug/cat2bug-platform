<template>
  <div class="statistic-box">
    <cat2-bug-title :title="title"></cat2-bug-title>
    <slot name="content"></slot>
  </div>
</template>

<script>
import Cat2BugTitle from './Title'
export default {
  name: "Card",
  components: {Cat2BugTitle},
  props: {
    title: {
      type: String,
      default: null
    }
  }
}
</script>

<style scoped>
  .statistic-box {
    border: 1px solid #EBEEF5;
    margin-bottom: 10px;
    padding: 10px 30px;
    border-radius: 5px;
  }
  .cat2-but-title {
    height: 100px;
    width: 100%;
  }
</style>
