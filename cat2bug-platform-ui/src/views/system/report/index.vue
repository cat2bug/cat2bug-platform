<template>
  <div class="app-container case-body report-page report-list-layout project-list-page-host" ref="reportMain">
    <project-label class="report-project-label" :project-id="projectId" />
    <div ref="reportTools" class="report-tools" :class="{ 'wrapped-tools': reportToolsWrapped }">
      <el-form class="left" :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px" :class="{ 'list-query-keyboard-nav': listQueryNavActive }">
        <el-form-item label="" prop="reportTitle" class="report-hint-query list-query-nav-item" data-query-key="reportTitle">
          <el-input
            v-model="queryParams.reportTitle"
            :placeholder="$t('report.please-enter-title')"
            prefix-icon="el-icon-tickets"
            size="small"
            clearable
            @input="handleQuery"
          />
        </el-form-item>
        <el-form-item label="" class="list-query-nav-item" data-query-key="reportTime">
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
        <el-form-item label="" prop="createById" class="list-query-nav-item" data-query-key="createById">
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
          <el-popover
            placement="top"
            trigger="click"
            popper-class="defect-column-picker-popover"
            v-model="reportColumnPickerVisible"
            @show="onColumnPickerPopoverShow"
            @hide="onColumnPickerPopoverHide"
          >
            <div class="defect-column-picker-head">
              <i class="el-icon-s-fold"></i>
              <h4>{{ $t('display-field') }}</h4>
            </div>
            <el-divider class="defect-field-divider"></el-divider>
            <el-checkbox-group
              :key="'report-colpick-' + reportColumnPickerRev"
              v-model="columnPickerCheckedKeys"
              class="defect-column-picker"
              @change="onReportColumnPickerChange"
            >
              <el-checkbox v-for="c in reportColumnPickerOptions" :key="c.key" :label="c.key">{{ $t(c.key) }}</el-checkbox>
            </el-checkbox-group>
            <el-button class="report-list-hint-columns" style="padding: 9px;" plain slot="reference" icon="el-icon-s-fold" size="small"></el-button>
          </el-popover>
          <el-button
            class="report-hint-batch-delete"
            type="danger"
            plain
            icon="el-icon-delete"
            size="small"
            :disabled="multiple"
            @click="handleDelete"
            v-hasPermi="['system:report:remove']"
          >{{ $t('batch-delete') }}</el-button>
          <cat2-bug-report-template-select
            ref="reportTemplateSelect"
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
      sort-column-cache-key="report_table_sort_column_key"
      sort-type-cache-key="report_table_sort_type_key"
      :columns="reportTableColumnDefaults"
      :data="reportList"
      :loading="loading"
      :table-max-height="reportTableBodyMaxHeight"
      @selection-change="handleSelectionChange"
      @row-click="rowClickHandle"
      @columns-change="onReportTableColumnsChange"
      @sort-change="handleSortChange"
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
          <span class="report-row-kbd-hint-anchor">{{ scope.row.reportTitle }}</span>
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

    <div v-show="total>0" ref="reportPaginationBand" class="report-table-pagination-band table-pagination-band">
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
import pageActionHints from '@/mixins/page-action-hints'
import listQueryKeyboardNav from '@/mixins/list-query-keyboard-nav'
import columnPickerPopoverKbd from '@/mixins/column-picker-popover-kbd'
import { shortcutStore } from '@/plugins/shortcut/shortcut-store'
import { checkPermi } from '@/utils/permission'
import {
  assignRowHintLetters,
  collectHintLettersFromToolbar,
  getDefectTableScrollBody,
  isRowIntersectingContainer,
  resolveDefectTableRowHintAnchor,
  resolveDefectTableRowHintPositionRect,
  resolveElTableRowData,
  scrollTableBodyByArrow
} from '@/utils/defect-row-kbd-hints'

const REPORT_KBD_SCOPE = 'report'

export default {
  name: "Report",
  mixins: [pageActionHints, listQueryKeyboardNav, columnPickerPopoverKbd],
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
      reportColumnPickerVisible: false,
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
        orderByColumn: null,
        isAsc: null,
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
  created() {
    this.queryParams.orderByColumn = this.$cache.local.get('report_table_sort_column_key') || null
    this.queryParams.isAsc = this.$cache.local.get('report_table_sort_type_key') || null
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
    this.registerReportShortcuts();
  },
  activated() {
    this.registerReportShortcuts();
    this.$nextTick(() => {
      this.$_bindListQueryNavFocusIn();
      this.$_bindListQueryNavToolbarFocusIn();
    });
  },
  deactivated() {
    if (this.$shortcut) this.$shortcut.unregisterPage(REPORT_KBD_SCOPE);
  },
  // 移除滚动条监听
  destroyed() {
    window.removeEventListener("resize", this.syncReportToolsWrapped);
    window.removeEventListener("resize", this.syncReportTableBodyMaxHeight);
    this.destroyReportListBodyResizeObserver();
    if (this.$shortcut) this.$shortcut.unregisterPage(REPORT_KBD_SCOPE);
  },
  beforeDestroy() {
    if (this.$shortcut) this.$shortcut.unregisterPage(REPORT_KBD_SCOPE);
  },
  methods: {
    registerReportShortcuts() {
      if (!this.$shortcut) return
      this.$shortcut.registerPage(REPORT_KBD_SCOPE, [
        { key: 'create', defaultLetter: 'E', run: () => this.shortcutCreateReport() }
      ])
    },
    getPageActionHintContainer() {
      return this.$refs.reportMain || this.$el
    },
    getReportCreateHintAnchor() {
      const sel = this.$refs.reportTemplateSelect
      const ref = sel && sel.$refs && sel.$refs.triggerButton
      return ref ? (ref.$el || ref) : null
    },
    getPageActionHints() {
      const L = (key, def) => shortcutStore.getLetter(`action.${REPORT_KBD_SCOPE}.${key}`, def)
      return [
        {
          key: 'create',
          letter: L('create', 'E'),
          badgeSelector: '.report-hint-create',
          resolveAnchor: () => this.getReportCreateHintAnchor(),
          floatOffset: { placement: 'bottom-right-outset', outset: 2, dy: 5 },
          run: () => this.shortcutCreateReport(),
          visible: () => checkPermi(['system:report:add'])
        }
      ]
    },
    /** ⌘ 按住：表格可见行标题列动态徽标（1–9 优先，字母补位） */
    getPageDynamicActionHints(ctx) {
      const used = (ctx && ctx.usedLetters) ? new Set(ctx.usedLetters) : new Set()
      collectHintLettersFromToolbar(this.getPageActionHints()).forEach((ch) => used.add(ch))
      const rowFloat = { placement: 'center-cell' }
      return this.buildReportTableRowActionHints(used, rowFloat)
    },
    buildReportTableRowActionHints(usedLetters, rowFloat) {
      if (this.loading || this.isReportViewDrawerOpen() || this.open) return []
      const cat = this.$refs.cat2BugTable
      if (!cat || !cat.$el) return []
      const bodyWrap = getDefectTableScrollBody(cat.$el)
      if (!bodyWrap) return []
      const tableRoot = cat.$el
      const list = this.reportList || []
      const seen = new Set()
      const anchors = []
      bodyWrap.querySelectorAll('tbody tr.el-table__row').forEach((tr, rowIndex) => {
        if (!isRowIntersectingContainer(tr, bodyWrap)) return
        const row = resolveElTableRowData(tr) || list[rowIndex]
        if (!row || row.reportId == null) return
        const reportId = String(row.reportId)
        if (seen.has(reportId)) return
        seen.add(reportId)
        const anchor = tr.querySelector('.report-row-kbd-hint-anchor') || resolveDefectTableRowHintAnchor(tr)
        if (!anchor) return
        anchors.push({
          anchor,
          getAnchorRect: () => resolveDefectTableRowHintPositionRect(tr, tableRoot),
          skipViewportCheck: true,
          run: () => this.rowClickHandle(row)
        })
      })
      const letters = assignRowHintLetters(anchors.length, usedLetters)
      return anchors.map((item, i) => ({
        ...item,
        letter: letters[i],
        floatOffset: rowFloat,
        key: `row-${i}`
      })).filter((item) => item.letter)
    },
    getListQueryNavItems() {
      return [
        { key: 'reportTitle' },
        { key: 'reportTime' },
        { key: 'createById' }
      ]
    },
    getListQueryNavToolbarRef() {
      return 'reportToolsRight'
    },
    getColumnPickerTriggerEl() {
      const root = this.getPageActionHintContainer()
      return root && root.querySelector('.report-list-hint-columns')
    },
    closeColumnPickerPopoverKbd() {
      this.reportColumnPickerVisible = false
    },
    getListQueryNavFocusEl(key) {
      const itemEl = this.getListQueryNavItemEl(key)
      if (!itemEl) return null
      if (key === 'reportTime') {
        return itemEl.querySelector('.el-range-input') ||
          itemEl.querySelector('input.el-input__inner')
      }
      if (key === 'createById') {
        return itemEl.querySelector('.select-project-member-input')
      }
      return itemEl.querySelector('input.el-input__inner')
    },
    getPageActionHintScrollRoots() {
      const cat = this.$refs.cat2BugTable
      if (!cat || !cat.$el) return []
      const bodyWrap = getDefectTableScrollBody(cat.$el)
      return bodyWrap ? [bodyWrap] : []
    },
    isReportViewDrawerOpen() {
      const vr = this.$refs.viewReport
      return !!(vr && vr.visible)
    },
    isReportColumnPickerOpen() {
      const popper = document.querySelector('.defect-column-picker-popover')
      if (!popper || popper.getAttribute('aria-hidden') === 'true') return false
      const rect = popper.getBoundingClientRect()
      return rect.width > 0 && rect.height > 0
    },
    getReportTableScrollBody() {
      const cat = this.$refs.cat2BugTable
      if (!cat || !cat.$el) return null
      return getDefectTableScrollBody(cat.$el)
    },
    /** ⌘/Ctrl + 方向键：报告列表垂直/水平滚动 */
    handlePageModifierArrowScroll(e) {
      if (this.isReportViewDrawerOpen() || this.open || this.isReportColumnPickerOpen()) {
        return false
      }
      const bodyWrap = this.getReportTableScrollBody()
      if (!bodyWrap) return false
      return scrollTableBodyByArrow(bodyWrap, e.key)
    },
    shortcutFocusQuery() {
      this.enterListQueryKeyboardNav()
    },
    shortcutCreateReport() {
      if (!checkPermi(['system:report:add'])) return
      const sel = this.$refs.reportTemplateSelect
      if (sel && typeof sel.openTemplatePopover === 'function') {
        sel.openTemplatePopover()
        this.$nextTick(() => {
          const pop = sel.$refs && sel.$refs.templatePopover
          if (pop && pop.showPopper) return
          const btn = this.getReportCreateHintAnchor()
          if (btn && typeof btn.click === 'function') {
            btn.click()
          }
        })
        return
      }
      const btn = this.getReportCreateHintAnchor() ||
        (this.getPageActionHintContainer() && this.getPageActionHintContainer().querySelector('.report-hint-create'))
      if (btn && typeof btn.click === 'function') {
        btn.click()
      }
    },
    shortcutBatchDelete() {
      if (!checkPermi(['system:report:remove']) || this.multiple) return
      this.handleDelete({ stopPropagation() {} })
    },
    shortcutChangePage(delta) {
      const root = this.getPageActionHintContainer()
      if (!root || typeof root.querySelector !== 'function') return
      const btn = root.querySelector(
        delta < 0 ? '.report-table-pagination .btn-prev' : '.report-table-pagination .btn-next'
      )
      if (btn && !btn.classList.contains('disabled') && typeof btn.click === 'function') {
        btn.click()
      }
    },
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
    handleSortChange(column) {
      this.queryParams.orderByColumn = column.order ? column.prop : null
      this.queryParams.isAsc = column.order
      this.queryParams.pageNum = 1
      this.getList()
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
}
.report-page ::v-deep h3.report-project-label {
  margin-top: 0;
  margin-bottom: 0;
}
.report-list-body {
  flex: 1 1 0%;
  min-height: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
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
  margin-top: 0;
  margin-bottom: 0;
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
.report-table-pagination-band {
  flex-shrink: 0;
}
.report-hint-batch-delete,
::v-deep .report-hint-create {
  position: relative;
  overflow: visible !important;
}
</style>
