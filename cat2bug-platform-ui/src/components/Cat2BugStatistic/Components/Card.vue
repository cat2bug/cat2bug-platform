<template>
  <div class="statistic-box">
    <div class="statistic-box-header">
      <cat2-bug-title :title="title"></cat2-bug-title>
      <div class="statistic-box-tools">
        <slot name="left-tools"></slot>
        <slot name="default-tools">
          <svg-icon class="statistic-box-button" v-for="(tool,index) in tools" :key="index" :icon-class="tool.icon" @click="toolsHandle($event,tool)" />
        </slot>
        <slot name="right-tools"></slot>
      </div>
    </div>
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
    },
    tools: {
      type: Array,
      default: ()=>[]
    }
  },
  methods: {
    toolsHandle(e,tool) {
      this.$emit('tools-click',e,tool);
      e.stopPropagation();
    }
  }
}
</script>

<style lang="scss" scoped>
  .statistic-box {
    background-color: #FFFFFF;
    border: 1px solid #EBEEF5;
    padding: 10px 30px;
    border-radius: 5px;
    .statistic-box-header {
      display: inline-flex;
      flex-direction: row;
      justify-content: space-between;
      align-items: center;
      width: 100%;
      .cat2-bug-title {
        flex: 1
      }
      .statistic-box-tools {
        margin-right: -5px;
        font-size: 14px;
        .statistic-box-button {
          cursor: pointer;
          color: #C0C4CC;
          margin: 0px 5px;
        }
        .statistic-box-button:hover {
          color: #409EFF;
        }
      }
    }
  }
  .cat2-but-title {
    height: 100px;
    width: 100%;
  }
</style>
