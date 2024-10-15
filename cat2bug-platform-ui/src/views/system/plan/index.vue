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
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="" prop="planVersion">
          <el-input
            v-model="queryParams.planVersion"
            :placeholder="$t('plan.enter-version')"
            prefix-icon="el-icon-discount"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
      </el-form>
      <div class="right">
<!--        <el-button size="mini" icon="el-icon-setting" circle @click="handleOption"></el-button>-->
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['system:plan:add']"
        >{{ $t('plan.create') }}</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="planList">
<!--      <el-table-column type="selection" width="55" align="center" />-->
      <el-table-column :label="$t('plan.name')" align="center" prop="planName" />
      <el-table-column :label="$t('plan.version')" align="center" prop="planVersion" />
      <el-table-column :label="$t('plan.start-time')" align="center" prop="planStartTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.planStartTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('plan.end-time')" align="center" prop="planEndTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.planEndTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('updateBy')" align="center" prop="updateById" />
      <el-table-column :label="$t('updateTime')" align="center" prop="updateTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('operate')" align="center" class-name="small-padding fixed-width">
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
    <add-plan-dialog ref="planDialog" @add="getList" @update="getList" />
    <handle-plan-dialog ref="handlePlanDialog" />
    <dict-option-dialog ref="planItemState" title="测试状态管理" :dictType="dict.type.plan_item_state" />
  </div>
</template>

<script>
import ProjectLabel from "@/components/Project/ProjectLabel";
import { listPlan, delPlan } from "@/api/system/plan";
import AddPlanDialog from "@/views/system/plan/AddPlanDialog";
import HandlePlanDialog from "@/views/system/plan/HandlePlanDialog";
import DictOptionDialog from "@/components/DictOptionDialog";

export default {
  name: "Plan",
  dicts: ['plan_item_state'],
  components:{ ProjectLabel, AddPlanDialog, HandlePlanDialog, DictOptionDialog },
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
        projectId: null,
        reportId: null
      },
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询测试计划列表 */
    getList() {
      this.loading = true;
      this.queryParams.params = {};
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
      const planIds = row.planId || this.ids;
      this.$modal.confirm('是否确认删除测试计划编号为"' + planIds + '"的数据项？').then(function() {
        return delPlan(planIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
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
  align-items: center;
  justify-content: space-between;
}
</style>
