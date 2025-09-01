<template>
  <div v-loading="loading" class="defect-state-chart" :style="{width:width}">
    <h1 v-if="!query.planId">{{ $t('dashboard.plan-statistics.title') }}</h1>
    <div v-else class="chart-tools">
      <el-select v-model="query.planId" placeholder="" size="mini" class="planSelect" @change="handlePlanChange">
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
      <el-tooltip class="item" effect="dark" :content="$t('export')" placement="right-end">
        <el-button type="text" icon="el-icon-download" @click="handleExport"></el-button>
      </el-tooltip>
    </div>
    <div class="plan-statistics" @click="handlePlanClick">
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
      <!--          缺陷总数-->
      <defect-total :plan="selectPlan"></defect-total>
      <!--          缺陷发现率-->
      <defect-discovery-rate :plan="selectPlan"></defect-discovery-rate>
      <!--          缺陷修复率-->
      <defect-repair-rate :plan="selectPlan"></defect-repair-rate>
      <!--          缺陷探测率-->
      <defect-detection-rate :plan="selectPlan"></defect-detection-rate>
      <!--          缺陷严重率-->
      <defect-severity-rate :plan="selectPlan"></defect-severity-rate>
      <!--          缺陷重开率-->
      <defect-restart-rate :plan="selectPlan"></defect-restart-rate>
      <!--          缺陷逃逸率-->
      <defect-escape-rate :plan="selectPlan"></defect-escape-rate>
      <!--          缺陷密度-->
      <defect-density :plan="selectPlan"></defect-density>
      <!--          缺陷平均时长-->
      <defect-repair-avg-hour :plan="selectPlan"></defect-repair-avg-hour>
    </div>
  </div>
</template>

<script>
// 用例统计
import DefectDiscoveryRate from "@/components/Plan/statistics/DefectDiscoveryRate";
import DefectTotal from "@/components/Plan/statistics/DefectTotal";
import DefectRepairRate from "@/components/Plan/statistics/DefectRepairRate";
import DefectDensity from "@/components/Plan/statistics/DefectDensity";
import DefectDetectionRate from "@/components/Plan/statistics/DefectDetectionRate";
import DefectSeverityRate from "@/components/Plan/statistics/DefectSeverityRate";
import DefectRestartRate from "@/components/Plan/statistics/DefectRestartRate";
import DefectEscapeRate from "@/components/Plan/statistics/DefectEscapeRate";
import DefectRepairAvgHour from "@/components/Plan/statistics/DefectRepairAvgHour";
import {getPlan, listPlan} from "@/api/system/plan";
import Cat2BugLevel from "@/components/Cat2BugLevel";
import Step from "@/views/system/case/components/step";
import TreePlanItemModule from "@/views/system/plan/TreePlanItemModule";
import {Multipane, MultipaneResizer} from "vue-multipane";
import FocusMemberList from "@/components/FocusMemberList";
import Cat2BugPreviewImage from "@/components/Cat2BugPreviewImage";
import HandleCaseOfPlan from "@/components/Plan/HandleCaseOfPlan";
import RowListMember from "@/components/RowListMember";
import Cat2BugText from "@/components/Cat2BugText";
import CaseList from "@/components/Plan/HandlePlanDialog/CaseList";
import DefectList from "@/components/Plan/HandlePlanDialog/DefectList";

export default {
  name: "PlanStatisticsChart",
  components: {
    DefectDiscoveryRate, DefectTotal, DefectRepairRate, DefectDensity, DefectDetectionRate, DefectSeverityRate, DefectRestartRate, DefectEscapeRate, DefectRepairAvgHour
  },
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
    /** 导出按钮操作 */
    handleExport() {
      this.download('/system/dashboard/'+this.projectId+'/plan-statistics/export', {}, `${ this.$i18n.t('dashboard.plan-statistics.title') }_${new Date().getTime()}.xlsx`)
    },
    handlePlanClick() {
      const targetRoute = this.$router.resolve({ path:'/project/plan', query: {planId:this.query.planId}});
      window.open(targetRoute.href, '_blank');
    },
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
.defect-state-chart:hover {
  cursor: pointer;
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
.chart-tools {
  width: 100%;
  padding-right: 10px;
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
  ::v-deep .el-input__suffix {
    display: inline-flex;
    align-items: center;
  }
}
</style>
