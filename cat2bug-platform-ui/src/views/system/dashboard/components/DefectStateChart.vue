<template>
  <div class="defect-state-chart" :style="{height:height,width:width}">
    <el-radio-group v-model="query.timeType" size="mini" @input="handleTimeTypeChange" class="time-type">
      <el-radio-button v-for="tt in timeTypeList" :label="tt.value">{{$t(tt.label).toString()}}</el-radio-button>
    </el-radio-group>

    <div v-loading="loading" ref="defectStateChart" :class="className" :style="{height:height,width:width}" />
  </div>
</template>

<script>
import * as echarts from 'echarts';
require('echarts/theme/macarons') // echarts theme
import resize from "@/views/dashboard/mixins/resize";
import {defectLine} from "@/api/system/dashboard";

export default {
  name: "DefectStateChart",
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
      default: '350px'
    },
    autoResize: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      loading: false,
      chart: null,
      timeTypeList: [
        {value:'day', label: 'dashboard.day'},
        {value:'month', label: 'dashboard.month'}],
      title: this.$i18n.t('dashboard.defect-state.day'),
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
    this.getDefectLine();
  },
  methods: {
    getDefectLine() {
      this.loading = true;
      defectLine(this.projectId, this.query).then(res=>{
        this.loading = false;
        const series = Object.keys(res.data.data).map(k=>{
          return {
            name: this.$i18n.t(k),
            type: 'line',
            data: res.data.data[k]
          }
        })
        const types = res.data.types.map(t=>{
          return this.$i18n.t(t);
        });
        const times = res.data.times;
        console.log(types, times, series);
        this.setOptions(types, times, series);
      }).catch(e=>this.loading=false);
    },
    initChart() {
      this.chart = echarts.init(this.$refs.defectStateChart, 'macarons')
      this.setOptions([],[], [])
    },
    setOptions(legendData, xData, series) {
      this.chart.setOption({
          title: {
            text: this.title
          },
          tooltip: {
            trigger: 'axis'
          },
          legend: {
            data: legendData
          },
          grid: {
            left: '3%',
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
          this.title = this.$i18n.t('dashboard.defect-state.day');
          break;
        case 'month':
          this.title = this.$i18n.t('dashboard.defect-state.month');
          break;
      }
      this.getDefectLine();
    }
  }
}
</script>

<style lang="scss" scoped>
.defect-state-chart {
  position: relative;
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
