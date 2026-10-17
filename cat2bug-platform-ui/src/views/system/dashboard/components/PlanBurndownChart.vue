<template>
  <div class="defect-state-chart plan-burndown-chart" :style="{ height, width }">
    <h1 v-if="!query.planId">{{ $t('dashboard.plan-burndown-chart.title') }}</h1>
    <el-select v-else v-model="query.planId" placeholder="" size="small" class="planSelect" @change="handlePlanChange">
      <template #prefix>
        <div class="prefix">
          <h1>{{ $t('dashboard.plan-burndown-chart.title') }}</h1>
          <span>[ {{ selectPlanName(query.planId) }} ]</span>
        </div>
      </template>
      <el-option
        v-for="item in planList"
        :key="item.planId"
        :label="item.planName"
        :value="item.planId">
      </el-option>
    </el-select>
    <div
      v-loading="loading"
      ref="planBurndownChart"
      :class="[className, 'plan-burndown-chart__canvas']"
    />
  </div>
</template>

<script>
// 测试计划燃尽图
import resize from "@/views/dashboard/mixins/resize";
import {dashboardPlanList, planBurndown} from "@/api/system/dashboard";
import { disconnectEchartsResize, initEchartsChart, observeEchartsResize, safeSetOption } from '@/components/Cat2BugStatistic/utils/echarts-resize';
import { computed } from 'vue';
import { storeToRefs } from 'pinia';
import { useSettingsStore } from '@/stores/settings';
import { useUserStore } from '@/stores/user';

export default {
  name: "PlanBurndownChart",
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
      planList: [],
      query: {
        planId: null
      },
      lastXData: [],
      lastSeries: []
    }
  },
  computed: {
    /** 获取项目id */
    projectId() {
      return this.currentProjectId;
    },
    isDarkTheme() {
      return this.themeMode === 'dark';
    },
    selectPlanName: function () {
      return function (planId) {
        return this.planList.find(p=>p.planId==planId)?.planName;
      }
    }
  },
  watch: {
    themeMode() {
      if (this.chart) {
        this.setOptions(this.lastXData, this.lastSeries);
      }
    }
  },
  mounted() {
    this.getPlanList();
  },
  beforeDestroy() {
    disconnectEchartsResize(this.chart);
    if (this.chart) {
      this.chart.dispose();
      this.chart = null;
    }
  },
  methods: {
    getPlanList() {
      this.loading = true;
      const self = this;
      let query = {
        pageNum: 1,
        pageSize: 99,
        projectId: this.projectId
      }
      dashboardPlanList(this.projectId, query).then(res=>{
        this.planList = res.rows;
        if(this.planList && this.planList.length>0) {
          this.query.planId = this.planList[0].planId;
        } else {
          this.loading = false;
        }

        this.$nextTick(() => {
          self.initChart();
          if (self.query.planId) {
            self.handlePlanChange();
          }
        });
      }).catch(e=>{
        this.loading = false;
      })
    },
    handlePlanChange() {
      this.loading = true;
      planBurndown(this.projectId, this.query.planId).then(res=>{
        this.loading = false;
        let xData= [], series=[];
        res.data.forEach(p=>{
          xData.push(p.key);
          series.push(p.value);
        });
        this.setOptions(xData, series);
      }).catch(e=>this.loading = false);
    },
    initChart() {
      this.chart = initEchartsChart(this.$refs.planBurndownChart)
      this.setOptions([], [])
      observeEchartsResize(this.chart, this.$refs.planBurndownChart)
    },
    setOptions(xData, series) {
      if (!this.chart) return
      this.lastXData = xData || [];
      this.lastSeries = series || [];
      const dark = this.isDarkTheme;
      const axisColor = dark ? '#909399' : '#606266';
      const lineColor = dark ? '#363637' : '#dcdfe6';
      const splitColor = dark ? '#2b2b2c' : '#ebeef5';
      safeSetOption(this.chart, {
        tooltip: {
          trigger: 'axis',
          backgroundColor: dark ? '#252529' : '#ffffff',
          borderColor: dark ? '#363637' : '#ebeef5',
          textStyle: {
            color: dark ? '#e5eaf3' : '#303133'
          }
        },
        xAxis: {
          type: 'category',
          data: xData,
          axisLabel: { color: axisColor },
          axisLine: { lineStyle: { color: lineColor } },
          axisTick: { lineStyle: { color: lineColor } }
        },
        yAxis: {
          type: 'value',
          axisLabel: { color: axisColor },
          axisLine: { lineStyle: { color: lineColor } },
          splitLine: { lineStyle: { color: splitColor } }
        },
        grid: {
          top: 10,
          left: 40,
          right: 16,
          bottom: 24,
          containLabel: true
        },
        series: [
          {
            data: series,
            type: 'bar',
            itemStyle: {
              color: '#1890ff'
            },
            showBackground: true,
            backgroundStyle: {
              color: dark ? 'rgba(255, 255, 255, 0.06)' : 'rgba(180, 180, 180, 0.2)'
            }
          }
        ]
      })
    },
  }
}
</script>

<style lang="scss" scoped>
.defect-state-chart.plan-burndown-chart {
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-sizing: border-box;

  > h1 {
    margin: 5px 0 0;
    font-size: 24px;
    color: var(--text-color-primary);
    flex-shrink: 0;
  }

  .plan-burndown-chart__canvas {
    flex: 1 1 auto;
    min-height: 0;
    width: 100% !important;
    height: auto !important;
    overflow: hidden;
  }
}
.planSelect {
  flex-shrink: 0;
  width: 100%;
  .prefix {
    display: inline-flex;
    flex-direction: row;
    align-items: center;
    gap: 15px;
    > *:first-child {
      flex-shrink: 0;
    }
    > *:last-child {
      flex-shrink: 0;
      font-size: 1rem;
    }
  }
  :deep(.el-select__wrapper){
    background-color: transparent !important;
    border: none !important;
    box-shadow: none !important;
    height: 50px !important;
    min-height: 50px !important;
    padding-left: 0 !important;
    padding-right: 24px !important;
  }
  :deep(.el-select__wrapper:hover),
  :deep(.el-select__wrapper.is-focused),
  :deep(.el-select__wrapper.is-hovering){
    border: none !important;
    box-shadow: none !important;
  }
  :deep(.el-select__selection),
  :deep(.el-select__selected-item),
  :deep(.el-select__placeholder){
    opacity: 0 !important;
  }
  :deep(.el-input),
  :deep(.el-input__wrapper),
  :deep(.el-input__inner){
    background-color: transparent !important;
    border-color: transparent !important;
    box-shadow: none !important;
  }
  :deep(input){
    width: 100%;
    height: 50px;
    line-height: 48px;
    border: 0 solid #DCDFE6;
    color: transparent !important;
    -webkit-text-fill-color: transparent !important;
    background-color: transparent !important;
  }
  :deep(.el-select__caret),
  :deep(.el-select__suffix){
    color: var(--text-color-secondary);
  }
  :deep(.el-input__prefix),
  :deep(.el-select__prefix){
    width: 100%;
    display: inline-flex;
    flex-direction: row;
    justify-content: flex-start;
    pointer-events: none;
    .prefix {
      h1 {
        margin: 5px 0;
        font-size: 24px;
        color: var(--text-color-primary);
      }
      span {
        color: var(--text-color-secondary);
      }
    }
  }
}
</style>
