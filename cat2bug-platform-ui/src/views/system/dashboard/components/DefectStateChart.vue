<template>
  <div v-loading="loading" class="defect-state-chart" :style="{height:height,width:width}">
    <div class="chart-tools">
      <el-radio-group v-model="query.timeType" size="small" @input="handleTimeTypeChange">
        <el-radio-button v-for="tt in timeTypeList" :key="tt.value" :value="tt.value">{{$t(tt.label).toString()}}</el-radio-button>
      </el-radio-group>
      <el-tooltip class="item" effect="dark" :content="$t('export')" placement="right-end">
        <el-button link :icon="$legacyIcon('el-icon-download')" @click="handleExport"></el-button>
      </el-tooltip>
    </div>
    <h1 class="title">{{ $t(title) }}</h1>
    <div v-loading="loading" ref="defectStateChart" :class="className" :style="{height:height,width:width}" />
  </div>
</template>

<script>
import resize from "@/views/dashboard/mixins/resize";
import { defectLine } from "@/api/system/dashboard";
import { disconnectEchartsResize, initEchartsChart, observeEchartsResize, safeSetOption } from '@/components/Cat2BugStatistic/utils/echarts-resize';
import { computed } from 'vue';
import { storeToRefs } from 'pinia';
import { useSettingsStore } from '@/stores/settings';
import { useUserStore } from '@/stores/user';
import { buildDashboardAxisStyle, getDashboardChartTheme } from '@/utils/dashboard-chart-theme';

export default {
  name: "DefectStateChart",
  mixins: [resize],
  setup() {
    const settingsStore = useSettingsStore();
    const userStore = useUserStore();
    const { themeMode } = storeToRefs(settingsStore);
    const { config } = storeToRefs(userStore);
    const currentProjectId = computed(() => Number.parseInt(config.value?.currentProjectId, 10));
    return {
      themeMode,
      currentProjectId
    };
  },
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
      title: 'dashboard.defect-state.day',
      query: {
        timeType: 'day'
      }
    }
  },
  computed: {
    isDarkTheme() {
      return this.themeMode === 'dark';
    },
    /** 获取项目id */
    projectId() {
      return this.currentProjectId;
    },
  },
  watch: {
    themeMode() {
      this.getDefectLine();
    }
  },
  mounted() {
    this.initChart();
    this.getDefectLine();
  },
  beforeDestroy() {
    disconnectEchartsResize(this.chart);
    if (this.chart) {
      this.chart.dispose();
      this.chart = null;
    }
  },
  methods: {
    /** 导出按钮操作 */
    handleExport() {
      this.download('/system/dashboard/'+this.projectId+'/defect-line/export', {
        ...this.query
      }, `${ this.$i18n.t(this.title) }_${new Date().getTime()}.xlsx`)
    },
    getDefectLine() {
      this.loading = true;
      defectLine(this.projectId, this.query).then(res=>{
        this.loading = false;
        const series = [];
        res.data.types.forEach(k=>{
          if(!res.data.data[k]) return;
          series.push({
            name: this.$i18n.t(k),
              type: 'line',
            data: res.data.data[k]
          });
        })
        const types = res.data.types.map(t=>{
          return this.$i18n.t(t);
        });
        const times = res.data.times;
        this.setOptions(types, times, series);
      }).catch(e=>this.loading=false);
    },
    initChart() {
      this.chart = initEchartsChart(this.$refs.defectStateChart)
      this.setOptions([], [], [])
      observeEchartsResize(this.chart, this.$refs.defectStateChart)
    },
    setOptions(legendData, xData, series) {
      if (!this.chart) return
      const theme = getDashboardChartTheme(this.isDarkTheme);
      const axisStyle = buildDashboardAxisStyle(theme);
      safeSetOption(this.chart, {
          title: {
            text: ''
          },
          tooltip: {
            trigger: 'axis',
            backgroundColor: theme.tooltipBg,
            borderColor: theme.tooltipBorder,
            textStyle: {
              color: theme.tooltipText
            }
          },
          legend: {
            top: '35',
            left: '3%',
            right: '4%',
            type: 'scroll',
            width: 'auto',
            data: legendData,
            textStyle: {
              color: theme.legendText
            },
            pageTextStyle: {
              color: theme.legendText
            },
            inactiveColor: theme.legendInactive
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
            data: xData,
            ...axisStyle
          },
          yAxis: {
            type: 'value',
            ...axisStyle,
            splitLine: { lineStyle: { color: theme.splitColor } }
          },
          series: series
      }, true);
    },
    /** 处理时间类型变更 */
    handleTimeTypeChange(val) {
      switch (this.query.timeType) {
        case 'day':
          this.title = 'dashboard.defect-state.day';
          break;
        case 'month':
          this.title = 'dashboard.defect-state.month';
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
  width: 100%;
  box-sizing: border-box;

  .chart {
    width: 100% !important;
    min-width: 0;
  }

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
