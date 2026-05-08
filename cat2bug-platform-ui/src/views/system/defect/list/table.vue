<template>
  <div class="defect-table-root">
    <!-- 查询与表格工具栏独占一行，下方左侧交付物树与右侧表格顶端对齐（与用例页一致） -->
    <div class="defect-table-tools defect-table-tools-bar defect-view-toolbar">
      <slot name="left-tools"></slot>
      <div class="table-tools row">
        <el-popover placement="top" trigger="click">
          <div class="row">
            <i class="el-icon-s-fold"></i>
            <h4>{{ $t('display-field') }}</h4>
          </div>
          <el-divider class="defect-field-divider"></el-divider>
          <el-checkbox-group v-model="columnPickerCheckedKeys" class="defect-column-picker" @change="onColumnPickerChange">
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
        <slot name="right-tools"></slot>
      </div>
    </div>
    <multipane
      layout="vertical"
      ref="multiPane"
      class="custom-resizer"
      :class="{ 'custom-resizer--tree-hidden': !showModuleTree }"
      @paneResizeStop="dragStopHandle"
    >
      <div v-if="showModuleTree" ref="treeModule" class="defect-tree-module" :style="treeModuleStyle">
        <tree-module
          ref="treeModuleRef"
          :project-id="projectId"
          :show-sidebar-toggle="true"
          @toggle-sidebar="toggleModuleTreeVisible"
          @node-click="moduleTreeClickHandle"
          v-resize="setDragComponentSize"
        />
      </div>
      <multipane-resizer ref="paneResizer" v-show="showModuleTree" :style="multipaneStyle"></multipane-resizer>
      <div ref="defectTableContext" class="defect-table-context">
        <!-- 宽表横向滚动限制在本层，避免整页 main-container 底部出现横向条与分页同一底边叠在一起「压住」分页 -->
        <div class="defect-table-x-scroll">
        <cat2-bug-table
          ref="cat2BugTable"
          cache-key="defect-table"
          :columns="tableColumnDefaults"
          :data="defectList"
          :loading="loading"
          :enable-header-sort="false"
          v-resize="setDragComponentSize"
          @sort-change="sortChangeHandle"
          @columns-change="onTableColumnsChange">
          <template #prepend>
            <el-table-column
              v-if="!showModuleTree"
              key="defect-sidebar-expand-col"
              fixed
              width="30"
              align="center"
              label-class-name="defect-sidebar-expand-header-cell"
              class-name="defect-sidebar-expand-body-cell">
              <template slot="header">
                <el-tooltip :content="$t('case.show-module-tree')" placement="bottom">
                  <span
                    class="defect-sidebar-expand-trigger"
                    role="button"
                    tabindex="0"
                    @click.stop="toggleModuleTreeVisible"
                    @keyup.enter.stop.prevent="toggleModuleTreeVisible"
                  >
                    <svg-icon icon-class="menu" class-name="defect-sidebar-expand-svg" />
                  </span>
                </el-tooltip>
              </template>
              <template slot-scope>
                <span class="defect-sidebar-expand-body-placeholder" />
              </template>
            </el-table-column>
          </template>
          <template #columns="{ scope, column }">
            <span v-if="column.prop==='projectNum'" >{{ '#' + scope.row[column.prop] }}</span>
            <defect-type-flag v-else-if="column.prop==='defectTypeName'" :defect="scope.row" />
            <div v-else-if="column.prop==='defectName'" class="table-defect-title">
              <focus-member-list
                v-show="scope.row.focusList && scope.row.focusList.length>0"
                v-model="scope.row.focusList"
                module-name="defect"
                :data-id="scope.row.defectId" />
              <el-link type="primary" @click="handleClickTableRow(scope.row)">{{ scope.row.defectName }}</el-link>
              <div class="defect-statistics">
                <div>
                  <i class="el-icon-time"></i>
                  <span>{{ $t('defect.life-time') }}:</span>
                  <span class="defect-statistics-value">{{defectLife(scope.row)}}</span>
                </div>
                <div>
                  <i class="el-icon-document-delete"></i>
                  <span>{{$i18n.t('reject')}}:</span>
                  <span class="defect-statistics-value">{{scope.row.rejectCount}}</span>
                </div>
              </div>
            </div>
            <level-tag v-else-if="column.prop==='defectLevel'" :options="dict.type.defect_level" :value="scope.row.defectLevel"/>
            <defect-state-flag v-else-if="column.prop==='defectState'" :defect="scope.row" />
            <cat2-bug-preview-image v-else-if="column.prop==='imgUrls'" :images="getUrl(scope.row.imgUrls)" />
            <div v-else-if="column.prop==='annexUrls'" class="annex-list">
              <cat2-bug-text :content="file" type="down" :tooltip="file" v-for="(file,index) in getUrl(scope.row.annexUrls)" :key="index" />
            </div>
            <span v-else-if="column.prop==='updateTime'">{{ parseTime(scope.row.updateTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
            <span v-else-if="column.prop==='planStartTime'">{{ parseTime(scope.row.planStartTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
            <span v-else-if="column.prop==='planEndTime'">{{ parseTime(scope.row.planEndTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
            <row-list-member v-else-if="column.prop==='createMember'" :members="[scope.row.createMember]"></row-list-member>
            <row-list-member v-else-if="column.prop==='handleBy'" :members="scope.row.handleByList"></row-list-member>
            <span v-else>{{ scope.row[column.prop] }}</span>
          </template>
          <template #append>
            <el-table-column :label="$t('operate')" align="center" class-name="no-drag small-padding fixed-width" min-width="250" fixed="right">
              <template slot-scope="scope">
                <div class="row">
                  <defect-tools class="defect-row-tools" :is-text="true" :defect="scope.row" size="mini" :is-show-icon="true" @view="handleClickTableRow" @delete="refreshSearch" @update="refreshSearch" @log="refreshSearch"></defect-tools>
                </div>
              </template>
            </el-table-column>
          </template>
        </cat2-bug-table>
        </div>

        <div v-show="total>0" class="defect-table-pagination-band">
          <pagination
            class="defect-table-pagination"
            :total="total"
            :page.sync="queryParams.pageNum"
            :limit.sync="queryParams.pageSize"
            @pagination="search(queryParams)"
          />
        </div>
      </div>
    </multipane>
  </div>
</template>

<script>
import { Multipane, MultipaneResizer } from "vue-multipane";
import TreeModule from "@/components/Module/TreeModule";
import LevelTag from "@/components/LevelTag";
import Cat2BugText from "@/components/Cat2BugText";
import RowListMember from "@/components/RowListMember";
import Cat2BugPreviewImage from "@/components/Cat2BugPreviewImage";
import FocusMemberList from "@/components/FocusMemberList";
import DefectTypeFlag from "@/components/Defect/DefectTypeFlag";
import DefectStateFlag from "@/components/Defect/DefectStateFlag";
import DefectTools from "@/components/Defect/DefectTools";
import Cat2BugTable from "@/components/Cat2BugTable";
import {listDefect} from "@/api/system/defect";
import {lifeTime} from "@/utils/defect";
import { TableOptions } from "@/views/system/defect/list/table-options";
import paneResizerHandleViewport from "@/mixins/paneResizerHandleViewport";

const DEFECT_TREE_MODULE_WIDTH_CACHE_KEY = "defect_tree_module_width";
/** 缺陷列表左侧交付物树是否展开（本地缓存） */
const DEFECT_TREE_MODULE_VISIBLE_CACHE_KEY = "defect_tree_module_visible";

export default {
  name: "DefectTable",
  dicts: ['defect_level'],
  mixins: [paneResizerHandleViewport],
  components: {
    Multipane,
    MultipaneResizer,
    TreeModule,
    RowListMember,
    Cat2BugPreviewImage,
    LevelTag,
    FocusMemberList,
    DefectTypeFlag,
    DefectStateFlag,
    DefectTools,
    Cat2BugText,
    Cat2BugTable
  },
  directives: {
    resize: {
      bind(el, binding) {
        let width = "";
        let height = "";
        function isResize() {
          const style = document.defaultView.getComputedStyle(el);
          if (width !== style.width || height !== style.height) {
            binding.value({ width: style.width, height: style.height });
          }
          width = style.width;
          height = style.height;
        }
        el.__vueSetInterval__ = setInterval(isResize, 300);
      },
      unbind(el) {
        clearInterval(el.__vueSetInterval__);
      }
    }
  },
  data() {
    return {
      mouseFlag: false,
      mouseOffset: 0,
      mouseClickTime: 0,
      isCheckAll: false,
      isIndeterminate: false,
      checkedDefectList: [],
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      total: 0,
      defectList: [],
      queryParams: this.query,
      treeModuleStyle: { "--treeModuleWidth": "300px" },
      /** 与用例页一致：拖动条中间竖线高度由树/表格区域较大 scrollHeight 决定 */
      multipaneStyle: { "--marginTop": "0px" },
      /** 是否显示左侧交付物列表 */
      showModuleTree: true,
      /** 缺陷列表表格默认列（克隆自 table-options，避免与全局常量引用互相污染） */
      tableColumnDefaults: TableOptions.map(c => ({ ...c })),
      columnPickerCheckedKeys: [],
      /** 与 Cat2BugTable 列顺序一致，供「显示字段」勾选列表排序（含 Excel 拖列写回缓存后） */
      defectPickerColumnList: null,
    }
  },
  props: {
    query: {
      type: Object,
      default: () => {
        return {
          pageNum: 1,
          pageSize: 10,
          orderByColumn: 'projectNum',
          isAsc: 'desc',
          defectType: null,
          defectName: null,
          nameVersionKeyword: null,
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
          params:{}
        }
      }
    },
    projectId: {
      type: Number,
      default: null
    }
  },
  computed: {
    defectColumnPickerOptions() {
      const ordered = this.defectPickerColumnList;
      if (ordered && ordered.length) {
        return ordered.map((c) => ({ ...c }));
      }
      return TableOptions.filter((c) => c.showInColumnPicker !== false);
    },
    defectLife: function () {
      return function (defect) {
        return lifeTime(defect);
      }
    },
    getUrl: function () {
      return function (urls){
        if(urls) {
          let files = urls ? urls.split(',') : [];
          return files.map(i => {
            return process.env.VUE_APP_BASE_API + i;
          })
        } else {
          return [];
        }
      }
    },
    getFileName: function () {
      return function (url) {
        if(!url) return null;
        let arr = url.split('\/');
        return arr[arr.length-1];
      }
    },
  },
  watch: {
    "$i18n.locale": function () {
      this.$nextTick(() => {
        this.$refs.cat2BugTable && this.$refs.cat2BugTable.doLayout();
      });
    },
  },
  created() {
    const treeVis = this.$cache.local.get(DEFECT_TREE_MODULE_VISIBLE_CACHE_KEY);
    this.showModuleTree = !(treeVis === "0" || treeVis === "false");
  },
  mounted() {
    this.getTreeModuleWidth();
    this.$nextTick(() => {
      this.setDragComponentSize();
    });
  },
  methods: {
    init() {
      this.$nextTick(() => {
        if (this.$refs.treeModuleRef) {
          this.$refs.treeModuleRef.reloadData();
        }
        this.setDragComponentSize();
      });
    },
    /** 与顶部 SelectModule 一致：按交付物 moduleId 筛选缺陷 */
    moduleTreeClickHandle(moduleId) {
      const id = (moduleId !== undefined && moduleId !== null && moduleId !== "") ? moduleId : null;
      this.queryParams.moduleId = id;
      this.queryParams.pageNum = 1;
      this.search(this.queryParams);
    },
    getTreeModuleWidth() {
      const w = this.$cache.session.get(DEFECT_TREE_MODULE_WIDTH_CACHE_KEY);
      this.treeModuleStyle["--treeModuleWidth"] = (w ? w : 300) + "px";
    },
    cacheTreeModuleWidth() {
      if (this.$refs.treeModule) {
        this.$cache.session.set(DEFECT_TREE_MODULE_WIDTH_CACHE_KEY, this.$refs.treeModule.clientWidth);
      }
    },
    dragStopHandle() {
      if (this.showModuleTree) {
        this.cacheTreeModuleWidth();
      }
      this.setDragComponentSize();
    },
    toggleModuleTreeVisible() {
      this.showModuleTree = !this.showModuleTree;
      this.$cache.local.set(DEFECT_TREE_MODULE_VISIBLE_CACHE_KEY, this.showModuleTree ? "1" : "0");
      this.$nextTick(() => {
        this.setDragComponentSize();
        if (this.$refs.cat2BugTable) {
          this.$refs.cat2BugTable.doLayout();
        }
      });
    },
    /** 与用例页 setDragComponentSize 一致：拖动条竖线高度随树/表格区域增高 */
    setDragComponentSize() {
      this.multipaneStyle["--marginTop"] = "0px";
      this.$nextTick(() => {
        const treeH = this.$refs.treeModule ? (this.$refs.treeModule.scrollHeight || 0) : 0;
        const ctx = this.$refs.defectTableContext;
        const ctxH = ctx ? (ctx.scrollHeight || 0) : 0;
        /* 勿用 document.body.scrollHeight：会把竖线撑到整页，multipane 行被拉高，未满一屏时出现内层滚动与分页下大片空白 */
        const pageHeight = Math.max(treeH, ctxH, 8);
        this.multipaneStyle["--marginTop"] = pageHeight + "px";
        this.$refs.cat2BugTable && this.$refs.cat2BugTable.doLayout && this.$refs.cat2BugTable.doLayout();
        this.scheduleSyncPaneResizerHandle();
      });
    },
    onTableColumnsChange(columns) {
      this.columnPickerCheckedKeys = columns
        .filter((c) => c.visible && c.showInColumnPicker !== false)
        .map((c) => c.key);
      this.defectPickerColumnList = columns.filter((c) => c.showInColumnPicker !== false).map((c) => ({ ...c }));
    },
    onColumnPickerChange(keys) {
      this.$refs.cat2BugTable && this.$refs.cat2BugTable.setColumnsVisible(keys);
    },
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
    search(params) {
      this.loading = true;
      this.queryParams = params;
      listDefect(params).then(response => {
        this.loading = false;
        this.defectList = response.rows;
        this.total = response.total;
        this.$nextTick(() => this.setDragComponentSize());
      });
    },
    refreshSearch() {
      this.$emit('refresh');
    },
    clickImageHandle(event){
      event.stopPropagation();
    },
    handleClickTableRow(defect) {
      if(defect && defect.defectId)
        this.$emit('defect-click',defect);
    },
    handleDown(event, file) {
      const a = document.createElement("a");
      const e = new MouseEvent("click");
      a.href = file;
      a.dispatchEvent(e);
      event.stopPropagation();
    },
  }
}
</script>

<style lang="scss" scoped>
@import "~@/assets/styles/multipane-resizer-grip.scss";

.defect-table-root {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  flex: 1 1 0%;
  min-height: 0;
}
/*
 * 表体区域：按内容增高，max-height 不超过父级，行数多时在本层纵向滚动。
 * 勿用 flex:1 撑满父列：会在表底与分页之间塞入大块空白，改变「列表—分页」间距观感。
 */
.defect-table-x-scroll {
  flex: 0 1 auto;
  min-width: 0;
  min-height: 0;
  max-height: 100%;
  width: 100%;
  overflow-x: auto;
  overflow-y: auto;
  /* 固定列 / 宽表时确保横向滚动发生在本层，减轻外层出现双横条 */
  -webkit-overflow-scrolling: touch;
}
.defect-table-tools-bar {
  flex-shrink: 0;
  width: 100%;
}
/* 与用例页共用 multipane-resizer-grip：轨道 + 圆角手柄，竖线高度 --marginTop */
.custom-resizer.multipane {
  flex: 1 1 0%;
  width: 100%;
  min-width: 0;
  min-height: 0;
  height: auto;
  align-items: stretch;
  user-select: none;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
}
.custom-resizer--tree-hidden > .defect-table-context {
  flex: 1 1 100%;
  width: 100%;
}
.custom-resizer > .multipane-resizer {
  flex-shrink: 0;
  /* 与 vue-multipane 竖向布局一致：负 margin + left 一半宽度，条骑在左右列缝上，不占一整列留白 */
  margin: 0 0 0 -8px;
  left: 4px;
  display: flex;
  justify-content: center;
  /* 竖线贴顶，与左右交付物树/表格同起点，避免分隔列上方一条白缝 */
  align-items: flex-start;
  height: 100%;
  width: 8px;
  cursor: col-resize;
  position: relative;
  box-sizing: border-box;
  z-index: 350;
  @include multipane-resizer-vertical-appearance;
}
.defect-tree-module {
  width: var(--treeModuleWidth);
  max-width: 75%;
  flex-shrink: 0;
  align-self: stretch;
  display: flex;
  flex-direction: column;
  min-height: 0;
  box-sizing: border-box;
}
.defect-tree-module ::v-deep > .tree {
  flex: 1 1 0%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.defect-tree-module ::v-deep .el-tree {
  flex: 1 1 0%;
  min-height: 0;
  overflow-y: auto;
}
.defect-table-context {
  flex: 1 1 0%;
  min-width: 0;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: hidden;
  display: flex;
  flex-direction: column;
  /*
   * 分页上下留白：父级 defect-page 底 padding 为 0（竖线可贴底），此处将 --defect-page-bottom-pad 置 0，
   * margin-bottom 计算比原先少减 20px，等效为分页区相对底部多留约 20px，与原先「页底 padding + 分页边距」观感一致；safe-area 仍参与 calc。
   */
  --defect-pagination-v-gap: 28px;
  --defect-page-bottom-pad: 0px;
  /* 分页整体略上移，加大与页面内容底的间距 */
  --defect-pagination-extra-bottom: 14px;
}
.defect-table-pagination-band {
  flex-shrink: 0;
  margin-top: var(--defect-pagination-v-gap);
  margin-bottom: max(
    0px,
    calc(
      var(--defect-pagination-v-gap) - var(--defect-page-bottom-pad) -
        env(safe-area-inset-bottom, 0px) + var(--defect-pagination-extra-bottom)
    )
  );
}
::v-deep .defect-table-pagination.pagination-container {
  margin-top: 0 !important;
  margin-bottom: 0 !important;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  padding-left: 16px;
  padding-right: 20px;
}
.defect-sidebar-expand-trigger {
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
.defect-sidebar-expand-trigger:hover {
  color: #409eff;
  background-color: #ecf5ff;
}
.defect-sidebar-expand-trigger:focus-visible {
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.35);
}
.defect-sidebar-expand-trigger ::v-deep .defect-sidebar-expand-svg {
  width: 12px;
  height: 12px;
  vertical-align: middle;
  display: block;
}
::v-deep .defect-sidebar-expand-header-cell {
  padding: 0 !important;
  text-align: center;
}
::v-deep .defect-sidebar-expand-header-cell .cell {
  padding: 0 !important;
  overflow: visible !important;
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
}
::v-deep .el-table__fixed-header-wrapper .defect-sidebar-expand-header-cell .cell {
  overflow: visible !important;
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
}
::v-deep .defect-sidebar-expand-body-cell .cell {
  padding: 4px 0 !important;
}
.defect-sidebar-expand-body-placeholder {
  display: block;
  width: 1px;
  height: 1px;
  visibility: hidden;
}
.defect-table-tools {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  align-items: center;
  align-content: flex-start;
  row-gap: 8px;
  column-gap: 12px;
  > * {
    display: block;
    justify-content: flex-start;
    margin-bottom: 0px;
    ::v-deep .el-form-item {
      margin-bottom: 0px;
    }
  }
  .table-tools {
    align-items: center;
    > * {
      margin-bottom: 0;
    }
  }
}
.defect-field-divider {
  margin: 8px 0px;
}
/** 显示字段：勿限制 checkbox-group 宽高（原先 15px 会导致选项重叠错乱） */
.defect-column-picker {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 220px;
  max-height: 380px;
  overflow-y: auto;
  padding-right: 4px;
}
.defect-column-picker ::v-deep .el-checkbox {
  display: flex;
  align-items: center;
  margin-right: 0;
  white-space: nowrap;
}
.defect-column-picker ::v-deep .el-checkbox__input {
  flex-shrink: 0;
}
.defect-column-picker ::v-deep .el-checkbox__label {
  line-height: 1.4;
  padding-left: 8px;
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
/* 缺陷名称列：覆盖 el-table .cell 默认 nowrap，使标题内 \\n 在表格中换行展示 */
.defect-table-root ::v-deep td.defect-name-col .cell {
  white-space: pre-line !important;
  word-break: break-word;
}
.table-defect-title {
  display: inline-flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  .el-link {
    flex: 1;
    padding-left: 5px;
    white-space: inherit;
    word-break: break-word;
    line-height: 1.45;
  }
}
.el-table {
  ::v-deep table {
    width: 100% !important;
  }
}
.defect-statistics {
  display: inline-flex;
  flex-direction: row;
  gap: 10px;
  font-size: 10px;
  > div {
    padding: 0px 5px;
    border-radius: 3px;
    background-color: #f9fbff;
  }
  i {
    margin-right: 2px;
  }
  .defect-statistics-value {
    padding-left: 3px;
    font-size: 11px;
    color: #303133;
  }
}
.annex-link {
  white-space: normal;
  text-align: center;
  word-break: break-all;
}
.defect-row-tools {
  margin-left: 10px;
}
.annex-list {
  display: inline-flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  > *:last-child {
    border-bottom: 0px;
  }
  > * {
    border-bottom: 1px dashed #E4E7ED;
  }
}
</style>
