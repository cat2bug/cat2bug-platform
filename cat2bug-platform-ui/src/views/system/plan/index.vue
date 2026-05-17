<template>
  <div class="app-container case-body plan-page plan-list-layout">
    <project-label class="plan-project-label" />
    <div ref="planTools" class="plan-tools" :class="{ 'wrapped-tools': planToolsWrapped }">
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
      <div ref="planToolsRight" class="plan-tools-right">
        <el-popover placement="top" trigger="click">
          <div class="row">
            <i class="el-icon-s-fold"></i>
            <h4>{{ $t('display-field') }}</h4>
          </div>
          <el-divider class="plan-field-divider"></el-divider>
          <el-checkbox-group
            :key="'plan-list-colpick-' + planColumnPickerRev"
            v-model="columnPickerCheckedKeys"
            class="col"
            @change="onPlanColumnPickerChange"
          >
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

    <div ref="planListBody" class="plan-list-body">
      <div class="plan-table-x-scroll">
    <cat2-bug-table
      ref="cat2BugTable"
      cache-key="plan-table"
      field-list-cache-key="plan-table-field-list"
      :persist-sort="false"
      :columns="planTableColumnDefaults"
      :data="planList"
      :loading="loading"
      :table-max-height="planTableBodyMaxHeight"
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
        <el-table-column :label="$t('operate')" align="left" class-name="no-drag cat2bug-operate-column" fixed="right">
          <template slot-scope="scope">
            <div class="table-operate cat2bug-operate-tools">
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
      </div>

    <div v-show="total>0" ref="planPaginationBand" class="plan-table-pagination-band">
      <pagination
        class="plan-table-pagination"
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="getList"
      />
    </div>
    </div>
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
      planColumnPickerRev: 0,
      /** 与 Cat2BugTable columns-change 列顺序一致 */
      planPickerColumnList: null,
      planToolsWrapped: false,
      /** 表体 max-height（与用例页一致：列表内纵向滚动） */
      planTableBodyMaxHeight: null,
    };
  },
  watch: {
    "$i18n.locale": function () {
      this.$nextTick(() => {
        this.$refs.cat2BugTable && this.$refs.cat2BugTable.doLayout();
        this.syncPlanTableBodyMaxHeight();
      });
    },
  },
  computed: {
    planColumnPickerOptions() {
      const ordered = this.planPickerColumnList;
      if (ordered && ordered.length) {
        return ordered.map(c => ({ ...c }));
      }
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
    this.$nextTick(() => {
      this.syncPlanToolsWrapped();
      this.initPlanListBodyResizeObserver();
      this.syncPlanTableBodyMaxHeight();
    });
    window.addEventListener("resize", this.syncPlanToolsWrapped);
    window.addEventListener("resize", this.syncPlanTableBodyMaxHeight);
    const actionPlanId = this.$route.query.planId;
    if(actionPlanId && checkPermi(['system:plan:run'])) {
      this.handlePlanRun({planId:actionPlanId})
    }
  },
  destroyed() {
    // 移除滚动条监听
    this.$floatMenu.windowsDestory();
    window.removeEventListener("resize", this.syncPlanToolsWrapped);
    window.removeEventListener("resize", this.syncPlanTableBodyMaxHeight);
    this.destroyPlanListBodyResizeObserver();
  },
  methods: {
    initPlanListBodyResizeObserver() {
      if (typeof ResizeObserver === 'undefined') return;
      this.destroyPlanListBodyResizeObserver();
      const el = this.$refs.planListBody;
      if (!el) return;
      this._planListBodyResizeObserver = new ResizeObserver(() => {
        this.syncPlanTableBodyMaxHeight();
      });
      this._planListBodyResizeObserver.observe(el);
    },
    destroyPlanListBodyResizeObserver() {
      if (this._planListBodyResizeObserver) {
        this._planListBodyResizeObserver.disconnect();
        this._planListBodyResizeObserver = null;
      }
    },
    syncPlanTableBodyMaxHeight() {
      this.$nextTick(() => {
        const body = this.$refs.planListBody;
        if (!body || !body.clientHeight) return;
        const pagEl = this.$refs.planPaginationBand;
        let reserveBelowTable = 0;
        if (this.total > 0 && pagEl && pagEl.offsetParent !== null) {
          const st = window.getComputedStyle(pagEl);
          reserveBelowTable =
            pagEl.offsetHeight +
            parseFloat(st.marginTop || "0") +
            parseFloat(st.marginBottom || "0");
        }
        const next = Math.max(120, Math.floor(body.clientHeight - reserveBelowTable - 2));
        if (this.planTableBodyMaxHeight !== next) {
          this.planTableBodyMaxHeight = next;
          this.$nextTick(() => {
            const tbl = this.$refs.cat2BugTable;
            if (tbl && typeof tbl.doLayout === "function") tbl.doLayout();
          });
        }
      });
    },
    checkPermi,
    strFormat,
    onPlanTableColumnsChange(columns) {
      this.planColumnPickerRev += 1;
      const picker = columns.filter(c => c.showInColumnPicker !== false).map(c => ({ ...c }));
      this.$set(this, 'planPickerColumnList', picker);
      this.columnPickerCheckedKeys = columns.filter(c => c.visible && c.showInColumnPicker !== false).map(c => c.key);
      this.$nextTick(() => {
        this.$refs.cat2BugTable && this.$refs.cat2BugTable.doLayout();
        this.syncPlanTableBodyMaxHeight();
      });
    },
    syncPlanToolsWrapped() {
      const measure = () => {
        const tools = this.$refs.planTools;
        const left = tools && tools.querySelector(".left");
        const right = this.$refs.planToolsRight;
        if (!tools || !left || !right) {
          this.planToolsWrapped = false;
          return;
        }
        this.planToolsWrapped = right.offsetTop - left.offsetTop > 4;
      };

      this.$nextTick(() => {
        // 先回到默认单行布局再测量，避免 wrapped 样式影响判断导致无法恢复一行
        if (this.planToolsWrapped) {
          this.planToolsWrapped = false;
          this.$nextTick(measure);
          return;
        }
        measure();
      });
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
        this.syncPlanToolsWrapped();
        this.$nextTick(() => {
          this.syncPlanTableBodyMaxHeight();
        });
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
/* 与用例页 .case-body：底边留白交给分页区 margin，左右与顶 20px */
.app-container.case-body {
  padding: 20px 20px 0;
  box-sizing: border-box;
}
/* 与用例页 case-body：主列撑满视口，列表与分页在 plan-list-body 内布局 */
.plan-page.plan-list-layout {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  min-height: 0;
  flex: 1 1 auto;
  width: 100%;
  box-sizing: border-box;
  /* 与用例页 .case-page 工具条上下留白一致 */
  --case-toolbar-v-gap: 8px;
}
/* 与缺陷列表 .defect-table-context：占满工具条下主区剩余高度，短表时表体仍用 max-height 撑满，分页置底 */
.plan-list-body {
  flex: 1 1 0%;
  min-height: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  --defect-pagination-v-gap: 28px;
}
.plan-table-x-scroll {
  width: 100%;
  min-width: 0;
  overflow-x: auto;
  overflow-y: hidden;
  -webkit-overflow-scrolling: touch;
}
/* ProjectLabel 根为 h3：类在根上，与用例页 h3 下边距一致 */
.plan-page ::v-deep h3.plan-project-label {
  margin-top: 0;
  margin-bottom: 10px;
}
/* 与用例页 .case-view-toolbar：上下 8px，勿再叠 10px + 组件 h3 默认 20px */
.plan-tools {
  width: 100%;
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  align-items: center;
  align-content: flex-start;
  /* 同行时左右拉开；右侧单独换行时该行仅一项，会靠左 */
  justify-content: flex-start;
  column-gap: 12px;
  row-gap: 8px;
  margin-top: var(--case-toolbar-v-gap, 8px);
  margin-bottom: var(--case-toolbar-v-gap, 8px);
  .el-form-item {
    margin-bottom: 0;
  }
}

/* 左侧查询表单：占满剩余宽，可内部换行 */
.plan-tools > .left {
  /* 左侧优先占据第一行可用空间；空间不足时先挤压自身，再让右侧换行 */
  flex: 1 1 auto;
  min-width: 0;
  max-width: 100%;
  width: auto;
  display: flex;
  /* 默认先不换行，优先触发右侧换到下一行 */
  flex-wrap: nowrap;
  align-items: center;
  row-gap: 8px;
  column-gap: 8px;
  box-sizing: border-box;
  ::v-deep .el-form-item {
    margin-right: 0;
  }
}

/* 右侧工具：默认自适应；空间不够会被挤到下一行 */
.plan-tools-right {
  flex: 0 0 auto;
  display: inline-flex;
  flex-wrap: wrap;
  justify-content: flex-start;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

/* 右侧工具换到下一行时，左侧查询改为逐行满宽 */
.plan-tools.wrapped-tools {
  /* 左侧进入换行态后：每个查询项独占一行并铺满（与缺陷页的移动端策略一致） */
  > .left {
    flex: 1 1 100%;
    width: 100%;
    max-width: 100%;
    display: block;
    box-sizing: border-box;
  }
  > .left ::v-deep .el-form-item {
    display: block;
    width: 100% !important;
    margin-right: 0;
    margin-bottom: 8px;
  }
  > .left ::v-deep .el-form-item:last-child {
    margin-bottom: 0;
  }
  > .left ::v-deep .el-form-item .el-form-item__content,
  > .left ::v-deep .el-form-item .el-input {
    width: 100% !important;
    max-width: 100%;
    box-sizing: border-box;
  }
  > .left ::v-deep .el-form-item .el-input__inner {
    width: 100% !important;
    box-sizing: border-box;
  }
  > .plan-tools-right {
    margin-left: 0;
    flex: 1 1 100%;
    width: 100%;
    justify-content: flex-start;
  }
}

@media screen and (max-width: 576px) {
  /* 右侧工具单独一行：字段按钮 + 新建同一行，新建填充剩余宽度 */
  .plan-tools-right {
    flex: 1 1 100%;
    width: 100%;
    max-width: 100%;
    display: flex;
    flex-wrap: nowrap;
    align-items: center;
    gap: 8px;
  }
  .plan-tools-right > *:first-child {
    flex-shrink: 0;
  }
  .plan-tools-right > .el-button {
    flex: 1 1 auto;
    min-width: 0;
    width: auto;
  }
  .plan-tools-right > .el-button .el-icon {
    margin-right: 4px;
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
  display: inline-flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
  flex-wrap: wrap;
  white-space: nowrap;
}
.plan-field-divider {
  margin: 8px 0px;
}
/* plan-tools-right 已在上方按响应式统一定义 */
.text-row3 {
  word-break: break-all;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
/* 与缺陷列表 .defect-table-pagination-band 一致 */
.plan-table-pagination-band {
  flex-shrink: 0;
  margin-top: var(--defect-pagination-v-gap);
  margin-bottom: calc(40px + env(safe-area-inset-bottom, 0px));
}
::v-deep .plan-table-pagination.pagination-container {
  margin-top: 0 !important;
  margin-bottom: 0 !important;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  padding-left: 0 !important;
  padding-right: 0 !important;
}
</style>
