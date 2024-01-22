<template>
  <div class="statistic-tools">
    <component :is="sc.name" :params="sc.params" v-for="(sc,index) in statisticComponents" :key="index"/>
  </div>
</template>

<script>
import Cat2ButTitle from "./Components/Title"
import DefectType from "./Statistic/DefectType"
import DefectState from "./Statistic/DefectState"
import DefectModule from "./Statistic/DefectModule"
import DefectBurnDownChart from "./Statistic/DefectBurnDownChart"

export default {
  name: "Cat2BugStatistic",
  components:{Cat2ButTitle, DefectType,DefectState,DefectModule,DefectBurnDownChart},
  data() {
    return {
      statisticComponents: [{
        title: '类型统计',
        name: 'DefectType',
        params: {}
      },{
        title: '状态统计',
        name: 'DefectState',
        params: {}
      },{
        title: '模块排行',
        name: 'DefectModule',
        params: {}
      },{
        title: '缺陷燃尽图',
        name: 'DefectBurnDownChart',
        params: {}
      }]
    }
  },
  props: {
    params: {
      type: Object,
      default: {}
    }
  },
  methods: {
    search(params) {
      this.$parent.search && this.$parent.search(params);
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
    gap:15px
  }
</style>
