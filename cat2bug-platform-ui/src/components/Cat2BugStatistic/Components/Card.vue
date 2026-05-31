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
    <div class="statistic-box-body">
      <slot name="content"></slot>
    </div>
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
    box-sizing: border-box;
    background-color: var(--statistic-card-bg, #f8f8f9);
    border: 1px solid var(--border-color-light, #ebeef5);
    padding: var(--statistic-card-padding, 8px 12px);
    border-radius: var(--cat2bug-border-radius, 4px);
    display: flex;
    flex-direction: column;
    width: 100%;
    height: 100%;
    min-height: var(--statistic-card-min-height, 0);

    .statistic-box-header {
      display: flex;
      flex-direction: row;
      justify-content: space-between;
      align-items: center;
      gap: 8px;
      min-height: var(--statistic-card-header-min-height, 17px);
      margin-bottom: var(--statistic-card-header-gap, 6px);
      padding-bottom: var(--statistic-card-header-gap, 6px);
      border-bottom: 1px solid var(--border-color-lighter, #f2f6fc);

      .cat2-bug-title {
        flex: 1;
        min-width: 0;
      }

      .statistic-box-tools {
        flex-shrink: 0;
        display: inline-flex;
        align-items: center;
        font-size: 12px;

        .statistic-box-button {
          cursor: pointer;
          color: var(--cat2bug-input-icon-color, #c0c4cc);
          width: 14px;
          height: 14px;
          margin: 0;
          padding: 2px;
          border-radius: 2px;
          transition: color 0.2s, background-color 0.2s;

          &:hover {
            color: #409eff;
            background-color: rgba(64, 158, 255, 0.08);
          }
        }
      }
    }

    .statistic-box-body {
      min-height: 0;
      flex: 1 1 auto;
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }
  }
</style>
