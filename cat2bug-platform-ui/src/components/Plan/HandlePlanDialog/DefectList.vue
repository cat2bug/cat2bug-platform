<template>
  <div class="plan-item-content plan-item-drawer-layout">
    <div class="plan-item-query defect-table-tools defect-table-tools-bar">
      <div class="plan-query-main">
        <el-form :model="queryParams" ref="queryForm" class="plan-query-inline-form" size="small" :inline="true" label-width="0">
          <el-form-item class="plan-query-switch-item" label-width="0">
            <slot name="query"></slot>
          </el-form-item>
          <el-form-item prop="defectName">
            <el-input
              size="small"
              v-model="queryParams.defectName"
              :placeholder="$t('defect.enter-name')"
              prefix-icon="el-icon-search"
              clearable
              @input="search()"
            />
          </el-form-item>
          <el-form-item prop="defectState">
            <el-select size="small" v-model="queryParams.params.defectStates" multiple collapse-tags clearable :placeholder="$t('defect.select-state')" @change="search()">
              <el-option
                v-for="state in defectStates"
                :key="state.key"
                :label="$i18n.t(state.value)"
                :value="state.key">
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item prop="handleBy" class="padding-top-3px">
            <select-project-member
              v-model="queryParams.handleBy"
              :project-id="projectId"
              placeholder="defect.select-handle-by"
              :is-head="false"
              size="small"
              icon="el-icon-user"
              @input="search()"
            />
          </el-form-item>
        </el-form>
      </div>
      <div class="handle-plan-tools-right">
        <el-popover
          placement="top"
          trigger="click">
          <div class="row">
            <i class="el-icon-s-fold"></i>
            <h4>{{$t('defect.display-field')}}</h4>
          </div>
          <el-divider class="defect-field-divider"></el-divider>
          <el-checkbox-group
            :key="'plan-defect-colpick-' + planDefectColumnPickerRev"
            v-model="columnPickerCheckedKeys"
            class="col"
            @change="onColumnPickerChange"
          >
            <el-checkbox v-for="c in defectColumnPickerOptions" :key="c.key" :label="c.key">{{ $t(c.key) }}</el-checkbox>
          </el-checkbox-group>
          <el-button
            style="padding: 9px;"
            plain
            slot="reference"
            icon="el-icon-s-fold"
            size="mini"
          ></el-button>
        </el-popover>
      </div>
    </div>
    <multipane
      layout="vertical"
      ref="multiPane"
      class="custom-resizer plan-drawer-defect-multipane"
      :class="{ 'custom-resizer--tree-hidden': !showModuleTree }"
      @paneResizeStop="dragStopHandle"
    >
      <div v-if="showModuleTree" class="tree-module" ref="treeModule" :style="treeModuleStyle">
        <slot name="tree" :toolbar-sync-height="deliverableToolbarSyncHeight" />
      </div>
      <multipane-resizer ref="paneResizer" v-show="showModuleTree" :style="[multipaneStyle, paneResizerRuleVars]" />
      <div ref="caseContext" class="plan-item-content-list plan-drawer-defect-list-pane">
        <div ref="planDrawerTableScroll" class="plan-drawer-table-scroll">
    <cat2-bug-table
      ref="cat2BugTable"
      :table-max-height="planDrawerTableMaxHeight"
      field-list-cache-key="plan-defect-table-field-list"
      :sort-column-cache-key="planDefectSortColumnKey"
      :sort-type-cache-key="planDefectSortTypeKey"
      :columns="planDefectTableColumns"
      :data="defectList"
      :loading="loading"
      v-resize="setDragComponentSize"
      @sort-change="sortChangeHandle"
      @selection-change="handleSelectionChange"
      @row-click="handleClickTableRow"
      @columns-change="onTableColumnsChange"
      @native-mousedown="handleTableMouseDown"
      @native-mouseup="handleTableMouseUp"
      @native-mousemove="handleTableMouseMove"
    >
      <template #prepend>
        <el-table-column
          v-if="!showModuleTree"
          key="plan-defect-sidebar-expand-col"
          fixed
          width="30"
          align="center"
          label-class-name="plan-defect-sidebar-expand-header-cell"
          class-name="plan-defect-sidebar-expand-body-cell"
        >
          <template slot="header">
            <el-tooltip :content="$t('case.show-module-tree')" placement="bottom">
              <span
                class="plan-defect-sidebar-expand-trigger"
                role="button"
                tabindex="0"
                @click.stop="emitExpandModuleTree"
                @keyup.enter.stop.prevent="emitExpandModuleTree"
              >
                <svg-icon icon-class="menu" class-name="plan-defect-sidebar-expand-svg" />
              </span>
            </el-tooltip>
          </template>
          <template slot-scope>
            <span class="plan-defect-sidebar-expand-body-placeholder" />
          </template>
        </el-table-column>
      </template>
      <template #columns="{ scope, column }">
        <span v-if="column.prop === 'projectNum'">{{ '#' + scope.row[column.prop] }}</span>
        <defect-type-flag v-else-if="column.prop === 'defectTypeName'" :defect="scope.row" />
        <div v-else-if="column.prop === 'defectName'" class="table-defect-title">
          <focus-member-list
            v-show="scope.row.focusList && scope.row.focusList.length > 0"
            v-model="scope.row.focusList"
            module-name="defect"
            :data-id="scope.row.defectId"
          />
          <el-link type="primary" @click="handleClickTableRow(scope.row)">{{ scope.row.defectName }}</el-link>
          <div class="defect-statistics">
            <div>
              <i class="el-icon-time"></i>
              <span>{{ $t('defect.life-time') }}:</span>
              <span class="defect-statistics-value">{{ defectLife(scope.row) }}</span>
            </div>
            <div>
              <i class="el-icon-document-delete"></i>
              <span>{{ $i18n.t('reject') }}:</span>
              <span class="defect-statistics-value">{{ scope.row.rejectCount }}</span>
            </div>
          </div>
        </div>
        <level-tag v-else-if="column.prop === 'defectLevel'" :options="dict.type.defect_level" :value="scope.row.defectLevel" />
        <defect-state-flag v-else-if="column.prop === 'defectState'" :defect="scope.row" />
        <cat2-bug-preview-image v-else-if="column.prop === 'imgUrls'" :images="getUrl(scope.row.imgUrls)" />
        <div v-else-if="column.prop === 'annexUrls'" class="annex-list">
          <cat2-bug-text :content="file" type="down" :tooltip="file" v-for="(file, index) in getUrl(scope.row.annexUrls)" :key="index" />
        </div>
        <span v-else-if="column.prop === 'updateTime'">{{ parseTime(scope.row.updateTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        <span v-else-if="column.prop === 'planStartTime'">{{ parseTime(scope.row.planStartTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        <span v-else-if="column.prop === 'planEndTime'">{{ parseTime(scope.row.planEndTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        <row-list-member v-else-if="column.prop === 'createMember'" :members="[scope.row.createMember]" />
        <row-list-member v-else-if="column.prop === 'handleBy'" :members="scope.row.handleByList" />
        <span v-else>{{ scope.row[column.prop] }}</span>
      </template>
      <template #append>
        <el-table-column :label="$t('operate')" align="left" class-name="no-drag cat2bug-operate-column" fixed="right">
          <template slot-scope="scope">
            <defect-tools
              class="defect-row-tools cat2bug-operate-tools"
              :exclusions="defectToolsExclusions"
              :is-text="true"
              :defect="scope.row"
              size="mini"
              :is-show-icon="true"
              @delete="handleDelete"
              @update="search"
              @log="search"
            />
          </template>
        </el-table-column>
      </template>
    </cat2-bug-table>
        </div>
    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="search(queryParams)"
    />
      </div>
    </multipane>
  </div>
</template>

<script>
import LevelTag from "@/components/LevelTag";
import Cat2BugText from "@/components/Cat2BugText";
import RowListMember from "@/components/RowListMember";
import Cat2BugPreviewImage from "@/components/Cat2BugPreviewImage";
import FocusMemberList from "@/components/FocusMemberList";
import DefectTypeFlag from "@/components/Defect/DefectTypeFlag";
import DefectStateFlag from "@/components/Defect/DefectStateFlag";
import DefectTools from "@/components/Defect/DefectTools";
import Cat2BugTable from "@/components/Cat2BugTable";
import SelectModule from "@/components/Module/SelectModule";
import SelectProjectMember from "@/components/Project/SelectProjectMember";
import { TableOptions } from "@/views/system/defect/list/table-options";
import {parseTime} from "@/utils/ruoyi";
import {lifeTime} from "@/utils/defect";
import {listDefectOfPlan} from "@/api/system/plan";
import {getDefectState} from "@/api/system/DefectState";
import { Multipane, MultipaneResizer } from "vue-multipane";
import paneResizerHandleViewport from "@/mixins/paneResizerHandleViewport";

const TREE_MODULE_WIDTH_CACHE_KEY = "plan_case_tree_module_width";

/** 用例表排序的列 */
const DEFECT_TABLE_SORT_COLUMN = 'plan-defect_table_sort_column_key';
/** 用例表排序的类型（正序、倒叙） */
const DEFECT_TABLE_SORT_TYPE = 'plan-defect_table_sort_type_key';
export default {
  name: "DefectList",
  dicts: ['defect_level'],
  mixins: [paneResizerHandleViewport],
  components: { Multipane, MultipaneResizer, LevelTag, Cat2BugText, RowListMember, Cat2BugPreviewImage, FocusMemberList, DefectTypeFlag, DefectStateFlag, DefectTools, Cat2BugTable, SelectModule, SelectProjectMember },
  props: {
    showModuleTree: {
      type: Boolean,
      default: true,
    },
  },
  data() {
    return {
      multipaneStyle: {},
      treeModuleStyle: { "--treeModuleWidth": "300px" },
      planDefectSortColumnKey: DEFECT_TABLE_SORT_COLUMN,
      planDefectSortTypeKey: DEFECT_TABLE_SORT_TYPE,
      planDefectTableColumns: TableOptions.map((c) => ({ ...c })),
      columnPickerCheckedKeys: [],
      planDefectColumnPickerRev: 0,
      defectPickerColumnList: null,
      // 鼠标是否点击
      mouseFlag: false,
      // 鼠标移动的偏移量
      mouseOffset: 0,
      loading: false,
      defectToolsExclusions: ['delete'],
      plan: {},
      planItem: {},
      // 是否选择了所有
      isCheckAll: false,
      // 全选组件的状态
      isIndeterminate: false,
      // 勾选的缺陷ID列表
      checkedDefectList: [],
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 总条数
      total: 0,
      // 缺陷表格数据
      defectList: [],
      // 缺陷状态数组
      defectStates: [],
      // 查询条件
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        orderByColumn: 'projectNum',
        isAsc: 'desc',
        defectType: null,
        defectName: null,
        projectId: 0,
        testPlanId: null,
        caseId: null,
        dataSources: null,
        dataSourcesParams: null,
        moduleId: null,
        moduleVersion: null,
        createBy: null,
        updateTime: null,
        createTime: null,
        updateBy: null,
        defectState: null,
        handleBy: null,
        handleTime: null,
        defectLevel: null,
        params:{
          defectStates: null
        }
      },
      deliverableToolbarSyncHeight: null,
      planDrawerTableMaxHeight: null,
    }
  },
  mounted() {
    this.$nextTick(() => this.initPlanDrawerTableScrollResize());
  },
  watch: {
    "$i18n.locale": function () {
      this.$nextTick(() => {
        this.$refs.cat2BugTable && this.$refs.cat2BugTable.doLayout();
        this.$nextTick(() => this.syncDeliverableToolbarWithTableHeader());
      });
    },
  },
  destroyed() {
    this.destroyDeliverableTableHeaderHeightSync();
    this.destroyPlanDrawerTableScrollResize();
  },
  directives: {
    resize: {
      // 指令的名称
      bind(el, binding) {
        // el为绑定的元素，binding为绑定给指令的对象
        let width = ''
        let height = ''
        function isResize() {
          const style = document.defaultView.getComputedStyle(el);
          if (width !== style.width || height !== style.height) {
            binding.value({ width: style.width, height: style.height }) // 关键(这传入的是函数,所以执行此函数)
          }
          width = style.width
          height = style.height
        }
        el.__vueSetInterval__ = setInterval(isResize, 300)
      },
      unbind(el) {
        clearInterval(el.__vueSetInterval__)
      }
    }
  },
  computed: {
    /** 项目ID */
    projectId: function () {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    /** 缺陷批量工具是否可用 */
    batchToolsDisabled: function () {
      return function (defect) {
        return this.checkedDefectList.filter(d=>d==defect.defectId).length==0;
      }
    },
    /** 缺陷的存活时间 */
    defectLife: function () {
      return function (defect) {
        return lifeTime(defect);
      }
    },
    defectColumnPickerOptions() {
      const ordered = this.defectPickerColumnList;
      if (ordered && ordered.length) {
        return ordered.map((c) => ({ ...c }));
      }
      return TableOptions.filter((c) => c.showInColumnPicker !== false);
    },
    getUrl: function () {
      return function (urls){
        let imgs = urls?urls.split(','):[];
        return imgs.map(i=>{
          return process.env.VUE_APP_BASE_API + i;
        })
      }
    },
    getFileName: function () {
      return function (url) {
        if(!url) return null;
        let arr = url.split('\/');
        return arr[arr.length-1];
      }
    },
    paneResizerRuleVars() {
      const h = this.deliverableToolbarSyncHeight;
      return {
        '--multipane-header-rule-offset': h != null && h > 0 ? `${h}px` : '48px',
      };
    },
  },
  created() {
    this.initDefectState();
  },
  methods: {
    parseTime,
    emitExpandModuleTree() {
      this.$emit('expand-module-tree');
    },
    getTreeModuleWidth() {
      const w = this.$cache.session.get(TREE_MODULE_WIDTH_CACHE_KEY);
      this.treeModuleStyle["--treeModuleWidth"] = (w ? w : 300) + "px";
    },
    cacheTreeModuleWidth() {
      if (this.$refs.treeModule) {
        this.$cache.session.set(TREE_MODULE_WIDTH_CACHE_KEY, this.$refs.treeModule.clientWidth);
      }
    },
    dragStopHandle() {
      if (this.showModuleTree && this.$refs.treeModule) {
        this.cacheTreeModuleWidth();
      }
      this.setDragComponentSize();
    },
    /** 初始化缺陷状态 */
    initDefectState() {
      getDefectState().then(res=>{
        this.defectStates = res.data;
      });
    },
    setDragComponentSize() {
      this.$nextTick(() => {
        this.updatePlanDrawerTableMaxHeight();
        this.$nextTick(() => {
          this.$refs.cat2BugTable && this.$refs.cat2BugTable.doLayout && this.$refs.cat2BugTable.doLayout();
          this.scheduleSyncPaneResizerHandle();
          this.$emit("resize");
        });
      });
    },
    updatePlanDrawerTableMaxHeight() {
      const pane = this.$refs.caseContext;
      if (!pane || !pane.clientHeight) return;
      let reserveBelowTable = 0;
      const pagEl = pane.querySelector(".pagination-container");
      if (this.total > 0 && pagEl && pagEl.offsetParent !== null) {
        const st = window.getComputedStyle(pagEl);
        reserveBelowTable =
          pagEl.offsetHeight +
          parseFloat(st.marginTop || "0") +
          parseFloat(st.marginBottom || "0");
      }
      const h = Math.floor(pane.clientHeight - reserveBelowTable - 2);
      if (h < 48) return;
      if (this.planDrawerTableMaxHeight !== h) {
        this.planDrawerTableMaxHeight = h;
      }
    },
    initPlanDrawerTableScrollResize() {
      this.destroyPlanDrawerTableScrollResize();
      const pane = this.$refs.caseContext;
      if (!pane) return;
      if (typeof ResizeObserver !== "undefined") {
        this._planDrawerTableScrollResizeObserver = new ResizeObserver(() => {
          this.updatePlanDrawerTableMaxHeight();
          this.$nextTick(() => {
            this.$refs.cat2BugTable && this.$refs.cat2BugTable.doLayout && this.$refs.cat2BugTable.doLayout();
          });
        });
        this._planDrawerTableScrollResizeObserver.observe(pane);
      }
      this.updatePlanDrawerTableMaxHeight();
    },
    destroyPlanDrawerTableScrollResize() {
      if (this._planDrawerTableScrollResizeObserver) {
        this._planDrawerTableScrollResizeObserver.disconnect();
        this._planDrawerTableScrollResizeObserver = null;
      }
    },
    syncDeliverableToolbarWithTableHeader() {
      this.$nextTick(() => {
        const cat = this.$refs.cat2BugTable;
        const elTable = cat && cat.$refs && cat.$refs.elTable;
        if (!elTable || !elTable.$el) {
          return;
        }
        const headerWrap = elTable.$el.querySelector(".el-table__header-wrapper");
        if (!headerWrap) {
          return;
        }
        if (
          this._deliverableTableHeaderObservedEl &&
          this._deliverableTableHeaderObservedEl !== headerWrap
        ) {
          this.destroyDeliverableTableHeaderHeightSync();
          this.initDeliverableTableHeaderHeightSync();
          return;
        }
        const rect = headerWrap.getBoundingClientRect();
        const h = Math.round(rect.height || headerWrap.offsetHeight || 0);
        if (h < 1) {
          return;
        }
        if (h > 0 && h !== this.deliverableToolbarSyncHeight) {
          this.deliverableToolbarSyncHeight = h;
        }
      });
    },
    initDeliverableTableHeaderHeightSync() {
      this.destroyDeliverableTableHeaderHeightSync();
      const cat = this.$refs.cat2BugTable;
      const elTable = cat && cat.$refs && cat.$refs.elTable;
      if (!elTable || !elTable.$el) {
        this.syncDeliverableToolbarWithTableHeader();
        return;
      }
      const headerWrap = elTable.$el.querySelector(".el-table__header-wrapper");
      if (!headerWrap) {
        this.syncDeliverableToolbarWithTableHeader();
        return;
      }
      this._deliverableTableHeaderObservedEl = headerWrap;
      this.syncDeliverableToolbarWithTableHeader();
      if (typeof ResizeObserver === "undefined") {
        return;
      }
      this._deliverableTableHeaderResizeObserver = new ResizeObserver(() => {
        this.syncDeliverableToolbarWithTableHeader();
      });
      this._deliverableTableHeaderResizeObserver.observe(headerWrap);
    },
    destroyDeliverableTableHeaderHeightSync() {
      if (this._deliverableTableHeaderResizeObserver) {
        this._deliverableTableHeaderResizeObserver.disconnect();
        this._deliverableTableHeaderResizeObserver = null;
      }
      this._deliverableTableHeaderObservedEl = null;
    },
    initFloatMenu() {
      this.$emit('init-float-menu');
    },
    open(planId, projectId, query) {
      this.queryParams.projectId = projectId;
      this.queryParams = {...this.queryParams, ...query}
      this.queryParams.planId = planId;
      if(!this.queryParams.params) {
        this.queryParams.params = {};
      }
      this.getTreeModuleWidth();
      this.init();
      this.search(this.queryParams);
    },
    init() {
      this.$nextTick(() => {
        this.initSort();
      });
    },
    /** 初始化排序数据 */
    initSort() {
      this.queryParams.isAsc = this.$cache.local.get(DEFECT_TABLE_SORT_TYPE) || null;
      this.queryParams.orderByColumn = this.$cache.local.get(DEFECT_TABLE_SORT_COLUMN) || null;
      this.$nextTick(() => {
        this.$refs.cat2BugTable && this.$refs.cat2BugTable.sort(this.queryParams.orderByColumn, this.queryParams.isAsc);
        this.initDeliverableTableHeaderHeightSync();
        this.setDragComponentSize();
      });
    },
    onTableColumnsChange(columns) {
      this.planDefectColumnPickerRev += 1;
      const picker = columns.filter((c) => c.showInColumnPicker !== false).map((c) => ({ ...c }));
      this.$set(this, 'defectPickerColumnList', picker);
      this.columnPickerCheckedKeys = columns
        .filter((c) => c.visible && c.showInColumnPicker !== false)
        .map((c) => c.key);
      this.$nextTick(() => this.syncDeliverableToolbarWithTableHeader());
    },
    onColumnPickerChange(keys) {
      this.$refs.cat2BugTable && this.$refs.cat2BugTable.setColumnsVisible(keys);
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.defectId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 排序改变的处理 */
    sortChangeHandle(e) {
      if(e.order){
        switch (e.prop) {
          case 'defectStateName':
            this.queryParams.orderByColumn='defectState';
            break;
          case 'defectTypeName':
            this.queryParams.orderByColumn='defectType';
            break;
          default:
            this.queryParams.orderByColumn=e.prop;
            break;
        }
        this.queryParams.isAsc=e.order;
      } else {
        this.queryParams.orderByColumn=null;
        this.queryParams.isAsc=null;
      }
      this.queryParams.pageNum = 1;
      this.search(this.queryParams);
    },
    /** 查询缺陷列表 */
    search(params) {
      this.loading = true;
      this.queryParams = params||this.queryParams;
      listDefectOfPlan(this.queryParams.planId, this.queryParams).then(response => {
        this.loading = false;
        this.defectList = response.rows;
        this.total = response.total;
        this.$nextTick(() => {
          this.initDeliverableTableHeaderHeightSync();
          this.syncDeliverableToolbarWithTableHeader();
          this.setDragComponentSize();
        });
      });
    },
    /** 处理删除缺陷 */
    handleDelete() {
      this.search();
      this.$emit('delete');
    },
    /** 显示图片操作 */
    clickImageHandle(event){
      event.stopPropagation();
    },
    /** 处理点击了表格中的某一行 */
    handleClickTableRow(defect) {
      this.$emit('defect-click',defect);
    },
    /** 下载附件操作 */
    handleDown(event, file) {
      const a = document.createElement("a");
      const e = new MouseEvent("click");
      a.href = file;
      a.dispatchEvent(e);
      event.stopPropagation();
    },
    /** 全选组件勾选状态的改变处理 */
    handleCheckAllChange(value) {
      this.checkedDefectList = value ? this.defectList.map(d=>d.defectId) : [];
      this.isIndeterminate = false;
    },
    /** 勾选某个缺陷的改变处理 */
    handleCheckedDefectChange(value) {
      let checkedCount = value.length;
      this.checkAll = checkedCount === this.defectList.length;
      this.isIndeterminate = checkedCount > 0 && checkedCount < this.defectList.length;

    },
    /** 阻止冒泡事件传递处理 */
    handleStopPropagation(event) {
      event.stopPropagation();
    },

    /** 处理鼠标在表格点下事件 */
    handleTableMouseDown(e) {
      this.mouseOffset = e.clientX;
      this.mouseFlag = true;
    },
    /** 处理鼠标在表格点起事件 */
    handleTableMouseUp(e) {
      this.mouseFlag = false;
    },
    /** 处理鼠标在表格移动事件 */
    handleTableMouseMove(e) {
      const elTable = this.$refs.cat2BugTable && this.$refs.cat2BugTable.$refs.elTable;
      if (!elTable) return;
      let tableBody = elTable.bodyWrapper;
      if (this.mouseFlag) {
        // 设置水平方向的元素的位置
        tableBody.scrollLeft -= (- this.mouseOffset + (this.mouseOffset = e.clientX));
      }
    },
    /** 处理计划项状态搜索事件 */
    handlePlanItemStateSearch(state) {

    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/assets/styles/multipane-resizer-grip.scss";

.plan-item-drawer-layout {
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
  height: 100%;
}
.plan-drawer-defect-multipane.custom-resizer {
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
  align-items: stretch;
  overflow: hidden;
  /* 左右边距由 HandlePlanDialog .plan-run-content 统一为 10px */
  padding-left: 0;
  padding-right: 0;
  box-sizing: border-box;
  user-select: none;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
}
.custom-resizer--tree-hidden > .plan-item-content-list {
  flex: 1 1 100%;
  width: 100%;
}
.custom-resizer > .multipane-resizer {
  flex-shrink: 0;
  margin: 0 0 0 -8px;
  left: 4px;
  width: 8px;
  cursor: col-resize;
  position: relative;
  box-sizing: border-box;
  z-index: 350;
  background-image: linear-gradient(#dfe6ec, #dfe6ec);
  background-size: 100% 1px;
  background-position: 0 calc(var(--multipane-header-rule-offset, 48px) - 1px);
  background-repeat: no-repeat;
  @include multipane-resizer-vertical-appearance;
}
.tree-module {
  width: var(--treeModuleWidth);
  max-width: 75%;
  flex-shrink: 0;
  align-self: stretch;
  display: flex;
  flex-direction: column;
  min-height: 0;
  box-sizing: border-box;
  overflow: hidden;
}
.tree-module ::v-deep > .tree {
  flex: 1 1 0%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.plan-item-content {
  flex-grow: 1;
  overflow:hidden;
  height: 100%;
  padding-bottom: 0;
}
.plan-drawer-defect-list-pane.plan-item-content-list {
  flex: 1 1 0%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
/* 高度随表格内容走；纵向上限由 planDrawerTableMaxHeight（测父列减分页）交给 el-table */
.plan-drawer-table-scroll {
  flex: 0 1 auto;
  align-self: stretch;
  width: 100%;
  min-width: 0;
  overflow-x: auto;
  overflow-y: hidden;
}
.plan-drawer-defect-list-pane ::v-deep .el-table__body-wrapper,
.plan-drawer-defect-list-pane ::v-deep .el-table__fixed-body-wrapper {
  background-color: #fff !important;
}
/* ruoyi.scss 给分页容器 height:25px + 内部 absolute，预留高度不足会压住上方表格 */
.plan-drawer-defect-list-pane ::v-deep .pagination-container {
  flex-shrink: 0;
  margin-bottom: 30px !important;
  height: auto !important;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  flex-wrap: wrap;
}
.plan-drawer-defect-list-pane ::v-deep .pagination-container .el-pagination {
  position: relative !important;
  right: auto !important;
}
.plan-item-query {
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
  width: 100%;
  flex-shrink: 0;
  box-sizing: border-box;
  margin-bottom: 8px;
  .el-form-item {
    margin-bottom: 5px;
  }
}
.plan-item-query.defect-table-tools.defect-table-tools-bar {
  flex-wrap: wrap;
  row-gap: 8px;
  column-gap: 12px;
  align-content: flex-start;
  align-items: center;
}
/* 与缺陷页 .defect-tools-search：勿设 width:100%，否则占满整行会把右侧工具挤换行 */
.plan-query-main {
  flex: 1 1 auto;
  min-width: 0;
  max-width: 100%;
  box-sizing: border-box;
}
.plan-query-main ::v-deep .list-switch {
  flex: 0 0 auto;
  flex-shrink: 0;
  white-space: nowrap;
  margin-bottom: 0 !important;
  display: inline-flex !important;
  align-items: center;
}
.plan-query-main .plan-query-inline-form.el-form--inline {
  display: flex !important;
  flex-wrap: wrap;
  align-items: center;
  row-gap: 8px;
  column-gap: 8px;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}
.plan-query-main ::v-deep .el-form--inline .el-form-item {
  flex: 0 1 auto;
  min-width: 0;
  margin-right: 0 !important;
  margin-bottom: 0 !important;
  display: flex;
  align-items: center;
}
.plan-query-main ::v-deep .el-form--inline .el-form-item.plan-query-switch-item {
  flex: 0 0 auto;
  min-width: 0;
}
.plan-query-main ::v-deep .plan-query-switch-item .el-form-item__content {
  width: auto !important;
  margin-left: 0 !important;
}
@media screen and (max-width: 576px) {
  .plan-query-main {
    width: 100%;
    max-width: 100%;
  }
  .plan-query-main ::v-deep .el-form--inline .el-form-item:not(.plan-query-switch-item) {
    flex: 1 1 100%;
    max-width: 100%;
  }
  .plan-query-main ::v-deep .el-form-item__content {
    width: 100% !important;
    max-width: 100%;
    box-sizing: border-box;
  }
  .plan-query-main ::v-deep .el-input,
  .plan-query-main ::v-deep .el-select,
  .plan-query-main ::v-deep .select-project-member-input {
    width: 100% !important;
    max-width: 100%;
    box-sizing: border-box;
  }
}
.handle-plan-tools-right {
  display: inline-flex;
  flex-direction: row;
  flex-shrink: 0;
  gap: 10px;
}
.plan-item-field-divider {
  margin: 8px 0px;
}
.row {
  display: flex;
  flex-direction: row;
  align-items: center;
  > * {
    margin: 0px 5px 0px 0px;
  }
}
.col {
  display: flex;
  flex-direction: column;
}
.defect-field-divider {
  margin: 8px 0px;
}
.plan-defect-sidebar-expand-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  padding: 0;
  box-sizing: border-box;
  cursor: pointer;
  color: #909399;
  font-size: 12px;
  line-height: 1;
  flex-shrink: 0;
  border-radius: 4px;
}
.plan-defect-sidebar-expand-trigger:hover {
  color: #409eff;
  background-color: #ecf5ff;
}
.plan-defect-sidebar-expand-trigger:focus-visible {
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.35);
}
.plan-defect-sidebar-expand-trigger ::v-deep .plan-defect-sidebar-expand-svg {
  width: 12px;
  height: 12px;
  vertical-align: middle;
  display: block;
}
::v-deep .plan-defect-sidebar-expand-header-cell {
  padding: 0 !important;
  text-align: center;
}
::v-deep .plan-defect-sidebar-expand-header-cell .cell {
  padding: 0 !important;
  overflow: visible !important;
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
}
::v-deep .el-table__fixed-header-wrapper .plan-defect-sidebar-expand-header-cell .cell {
  overflow: visible !important;
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
}
::v-deep .plan-defect-sidebar-expand-body-cell .cell {
  padding: 4px 0 !important;
}
.plan-defect-sidebar-expand-body-placeholder {
  display: block;
  width: 1px;
  height: 1px;
  visibility: hidden;
}
</style>
