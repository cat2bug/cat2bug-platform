<template>
  <div class="statistic-tools">
    <component class="statistic-box" v-for="(sc,index) in statisticComponents" :key="index" :is="sc.name" :params="sc.params" />
  </div>
</template>

<script>
import DefectType from "./Statistic/DefectType"
import DefectState from "./Statistic/DefectState"
import DefectModule from "./Statistic/DefectModule"

export default {
  name: "Cat2BugStatistic",
  components:{DefectType,DefectState,DefectModule},
  data() {
    return {
      statisticComponents: [{
        name: 'DefectType',
        params: {}
      },{
        name: 'DefectState',
        params: {}
      },{
        name: 'DefectModule',
        params: {}
      }]
    }
  },
  props: {
    params: {
      type: Object,
      default: {}
    }
  }
}
</script>

<style scoped>
  .statistic-tools {
    display: flex;
    justify-content: flex-start;
    align-content: center;
    flex-direction: row;
    flex-wrap: wrap;
    gap:20px
  }
  .statistic-box {
    border: 1px solid #EBEEF5;
    margin-bottom: 10px;
    padding: 10px 30px;
    border-radius: 5px;
  }
</style>
