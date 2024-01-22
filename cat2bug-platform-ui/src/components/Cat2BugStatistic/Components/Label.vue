<template>
  <div class="cat2bug-statistic-label">
    <svg-icon :icon-class="icon" :style="`color: ${iconColor||'#606266'};`" />
    <label>{{ label }}</label>
    <slot name="content">
      <span>{{ content }}</span>
    </slot>
  </div>
</template>

<script>
export default {
  name: "Label",
  props: {
    icon: {
      type: String,
      default: null
    },
    iconColor: {
      type: String,
      default: null
    },
    label: {
      type: String,
      default: null
    },
    content: {
      type: [String,Number],
      default: null
    }
  }
}
</script>

<style scoped>
  .cat2bug-statistic-label {
    margin: 5px 0px;
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: flex-start;
  }
  label {
    font-size: 13px;
    font-weight: 500;
    margin-right: 10px;
    margin-left: 5px;
    min-width: 60px;
  }
  span {
    font-size: 13px;
    text-align: right;
    min-width: 80px;
  }
</style>
