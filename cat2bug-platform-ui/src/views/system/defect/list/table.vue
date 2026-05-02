<template>
  <div class="defect-table-root">
    <!-- 查询与表格工具栏独占一行，下方左侧交付物树与右侧表格顶端对齐（与用例页一致） -->
    <div class="defect-table-tools defect-table-tools-bar">
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
        <div>
          <slot name="right-tools"></slot>
        </div>
      </div>
    </div>
    <multipane
      layout="vertical"
      ref="multiPane"
      class="defect-table-multipane"
      :class="{ 'defect-table-multipane--tree-hidden': !showModuleTree }"
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
      <multipane-resizer v-show="showModuleTree" :style="multipaneStyle"></multipane-resizer>
      <div ref="defectTableContext" class="defect-table-context">
        <cat2-bug-table
          ref="cat2BugTable"
          cache-key="defect-table"
          :columns="tableColumnDefaults"
          :data="defectList"
          :loading="loading"
          v-resize="setDragComponentSize"
          @sort-change="sortChangeHandle"
          @columns-change="onTableColumnsChange">
          <template #prepend>
            <el-table-column
              v-if="!showModuleTree"
              key="defect-sidebar-expand-col"
              fixed
              width="40"
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

const DEFECT_TREE_MODULE_WIDTH_CACHE_KEY = "defect_tree_module_width";
/** 缺陷列表左侧交付物树是否展开（本地缓存） */
const DEFECT_TREE_MODULE_VISIBLE_CACHE_KEY = "defect_tree_module_visible";

export default {
  name: "DefectTable",
  dicts: ['defect_level'],
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
      multipaneStyle: { "--marginTop": "0px" },
      treeModuleStyle: { "--treeModuleWidth": "300px" },
      /** 是否显示左侧交付物列表 */
      showModuleTree: true,
      /** 缺陷列表表格默认列（克隆自 table-options，避免与全局常量引用互相污染） */
      tableColumnDefaults: TableOptions.map(c => ({ ...c })),
      columnPickerCheckedKeys: [],
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
      return TableOptions.filter(c => c.showInColumnPicker !== false);
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
    this.$nextTick(() => this.setDragComponentSize());
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
    setDragComponentSize() {
      this.multipaneStyle["--marginTop"] = "0px";
      this.$nextTick(() => {
        const treeH = this.$refs.treeModule ? (this.$refs.treeModule.scrollHeight || 0) : 0;
        const ctxH = this.$refs.defectTableContext ? (this.$refs.defectTableContext.scrollHeight || 0) : 0;
        const pageHeight = Math.max(treeH, ctxH, document.body.scrollHeight - 170);
        this.multipaneStyle["--marginTop"] = pageHeight + "px";
      });
    },
    onTableColumnsChange(columns) {
      this.columnPickerCheckedKeys = columns.filter(c => c.visible && c.showInColumnPicker !== false).map(c => c.key);
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
.defect-table-root {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: stretch;
}
.defect-table-tools-bar {
  flex-shrink: 0;
  width: 100%;
}
.defect-table-multipane {
  flex: 1;
  min-height: 0;
  width: 100%;
  height: auto;
  user-select: none;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
}
.defect-table-multipane--tree-hidden > .defect-table-context {
  flex: 1 1 100%;
  width: 100%;
}
.defect-table-multipane > .multipane-resizer {
  margin: 0;
  left: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  width: 8px;
  cursor: col-resize;
  position: relative;
  &:before {
    display: block;
    content: "";
    width: 1px;
    height: var(--marginTop);
    background-color: #dcdfe6;
  }
  &:after {
    content: "";
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 4px;
    height: 30px;
    border-left: 1px solid #c0c4cc;
    border-right: 1px solid #c0c4cc;
    border-radius: 2px;
  }
  &:hover {
    &:before {
      background-color: #b0b0b0;
    }
    &:after {
      border-color: #909399;
    }
  }
}
.defect-tree-module {
  width: var(--treeModuleWidth);
  max-width: 75%;
  flex-shrink: 0;
}
.defect-table-context {
  flex-grow: 1;
  min-width: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.defect-sidebar-expand-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
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
  width: 14px;
  height: 14px;
  vertical-align: middle;
  display: block;
}
::v-deep .defect-sidebar-expand-header-cell {
  padding: 0 !important;
  text-align: center;
}
::v-deep .defect-sidebar-expand-header-cell .cell {
  padding: 0 4px !important;
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
  padding: 4px 2px !important;
}
.defect-sidebar-expand-body-placeholder {
  display: block;
  width: 1px;
  height: 1px;
  visibility: hidden;
}
@media screen and (max-width: 980px) {
  .defect-table-tools {
    justify-content: flex-end;
  }
}
@media screen and (min-width: 980px) {
  .defect-table-tools {
    justify-content: space-between;
  }
}
.defect-table-tools {
  display: flex;
  flex-direction: row;
  align-items: center;
  > * {
    display: inline-block;
    justify-content: flex-start;
    margin-bottom: 0px;
    ::v-deep .el-form-item {
      margin-bottom: 0px;
    }
  }
  .table-tools {
    align-items: flex-start;
    padding-top: 3px;
    > * {
      margin-bottom: 10px;
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
.table-defect-title {
  display: inline-flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  .el-link {
    flex: 1;
    padding-left: 5px;
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
