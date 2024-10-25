<template>
  <div class="app-container">
    <project-label />
    <div class="plan-tools">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item label="" prop="planName">
          <el-input
            v-model="queryParams.planName"
            :placeholder="$t('plan.enter-name')"
            prefix-icon="el-icon-files"
            clearable
            size="small"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="" prop="planVersion">
          <el-input
            v-model="queryParams.planVersion"
            :placeholder="$t('plan.enter-version')"
            prefix-icon="el-icon-discount"
            clearable
            size="small"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
      </el-form>
      <div class="right">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          @click="handleAdd"
          size="small"
          v-hasPermi="['system:plan:add']"
        >{{ $t('plan.create') }}</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="planList">
<!--      <el-table-column type="selection" width="55" align="center" />-->
      <el-table-column :label="$t('plan.name')" align="start" prop="planName" />
      <el-table-column :label="$t('plan.version')" align="center" prop="planVersion" width="200"/>
      <el-table-column :label="$t('plan.time')" align="center" prop="planStartTime" width="260">
        <template slot-scope="scope">
          <div class="col" v-show="scope.row.planStartTime && scope.row.planEndTime">
            <span>{{$t('time-form')}}: {{ parseTime(scope.row.planStartTime, strFormat($t('year-month-day-hour-minute-second'),'{y}','{m}','{d}','{h}','{i}','{s}')) }}</span>
            <span>{{$t('time-to')}}: {{ parseTime(scope.row.planEndTime, strFormat($t('year-month-day-hour-minute-second'),'{y}','{m}','{d}','{h}','{i}','{s}')) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column :label="$t('updateBy')" align="center" prop="updateById" width="120">
        <template slot-scope="scope">
          <cat2-bug-avatar :member="member(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column :label="$t('updateTime')" align="center" prop="updateTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('plan.process')" align="center" width="150">
        <template slot-scope="scope">
          <div class="plan-progress">
            <el-progress :percentage="planProcessValue(scope.row)" :format="planProcessContent"></el-progress>
            <span>{{(scope.row.passCount+scope.row.failCount)}}/{{scope.row.itemTotal}}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column :label="$t('operate')" align="center" class-name="small-padding fixed-width" width="200">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-tickets"
            @click="handlePlanRun(scope.row)"
            v-hasPermi="['system:plan:run']"
          >{{ $t('plan.run') }}</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:plan:edit']"
          >{{ $t('modify') }}</el-button>
          <el-button
            size="mini"
            type="text"
            class="red"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:plan:remove']"
          >{{ $t('delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />
    <add-plan-dialog ref="planDialog" @add="getList" @update="getList" @close="initFloatMenu" />
    <handle-plan-dialog ref="handlePlanDialog" @change="getList" @close="initFloatMenu" />
    <dict-option-dialog ref="planItemState" title="测试状态管理" :dictType="dict.type.plan_item_state" />
  </div>
</template>

<script>
import ProjectLabel from "@/components/Project/ProjectLabel";
import Cat2BugAvatar from "@/components/Cat2BugAvatar";
import { listPlan, delPlan } from "@/api/system/plan";
import AddPlanDialog from "@/views/system/plan/AddPlanDialog";
import HandlePlanDialog from "@/views/system/plan/HandlePlanDialog";
import DictOptionDialog from "@/components/DictOptionDialog";
import {strFormat} from "@/utils";

export default {
  name: "Plan",
  dicts: ['plan_item_state'],
  components:{ ProjectLabel, AddPlanDialog, HandlePlanDialog, DictOptionDialog, Cat2BugAvatar },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 子表选中数据
      checkedSysPlanItem: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 测试计划表格数据
      planList: [],
      // 缺陷时间范围
      daterangePlanStartTime: [],
      // 缺陷时间范围
      daterangePlanEndTime: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        planName: null,
        planVersion: null,
        planStartTime: null,
        planEndTime: null,
        createById: null,
        updateById: null,
        projectId: this.projectId,
        reportId: null
      },
    };
  },
  computed: {
    /** 项目ID */
    projectId: function () {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    /** 成员结构 */
    member: function () {
      return function (plan) {
        return {
          nickName: plan.updateBy
        }
      }
    },
    /** 计划进度显示的内容 */
    planProcessContent: function () {
      return function (percentage) {
        return '';
      }
    },
    /** 计划进度 */
    planProcessValue: function () {
      return function (plan) {
        if(plan.itemTotal>0) {
          return parseInt((plan.passCount+plan.failCount) / plan.itemTotal * 100);
        } else {
          return 0;
        }
      }
    },
  },
  created() {
    this.getList();
  },
  mounted() {
    this.initFloatMenu();
  },
  destroyed() {
    // 移除滚动条监听
    this.$floatMenu.windowsDestory();
  },
  methods: {
    strFormat,
    /** 初始化浮动菜单 */
    initFloatMenu() {
      this.$floatMenu.windowsInit(document.querySelector('.main-container'));
      this.$floatMenu.resetMenus([{
        id: 'addPlan',
        name: 'plan.create',
        visible: true,
        plain: true,
        type: 'primary',
        icon: 'add-tab',
        prompt: 'plan.create',
        permissions: ['system:plan:add'],
        click : this.handleAdd
      }]);
    },
    /** 查询测试计划列表 */
    getList() {
      this.loading = true;
      this.queryParams.params = {};
      this.queryParams.projectId = this.projectId
      if (null != this.daterangePlanStartTime && '' != this.daterangePlanStartTime) {
        this.queryParams.params["beginPlanStartTime"] = this.daterangePlanStartTime[0];
        this.queryParams.params["endPlanStartTime"] = this.daterangePlanStartTime[1];
      }
      if (null != this.daterangePlanEndTime && '' != this.daterangePlanEndTime) {
        this.queryParams.params["beginPlanEndTime"] = this.daterangePlanEndTime[0];
        this.queryParams.params["endPlanEndTime"] = this.daterangePlanEndTime[1];
      }
      listPlan(this.queryParams).then(response => {
        this.planList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.daterangePlanStartTime = [];
      this.daterangePlanEndTime = [];
      this.resetForm("queryForm");
      this.handleQuery();
    },
    handleAdd() {
      this.$refs.planDialog.openAdd();
    },
    handleUpdate(plan) {
      this.$refs.planDialog.openUpdate(plan);
    },
    handlePlanRun(plan) {
      this.$refs.handlePlanDialog.open(plan.planId);
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      this.$modal.confirm(strFormat(this.$i18n.t('plan.delete-prompt'), row.planName)).then(function() {
        return delPlan(row.planId);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess(this.$i18n.t('delete.success'));
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/plan/export', {
        ...this.queryParams
      }, `plan_${new Date().getTime()}.xlsx`)
    },
    /** 打开配置对话框操作 */
    handleOption() {
      this.$refs.planItemState.open();
    }
  }
};
</script>
<style scoped lang="scss">
.plan-tools {
  width: 100%;
  display: inline-flex;
  flex-direction: row;
  align-items: flex-start;
  justify-content: space-between;
  .el-form-item {
    margin-bottom: 10px;
  }
}
.plan-progress {
  width: 100%;
  display: inline-flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  > .el-progress {
    flex: 1;
    ::v-deep .el-progress-bar {
      display: inline-flex;
      margin-right: 0px;
      padding-right: 5px;
      > .el-progress-bar__outer {
        width: 100%;
      }
    }
    ::v-deep .el-progress__text {
      display: none;
    }
  }
}
.col {
  display: inline-flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}
</style>
