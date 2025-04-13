<template>
  <div v-loading="loading" class="defect-state-chart" :style="{width:width}">
    <div class="chart-tools">
      <el-radio-group v-model="query.timeType" size="mini" @input="handleTimeTypeChange">
        <el-radio-button v-for="tt in timeTypeList" :key="tt.value" :label="tt.value">{{$t(tt.label).toString()}}</el-radio-button>
      </el-radio-group>
      <el-tooltip class="item" effect="dark" :content="$t('export')" placement="right-end">
        <el-button type="text" icon="el-icon-download" @click="handleExport"></el-button>
      </el-tooltip>
    </div>
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
    /** 导出按钮操作 */
    handleExport() {
      this.download('/system/dashboard/'+this.projectId+'/member-defect-line/export', {
        ...this.query
      }, `${ this.$i18n.t(this.title) }_${new Date().getTime()}.xlsx`)
    },
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
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: xData
        },
        yAxis: {
          type: 'value'
        },
        series: series
      },true);
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
.chart-tools {
  position: absolute;
  z-index: 9;
  top: 7px;
  right: 10px;
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
  ::v-deep .el-radio-button__inner {
    padding: 3px 5px;
  }
}
</style>
