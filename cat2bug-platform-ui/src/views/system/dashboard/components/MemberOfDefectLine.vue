<template>
  <div v-loading="loading" class="defect-state-chart" :style="{width:width}">
    <el-radio-group v-model="query.timeType" size="mini" @input="handleTimeTypeChange" class="time-type">
      <el-radio-button v-for="tt in timeTypeList" :key="tt.value" :label="tt.value">{{$t(tt.label).toString()}}</el-radio-button>
    </el-radio-group>
    <h1 class="title">{{ $t(this.title) }}</h1>
    <div v-loading="loading" ref="memberDefectLineChart" :class="className" :style="{height:height,width:width}" />
  </div>
</template>

<script>
// 成员处理缺陷排行

import {memberLineOfDefects, memberRankOfDefects} from "@/api/system/dashboard";
import Cat2BugAvatar from "@/components/Cat2BugAvatar";
import * as echarts from "echarts";
import resize from "@/views/dashboard/mixins/resize";

export default {
  name: "MemberOfDefectLine",
  components: { Cat2BugAvatar },
  mixins: [resize],
  props: {
    className: {
      type: String,
      default: 'chart'
    },
    width: {
      type: String,
      default: '100%'
    },
    height: {
      type: String,
      default: '200px'
    },
  },
  data() {
    return {
      loading: false,
      title: 'dashboard.member-Defect-line.day.title',
      memberList: [],
      timeTypeList: [
        {value:'day', label: 'dashboard.day'},
        {value:'month', label: 'dashboard.month'}],
      query: {
        timeType: 'day'
      }
    }
  },
  computed: {
    /** 获取项目id */
    projectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
  },
  mounted() {
    this.initChart();
    this.getMemberLineOfDefects();
  },
  methods: {
    getMemberLineOfDefects() {
      this.loading = true;
      memberLineOfDefects(this.projectId, this.query).then(res=>{
        this.loading = false;
        const memberNames = [...new Set(res.data.data.map(d=>d.nickName))];
        const times = res.data.time;
        const memberData = memberNames.map(m=>{
          const data = times.map(t=>{
            let val = res.data.data.find(d=>d.nickName===m && d.createTime===t);
            return val?val.defectTodayCount:0;
          });
          return {
            name: m,
            type: 'line',
            data: data
          }
        });

        this.$nextTick(()=>{
          this.setOptions(memberNames, times, memberData);
        });
      }).catch(e=>this.loading=false);
    },
    initChart() {
      this.chart = echarts.init(this.$refs.memberDefectLineChart, 'macarons')
      this.setOptions([],[])
    },
    setOptions(legendData, xData, series) {
      this.chart.setOption({
        title: {
          text: ''
        },
        tooltip: {
          trigger: 'axis'
        },
        legend: {
          top: '35',
          left: '3%',
          right: '4%',
          type: 'scroll',
          width: 'auto',
          data: legendData
        },
        grid: {
          top: '70',
          left: '4%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        toolbox: {
          feature: {
            saveAsImage: {}
          }
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: xData
        },
        yAxis: {
          type: 'value'
        },
        series: series
      })
    },
    /** 处理时间类型变更 */
    handleTimeTypeChange(val) {
      switch (this.query.timeType) {
        case 'day':
          this.title = 'dashboard.member-Defect-line.day.title';
          break;
        case 'month':
          this.title = 'dashboard.member-Defect-line.month.title';
          break;
      }
      this.getMemberLineOfDefects();
    }
  }
}
</script>

<style lang="scss" scoped>
.defect-state-chart {
  position: relative;
  .title {
    position: absolute;
    top: 7px;
    margin-top: 0px;
  }
}
.time-type {
  position: absolute;
  z-index: 9;
  top: 7px;
  right: 40px;
  ::v-deep .el-radio-button__inner {
    padding: 3px 5px;
  }
}
</style>
