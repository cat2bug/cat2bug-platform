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
      <div class="plan-tools-right">
        <el-popover
          placement="top"
          trigger="click">
          <div class="row">
            <i class="el-icon-s-fold"></i>
            <h4>{{$t('display-field')}}</h4>
          </div>
          <el-divider class="plan-field-divider"></el-divider>
          <el-checkbox-group v-model="tableShowFieldList" class="col" @change="checkedFieldListChange">
            <el-checkbox v-for="field in tableAllFieldList" :label="field" :key="field">{{$t(field)}}</el-checkbox>
          </el-checkbox-group>
          <el-button
            style="padding: 9px;"
            plain
            slot="reference"
            icon="el-icon-s-fold"
            size="mini"
          ></el-button>
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

    <el-table ref="table" v-loading="loading" :data="planList">
<!--      <el-table-column type="selection" width="55" align="center" />-->
      <el-table-column v-if="showField('id')" :label="$t('id')" :key="$t('id')" align="center" prop="planNumber" width="80" sortable fixed>
        <template slot-scope="scope">
          <span>{{ planNumber(scope.row) }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="showField('plan.name')" :label="$t('plan.name')" :key="$t('plan.name')" align="start" prop="planName" min-width="150" fixed />
      <el-table-column v-if="showField('plan.version')" :label="$t('plan.version')" :key="$t('plan.version')" align="center" prop="planVersion" width="100"/>
      <el-table-column v-if="showField('plan.time')" :label="$t('plan.time')" :key="$t('plan.time')" align="center" prop="planStartTime" width="260">
        <template slot-scope="scope">
          <div class="col col-center" v-show="scope.row.planStartTime && scope.row.planEndTime">
            <span>{{$t('time-form')}}: {{ parseTime(scope.row.planStartTime, strFormat($t('year-month-day-hour-minute-second'),'{y}','{m}','{d}','{h}','{i}','{s}')) }}</span>
            <span>{{$t('time-to')}}: {{ parseTime(scope.row.planEndTime, strFormat($t('year-month-day-hour-minute-second'),'{y}','{m}','{d}','{h}','{i}','{s}')) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column v-if="showField('updateBy')" :label="$t('updateBy')" :key="$t('updateBy')" align="center" prop="updateById" width="120">
        <template slot-scope="scope">
          <row-list-member :members="member(scope.row)"></row-list-member>
        </template>
      </el-table-column>
      <el-table-column v-if="showField('updateTime')" :label="$t('updateTime')" :key="$t('updateTime')" align="center" prop="updateTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="showField('plan.process')" :label="$t('plan.process')" :key="$t('plan.process')" align="center" width="150">
        <template slot-scope="scope">
          <div class="plan-progress">
            <el-progress :percentage="planProcessValue(scope.row)" :format="planProcessContent"></el-progress>
            <span>{{scope.row.passCount}}/{{scope.row.itemTotal}}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column v-if="showField('remark')" :label="$t('remark')" :key="$t('remark')" align="center" prop="remark">
        <template slot-scope="scope">
          <el-tooltip class="item" effect="dark" :content="scope.row.remark" placement="top">
            <span class="text-row3">{{scope.row.remark}}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column :label="$t('operate')" align="left" class-name="small-padding fixed-width" fixed="right" width="200">
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
import RowListMember from "@/components/RowListMember";
import {listPlan, delPlan, copyPlan} from "@/api/system/plan";
import AddPlanDialog from "@/views/system/plan/AddPlanDialog";
import HandlePlanDialog from "@/views/system/plan/HandlePlanDialog";
import DictOptionDialog from "@/components/DictOptionDialog";
import {strFormat} from "@/utils";
import {checkPermi} from "@/utils/permission";

/** 需要显示的缺陷字段列表在缓存的key值 */
const PLAN_TABLE_FIELD_LIST_CACHE_KEY='plan-table-field-list';

export default {
  name: "Plan",
  dicts: ['plan_item_state'],
  components:{ ProjectLabel, AddPlanDialog, HandlePlanDialog, DictOptionDialog, RowListMember },
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
      // 选中的表格列数据集合
      tableShowFieldList: [],
      // 表格里全部列数据集合
      tableAllFieldList: [
        'id','plan.name','plan.version','plan.time','updateBy','updateTime','plan.process','remark'
      ],
    };
  },
  watch: {
    "$i18n.locale": function (newVal, oldVal) {
      this.$refs.table.doLayout();
    },
  },
  computed: {
    /** 字段是否显示 */
    showField: function () {
      return function (field) {
        return this.tableShowFieldList.filter(f=>f==field).length>0;
      }
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
    this.refreshShowFields();
  },
  destroyed() {
    // 移除滚动条监听
    this.$floatMenu.windowsDestory();
  },
  methods: {
    checkPermi,
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
    /** 缺陷列表属性字段改变操作 */
    checkedFieldListChange(field) {
      this.saveShowFields(field);
      this.refreshShowFields();
    },
    /** 设置列表显示的属性字段 */
    refreshShowFields() {
      const fieldList = this.getShowFields();
      if(fieldList) {
        this.tableShowFieldList = fieldList;
      } else {
        this.tableShowFieldList = [];
        this.tableAllFieldList.forEach(f=>{
          this.tableShowFieldList.push(f);
        });
      }
      this.$nextTick(()=>{
        this.$refs.table.doLayout();
      });
    },
    /** 保存表格显示哪些属性 */
    saveShowFields(field) {
      this.$cache.local.setJSON(PLAN_TABLE_FIELD_LIST_CACHE_KEY,field);
    },
    /** 获取表格显示哪些属性 */
    getShowFields() {
      return this.$cache.local.getJSON(PLAN_TABLE_FIELD_LIST_CACHE_KEY);
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
  margin-bottom: 10px;
}
.text-row3 {
  word-break: break-all;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
