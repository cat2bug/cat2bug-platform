<template>
  <div class="defect-state-chart" :style="{height:height,width:width}">
    <h1 v-if="!query.planId">{{ $t('dashboard.plan-burndown-chart.title') }}</h1>
    <el-select v-else v-model="query.planId" placeholder="" size="mini" class="planSelect" @change="handlePlanChange">
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
    <div v-loading="loading" ref="planBurndownChart" :class="className" :style="{height:height,width:width}" />
  </div>
</template>

<script>
// 测试计划燃尽图
import * as echarts from '@/assets/js/echarts.min.js';
import resize from "@/views/dashboard/mixins/resize";
import {dashboardPlanList, planBurndown} from "@/api/system/dashboard";
import { mapGetters } from 'vuex';

export default {
  name: "PlanBurndownChart",
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
      planList: [],
      query: {
        planId: null
      },
      lastXData: [],
      lastSeries: []
    }
  },
  computed: {
    ...mapGetters(['themeMode']),
    /** 获取项目id */
    projectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
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

        this.$nextTick(()=>{
          self.initChart();
        });
        this.handlePlanChange();
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
      this.chart = echarts.init(this.$refs.planBurndownChart, 'macarons')
      this.setOptions([],[])
    },
    setOptions(xData, series) {
      this.lastXData = xData || [];
      this.lastSeries = series || [];
      const dark = this.isDarkTheme;
      const axisColor = dark ? '#909399' : '#606266';
      const lineColor = dark ? '#363637' : '#dcdfe6';
      const splitColor = dark ? '#2b2b2c' : '#ebeef5';
      this.chart.setOption({
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
        grid: [{
          top: 10,
          left: 40,
          right: 16,
          bottom: 24,
          containLabel: true
        }],
        series: [
          {
            data: series,
            type: 'bar',
            itemStyle: {
              color: '#409eff'
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
.defect-state-chart {
  position: relative;

  > h1 {
    margin: 5px 0 0;
    font-size: 24px;
    color: var(--text-color-primary);
  }
}
.planSelect {
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
  ::v-deep .el-input,
  ::v-deep .el-input__inner {
    background-color: transparent !important;
    border-color: transparent !important;
    box-shadow: none !important;
  }
  ::v-deep input {
    width: 100%;
    height: 50px;
    line-height: 48px;
    border: 0 solid #DCDFE6;
    color: transparent !important;
    -webkit-text-fill-color: transparent !important;
    background-color: transparent !important;
  }
  ::v-deep .el-select__caret {
    height: 38px;
    color: var(--text-color-secondary);
  }
  ::v-deep .el-input__prefix {
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
