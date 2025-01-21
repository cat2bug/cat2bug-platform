<template>
  <div class="defect-state-chart" :style="{height:height,width:width}">
    <h1 v-if="!query.planId">{{ $t('dashboard.plan-burndown-chart.title') }}</h1>
    <el-select v-else v-model="query.planId" placeholder="" size="mini" class="planSelect" @change="handlePlanChange">
      <template #prefix>
        <h1>{{ $t('dashboard.plan-burndown-chart.title') }}</h1>
        <span>{{ selectPlanName(query.planId) }}</span>
      </template>
      <el-option
        v-for="item in planList"
        :key="item.planId"
        :label="item.planName"
        :value="item.planId">
      </el-option>
    </el-select>
    <el-empty v-if="!query.planId" description=""></el-empty>
    <div v-else v-loading="loading" ref="planBurndownChart" :class="className" :style="{height:height,width:width}" />
  </div>
</template>

<script>
// 测试计划燃尽图
import * as echarts from 'echarts';
require('echarts/theme/macarons') // echarts theme
import resize from "@/views/dashboard/mixins/resize";
import {defectLine, planBurndown} from "@/api/system/dashboard";
import {listPlan} from "@/api/system/plan";

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
      }
    }
  },
  computed: {
    /** 获取项目id */
    projectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    selectPlanName: function () {
      return function (planId) {
        return this.planList.find(p=>p.planId==planId)?.planName;
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
      listPlan(query).then(res=>{
        this.planList = res.rows;
        if(this.planList && this.planList.length>0) {
          this.query.planId = this.planList[0].planId;
          this.$nextTick(()=>{
            self.initChart();
          });
          this.handlePlanChange();
        } else {
          this.loading = false;
        }
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
      this.chart.setOption({
        xAxis: {
          type: 'category',
          data: xData
        },
        yAxis: {
          type: 'value'
        },
        grid: [{
          top: 10,
        }],
        series: [
          {
            data: series,
            type: 'bar',
            showBackground: true,
            backgroundStyle: {
              color: 'rgba(180, 180, 180, 0.2)'
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
}
.planSelect {
  width: 100%;
  ::v-deep input {
    height: 60px;
    line-height: 58px;
    border: 0px solid #DCDFE6;
    color: #0000;
  }
  ::v-deep .el-select__caret {
    height: 38px;
  }
  ::v-deep .el-input__prefix {
    width: 100%;
    display: inline-flex;
    flex-direction: column;
    justify-content: flex-start;
    >* {
      width: 100%;
      color: rgb(48, 49, 51);
      text-align: start;
    }
    h1 {
      margin: 5px 0px;
      font-size: 24px;
    }
  }
}
</style>
