<template>
  <div class="defect-table-root">
    <!-- 查询与表格工具栏独占一行，下方左侧交付物树与右侧表格顶端对齐（与用例页一致） -->
    <div class="defect-table-tools defect-table-tools-bar defect-view-toolbar">
      <slot name="left-tools"></slot>
      <div class="table-tools row">
        <el-popover placement="top" trigger="click" popper-class="defect-column-picker-popover" @show="emitColumnPickerVisible(true)" @hide="emitColumnPickerVisible(false)">
          <div class="row">
            <i class="el-icon-s-fold"></i>
            <h4>{{ $t('display-field') }}</h4>
          </div>
          <el-divider class="defect-field-divider"></el-divider>
          <el-checkbox-group
            :key="'defect-colpick-' + defectColumnPickerRev"
            v-model="columnPickerCheckedKeys"
            class="defect-column-picker"
            @change="onColumnPickerChange"
          >
            <el-checkbox
              v-for="c in defectColumnPickerOptions"
              :key="c.key"
              :label="c.key"
            >{{ c.isCustomField && c.fieldLabel ? c.fieldLabel : (c.label || $t(c.key)) }}</el-checkbox>
          </el-checkbox-group>
          <el-button
            style="padding: 9px;"
            plain
            class="defect-list-hint-columns"
            slot="reference"
            icon="el-icon-s-fold"
            size="small"
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
          :toolbar-sync-height="defectTreeToolbarHeight"
          @toggle-sidebar="toggleModuleTreeVisible"
          @node-click="moduleTreeClickHandle"
          v-resize="setDragComponentSize"
        />
      </div>
      <multipane-resizer ref="paneResizer" v-show="showModuleTree" :style="[multipaneStyle, paneResizerRuleVars]"></multipane-resizer>
      <div
        ref="defectTableContext"
        v-loading="loading"
        class="defect-table-context"
        :element-loading-text="$t('loading')"
      >
        <!-- 宽表横向滚动限制在本层，避免整页 main-container 底部出现横向条与分页同一底边叠在一起「压住」分页 -->
        <div class="defect-table-x-scroll">
        <cat2-bug-table
          v-if="defectTableColumnsReady"
          ref="cat2BugTable"
          cache-key="defect-table"
          preserve-column-order
          :columns="resolvedDefectTableColumns"
          :data="defectList"
          :operate-column-max-width="250"
          :table-max-height="defectTableBodyMaxHeight"
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
            <span
              v-if="column.prop==='projectNum'"
              class="defect-row-kbd-hint-anchor"
              :data-defect-id="scope.row.defectId"
            >{{ '#' + scope.row[column.prop] }}</span>
            <defect-type-flag v-else-if="column.prop==='defectTypeName'" :defect="scope.row" />
            <div v-else-if="column.prop==='defectName'" class="table-defect-title">
              <el-link type="primary" :title="scope.row.defectName" @click="handleClickTableRow(scope.row)">{{ scope.row.defectName }}</el-link>
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
            <cat2-bug-preview-image
              v-else-if="column.prop==='imgUrls'"
              class="defect-list-cell-image"
              :images="getUrl(scope.row.imgUrls)"
            />
            <div v-else-if="column.prop==='annexUrls'" class="annex-list">
              <cat2-bug-text :content="file" type="down" :tooltip="file" v-for="(file,index) in getUrl(scope.row.annexUrls)" :key="index" />
            </div>
            <span v-else-if="column.prop==='updateTime'">{{ parseTime(scope.row.updateTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
            <span v-else-if="column.prop==='planStartTime'">{{ parseTime(scope.row.planStartTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
            <span v-else-if="column.prop==='planEndTime'">{{ parseTime(scope.row.planEndTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
            <row-list-member v-else-if="column.prop==='createMember'" :members="[scope.row.createMember]"></row-list-member>
            <row-list-member v-else-if="column.prop==='handleBy'" :members="scope.row.handleByList"></row-list-member>
            <cat2-bug-preview-image
              v-else-if="column.customFieldKey && isCustomFieldType(column, 'image')"
              class="defect-list-cell-image"
              :images="customFieldUrls(scope.row, column)"
            />
            <div v-else-if="column.customFieldKey && isCustomFieldType(column, 'file')" class="annex-list">
              <cat2-bug-text
                :content="file"
                type="down"
                :tooltip="file"
                v-for="(file, index) in customFieldUrls(scope.row, column)"
                :key="index"
              />
            </div>
            <span
              v-else-if="column.customFieldKey"
              :style="customFieldEnumCellStyle(scope.row, column)"
            >{{ formatCustomFieldCell(scope.row, column) }}</span>
            <span v-else>{{ scope.row[column.prop] }}</span>
          </template>
          <template #append>
            <el-table-column
              :label="$t('operate')"
              align="left"
              class-name="no-drag cat2bug-operate-column"
              fixed="right">
              <template slot-scope="scope">
                <div class="defect-operate-cell cat2bug-operate-tools">
                  <defect-tools class="defect-row-tools" :is-text="true" :compact="true" :defect="scope.row" size="mini" :is-show-icon="true" @view="handleClickTableRow" @delete="refreshSearch" @restore="refreshSearch" @update="refreshSearch" @log="refreshSearch"></defect-tools>
                  <focus-member-list
                    v-show="scope.row.focusList && scope.row.focusList.length>0"
                    v-model="scope.row.focusList"
                    module-name="defect"
                    :data-id="scope.row.defectId"
                  />
                </div>
              </template>
            </el-table-column>
          </template>
        </cat2-bug-table>
        </div>

        <div v-show="total>0" ref="defectPaginationBand" class="defect-table-pagination-band table-pagination-band">
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
import {
  buildDefectTableColumnDefaults,
  clearCustomFieldColumnsCache,
  loadDefectColumnLayout,
  pruneDefectTableColumnCacheFromColumns,
  resolveDefectTableOrderByColumn,
  syncNewDefectTableColumnsIntoFieldListCache
} from "@/utils/defect-custom-field-columns";
import {
  defectDisplayFieldCheckedKeys,
  defectDisplayFieldPickerOptions,
  resolveDefectMergedColumns
} from "@/utils/defect-display-field";
import { enumOptionTextStyle, enumOptions, formatCustomFieldValue } from "@/components/DefectCustomField/format";
import paneResizerHandleViewport from "@/mixins/paneResizerHandleViewport";
import multipaneTreeTableHeightSync from "@/mixins/multipaneTreeTableHeightSync";

const DEFECT_TREE_MODULE_WIDTH_CACHE_KEY = "defect_tree_module_width";
/** 缺陷列表左侧交付物树是否展开（本地缓存） */
const DEFECT_TREE_MODULE_VISIBLE_CACHE_KEY = "defect_tree_module_visible";

export default {
  name: "DefectTable",
  dicts: ['defect_level'],
  mixins: [paneResizerHandleViewport, multipaneTreeTableHeightSync],
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
      /** 递增序号：忽略过期的 listDefect 响应，避免 Tab 快速切换时列表错乱 */
      defectListRequestGen: 0,
      queryParams: this.query,
      treeModuleStyle: { "--treeModuleWidth": "300px" },
      multipaneStyle: {},
      /** 是否显示左侧交付物列表 */
      showModuleTree: true,
      /** 缺陷列表表格默认列（克隆自 table-options，避免与全局常量引用互相污染） */
      tableColumnDefaults: TableOptions.map(c => ({ ...c })),
      columnPickerCheckedKeys: [],
      defectColumnPickerRev: 0,
      /** 拖列后的列顺序快照，供「显示字段」列表即时排序（$cache 非响应式） */
      defectColumnPickerLiveColumns: null,
      /** 字段管理列布局已加载后再挂载表格，避免 TableOptions 默认序写入缓存 */
      defectTableColumnsReady: false,
      /** 交付物列表标题栏高度（px），与右侧缺陷表 thead 行高对齐 */
      defectTreeToolbarHeight: null,
      /** 表体 max-height（与用例页一致：表体内纵向滚动） */
      defectTableBodyMaxHeight: null,
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
    resolvedDefectTableColumns() {
      if (!this.defectTableColumnsReady) return [];
      return resolveDefectMergedColumns(this.$cache.local, this.tableColumnDefaults);
    },
    defectColumnPickerOptions() {
      void this.defectColumnPickerRev;
      const src =
        this.defectColumnPickerLiveColumns && this.defectColumnPickerLiveColumns.length
          ? this.defectColumnPickerLiveColumns
          : this.resolvedDefectTableColumns;
      return defectDisplayFieldPickerOptions(src);
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
    paneResizerRuleVars() {
      const h = this.defectTreeToolbarHeight;
      return {
        '--multipane-header-rule-offset': h != null && h > 0 ? `${h}px` : '48px',
      };
    },
  },
  watch: {
    projectId(val, oldVal) {
      if (val !== oldVal) {
        clearCustomFieldColumnsCache();
        this.loadCustomFieldColumns();
      }
    },
    "$i18n.locale": function () {
      this.$nextTick(() => {
        this.$refs.cat2BugTable && this.$refs.cat2BugTable.doLayout();
        this.$nextTick(() => this.syncDefectTreeToolbarWithTableHeader());
      });
    },
  },
  created() {
    const treeVis = this.$cache.local.get(DEFECT_TREE_MODULE_VISIBLE_CACHE_KEY);
    this.showModuleTree = !(treeVis === "0" || treeVis === "false");
  },
  mounted() {
    this.getTreeModuleWidth();
    this.loadCustomFieldColumns();
    this.$nextTick(() => {
      this.initDefectTableHeaderHeightSync();
      this.initDefectTableBodyResizeObserver();
      this.setDragComponentSize();
    });
  },
  destroyed() {
    this.destroyDefectTableHeaderHeightSync();
    this.destroyDefectTableBodyResizeObserver();
  },
  methods: {
    emitColumnPickerVisible(visible) {
      this.$emit('column-picker-visible-change', visible)
    },
    isCustomFieldType(column, fieldType) {
      const meta = column && column.customFieldMeta;
      return !!(meta && meta.fieldType === fieldType);
    },
    customFieldUrls(row, column) {
      const key = column.customFieldKey;
      const cf = row.customFields;
      const val = cf && typeof cf === 'object' ? cf[key] : undefined;
      if (val == null || val === '') return [];
      const parts = Array.isArray(val) ? val : String(val).split(',');
      const base = process.env.VUE_APP_BASE_API || '';
      return parts
        .map((s) => String(s).trim())
        .filter(Boolean)
        .map((u) => (u.startsWith('http') ? u : base + u));
    },
    formatCustomFieldCell(row, column) {
      const key = column.customFieldKey;
      const meta = column.customFieldMeta;
      if (meta && (meta.fieldType === 'image' || meta.fieldType === 'file')) {
        return '';
      }
      const cf = row.customFields;
      const val = cf && typeof cf === 'object' ? cf[key] : undefined;
      return formatCustomFieldValue(meta, val);
    },
    customFieldEnumCellStyle(row, column) {
      const meta = column.customFieldMeta;
      if (!meta || meta.fieldType !== 'enum') return {};
      const key = column.customFieldKey;
      const cf = row.customFields;
      const val = cf && typeof cf === 'object' ? cf[key] : undefined;
      const opt = enumOptions(meta).find(o => o.key === val);
      return enumOptionTextStyle(opt);
    },
    loadCustomFieldColumns() {
      clearCustomFieldColumnsCache();
      this.defectTableColumnsReady = false;
      const baseOptions = TableOptions.map(c => ({ ...c }));
      if (!this.projectId) {
        this.tableColumnDefaults = baseOptions;
        this.defectTableColumnsReady = true;
        this.applyTableColumnsToCat2BugTable();
        return;
      }
      loadDefectColumnLayout(this.projectId, { force: true }).then(layout => {
        this.tableColumnDefaults = buildDefectTableColumnDefaults(baseOptions, layout);
        pruneDefectTableColumnCacheFromColumns(this.$cache.local, this.tableColumnDefaults);
        this.defectTableColumnsReady = true;
        this.$nextTick(() => {
          syncNewDefectTableColumnsIntoFieldListCache(this.$cache.local, this.tableColumnDefaults);
          this.applyTableColumnsToCat2BugTable();
        });
      });
    },
    applyTableColumnsToCat2BugTable() {
      this.$nextTick(() => {
        const table = this.$refs.cat2BugTable;
        if (!table || !table.setColumns) return;
        const merged = this.resolvedDefectTableColumns.map(c => ({ ...c }));
        table.setColumns(merged);
        this.defectColumnPickerLiveColumns = merged.map(c => ({ ...c }));
        this.columnPickerCheckedKeys = defectDisplayFieldCheckedKeys(merged);
        this.defectColumnPickerRev += 1;
      });
    },
    init() {
      this.loadCustomFieldColumns();
      this.$nextTick(() => {
        if (this.$refs.treeModuleRef) {
          this.$refs.treeModuleRef.reloadData();
        }
        this.initDefectTableHeaderHeightSync();
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
        this.$nextTick(() => this.syncDefectTreeToolbarWithTableHeader());
      });
    },
    /** 表格 doLayout + 分隔条手柄（竖线随 resizer 高度置底） */
    setDragComponentSize() {
      this.syncDefectTableBodyMaxHeight();
      this.$nextTick(() => {
        this.$refs.cat2BugTable && this.$refs.cat2BugTable.doLayout && this.$refs.cat2BugTable.doLayout();
        this.syncMultipaneTreeTableHeight('cat2BugTable');
        this.scheduleSyncPaneResizerHandle();
      });
    },
    initDefectTableBodyResizeObserver() {
      if (typeof ResizeObserver === "undefined") return;
      this.destroyDefectTableBodyResizeObserver();
      const el = this.$refs.defectTableContext;
      if (!el) return;
      this._defectTableBodyResizeObserver = new ResizeObserver(() => {
        this.syncDefectTableBodyMaxHeight();
      });
      this._defectTableBodyResizeObserver.observe(el);
    },
    destroyDefectTableBodyResizeObserver() {
      if (this._defectTableBodyResizeObserver) {
        this._defectTableBodyResizeObserver.disconnect();
        this._defectTableBodyResizeObserver = null;
      }
    },
    syncDefectTableBodyMaxHeight() {
      this.$nextTick(() => {
        const body = this.$refs.defectTableContext;
        if (!body || !body.clientHeight) return;
        const pagEl = this.$refs.defectPaginationBand;
        let reserveBelowTable = 0;
        if (this.total > 0 && pagEl && pagEl.offsetParent !== null) {
          const st = window.getComputedStyle(pagEl);
          reserveBelowTable =
            pagEl.offsetHeight +
            parseFloat(st.marginTop || "0") +
            parseFloat(st.marginBottom || "0");
        }
        const next = Math.max(120, Math.floor(body.clientHeight - reserveBelowTable - 2));
        if (this.defectTableBodyMaxHeight !== next) {
          this.defectTableBodyMaxHeight = next;
          this.$nextTick(() => {
            const tbl = this.$refs.cat2BugTable;
            if (tbl && typeof tbl.doLayout === "function") tbl.doLayout();
          });
        }
      });
    },
    syncDefectTreeToolbarWithTableHeader() {
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
          this._defectTableHeaderObservedEl &&
          this._defectTableHeaderObservedEl !== headerWrap
        ) {
          this.destroyDefectTableHeaderHeightSync();
          this.initDefectTableHeaderHeightSync();
          return;
        }
        const tr = headerWrap.querySelector("thead tr");
        if (!tr) {
          return;
        }
        const rect = tr.getBoundingClientRect();
        const h = Math.round(rect.height || tr.offsetHeight || 0);
        if (h > 0 && h !== this.defectTreeToolbarHeight) {
          this.defectTreeToolbarHeight = h;
        }
      });
    },
    initDefectTableHeaderHeightSync() {
      this.destroyDefectTableHeaderHeightSync();
      const cat = this.$refs.cat2BugTable;
      const elTable = cat && cat.$refs && cat.$refs.elTable;
      if (!elTable || !elTable.$el) {
        this.syncDefectTreeToolbarWithTableHeader();
        return;
      }
      const headerWrap = elTable.$el.querySelector(".el-table__header-wrapper");
      if (!headerWrap) {
        this.syncDefectTreeToolbarWithTableHeader();
        return;
      }
      this._defectTableHeaderObservedEl = headerWrap;
      this.syncDefectTreeToolbarWithTableHeader();
      if (typeof ResizeObserver === "undefined") {
        return;
      }
      this._defectTableHeaderResizeObserver = new ResizeObserver(() => {
        this.syncDefectTreeToolbarWithTableHeader();
      });
      this._defectTableHeaderResizeObserver.observe(headerWrap);
    },
    destroyDefectTableHeaderHeightSync() {
      if (this._defectTableHeaderResizeObserver) {
        this._defectTableHeaderResizeObserver.disconnect();
        this._defectTableHeaderResizeObserver = null;
      }
      this._defectTableHeaderObservedEl = null;
    },
    onTableColumnsChange(columns) {
      this.defectColumnPickerLiveColumns = (columns || []).map(c => ({ ...c }));
      this.defectColumnPickerRev += 1;
      this.columnPickerCheckedKeys = defectDisplayFieldCheckedKeys(columns);
      this.$nextTick(() => this.syncDefectTreeToolbarWithTableHeader());
    },
    onColumnPickerChange(keys) {
      this.columnPickerCheckedKeys = keys || [];
      this.$refs.cat2BugTable && this.$refs.cat2BugTable.setColumnsVisible(this.columnPickerCheckedKeys);
    },
    sortChangeHandle(e) {
      if (e.order) {
        const resolved = resolveDefectTableOrderByColumn(e.prop);
        if (!resolved) {
          this.queryParams.orderByColumn = null;
          this.queryParams.isAsc = null;
          this.$refs.cat2BugTable && this.$refs.cat2BugTable.clearSort();
        } else {
          this.queryParams.orderByColumn = resolved;
          this.queryParams.isAsc = e.order;
        }
      } else {
        this.queryParams.orderByColumn = null;
        this.queryParams.isAsc = null;
      }
      this.queryParams.pageNum = 1;
      this.search(this.queryParams);
    },
    search(params) {
      const gen = ++this.defectListRequestGen
      this.loading = true
      this.queryParams = params
      this.syncDefectTableBodyMaxHeight()
      listDefect(params).then(response => {
        if (gen !== this.defectListRequestGen) return
        this.defectList = response.rows
        this.total = response.total
        this.$nextTick(() => {
          this.syncDefectTreeToolbarWithTableHeader()
          this.setDragComponentSize()
        })
      }).catch(() => {
        if (gen !== this.defectListRequestGen) return
      }).finally(() => {
        if (gen === this.defectListRequestGen) {
          this.loading = false
        }
      })
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
 * 与用例页 .case-table-x-scroll：高度随表格，勿 flex:1 撑满否则分页被顶到视口底
 */
.defect-table-x-scroll {
  min-width: 0;
  width: 100%;
  overflow-x: auto;
  overflow-y: hidden;
  -webkit-overflow-scrolling: touch;
  background: transparent;
}
.defect-table-tools-bar {
  flex-shrink: 0;
  width: 100%;
}
/* 与用例页共用 multipane-resizer-grip：轨道 + 圆角手柄，竖线随 resizer 高度置底 */
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
  position: relative;
  ::v-deep .el-loading-mask {
    z-index: 20;
  }
}
.defect-table-pagination-band {
  flex-shrink: 0;
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
  color: var(--text-color-secondary);
  font-size: 12px;
  line-height: 1;
  flex-shrink: 0;
  border-radius: 4px;
}
.defect-sidebar-expand-trigger:hover {
  color: #409eff;
  background-color: var(--sidebar-expand-trigger-hover-bg, #ecf5ff);
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
  border-top-left-radius: 4px;
}
.defect-table-root ::v-deep .cat2-bug-table-wrap {
  background: transparent;
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
  row-gap: var(--cat2bug-toolbar-row-gap, 8px);
  column-gap: var(--cat2bug-toolbar-section-gap, 10px);
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
  gap: var(--cat2bug-toolbar-item-gap, 10px);
  > * {
    margin: 0px;
  }
}
/* 缺陷名称列：标题最多 4 行，超出省略；单元格内 \\n 仍可换行 */
.defect-table-root ::v-deep td.defect-name-col .cell {
  white-space: normal !important;
  word-break: break-word;
}
.table-defect-title {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  width: 100%;
  max-width: 100%;
  .el-link {
    flex: 0 1 auto;
    width: 100%;
    max-width: 100%;
    padding-left: 5px;
    white-space: pre-line;
    word-break: break-word;
    line-height: 1.45;
    overflow: hidden;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 4;
    line-clamp: 4;
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
  margin-left: 0;
}

/* 列表图片列：缩略图铺满单元格（cover） */
.defect-list-cell-image {
  ::v-deep .el-image {
    padding: 0;
    overflow: hidden;
  }
  ::v-deep .el-image__inner {
    width: 100% !important;
    height: 100% !important;
    object-fit: cover !important;
    object-position: center center;
  }
}

/* 操作列：未超 450px 单行自适应，超出后换行（由 Cat2BugTable data-operate-at-cap 控制） */
.defect-table-root ::v-deep .cat2-bug-table-wrap:not([data-operate-at-cap='true']) td.cat2bug-operate-column .cell {
  box-sizing: border-box;
  overflow: hidden;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.defect-table-root ::v-deep th.cat2bug-operate-column .cell {
  box-sizing: border-box;
  overflow: visible !important;
  flex-wrap: nowrap;
  white-space: nowrap;
  width: 100%;
  max-width: 100%;
  color: var(--table-header-color, inherit);
}

.defect-table-root ::v-deep .cat2-bug-table-wrap[data-operate-at-cap='true'] td.cat2bug-operate-column .cell {
  flex-wrap: wrap;
  align-items: flex-start;
  align-content: flex-start;
  white-space: normal;
  overflow: visible !important;
}

.defect-table-root ::v-deep .cat2-bug-table-wrap:not([data-operate-at-cap='true']) td.cat2bug-operate-column .defect-operate-cell,
.defect-table-root ::v-deep .cat2-bug-table-wrap:not([data-operate-at-cap='true']) td.cat2bug-operate-column .cat2bug-operate-tools,
.defect-table-root ::v-deep .cat2-bug-table-wrap:not([data-operate-at-cap='true']) td.cat2bug-operate-column .defect-tools,
.defect-table-root ::v-deep .cat2-bug-table-wrap:not([data-operate-at-cap='true']) td.cat2bug-operate-column .defect-tools__bar {
  width: auto !important;
  max-width: 100% !important;
  min-width: 0 !important;
  display: inline-flex;
  flex-direction: row;
  flex-wrap: nowrap !important;
  align-items: center;
  white-space: nowrap !important;
  box-sizing: border-box;
}

.defect-table-root ::v-deep .cat2-bug-table-wrap[data-operate-at-cap='true'] td.cat2bug-operate-column .defect-operate-cell,
.defect-table-root ::v-deep .cat2-bug-table-wrap[data-operate-at-cap='true'] td.cat2bug-operate-column .cat2bug-operate-tools,
.defect-table-root ::v-deep .cat2-bug-table-wrap[data-operate-at-cap='true'] td.cat2bug-operate-column .defect-tools,
.defect-table-root ::v-deep .cat2-bug-table-wrap[data-operate-at-cap='true'] td.cat2bug-operate-column .defect-tools__bar {
  width: 100% !important;
  max-width: 100% !important;
  min-width: 0 !important;
  display: inline-flex;
  flex-direction: row;
  flex-wrap: wrap !important;
  align-items: center;
  white-space: normal !important;
  box-sizing: border-box;
}

.defect-table-root ::v-deep td.cat2bug-operate-column .defect-tools__bar > *,
.defect-table-root ::v-deep td.cat2bug-operate-column .defect-tools > * {
  flex-shrink: 0;
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
