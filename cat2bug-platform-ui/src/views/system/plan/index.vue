<template>
  <div class="app-container">
    <project-label />
    <div class="plan-tools">
      <el-form class="left" :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item label="" prop="planName">
          <el-input
            v-model="queryParams.planName"
            :placeholder="$t('plan.enter-name')"
            prefix-icon="el-icon-files"
            clearable
            size="small"
            @input="handleQuery"
          />
        </el-form-item>
        <el-form-item label="" prop="planVersion">
          <el-input
            v-model="queryParams.planVersion"
            :placeholder="$t('plan.enter-version')"
            prefix-icon="el-icon-discount"
            clearable
            size="small"
            @input="handleQuery"
          />
        </el-form-item>
      </el-form>
      <div class="plan-tools-right">
        <el-popover placement="top" trigger="click">
          <div class="row">
            <i class="el-icon-s-fold"></i>
            <h4>{{ $t('display-field') }}</h4>
          </div>
          <el-divider class="plan-field-divider"></el-divider>
          <el-checkbox-group v-model="columnPickerCheckedKeys" class="col" @change="onPlanColumnPickerChange">
            <el-checkbox v-for="c in planColumnPickerOptions" :key="c.key" :label="c.key">{{ $t(c.key) }}</el-checkbox>
          </el-checkbox-group>
          <el-button style="padding: 9px;" plain slot="reference" icon="el-icon-s-fold" size="mini"></el-button>
        </el-popover>
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

    <cat2-bug-table
      ref="cat2BugTable"
      cache-key="plan-table"
      field-list-cache-key="plan-table-field-list"
      :persist-sort="false"
      :columns="planTableColumnDefaults"
      :data="planList"
      :loading="loading"
      @columns-change="onPlanTableColumnsChange"
    >
      <template #columns="{ scope, column }">
        <span v-if="column.prop==='planNumber'">{{ planNumber(scope.row) }}</span>
        <div v-else-if="column.prop==='planStartTime'" class="col col-center" v-show="scope.row.planStartTime && scope.row.planEndTime">
          <span>{{$t('time-form')}}: {{ parseTime(scope.row.planStartTime, strFormat($t('year-month-day-hour-minute-second'),'{y}','{m}','{d}','{h}','{i}','{s}')) }}</span>
          <span>{{$t('time-to')}}: {{ parseTime(scope.row.planEndTime, strFormat($t('year-month-day-hour-minute-second'),'{y}','{m}','{d}','{h}','{i}','{s}')) }}</span>
        </div>
        <row-list-member v-else-if="column.prop==='updateById'" :members="member(scope.row)" />
        <span v-else-if="column.prop==='updateTime'">{{ parseTime(scope.row.updateTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        <div v-else-if="column.prop==='itemTotal'" class="plan-progress">
          <el-progress :percentage="planProcessValue(scope.row)" :format="planProcessContent"></el-progress>
          <span>{{scope.row.passCount}}/{{scope.row.itemTotal}}</span>
        </div>
        <el-tooltip v-else-if="column.prop==='remark'" class="item" effect="dark" :content="scope.row.remark" placement="top">
          <span class="text-row3">{{scope.row.remark}}</span>
        </el-tooltip>
        <span v-else>{{ scope.row[column.prop] }}</span>
      </template>
      <template #append>
        <el-table-column :label="$t('operate')" align="center" class-name="small-padding fixed-width no-drag" fixed="right" width="200">
          <template slot-scope="scope">
            <div class="table-operate">
              <el-button
                size="mini"
                type="text"
                icon="el-icon-plus"
                @click="handlePlanCopy(scope.row)"
                v-hasPermi="['system:plan:add']"
              >{{ $t('copy') }}</el-button>
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
            </div>
          </template>
        </el-table-column>
      </template>
    </cat2-bug-table>

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
import RowListMember from "@/components/RowListMember";
import {listPlan, delPlan, copyPlan} from "@/api/system/plan";
import AddPlanDialog from "@/components/Plan/AddPlanDialog";
import HandlePlanDialog from "@/components/Plan/HandlePlanDialog";
import DictOptionDialog from "@/components/DictOptionDialog";
import Cat2BugTable from "@/components/Cat2BugTable";
import { PlanTableColumnDefaults } from "@/views/system/plan/plan-table-options";
import {strFormat} from "@/utils";
import {checkPermi} from "@/utils/permission";

export default {
  name: "Plan",
  dicts: ['plan_item_state'],
  components:{ ProjectLabel, AddPlanDialog, HandlePlanDialog, DictOptionDialog, RowListMember, Cat2BugTable },
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
      planTableColumnDefaults: PlanTableColumnDefaults.map(c => ({ ...c })),
      columnPickerCheckedKeys: [],
    };
  },
  watch: {
    "$i18n.locale": function () {
      this.$nextTick(() => {
        this.$refs.cat2BugTable && this.$refs.cat2BugTable.doLayout();
      });
    },
  },
  computed: {
    planColumnPickerOptions() {
      return PlanTableColumnDefaults.filter(c => c.showInColumnPicker !== false);
    },
    /** 用于显示的用例编号 */
    planNumber: function () {
      return function (val) {
        return (val && val.planNumber)?'#'+val.planNumber:'';
      }
    },
    /** 项目ID */
    projectId: function () {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    /** 成员结构 */
    member: function () {
      return function (plan) {
        return [{
          nickName: plan.updateBy,
          avatar: plan.updateByAvatar
        }]
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
          return parseInt(plan.passCount / plan.itemTotal * 100);
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
    const actionPlanId = this.$route.query.planId;
    if(actionPlanId && checkPermi(['system:plan:run'])) {
      this.handlePlanRun({planId:actionPlanId})
    }
  },
  destroyed() {
    // 移除滚动条监听
    this.$floatMenu.windowsDestory();
  },
  methods: {
    checkPermi,
    strFormat,
    onPlanTableColumnsChange(columns) {
      this.columnPickerCheckedKeys = columns.filter(c => c.visible && c.showInColumnPicker !== false).map(c => c.key);
    },
    onPlanColumnPickerChange(keys) {
      this.$refs.cat2BugTable && this.$refs.cat2BugTable.setColumnsVisible(keys);
    },
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
      this.queryParams.params.dataType='simple';
      this.queryParams.projectId = this.projectId;
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
    /** 添加计划 */
    handleAdd() {
      this.$refs.planDialog.openAdd();
    },
    /** 更新计划 */
    handleUpdate(plan) {
      this.$refs.planDialog.openUpdate(plan);
    },
    /** 复制计划 */
    handlePlanCopy(plan) {
      copyPlan(plan.planId).then(res=>{
        this.$modal.msgSuccess(this.$i18n.t('copy.success'));
        this.getList();
      });
    },
    /** 执行计划 */
    handlePlanRun(plan) {
      this.$refs.handlePlanDialog.open(plan.planId);
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      this.$modal.confirm(strFormat(this.$i18n.t('plan.delete-prompt'), row.planName)).then(function() {
        return delPlan(row.planId);
      }).then(() => {
        this.$modal.msgSuccess(this.$i18n.t('delete.success'));
        this.getList();
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
@media screen and (max-width: 980px) {
  .plan-tools > .left {
    display: none;
  }
}
@media screen and (min-width: 980px) {
  .plan-tools > .left {
    display: inline-flex;
  }
}
/* 与测试用例页 .case-tools 一致：项目切换与工具条上下间距 */
.plan-tools {
  width: 100%;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  margin-bottom: 10px;
  .el-form-item {
    margin-bottom: 0;
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
.row {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
  > * {
    margin: 0px;
  }
}
.col {
  display: inline-flex;
  flex-direction: column;
  justify-content: center;
}
.col-center {
  align-items: flex-start;
}
.table-operate {
  padding-left: 10px;
  display: inline-flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
  flex-wrap: wrap;
}
.plan-field-divider {
  margin: 8px 0px;
}
.plan-tools-right {
  display: inline-flex;
  flex-direction: row;
  width: 100%;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
}
.text-row3 {
  word-break: break-all;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
