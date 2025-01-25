<template>
  <div class="block" :style="{height:height,width:width,backgroundColor:color}"></div>
</template>

<script>
export default {
  name: "index",
  props: {
    color: {
      type: String,
      default: '#409EFF'
    },
    width: {
      type: String,
      default: '8px'
    },
    height: {
      type: String,
      default: '35px'
    },
  },
}
</script>

<style scoped>
.block {
  flex-shrink: 0;
  border-radius: 5px;
  margin: 0 15px 0 0;
}
</style>
