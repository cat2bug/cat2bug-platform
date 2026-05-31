<template>
  <div class="app-container case-body report-page report-list-layout project-list-page-host">
    <project-label class="report-project-label" :project-id="projectId" />
    <div ref="reportTools" class="report-tools" :class="{ 'wrapped-tools': reportToolsWrapped }">
      <el-form class="left" :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item label="" prop="reportTitle">
          <el-input
            v-model="queryParams.reportTitle"
            :placeholder="$t('report.please-enter-title')"
            prefix-icon="el-icon-tickets"
            size="small"
            clearable
            @input="handleQuery"
          />
        </el-form-item>
        <el-form-item label="">
          <el-date-picker
            v-model="daterangeReportTime"
            size="small"
            style="width: 240px"
            value-format="yyyy-MM-dd"
            type="daterange"
            range-separator="-"
            :start-placeholder="$t('start-time')"
            :end-placeholder="$t('end-time')"
            @change="handleQuery"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="" prop="createById">
          <select-project-member
            v-model="queryParams.createByIds"
            :project-id="projectId"
            placeholder="report.please-enter-pusher"
            :is-head="false"
            size="small"
            icon="el-icon-user"
            @input="handleQuery()"
          />
        </el-form-item>
      </el-form>
      <div ref="reportToolsRight" class="report-tools-right">
          <el-popover placement="top" trigger="click">
            <div class="report-picker-head row">
              <i class="el-icon-s-fold"></i>
              <h4>{{ $t('display-field') }}</h4>
            </div>
            <el-divider class="report-picker-divider"></el-divider>
            <el-checkbox-group
              :key="'report-colpick-' + reportColumnPickerRev"
              v-model="columnPickerCheckedKeys"
              class="report-column-picker"
              @change="onReportColumnPickerChange"
            >
              <el-checkbox v-for="c in reportColumnPickerOptions" :key="c.key" :label="c.key">{{ $t(c.key) }}</el-checkbox>
            </el-checkbox-group>
            <el-button style="padding: 9px;" plain slot="reference" icon="el-icon-s-fold" size="small"></el-button>
          </el-popover>
          <el-button
            type="danger"
            plain
            icon="el-icon-delete"
            size="small"
            :disabled="multiple"
            @click="handleDelete"
            v-hasPermi="['system:report:remove']"
          >{{ $t('batch-delete') }}</el-button>
          <cat2-bug-report-template-select
            size="small"
            v-hasPermi="['system:report:add']"
            @create="createReportHandle"
            :project-id="projectId" />
      </div>
    </div>
    <div ref="reportListBody" class="report-list-body">
      <div class="report-table-x-scroll">
    <cat2-bug-table
      ref="cat2BugTable"
      cache-key="report-table"
      :persist-sort="false"
      :columns="reportTableColumnDefaults"
      :data="reportList"
      :loading="loading"
      :table-max-height="reportTableBodyMaxHeight"
      @selection-change="handleSelectionChange"
      @row-click="rowClickHandle"
      @columns-change="onReportTableColumnsChange"
    >
      <template #prepend>
        <el-table-column type="selection" width="50" align="center" />
      </template>
      <template #columns="{ scope, column }">
        <report-type-flag v-if="column.prop==='reportKey'" :report="scope.row" />
        <div v-else-if="column.prop==='reportTitle'" class="table-report-title">
          <focus-member-list
            v-model="scope.row.focusList"
            module-name="report"
            :data-id="scope.row.reportId" />
          <span>{{ scope.row.reportTitle }}</span>
        </div>
        <span v-else-if="column.prop==='reportTime'">{{ parseTime(scope.row.reportTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        <span v-else-if="column.prop==='reportSource'">{{ scope.row.reportSource }}</span>
        <template v-else-if="column.prop==='createBy'">
          <el-tooltip v-if="scope.row.createBy" class="item" effect="dark" :content="scope.row.createBy" placement="top">
            <cat2-bug-avatar :member="member(scope.row)" />
          </el-tooltip>
        </template>
        <span v-else>{{ scope.row[column.prop] }}</span>
      </template>
      <template #append>
        <el-table-column :label="$t('operate')" align="left" class-name="no-drag cat2bug-operate-column" fixed="right">
          <template slot-scope="scope">
            <report-tools class="cat2bug-operate-tools" :report="scope.row" :is-text="true" :is-show-icon="true" @delete="getList" />
          </template>
        </el-table-column>
      </template>
    </cat2-bug-table>
      </div>

    <div v-show="total>0" ref="reportPaginationBand" class="report-table-pagination-band">
      <pagination
        class="report-table-pagination"
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="getList"
      />
    </div>
    </div>

    <!-- 添加或修改报告对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="报告标题" prop="reportTitle">
          <el-input v-model="form.reportTitle" placeholder="请输入报告标题" />
        </el-form-item>
        <el-form-item label="报告时间" prop="reportTime">
          <el-date-picker clearable
            v-model="form.reportTime"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择报告时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="报告描述" prop="reportDescription">
          <el-input v-model="form.reportDescription" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label=" 推送人ID" prop="createById">
          <el-input v-model="form.createById" placeholder="请输入 推送人ID" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
    <view-report ref="viewReport" :project-id="projectId" @delete="handleQuery" />
  </div>
</template>

<script>
import { listReport, getReport, delReport, addReport, updateReport } from "@/api/system/report";
import ProjectLabel from "@/components/Project/ProjectLabel";
import ViewReport from "@/components/Report/ViewReport";
import Step from "@/components/Case/CaseStep";
import ReportTools from "@/components/Report/ReportTools";
import FocusMemberList from "@/components/FocusMemberList";
import ReportTypeFlag from "@/components/Report/ReportTypeFlag";
import Cat2BugReportTemplateSelect from "@/components/Cat2BugReportTemplateSelect";
import SelectProjectMember from "@/components/Project/SelectProjectMember";
import Cat2BugAvatar from "@/components/Cat2BugAvatar";
import i18n from "@/utils/i18n/i18n";
import {delUser} from "@/api/system/user";
import store from "@/store";

import Cat2BugTable from "@/components/Cat2BugTable";
import { ReportTableColumnDefaults } from "@/views/system/report/report-table-options";

export default {
  name: "Report",
  components: { Step, ProjectLabel, ViewReport, ReportTools, FocusMemberList, ReportTypeFlag, Cat2BugReportTemplateSelect, Cat2BugAvatar, SelectProjectMember, Cat2BugTable },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 报告表格数据
      reportList: [],
      reportTableColumnDefaults: ReportTableColumnDefaults.map(c => ({ ...c })),
      columnPickerCheckedKeys: [],
      reportColumnPickerRev: 0,
      reportPickerColumnList: null,
      reportToolsWrapped: false,
      reportTableBodyMaxHeight: null,
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      //  推送人ID时间范围
      daterangeReportTime: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        reportTitle: null,
        reportTime: null,
        reportDataType: null,
        createById: null,
        params:{
          createByIds:[]
        }
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        reportTitle: [
          { required: true, message: "报告标题不能为空", trigger: "blur" }
        ],
      },
    };
  },
  computed: {
    reportColumnPickerOptions() {
      const ordered = this.reportPickerColumnList;
      if (ordered && ordered.length) {
        return ordered.map(c => ({ ...c }));
      }
      return ReportTableColumnDefaults.filter(c => c.showInColumnPicker !== false);
    },
    /** 获取项目id */
    projectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    member: function () {
      return function (report) {
        return {
          nickName: report.createBy,
          avatar: report.createByAvatar
        }
      }
    },
  },
  mounted() {
    if(this.$route.query.projectId) {
      let _this = this;
      store.dispatch('SwitchCurrentProject', this.$route.query.projectId).then(() => {
        _this.getList();
        if(_this.$route.query.reportId) {
          _this.$refs.viewReport.open(this.$route.query.reportId);
        }
      });
    } else {
      this.getList();
      if(this.$route.query.reportId) {
        this.$refs.viewReport.open(this.$route.query.reportId);
      }
    }
    this.$nextTick(() => {
      this.syncReportToolsWrapped();
      this.initReportListBodyResizeObserver();
      this.syncReportTableBodyMaxHeight();
    });
    window.addEventListener("resize", this.syncReportToolsWrapped);
    window.addEventListener("resize", this.syncReportTableBodyMaxHeight);
  },
  // 移除滚动条监听
  destroyed() {
    window.removeEventListener("resize", this.syncReportToolsWrapped);
    window.removeEventListener("resize", this.syncReportTableBodyMaxHeight);
    this.destroyReportListBodyResizeObserver();
  },
  methods: {
    initReportListBodyResizeObserver() {
      if (typeof ResizeObserver === 'undefined') return;
      this.destroyReportListBodyResizeObserver();
      const el = this.$refs.reportListBody;
      if (!el) return;
      this._reportListBodyResizeObserver = new ResizeObserver(() => {
        this.syncReportTableBodyMaxHeight();
      });
      this._reportListBodyResizeObserver.observe(el);
    },
    destroyReportListBodyResizeObserver() {
      if (this._reportListBodyResizeObserver) {
        this._reportListBodyResizeObserver.disconnect();
        this._reportListBodyResizeObserver = null;
      }
    },
    syncReportTableBodyMaxHeight() {
      this.$nextTick(() => {
        const body = this.$refs.reportListBody;
        if (!body || !body.clientHeight) return;
        const pagEl = this.$refs.reportPaginationBand;
        let reserveBelowTable = 0;
        if (this.total > 0 && pagEl && pagEl.offsetParent !== null) {
          const st = window.getComputedStyle(pagEl);
          reserveBelowTable =
            pagEl.offsetHeight +
            parseFloat(st.marginTop || "0") +
            parseFloat(st.marginBottom || "0");
        }
        const next = Math.max(120, Math.floor(body.clientHeight - reserveBelowTable - 2));
        if (this.reportTableBodyMaxHeight !== next) {
          this.reportTableBodyMaxHeight = next;
          this.$nextTick(() => {
            const tbl = this.$refs.cat2BugTable;
            if (tbl && typeof tbl.doLayout === "function") tbl.doLayout();
          });
        }
      });
    },
    syncReportToolsWrapped() {
      const measure = () => {
        const tools = this.$refs.reportTools;
        const left = tools && tools.querySelector(".left");
        const right = this.$refs.reportToolsRight;
        if (!tools || !left || !right) {
          this.reportToolsWrapped = false;
          return;
        }
        this.reportToolsWrapped = right.offsetTop - left.offsetTop > 4;
      };
      this.$nextTick(() => {
        if (this.reportToolsWrapped) {
          this.reportToolsWrapped = false;
          this.$nextTick(measure);
          return;
        }
        measure();
      });
    },
    /** 查询报告列表 */
    getList() {
      this.loading = true;
      this.queryParams.projectId = this.projectId;
      if (null != this.daterangeReportTime && '' != this.daterangeReportTime) {
        this.queryParams.params["beginReportTime"] = this.daterangeReportTime[0];
        this.queryParams.params["endReportTime"] = this.daterangeReportTime[1];
      }
      listReport(this.queryParams).then(response => {
        this.reportList = response.rows;
        this.total = response.total;
        this.loading = false;
        this.syncReportToolsWrapped();
        this.$nextTick(() => {
          this.syncReportTableBodyMaxHeight();
        });
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.queryCreateByIds = [];
      this.form = {
        reportId: null,
        reportTitle: null,
        reportTime: null,
        reportDescription: null,
        reportDataType: null,
        reportData: null,
        createById: null,
        params: {
          createByIds:[]
        }
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.daterangeReportTime = [];
      this.resetForm("queryForm");
      this.handleQuery();
    },
    rowClickHandle(report) {
      this.$refs.viewReport.open(report.reportId);
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加报告";
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.reportId != null) {
            updateReport(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addReport(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    handleCommand(cmd) {
      switch (cmd){
        case 'add':
          this.handleReport();
          break;
        default:
          break;
      }
    },
    createReportHandle(template) {
      this.getList();
    },
    onReportTableColumnsChange(columns) {
      this.reportColumnPickerRev += 1;
      const picker = columns.filter(c => c.showInColumnPicker !== false).map(c => ({ ...c }));
      this.$set(this, 'reportPickerColumnList', picker);
      this.columnPickerCheckedKeys = columns.filter(c => c.visible && c.showInColumnPicker !== false).map(c => c.key);
      this.$nextTick(() => {
        this.$refs.cat2BugTable && this.$refs.cat2BugTable.doLayout();
        this.syncReportTableBodyMaxHeight();
      });
    },
    onReportColumnPickerChange(keys) {
      this.$refs.cat2BugTable && this.$refs.cat2BugTable.setColumnsVisible(keys);
    },
    /** 多选框选中数据 */
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.reportId);
      this.single = selection.length != 1;
      this.multiple = !selection.length;
    },
    /** 删除按钮操作 */
    handleDelete(event) {
      if (!this.ids || this.ids.length == 0){
        this.$message.error(this.$i18n.t('report.batch-delete-empty-except').toString());
        return;
      }
      const reportIds = this.ids;
      this.$modal.confirm(
        this.$i18n.t('report.delete-select-report'),
        this.$i18n.t('prompted').toString(),
        {
          confirmButtonText: i18n.t('delete').toString(),
          cancelButtonText: i18n.t('cancel').toString(),
          confirmButtonClass: 'delete-button',
          type: "warning"
        }).then(function() {
        return delReport(reportIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess(this.$i18n.t('delete.success'));
      }).catch(() => {});
      event.stopPropagation();
    },
  }
};
</script>
<style>
.list {
  display: inline-flex;
  flex-direction: row;
  flex-wrap: wrap;
  /*width: 500px;*/
}
</style>
<style lang="scss" scoped>
.app-container.case-body {
  padding: 20px 20px 0;
  box-sizing: border-box;
}
.report-page.report-list-layout {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  min-height: 0;
  flex: 1 1 auto;
  width: 100%;
  box-sizing: border-box;
  --case-toolbar-v-gap: 8px;
}
.report-page ::v-deep h3.report-project-label {
  margin-top: 0;
  margin-bottom: 10px;
}
.report-list-body {
  flex: 1 1 0%;
  min-height: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  --defect-pagination-v-gap: 28px;
}
.report-table-x-scroll {
  width: 100%;
  min-width: 0;
  overflow-x: auto;
  overflow-y: hidden;
  -webkit-overflow-scrolling: touch;
}
.report-tools {
  width: 100%;
  display: inline-flex;
  flex-direction: row;
  flex-wrap: wrap;
  justify-content: flex-start;
  align-items: center;
  align-content: flex-start;
  column-gap: var(--cat2bug-toolbar-section-gap, 10px);
  row-gap: var(--cat2bug-toolbar-row-gap, 8px);
  margin-top: var(--case-toolbar-v-gap, 8px);
  margin-bottom: var(--case-toolbar-v-gap, 8px);
  .el-form-item {
    margin-bottom: 0;
  }
}
.report-tools > .left {
  flex: 1 1 auto;
  min-width: 0;
  max-width: 100%;
  width: auto;
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  row-gap: 8px;
  column-gap: var(--cat2bug-toolbar-item-gap, 10px);
  box-sizing: border-box;
  ::v-deep .el-form-item {
    margin-right: 0;
  }
}
.report-tools-right {
  flex: 0 0 auto;
  display: inline-flex;
  flex-wrap: wrap;
  justify-content: flex-start;
  align-items: center;
  gap: var(--cat2bug-toolbar-item-gap, 10px);
  margin-left: auto;
}
.report-tools.wrapped-tools {
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
  > .left ::v-deep .el-form-item .el-input,
  > .left ::v-deep .el-form-item .el-date-editor,
  > .left ::v-deep .el-form-item .select-project-member,
  > .left ::v-deep .el-form-item .el-popover__reference-wrapper,
  > .left ::v-deep .el-form-item .select-project-member-input {
    width: 100% !important;
    max-width: 100%;
    box-sizing: border-box;
  }
  > .report-tools-right {
    margin-left: 0;
    flex: 1 1 100%;
    width: 100%;
    justify-content: flex-start;
  }
}
.table-report-title {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-start;
}
.report-picker-head.row {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: var(--cat2bug-toolbar-item-gap, 10px);
}
.report-picker-head h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.3;
}
.report-picker-divider {
  margin: 8px 0;
}
/** 与缺陷列表「显示字段」同款：用 gap 控制间距，去掉 checkbox 默认大块外边距 */
.report-column-picker {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 220px;
  max-height: 380px;
  overflow-y: auto;
  padding-right: 4px;
}
.report-column-picker ::v-deep .el-checkbox {
  display: flex;
  align-items: center;
  margin-right: 0;
  margin-bottom: 0;
  height: auto;
  line-height: 1.4;
  white-space: nowrap;
}
.report-column-picker ::v-deep .el-checkbox__input {
  flex-shrink: 0;
}
.report-column-picker ::v-deep .el-checkbox__label {
  line-height: 1.4;
  padding-left: 8px;
}
/* 与缺陷列表 .defect-table-pagination-band 一致 */
.report-table-pagination-band {
  flex-shrink: 0;
  margin-top: var(--defect-pagination-v-gap);
  margin-bottom: calc(40px + env(safe-area-inset-bottom, 0px));
}
::v-deep .report-table-pagination.pagination-container {
  margin-top: 0 !important;
  margin-bottom: 0 !important;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  padding-left: 0 !important;
  padding-right: 0 !important;
}
</style>
