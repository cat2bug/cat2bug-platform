<template>
  <div class="app-container">
    <project-label />
    <div class="dashboard-row">
      <el-card class="box-card">
        <case-statistics-chart />
      </el-card>
      <el-card class="box-card">
        <defect-statistics-chart />
      </el-card>
      <el-card class="box-card">
        <module-statistics-chart />
      </el-card>
      <el-card class="box-card">
        <report-statistics-chart />
      </el-card>
      <el-card class="box-card">
        <document-statistics-chart />
      </el-card>
      <el-card class="box-card">
        <member-statistics-chart />
      </el-card>
    </div>
    <div class="dashboard-row">
      <el-card class="box-card">
        <defect-state-chart />
      </el-card>
      <el-card class="box-card">
        <member-of-defect-line />
      </el-card>
    </div>
    <div class="dashboard-row">
      <el-card class="box-card">
        <plan-statistics-chart />
      </el-card>
    </div>
    <div class="action-block dashboard-row">
      <el-card class="box-card">
        <action-list />
      </el-card>
      <div class="dashboard-col action-right">
        <el-card class="box-card">
          <member-of-defect-rank />
        </el-card>
        <el-card class="box-card">
          <plan-burndown-chart />
        </el-card>
      </div>
    </div>
  </div>
</template>

<script>
import ProjectLabel from "@/components/Project/ProjectLabel";
import DefectStateChart from "@/views/system/dashboard/components/DefectStateChart";
import CaseStatisticsChart from "@/views/system/dashboard/components/CaseStatisticsChart";
import DefectStatisticsChart from "@/views/system/dashboard/components/DefectStatisticsChart";
import ModuleStatisticsChart from "@/views/system/dashboard/components/ModuleStatisticsChart";
import ReportStatisticsChart from "@/views/system/dashboard/components/ReportStatisticsChart";
import DocumentStatisticsChart from "@/views/system/dashboard/components/DocumentStatisticsChart";
import MemberStatisticsChart from "@/views/system/dashboard/components/MemberStatisticsChart";
import ActionList from "@/views/system/dashboard/components/ActionList";
import MemberOfDefectRank from "@/views/system/dashboard/components/MemberOfDefectRank";
import PlanBurndownChart from "@/views/system/dashboard/components/PlanBurndownChart";
import MemberOfDefectLine from "@/views/system/dashboard/components/MemberOfDefectLine";
import PlanStatisticsChart from "@/views/system/dashboard/components/PlanStatisticsChart";
export default {
  name: "index",
  components: { ProjectLabel, DefectStateChart, CaseStatisticsChart, DefectStatisticsChart, ModuleStatisticsChart,
    ReportStatisticsChart, DocumentStatisticsChart, MemberStatisticsChart, ActionList, MemberOfDefectRank, PlanBurndownChart,
    MemberOfDefectLine, PlanStatisticsChart
  },
  data() {
    return {

    }
  },
  // 移除滚动条监听
  destroyed() {
    this.$floatMenu.windowsDestory();
  },
  mounted() {
    this.initFloatMenu();
  },
  methods: {
    /** 初始化浮动菜单 */
    initFloatMenu() {
      this.$floatMenu.windowsInit(document.querySelector('.main-container'));
      this.$floatMenu.resetMenus();
    },
  }
}
</script>

<style lang="scss" scoped>
@media screen and (max-width: 1400px) {
  .box-card {
    min-width: 320px;
  }
  .action-block {
    flex-direction: column-reverse !important;
    > *:first-child {
      width: 100%;
    }
    > .action-right {
      width: 100%;
      flex: 1;
    }
  }
}

@media screen and (min-width: 1400px) {
  .box-card {
    min-width: 100px;
  }
  .action-block {
    > .action-right {
      flex: 0 0 400px;
    }
  }
}
.dashboard-row {
  width: 100%;
  display: inline-flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 10px;
}
.dashboard-col {
  display: inline-flex;
  flex-direction: column;
  gap: 10px;
}
.dashboard-row > * {
  flex: 1;
}
.app-container > * {
  margin-top: 10px;
}
.app-container:first-child {
  margin-top: 0px;
}
.action-block {
  flex: 1;
  width: 100%;
  > *:first-child {
    flex: 1;
  }
}
.min-width-700 {
  min-width: 700px;
}
</style>
