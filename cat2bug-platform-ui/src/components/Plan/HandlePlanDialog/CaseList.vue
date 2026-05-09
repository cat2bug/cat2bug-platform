<template>
  <div class="plan-item-content plan-item-drawer-layout">
    <!-- 与缺陷 list/table.vue：查询条独占一行 -->
    <div class="plan-item-query defect-table-tools defect-table-tools-bar">
      <div class="row">
        <slot name="query"></slot>
        <el-form :model="query" ref="queryForm" size="small" :inline="true">
          <el-form-item label="" prop="caseName">
            <el-input
              size="small"
              v-model="query.params.caseName"
              :placeholder="$t('case.please-enter-title')"
              prefix-icon="el-icon-search"
              clearable
              @input="getPlanItemList()"
            />
          </el-form-item>
          <el-form-item label="" prop="planItemState">
            <el-select v-model="query.planItemState" :placeholder="$t('plan.enter-item-state')" clearable @change="getPlanItemList">
              <el-option
                v-for="dict in dict.type.plan_item_state"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
          <!--              <el-form-item label="" prop="updateById">-->
          <!--                <el-input-->
          <!--                  v-model="query.updateById"-->
          <!--                  placeholder="请输入更新人"-->
          <!--                  clearable-->
          <!--                  @keyup.enter.native="handleQuery"-->
          <!--                />-->
          <!--              </el-form-item>-->
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
          <el-divider class="plan-item-field-divider"></el-divider>
          <el-checkbox-group
            :key="'plan-case-colpick-' + planCaseColumnPickerRev"
            v-model="columnPickerCheckedKeys"
            class="col"
            @change="onPlanCaseColumnPickerChange"
          >
            <el-checkbox v-for="c in planCaseColumnPickerOptions" :key="c.key" :label="c.key">{{ $t(c.key) }}</el-checkbox>
          </el-checkbox-group>
          <el-button
            style="padding: 9px;"
            plain
            slot="reference"
            icon="el-icon-s-fold"
            size="small"
          ></el-button>
        </el-popover>
        <el-col :span="1.5">
          <el-button
            size="small"
            type="primary"
            icon="el-icon-check"
            :disabled="!multiple"
            v-hasPermi="['system:plan:edit']"
            @click="handlePlanItemState(null, 'pass')"
          >{{ $t('batch-pass') }}</el-button>
        </el-col>
      </div>
    </div>
    <!-- 下方：左侧交付物树 + 右侧表格（与缺陷页 multipane 一致） -->
    <multipane
      layout="vertical"
      ref="multiPane"
      class="custom-resizer plan-drawer-case-multipane"
      :class="{ 'custom-resizer--tree-hidden': !showModuleTree }"
      @paneResizeStop="dragStopHandle"
    >
      <div v-if="showModuleTree" class="tree-module" ref="treeModule" :style="treeModuleStyle">
        <slot name="tree" :toolbar-sync-height="deliverableToolbarSyncHeight" />
      </div>
      <multipane-resizer ref="paneResizer" v-show="showModuleTree" :style="[multipaneStyle, paneResizerRuleVars]" />
      <div ref="caseContext" class="plan-item-content-list plan-drawer-case-list-pane">
        <div ref="planDrawerTableScroll" class="plan-drawer-table-scroll">
    <cat2-bug-table
      ref="cat2BugTable"
      :table-max-height="planDrawerTableMaxHeight"
      cache-key="plan-item-case"
      field-list-cache-key="plan-item-table-field-list"
      :sort-column-cache-key="planItemSortColumnKey"
      :sort-type-cache-key="planItemSortTypeKey"
      :columns="planCaseTableColumns"
      :data="planItemList"
      :loading="loading"
      v-resize="setDragComponentSize"
      @sort-change="handleSortChange"
      @selection-change="handleSelectionChange"
      @columns-change="onPlanCaseColumnsChange"
      @native-mousedown="handleTableMouseDown"
      @native-mouseup="handleTableMouseUp"
      @native-mousemove="handleTableMouseMove"
    >
      <template #prepend>
        <el-table-column
          v-if="!showModuleTree"
          key="plan-case-sidebar-expand-col"
          fixed
          width="30"
          align="center"
          label-class-name="plan-case-sidebar-expand-header-cell"
          class-name="plan-case-sidebar-expand-body-cell"
        >
          <template slot="header">
            <el-tooltip :content="$t('case.show-module-tree')" placement="bottom">
              <span
                class="plan-case-sidebar-expand-trigger"
                role="button"
                tabindex="0"
                @click.stop="emitExpandModuleTree"
                @keyup.enter.stop.prevent="emitExpandModuleTree"
              >
                <svg-icon icon-class="menu" class-name="plan-case-sidebar-expand-svg" />
              </span>
            </el-tooltip>
          </template>
          <template slot-scope>
            <span class="plan-case-sidebar-expand-body-placeholder" />
          </template>
        </el-table-column>
        <el-table-column type="selection" width="50" align="center" />
      </template>
      <template #columns="{ scope, column }">
        <span v-if="column.prop === 'caseNum'">{{ caseNumber(scope.row) }}</span>
        <dict-tag v-else-if="column.prop === 'planItemState'" :options="dict.type.plan_item_state" :value="scope.row.planItemState" />
        <div v-else-if="column.prop === 'caseName'" class="table-case-title">
          <cat2-bug-text
            :type="checkPermi(['system:case:edit']) ? 'link' : 'text'"
            v-model="scope.row.caseName + ''"
            :tooltip="scope.row.caseName"
            @click="handleOpenEditCase(scope.row)"
          />
        </div>
        <cat2-bug-level v-else-if="column.prop === 'caseLevel'" :level="scope.row.caseLevel" />
        <cat2-bug-text v-else-if="column.prop === 'casePreconditions'" v-model="scope.row.casePreconditions" :tooltip="scope.row.casePreconditions" />
        <div v-else-if="column.prop === 'caseStep'" class="table-row-full-height">
          <step :steps="scope.row.caseStep" />
        </div>
        <cat2-bug-text v-else-if="column.prop === 'caseExpect'" v-model="scope.row.caseExpect" :tooltip="scope.row.caseExpect" />
        <cat2-bug-preview-image v-else-if="column.prop === 'imgUrls'" :images="getUrl(scope.row.imgUrls)" />
        <div v-else-if="column.prop === 'annexUrls'" class="annex-list">
          <cat2-bug-text :content="file" type="down" :tooltip="file" v-for="(file, index) in getUrl(scope.row.annexUrls)" :key="index" />
        </div>
        <row-list-member v-else-if="column.prop === 'updateBy'" :members="member(scope.row)" />
        <span v-else-if="column.prop === 'updateTime'">{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
        <span v-else>{{ scope.row[column.prop] }}</span>
      </template>
      <template #append>
        <el-table-column :label="$t('operate')" align="start" class-name="no-drag small-padding fixed-width" fixed="right" width="270">
          <template slot-scope="scope">
            <div class="plan-operate">
              <plan-item-tools v-model="scope.row" :plan="parentPlan" :project-id="projectId" @change="getPlanItemList" @close="initFloatMenu" />
            </div>
          </template>
        </el-table-column>
      </template>
    </cat2-bug-table>
        </div>
    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="query.pageNum"
      :limit.sync="query.pageSize"
      @pagination="getPlanItemList"
    />
    <handle-case-of-plan ref="handleCaseDialog" :module-id="planItem.moduleId" :append-to-body="true" @change="getPlanItemList" @close="initFloatMenu" />
      </div>
    </multipane>
  </div>
</template>

<script>
import HandleCaseOfPlan from "@/components/Plan/HandleCaseOfPlan";
import PlanItemTools from "@/components/Plan/PlanItemTools";
import Cat2BugText from "@/components/Cat2BugText";
import Cat2BugPreviewImage from "@/components/Cat2BugPreviewImage";
import Cat2BugLevel from "@/components/Cat2BugLevel";
import Cat2BugTable from "@/components/Cat2BugTable";
import DictTag from "@/components/DictTag";
import RowListMember from "@/components/RowListMember";
import Step from "@/views/system/case/components/step";
import { PlanItemCaseTableOptions } from "@/components/Plan/HandlePlanDialog/plan-item-case-table-options";
import {listPlanItem, updatePlanItem} from "@/api/system/PlanItem";
import {parseTime} from "@/utils/ruoyi";
import {checkPermi} from "@/utils/permission";
import { Multipane, MultipaneResizer } from "vue-multipane";
import paneResizerHandleViewport from "@/mixins/paneResizerHandleViewport";

const TREE_MODULE_WIDTH_CACHE_KEY = "plan_case_tree_module_width";

/** 测试子项不通过的状态key值 */
const PLAN_ITEM_STATE_NOT_PASS = 'not_pass';
/** 计划项排序的列 */
const PLAN_ITEM_SORT_COLUMN = 'plan_item_sort_column_key';
/** 计划项排序的类型（正序、倒叙） */
const PLAN_ITEM_SORT_TYPE = 'plan_item_sort_type_key';
export default {
  name: "CaseList",
  dicts: ['plan_item_state'],
  mixins: [paneResizerHandleViewport],
  components: { Multipane, MultipaneResizer, HandleCaseOfPlan, PlanItemTools, Cat2BugText, Cat2BugPreviewImage, Cat2BugLevel, Cat2BugTable, DictTag, RowListMember, Step },
  props: {
    /** 与缺陷页 table.vue：为 false 时在表格首列显示「展开交付物树」 */
    showModuleTree: {
      type: Boolean,
      default: true,
    },
  },
  data() {
    return {
      multipaneStyle: {},
      treeModuleStyle: { "--treeModuleWidth": "300px" },
      planItemSortColumnKey: PLAN_ITEM_SORT_COLUMN,
      planItemSortTypeKey: PLAN_ITEM_SORT_TYPE,
      planCaseTableColumns: PlanItemCaseTableOptions.map((c) => ({ ...c })),
      columnPickerCheckedKeys: [],
      planCaseColumnPickerRev: 0,
      planCasePickerColumnList: null,
      // 鼠标是否点击
      mouseFlag: false,
      // 鼠标移动的偏移量
      mouseOffset: 0,
      loading: false,
      // 是否多选
      multiple: false,
      plan: {},
      planItem: {},
      projectId: null,
      // 查询参数
      query: {
        pageNum: 1,
        pageSize: 10,
        moduleId: null,
        planId: null,
        projectId: this.projectId,
        params:{}
      },
      // 测试计划子项表格数据
      planItemList: [],
      // 总条数
      total: 0,
      // 表格的多选项
      ids: [],
      /** 交付物 Tab 标题区高度（px），与右侧表头对齐 */
      deliverableToolbarSyncHeight: null,
      /** 计划抽屉表格区域高度（px），用于 el-table max-height 固定表头 */
      planDrawerTableMaxHeight: null,
    }
  },
  mounted() {
    this.$nextTick(() => this.initPlanDrawerTableScrollResize());
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
  computed: {
    planCaseColumnPickerOptions() {
      const ordered = this.planCasePickerColumnList;
      if (ordered && ordered.length) {
        return ordered.map((c) => ({ ...c }));
      }
      return PlanItemCaseTableOptions.filter((c) => c.showInColumnPicker !== false);
    },
    parentPlan() {
      return this.$parent && this.$parent.plan ? this.$parent.plan : this.plan;
    },
    /** 用于显示的用例编号 */
    caseNumber: function () {
      return function (val) {
        return '#'+val.caseNum;
      }
    },
    /** 成员结构 */
    member: function () {
      return function (planItem) {
        return [{
          nickName: planItem.updateBy
        }]
      }
    },
    /** 字符转url数组 */
    getUrl: function () {
      return function (urls){
        let imgs = urls?urls.split(','):[];
        return imgs.map(i=>{
          return process.env.VUE_APP_BASE_API + i;
        })
      }
    },
    /** 分隔条上画出与表头底边对齐的横线（缝补树标题与表格 thead 之间空隙） */
    paneResizerRuleVars() {
      const h = this.deliverableToolbarSyncHeight;
      return {
        '--multipane-header-rule-offset': h != null && h > 0 ? `${h}px` : '48px',
      };
    },
  },
  created() {
    this.query.isAsc = this.$cache.local.get(PLAN_ITEM_SORT_TYPE) || null;
    this.query.orderByColumn = this.$cache.local.get(PLAN_ITEM_SORT_COLUMN) || null;
  },
  methods: {
    checkPermi,
    parseTime,
    emitExpandModuleTree() {
      this.$emit('expand-module-tree');
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
      const wrap = this.$refs.planDrawerTableScroll;
      if (!wrap) return;
      const h = Math.floor(wrap.clientHeight);
      if (h < 48) return;
      if (this.planDrawerTableMaxHeight !== h) {
        this.planDrawerTableMaxHeight = h;
      }
    },
    initPlanDrawerTableScrollResize() {
      this.destroyPlanDrawerTableScrollResize();
      const wrap = this.$refs.planDrawerTableScroll;
      if (!wrap) return;
      if (typeof ResizeObserver !== "undefined") {
        this._planDrawerTableScrollResizeObserver = new ResizeObserver(() => {
          this.updatePlanDrawerTableMaxHeight();
          this.$nextTick(() => {
            this.$refs.cat2BugTable && this.$refs.cat2BugTable.doLayout && this.$refs.cat2BugTable.doLayout();
          });
        });
        this._planDrawerTableScrollResizeObserver.observe(wrap);
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
        /* 用整块 header-wrapper 高度对齐左侧 Tab 外框（含边框），避免仅 thead tr 与 Tab height:border-box 差 1px */
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
      this.projectId = projectId;
      this.query.planId = planId;
      this.query.projectId = projectId;
      this.query = { ...this.query, ...query };
      this.getTreeModuleWidth();
      this.$nextTick(() => {
        this.$refs.cat2BugTable && this.$refs.cat2BugTable.sort(this.query.orderByColumn, this.query.isAsc);
        this.initDeliverableTableHeaderHeightSync();
        this.setDragComponentSize();
      });
      this.getPlanItemList();
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
    onPlanCaseColumnsChange(columns) {
      this.planCaseColumnPickerRev += 1;
      const picker = columns.filter((c) => c.showInColumnPicker !== false).map((c) => ({ ...c }));
      this.$set(this, 'planCasePickerColumnList', picker);
      this.columnPickerCheckedKeys = columns
        .filter((c) => c.visible && c.showInColumnPicker !== false)
        .map((c) => c.key);
      this.$nextTick(() => this.syncDeliverableToolbarWithTableHeader());
    },
    onPlanCaseColumnPickerChange(keys) {
      this.$refs.cat2BugTable && this.$refs.cat2BugTable.setColumnsVisible(keys);
    },
    refreshPlanHeader() {
      if (this.$parent && typeof this.$parent.getPlanInfo === 'function' && this.query.planId) {
        this.$parent.getPlanInfo(this.query.planId, false);
      }
    },
    /** 查询测试用例列表 */
    getPlanItemList() {
      this.loading = true;
      listPlanItem(this.query).then(response => {
        this.loading = false;
        this.planItemList = response.rows.map(i=>{
          i.defectList=[];
          return i;
        });
        this.total = response.total;
        this.$nextTick(() => {
          this.initDeliverableTableHeaderHeightSync();
          this.syncDeliverableToolbarWithTableHeader();
          this.setDragComponentSize();
        });
      });
    },
    /** 处理添加缺陷完成操作 */
    handleAddedDefect(defect) {
      let data = {
        planId: this.planItem.planId,
        planItemId: this.planItem.planItemId,
        params: {
          defectId: defect.defectId
        },
        planItemState: PLAN_ITEM_STATE_NOT_PASS,
      }
      updatePlanItem(data).then(res=>{
        this.refreshPlanHeader();
        this.getPlanItemList();
        this.$emit('change');
      })
    },
    /** 处理缺陷日志添加完成操作 */
    handleDefectLogAdded(log) {
      let data = {
        planItemId: this.planItem.planItemId,
        params: {
          defectId: log.defectId
        },
        planItemState: PLAN_ITEM_STATE_NOT_PASS,
      }
      updatePlanItem(data).then(res=>{
        this.refreshPlanHeader();
        this.getPlanItemList();
        this.$emit('change');
      });
    },
    /** 更改子项状态操作 */
    handlePlanItemState(planItem, state) {
      let data = {
        planItemState: state,
      }
      if(planItem) {
        data.planItemId = planItem.planItemId;
      } else if(this.ids.length>0) {
        data.params = {
          planItemIds:this.ids
        }
      }
      updatePlanItem(data).then(()=>{
        this.$message.success(this.$i18n.t('plan.pass-success').toString());
        this.getPlanItemList();
        this.$emit('change');
      });
    },
    handleSortChange(column) {
      this.query.isAsc = column.order;
      this.query.orderByColumn = column.prop;
      this.getPlanItemList();
    },
    /** 查询计划项 */
    handlePlanItemStateSearch(state) {
      this.query.planItemState = state;
      this.query.pageNum = 1;
      this.query.moduleId = null;
      this.getPlanItemList();
    },
    /** 下载附件操作 */
    handleDown(event, file) {
      const a = document.createElement("a");
      const e = new MouseEvent("click");
      a.href = file;
      a.dispatchEvent(e);
      event.stopPropagation();
    },
    /** 多选框选中数据 */
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.planItemId);
      this.multiple = selection.length;
    },
    /** 打开编辑用例窗口 */
    handleOpenEditCase(planItem) {
      this.$refs.handleCaseDialog.open(this.parentPlan, planItem, planItem.caseId, this.query);
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
.plan-drawer-case-multipane.custom-resizer {
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
  align-items: stretch;
  overflow: hidden;
  padding-left: 5px;
  padding-right: 5px;
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
  overflow: hidden;
  height: 100%;
  padding-bottom: 0;
}
.plan-drawer-case-list-pane.plan-item-content-list {
  flex: 1 1 0%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
/* 占满表格与分页之间的剩余高度；纵向滚动交给 el-table（table-max-height），以固定表头 */
.plan-drawer-table-scroll {
  flex: 1 1 0%;
  align-self: stretch;
  width: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}
/* 表体底色；表头 gutter / 滚动槽见 assets/styles/el-table-scrollbar.scss */
.plan-drawer-case-list-pane ::v-deep .el-table__body-wrapper,
.plan-drawer-case-list-pane ::v-deep .el-table__fixed-body-wrapper {
  background-color: #fff !important;
}
/* ruoyi.scss 给分页容器 height:25px + 内部 absolute，预留高度不足会压住上方表格 */
.plan-drawer-case-list-pane ::v-deep .pagination-container {
  flex-shrink: 0;
  margin-bottom: 30px !important;
  height: auto !important;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  flex-wrap: wrap;
}
.plan-drawer-case-list-pane ::v-deep .pagination-container .el-pagination {
  position: relative !important;
  right: auto !important;
}
.plan-item-query {
  display: inline-flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: flex-start;
  width: 100%;
  flex-shrink: 0;
  box-sizing: border-box;
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
.handle-plan-tools-right {
  display: inline-flex;
  flex-direction: row;
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
.table-row-full-height {
  position: absolute;
  top: 0px;
  left: 0;
  right: 0;
  bottom: 0;
  display: inline-flex;
  align-items: flex-start;
  padding: 5px 10px;
  overflow-y: auto;
  overflow-x: hidden;
  > .step {
    display: inline-flex;
    max-width: 300px;
    min-height: 100%;
    justify-content: center;
  }
}
/* 与缺陷 list/table.vue 表头展开侧栏列一致 */
.plan-case-sidebar-expand-trigger {
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
.plan-case-sidebar-expand-trigger:hover {
  color: #409eff;
  background-color: #ecf5ff;
}
.plan-case-sidebar-expand-trigger:focus-visible {
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.35);
}
.plan-case-sidebar-expand-trigger ::v-deep .plan-case-sidebar-expand-svg {
  width: 12px;
  height: 12px;
  vertical-align: middle;
  display: block;
}
::v-deep .plan-case-sidebar-expand-header-cell {
  padding: 0 !important;
  text-align: center;
}
::v-deep .plan-case-sidebar-expand-header-cell .cell {
  padding: 0 !important;
  overflow: visible !important;
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
}
::v-deep .el-table__fixed-header-wrapper .plan-case-sidebar-expand-header-cell .cell {
  overflow: visible !important;
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
}
::v-deep .plan-case-sidebar-expand-body-cell .cell {
  padding: 4px 0 !important;
}
.plan-case-sidebar-expand-body-placeholder {
  display: block;
  width: 1px;
  height: 1px;
  visibility: hidden;
}
</style>
