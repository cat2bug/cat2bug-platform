<template>
  <div class="step">
    <div class="step-row" v-for="(step, index) in steps" :key="index">
      <p>{{ index + 1 }}</p>
      <span>{{ step.stepDescribe }}</span>
    </div>
  </div>
</template>

<script>
export default {
  name: "step",
  props: {
    steps: {
      type: Array,
      default: ()=>[]
    }
  }
}
</script>

<style lang="scss" scoped>
.step {
  display: flex;
  flex-direction: column;
}
.step-row {
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  p {
    width: 15px;
    height: 15px;
    text-align: center;
    line-height: 15px;
    font-size: 12px;
    margin: 0px 10px;
    border-radius: 100%;
    background-color: #13ce66;
    color: #FFFFFF;
  }
  span {
    flex:1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    text-align: left;
    font-size: 14px;
  }
}
</style>
