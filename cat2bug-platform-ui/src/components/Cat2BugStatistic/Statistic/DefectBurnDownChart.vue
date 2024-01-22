<template>
  <cat2-bug-card :title="title">
    <template slot="content">
      <div id="defect-burn-down-chart-div"></div>
    </template>
  </cat2-bug-card>
</template>

<script>
import * as echarts from "echarts"
import Cat2BugCard from "../Components/Card"

// 缺陷燃尽图
export default {
  name: "DefectBurnDownChart",
  components: {Cat2BugCard},
  data() {
    return {
      title: "缺陷燃尽图",
      customColors: [
        {color: '#6f7ad3', percentage: 20},
        {color: 'rgb(245, 108, 108)', percentage: 40},
        {color: 'rgb(251, 177, 63)', percentage: 60},
        {color: '#1989fa', percentage: 80},
        {color: 'rgb(103, 194, 58)', percentage: 100},
      ]
    }
  },
  props: {
    params: {
      type: Object,
      default: ()=>[]
    }
  },
  mounted() {
    this.chartInit();
  },
  methods: {
    chartInit() {
      let chart = echarts.init(document.getElementById('defect-burn-down-chart-div'));
      let option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'cross',
            label: {
              backgroundColor: '#6a7985'
            }
          }
        },
        legend: {
          data: ['Email', 'Union Ads', 'Video Ads', 'Direct', 'Search Engine']
        },
        toolbox: {
          feature: {
            saveAsImage: {}
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: [
          {
            type: 'category',
            boundaryGap: false,
            data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
          }
        ],
        yAxis: [
          {
            type: 'value'
          }
        ],
        series: [
          {
            name: 'Email',
            type: 'line',
            stack: 'Total',
            areaStyle: {},
            emphasis: {
              focus: 'series'
            },
            data: [120, 132, 101, 134, 90, 230, 210]
          },
          {
            name: 'Union Ads',
            type: 'line',
            stack: 'Total',
            areaStyle: {},
            emphasis: {
              focus: 'series'
            },
            data: [220, 182, 191, 234, 290, 330, 310]
          },
          {
            name: 'Video Ads',
            type: 'line',
            stack: 'Total',
            areaStyle: {},
            emphasis: {
              focus: 'series'
            },
            data: [150, 232, 201, 154, 190, 330, 410]
          },
          {
            name: 'Direct',
            type: 'line',
            stack: 'Total',
            areaStyle: {},
            emphasis: {
              focus: 'series'
            },
            data: [320, 332, 301, 334, 390, 330, 320]
          },
          {
            name: 'Search Engine',
            type: 'line',
            stack: 'Total',
            label: {
              show: true,
              position: 'top'
            },
            areaStyle: {},
            emphasis: {
              focus: 'series'
            },
            data: [820, 932, 901, 934, 1290, 1330, 1320]
          }
        ]
      };
      option && chart.setOption(option);
      window.addEventListener('resize',()=>{
        chart.resize();
      });
    }
  }
}
</script>

<style scoped>
  #defect-burn-down-chart-div {
    width: 200px;
    height: 100px;
  }
</style>
