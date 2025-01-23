<template>
  <div v-loading="loading" class="defect-state-chart" :style="{width:width}">
    <h1 v-if="!query.planId">{{ $t('dashboard.plan-statistics.title') }}</h1>
    <el-select v-else v-model="query.planId" placeholder="" size="mini" class="planSelect" @change="handlePlanChange">
      <template #prefix>
        <div class="prefix">
          <h1>{{ $t('dashboard.plan-statistics.title') }}</h1>
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
    <div class="plan-statistics">
      <el-statistic
        group-separator=",">
        <template slot="title">
          <span>{{$t('case')}}: </span>
          <span class="click" style="color: rgb(19, 206, 102);">{{$t('case.pass-tested')}}</span>
          <span>/</span>
          <span class="click" style="color: #f56c6c;">{{$t('case.failed-tested')}}</span>
          <span>/</span>
          <span class="click" style="color: #909399;">{{$t('unexecuted')}}</span>
          <span>/</span>
          <span class="click" style="font-weight: 500;">{{$t('total')}}</span>
        </template>
        <template slot="formatter">
          <span class="click" style="color: rgb(19, 206, 102);">{{selectPlan.passCount||0}}</span>
          <span>/</span>
          <span class="click" style="color: #f56c6c;">{{selectPlan.failCount||0}}</span>
          <span>/</span>
          <span class="click" style="color: #909399;">{{selectPlan.unexecutedCount||0}}</span>
          <span>/</span>
          <span class="click" style="font-weight: 500;">{{selectPlan.itemTotal||0}}{{ $t('a') }}</span>
        </template>
      </el-statistic>
      <el-statistic
        :title="$t('plan.defect-count')"
      >
        <template slot="formatter">
          {{`${selectPlan.defectCount||0}`}}{{ $t('a') }}
        </template>
      </el-statistic>
      <el-statistic
        group-separator=","
        :title="$t('plan.defect-discovery-rate')"
      >
        <template slot="formatter">
          {{`${selectPlan.defectDiscoveryRate||0}`}}
        </template>
      </el-statistic>
      <el-statistic
        group-separator=","
        :title="$t('plan.defect-repair-rate')"
      >
        <template slot="formatter">
          {{`${selectPlan.defectRepairRate||0}`}}
        </template>
      </el-statistic>
      <el-statistic
        group-separator=","
        :title="$t('plan.defect-density')"
      >
        <template slot="formatter">
          {{`${selectPlan.defectDensity||0}`}}
        </template>
      </el-statistic>
      <el-statistic
        group-separator=","
        :title="$t('plan.defect-detection-rate')"
      >
        <template slot="formatter">
          {{`${selectPlan.defectDetectionRate||0}`}}
        </template>
      </el-statistic>
      <el-statistic
        group-separator=","
        :title="$t('plan.defect-severity-rate')"
      >
        <template slot="formatter">
          {{`${selectPlan.defectSeverityRate||0}`}}
        </template>
      </el-statistic>
      <el-statistic
        group-separator=","
        :title="$t('plan.defect-restart-rate')"
      >
        <template slot="formatter">
          {{`${selectPlan.defectRestartRate||0}`}}
        </template>
      </el-statistic>
      <el-statistic
        group-separator=","
        :title="$t('plan.defect-escape-rate')"
      >
        <template slot="formatter">
          {{`${selectPlan.defectEscapeRate||0}`}}
        </template>
      </el-statistic>
      <el-statistic
        group-separator=","
        :title="$t('plan.defect-repair-avg-hour')"
      >
        <template slot="formatter">
          {{`${selectPlan.defectRepairAvgHour||0}`}}h
        </template>
      </el-statistic>
    </div>
  </div>
</template>

<script>
// 用例统计

import {caseStatistics, planBurndown} from "@/api/system/dashboard";
import {getPlan, listPlan} from "@/api/system/plan";

export default {
  name: "PlanStatisticsChart",
  props: {
    width: {
      type: String,
      default: '100%'
    },
    height: {
      type: String,
      default: '100px'
    },
  },
  data() {
    return {
      loading: false,
      planList:[],
      selectPlan: {},
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
    /** 获取计划信息 */
    getPlanInfo(planId) {
      this.loading = true;
      getPlan(planId).then(response => {
        this.loading = false;
        this.selectPlan = response.data;
      }).catch(e=>this.loading = false);
    },
    handlePlanChange() {
      this.getPlanInfo(this.query.planId);
    },
    getPlanList() {
      this.loading = true;
      let query = {
        pageNum: 1,
        pageSize: 99,
        projectId: this.projectId
      }
      listPlan(query).then(res=>{
        this.planList = res.rows;
        if(this.planList && this.planList.length>0) {
          this.query.planId = this.planList[0].planId;
          this.handlePlanChange();
        } else {
          this.loading = false;
        }
      }).catch(e=>{
        this.loading = false;
      })
    },
  }
}
</script>

<style lang="scss" scoped>
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
  ::v-deep input {
    width: 100%;
    height: 50px;
    line-height: 48px;
    border: 0px solid #DCDFE6;
    color: #0000;
  }
  ::v-deep .el-select__caret {
    height: 38px;
  }
  ::v-deep .el-input__prefix {
    width: 100%;
    display: inline-flex;
    flex-direction: row;
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
.plan-statistics {
  width: 100%;
  display: inline-flex;
  flex-direction: row;
  flex-wrap: wrap;
  >* {
    flex: 1;
    min-width: 100px;
    border-right: 1px solid #E4E7ED;
    margin-bottom: 10px;
    padding: 0 5px;
  }
  >*:first-child {
    flex:0 0 350px;
  }
  >*:last-child {
    border-right-width: 0px;
  }
}
//@media screen and (min-width: 1650px) {
//  .plan-statistics {
//    > *:first-child {
//      min-width: 210px;
//      flex: 1
//    }
//  }
//}
//
//@media screen and (max-width: 1650px) {
//  .plan-statistics {
//    > * {
//      width: calc((100% - 50px) / 6);
//    }
//    > *:first-child, > *:nth-child(2) {
//      width: calc((100% - 50px) / 6 * 1.5 + 5px);
//    }
//  }
//}
//
//@media screen and (max-width: 780px) {
//  .plan-statistics {
//    > * {
//      width: calc((100% - 30px) / 4);
//    }
//    > *:first-child, > *:nth-child(2) {
//      width: calc((100% - 30px) / 4 * 1.5 + 5px);
//    }
//  }
//}
</style>
