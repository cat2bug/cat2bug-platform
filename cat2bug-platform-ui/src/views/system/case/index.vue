<template>
  <div class="app-container case-body case-page project-list-page-host">
    <project-label class="case-project-label" />
    <div
      ref="caseTools"
      class="case-view-toolbar case-table-tools defect-table-tools-bar defect-view-toolbar"
      :class="{ 'wrapped-tools': caseToolsWrapped }"
    >
        <div class="case-tools-search">
        <el-form v-show="showSearch" ref="queryForm" class="left" :model="queryParams" size="small" :inline="true" label-width="0px">
          <el-form-item prop="caseNum">
            <el-input
              v-model="queryParams.caseNum"
              size="small"
              prefix-icon="el-icon-s-flag"
              :placeholder="$t('case.please-enter-id')"
              clearable
              @input="handleQuery"
            />
          </el-form-item>
          <el-form-item prop="caseName">
            <el-input
              v-model="queryParams.caseName"
              size="small"
              prefix-icon="el-icon-files"
              :placeholder="$t('case.please-enter-title')"
              clearable
              @input="handleQuery"
            />
          </el-form-item>
          <el-form-item prop="caseLevel">
            <cat2-bug-select-level v-model="queryParams.caseLevel" icon="el-icon-s-data" :clearable="true" @change="handleQuery" />
          </el-form-item>
        </el-form>
        </div>

        <div ref="caseToolsRight" class="table-tools row case-right-tools" :class="{ 'buttons-wrapped': caseRightButtonsWrapped }">
        <el-popover placement="top" trigger="click">
          <div class="row">
            <i class="el-icon-s-fold" />
            <h4>{{ $t('display-field') }}</h4>
          </div>
          <el-divider class="case-field-divider" />
          <el-checkbox-group
            :key="'case-colpick-' + caseColumnPickerRev"
            v-model="columnPickerCheckedKeys"
            class="col"
            @change="onCaseColumnPickerChange"
          >
            <el-checkbox v-for="c in caseColumnPickerOptions" :key="c.key" :label="c.key">{{ $t(c.key) }}</el-checkbox>
          </el-checkbox-group>
          <el-button slot="reference" class="case-field-picker-btn" style="padding: 9px;" plain icon="el-icon-s-fold" size="small" />
        </el-popover>
        <el-button
          v-hasPermi="['system:case:remove']"
          class="case-batch-delete-btn"
          type="danger"
          plain
          icon="el-icon-delete"
          size="small"
          :disabled="multiple"
          @click="handleDelete"
        >{{ $t('batch-delete') }}</el-button>
        <el-dropdown
          v-hasPermi="['system:case:add']"
          class="case-add-dropdown"
          split-button
          size="small"
          type="primary"
          @click="handleAdd"
        >
          <div class="title">
            <i class="el-icon-plus" />
            <span>{{ $i18n.t('case.create') }}</span>
          </div>
          <el-dropdown-menu slot="dropdown" class="case-add-dropdown-menu">
            <el-dropdown-item @click.native="handleAdd"><i class="el-icon-plus" />{{ $i18n.t('case.create') }}</el-dropdown-item>
            <el-divider class="case-add-dropdown-divider" />
            <el-dropdown-item @click.native="handleImport"><i class="el-icon-upload2" />{{ $t('case.import') }}</el-dropdown-item>
            <el-dropdown-item @click.native="handleExport"><i class="el-icon-download" />{{ $t('case.export') }}</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
        <el-dropdown
          v-hasPermi="['system:case:add']"
          class="case-ai-add-dropdown"
          split-button
          size="small"
          type="success"
          @click="handleCloudCaseAdd"
        >
          <div class="title">
            <svg-icon icon-class="robot" />
            <span>{{ $i18n.t('case.ai-create') }}</span>
          </div>
          <el-dropdown-menu slot="dropdown" class="case-add-dropdown-menu case-ai-add-dropdown-menu">
            <el-dropdown-item @click.native="handleCloudCaseAdd"><svg-icon icon-class="robot" />{{ $t('case.ai-create') }}</el-dropdown-item>
            <!--              <el-dropdown-item @click.native="handleCloudCaseAdd2"><svg-icon icon-class="robot" />{{ $t('case.ai-create') }}2</el-dropdown-item>-->
          </el-dropdown-menu>
        </el-dropdown>
        </div>
    </div>
    <!--    模块树和用例列表区域-->
    <multipane ref="multiPane" layout="vertical" class="custom-resizer" :class="{ 'custom-resizer--tree-hidden': !showModuleTree }" @paneResizeStop="dragStopHandle">
      <!--      树形模块选择组件-->
      <div v-if="showModuleTree" ref="treeModule" class="tree-module" :style="treeModuleStyle">
        <tree-module
          ref="treeModuleRef"
          v-resize="setDragComponentSize"
          :project-id="projectId"
          :show-sidebar-toggle="true"
          :toolbar-sync-height="treeToolsToolbarHeight"
          @toggle-sidebar="toggleModuleTreeVisible"
          @node-click="moduleClickHandle"
        />
      </div>
      <multipane-resizer ref="paneResizer" v-show="showModuleTree" :style="[multipaneStyle, paneResizerRuleVars]"></multipane-resizer>
      <!--      用例列表-->
      <div ref="caseContext" class="case-context">
        <div ref="caseContextBody" class="case-context-body">
          <div class="case-table-x-scroll">
          <cat2-bug-table
            ref="cat2BugTable"
            cache-key="case-table"
            field-list-cache-key="case-table-field-list"
            sort-column-cache-key="case_table_sort_column_key"
            sort-type-cache-key="case_table_sort_type_key"
            v-resize="setDragComponentSize"
            :table-max-height="caseTableBodyMaxHeight"
            :columns="caseTableColumnDefaults"
            :data="caseList"
            :loading="loading"
            @columns-change="onCaseTableColumnsChange"
            @sort-change="handleSortChange"
            @selection-change="handleSelectionChange"
            @native-mousedown="handleTableMouseDown"
            @native-mouseup="handleTableMouseUp"
            @native-mousemove="handleTableMouseMove"
          >
            <template #prepend>
              <el-table-column
                v-if="!showModuleTree"
                key="case-sidebar-expand-col"
                fixed
                width="30"
                align="center"
                label-class-name="case-sidebar-expand-header-cell"
                class-name="case-sidebar-expand-body-cell"
              >
                <template slot="header">
                  <el-tooltip :content="$t('case.show-module-tree')" placement="bottom">
                    <span
                      class="case-sidebar-expand-trigger"
                      role="button"
                      tabindex="0"
                      @click.stop="toggleModuleTreeVisible"
                      @keyup.enter.stop.prevent="toggleModuleTreeVisible"
                    >
                      <svg-icon icon-class="menu" class-name="case-sidebar-expand-svg" />
                    </span>
                  </el-tooltip>
                </template>
                <template slot-scope>
                  <span class="case-sidebar-expand-body-placeholder" />
                </template>
              </el-table-column>
              <el-table-column type="selection" width="50" align="center" fixed />
            </template>
            <template #columns="{ scope, column }">
              <span v-if="column.prop==='caseNum'">{{ caseNumber(scope.row) }}</span>
              <div v-else-if="column.prop==='caseName'" class="table-case-title">
                <cat2-bug-text v-model="scope.row.caseName" :tooltip="scope.row.caseName" />
              </div>
              <cat2-bug-level v-else-if="column.prop==='caseLevel'" :level="scope.row.caseLevel" />
              <cat2-bug-text v-else-if="column.prop==='casePreconditions'" v-model="scope.row.casePreconditions" :tooltip="scope.row.casePreconditions" />
              <div v-else-if="column.prop==='caseStep'" class="table-row-full-height">
                <step :steps="scope.row.caseStep" />
              </div>
              <span v-else-if="column.prop==='caseData'">{{ scope.row.caseData }}</span>
              <cat2-bug-text v-else-if="column.prop==='caseExpect'" v-model="scope.row.caseExpect" :tooltip="scope.row.caseExpect" />
              <cat2-bug-preview-image v-else-if="column.prop==='imgUrls'" :images="getUrl(scope.row.imgUrls)" />
              <div v-else-if="column.prop==='annexUrls'" class="annex-list">
                <cat2-bug-text v-for="(file,index) in getUrl(scope.row.annexUrls)" :key="index" :content="file" type="down" :tooltip="file" />
              </div>
              <span v-else-if="column.prop==='updateTime'">{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
              <template v-else-if="column.prop==='defectProcessingCount'">
                <div class="case-defect-status-lines">
                  <p class="table-font table-font-red" @click="handleGoDefect($event, scope.row, $t('PENDING'),[0,3])">{{ $t('PENDING') }}:{{ scope.row.defectProcessingCount }}</p>
                  <p class="table-font table-font-orange" @click="handleGoDefect($event, scope.row, $t('PROCESSING'), [1])">{{ $t('PROCESSING') }}:{{ scope.row.defectAuditCount }}</p>
                  <p class="table-font table-font-green" @click="handleGoDefect($event, scope.row, $t('CLOSED'),[4])">{{ $t('CLOSED') }}:{{ scope.row.defectCloseCount }}</p>
                </div>
              </template>
              <span v-else>{{ scope.row[column.prop] }}</span>
            </template>
            <template #append>
              <el-table-column :label="$t('operate')" align="left" class-name="no-drag cat2bug-operate-column" fixed="right">
                <template slot-scope="scope">
                  <div class="cat2bug-operate-tools">
                    <el-button
                      v-hasPermi="['system:case:edit']"
                      size="small"
                      type="text"
                      icon="el-icon-edit"
                      @click="handleUpdate(scope.row)"
                    >
                      {{ $t('modify') }}</el-button>
                    <el-button
                      v-hasPermi="['system:case:remove']"
                      size="small"
                      type="text"
                      class="red"
                      icon="el-icon-delete"
                      @click="handleDelete($event,scope.row)"
                    >{{ $t('delete') }}</el-button>
                  </div>
                </template>
              </el-table-column>
            </template>
          </cat2-bug-table>
          </div>

          <div v-show="total>0" ref="casePaginationBand" class="case-table-pagination-band table-pagination-band">
            <pagination
              class="case-table-pagination"
              :total="total"
              :page.sync="queryParams.pageNum"
              :limit.sync="queryParams.pageSize"
              @pagination="getList"
            />
          </div>
        </div>
      </div>
    </multipane>
    <add-case ref="addCaseDialog" :module-id="queryParams.params.modulePid" @added="reloadData" />
    <add-defect ref="addDefect" :project-id="projectId" @added="reloadData" />
    <cloud-case ref="cloudCaseDialog" @added="reloadData" />
    <cloud-case2 ref="cloudCaseDialog2" @added="reloadData" />
    <!-- 用户导入对话框 -->
    <el-dialog :title="upload.title" :visible.sync="upload.open" width="400px" append-to-body>
      <el-upload
        ref="upload"
        :limit="1"
        accept=".xlsx, .xls"
        :headers="upload.headers"
        :action="upload.url + '?projectId=' + projectId"
        :disabled="upload.isUploading"
        :on-progress="handleFileUploadProgress"
        :on-success="handleFileSuccess"
        :auto-upload="false"
        drag
      >
        <i class="el-icon-upload" />
        <div class="el-upload__text">{{ $t('case.import-prompt') }}<em> {{ $t('click.upload') }}</em></div>
        <div slot="tip" class="el-upload__tip text-center">
          <span>{{ strFormat($t('case.import-file-format'), 'xls、xlsx') }}</span>
          <el-link type="primary" :underline="false" style="font-size:12px;vertical-align: baseline;" @click="importTemplate">
            {{ $t('download.template') }}</el-link>
        </div>
      </el-upload>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitFileForm">{{ $t('import' ) }}</el-button>
        <el-button @click="upload.open = false">{{ $t('cancel') }}</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import ProjectLabel from '@/components/Project/ProjectLabel'
import Cat2BugLevel from '@/components/Cat2BugLevel'
import Cat2BugText from '@/components/Cat2BugText'
import Cat2BugSelectLevel from '@/components/Cat2BugSelectLevel'
import Step from '@/views/system/case/components/step'
import TreeModule from '@/components/Module/TreeModule'
import { Multipane, MultipaneResizer } from 'vue-multipane'
import { listCase, delCase } from '@/api/system/case'
import AddCase from '@/components/Case/AddCase'
import AddDefect from '@/components/Defect/AddDefect'
import CloudCase from '@/components/Cloud/CloudCase'
import CloudCase2 from '@/components/Cloud/CloudCase2'
import FocusMemberList from '@/components/FocusMemberList'
import Cat2BugPreviewImage from '@/components/Cat2BugPreviewImage'
import Cat2BugTable from '@/components/Cat2BugTable'
import { CaseTableColumnDefaults } from '@/views/system/case/case-table-options'
import { checkPermi } from '@/utils/permission'
import { strFormat } from '@/utils'
import { resolveExportAssetHost } from '@/utils/ruoyi'
import {
  appendExportColumnParams,
  CASE_FIELD_LIST_KEY,
  getColumnsFromCat2BugTable,
  mergeTableColumns
} from '@/utils/excel-export-columns'
import { getToken } from '@/utils/auth'
import { setDefectTempTab } from '@/utils/defect'
import { setHeader } from '@/utils/request'
import paneResizerHandleViewport from '@/mixins/paneResizerHandleViewport'
import multipaneTreeTableHeightSync from '@/mixins/multipaneTreeTableHeightSync'
const TREE_MODULE_WIDTH_CACHE_KEY = 'case_tree_module_width'
/** 用例页左侧交付物树是否展开（本地缓存） */
const CASE_TREE_MODULE_VISIBLE_CACHE_KEY = 'case_tree_module_visible'

export default {
  name: 'Case',
  mixins: [paneResizerHandleViewport, multipaneTreeTableHeightSync],
  components: { ProjectLabel, AddCase, Cat2BugLevel, Step, TreeModule, Multipane, MultipaneResizer, AddDefect, CloudCase, CloudCase2, FocusMemberList, Cat2BugPreviewImage, Cat2BugSelectLevel, Cat2BugText, Cat2BugTable },
  directives: {
    resize: {
      // 指令的名称
      bind(el, binding) {
        // el为绑定的元素，binding为绑定给指令的对象
        let width = ''
        let height = ''
        function isResize() {
          const style = document.defaultView.getComputedStyle(el)
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
  data() {
    return {
      // 鼠标是否点击
      mouseFlag: false,
      // 鼠标移动的偏移量
      mouseOffset: 0,
      treeModuleStyle: { '--treeModuleWidth': '300px' },
      multipaneStyle: {},
      /** 是否显示左侧交付物列表 */
      showModuleTree: true,
      caseTableColumnDefaults: CaseTableColumnDefaults.map(c => ({ ...c })),
      columnPickerCheckedKeys: [],
      /** 与 Cat2BugTable columns-change 列顺序一致，供「显示字段」列表排序 */
      caseColumnPickerRev: 0,
      casePickerColumnList: null,
      // 遮罩层
      loading: false,
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
      // 测试用例表格数据
      caseList: [],
      /** el-table max-height（像素）：由右侧列可用高度减去分页区测量得到，不用 flex 撑表格高度 */
      caseTableBodyMaxHeight: null,
      // 弹出层标题
      title: '',
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        caseName: null,
        moduleId: null,
        caseType: null,
        caseExpect: null,
        caseLevel: null,
        casePreconditions: null,
        caseNum: null,
        projectId: this.projectId,
        orderByColumn: null,
        isAsc: null,
        params: {}
      },
      // 用例导入参数
      upload: {
        // 是否显示弹出层（用户导入）
        open: false,
        // 弹出层标题（用户导入）
        title: '',
        // 是否禁用上传
        isUploading: false,
        // 设置上传的请求头部
        headers: {},
        // 上传的地址
        url: process.env.VUE_APP_BASE_API + '/system/case/importData'
      },
      observer: null,
      caseToolsWrapped: false,
      caseRightButtonsWrapped: false,
      /** 交付物列表标题栏高度（px），与右侧用例表 thead 行高对齐 */
      treeToolsToolbarHeight: null
    }
  },
  computed: {
    caseColumnPickerOptions() {
      const ordered = this.casePickerColumnList
      if (ordered && ordered.length) {
        return ordered.map(c => ({ ...c }))
      }
      return CaseTableColumnDefaults.filter(c => c.showInColumnPicker !== false)
    },
    /** 用于显示的用例编号 */
    caseNumber: function() {
      return function(val) {
        return '#' + val.caseNum
      }
    },
    /** 项目ID */
    projectId: function() {
      return parseInt(this.$store.state.user.config.currentProjectId)
    },
    /** 字符转url数组 */
    getUrl: function() {
      return function(urls) {
        const imgs = urls ? urls.split(',') : []
        return imgs.map(i => {
          return process.env.VUE_APP_BASE_API + i
        })
      }
    },
    paneResizerRuleVars() {
      const h = this.treeToolsToolbarHeight
      return {
        '--multipane-header-rule-offset': h != null && h > 0 ? `${h}px` : '48px',
      }
    },
  },
  watch: {
    '$i18n.locale': function() {
      this.$nextTick(() => {
        this.$refs.cat2BugTable && this.$refs.cat2BugTable.doLayout()
        this.$nextTick(() => this.syncTreeToolbarWithTableHeader())
      })
    },
    showSearch() {
      this.syncCaseToolsWrapped()
    }
  },
  created() {
    const treeVis = this.$cache.local.get(CASE_TREE_MODULE_VISIBLE_CACHE_KEY)
    this.showModuleTree = !(treeVis === '0' || treeVis === 'false')
    this.queryParams.orderByColumn = this.$cache.local.get('case_table_sort_column_key') || null
    this.queryParams.isAsc = this.$cache.local.get('case_table_sort_type_key') || null
    this.handleQuery()
    this.refreshUploadHeaders()
  },
  mounted() {
    this.queryParams.projectId = this.projectId
    this.getTreeModuleWidth()
    this.$nextTick(() => {
        this.syncCaseToolsWrapped()
        this.initCaseToolsObserver()
        this.initCaseTableBodyResizeObserver()
        this.$nextTick(() => {
          this.initCaseTableHeaderHeightSync()
          this.syncCaseTableBodyMaxHeight()
          this.setDragComponentSize()
        })
      })
    window.addEventListener('resize', this.syncCaseToolsWrapped)
  },
  activated() {
    /** keep-alive 返回后刷新表格布局（固定列等） */
    this.$nextTick(() => {
      const tbl = this.$refs.cat2BugTable
      if (tbl && typeof tbl.doLayout === 'function') {
        tbl.doLayout()
      }
      this.syncCaseTableBodyMaxHeight()
      this.setDragComponentSize()
      this.syncCaseToolsWrapped()
      this.$nextTick(() => this.syncTreeToolbarWithTableHeader())
    })
  },
  destroyed() {
    // 移除滚动条监听
    window.removeEventListener('resize', this.syncCaseToolsWrapped)
    this.destroyCaseToolsObserver()
    this.destroyCaseTableHeaderHeightSync()
    this.destroyCaseTableBodyResizeObserver()
  },
  methods: {
    initCaseToolsObserver() {
      if (typeof ResizeObserver === 'undefined') return
      this.destroyCaseToolsObserver()
      const tools = this.$refs.caseTools
      if (!tools) return
      this.observer = new ResizeObserver(() => {
        this.syncCaseToolsWrapped()
      })
      this.observer.observe(tools)
    },
    destroyCaseToolsObserver() {
      if (this.observer && typeof this.observer.disconnect === 'function') {
        this.observer.disconnect()
      }
      this.observer = null
    },
    /** 测量用例表表头行高，同步到左侧交付物 tree-tools */
    syncTreeToolbarWithTableHeader() {
      this.$nextTick(() => {
        const cat = this.$refs.cat2BugTable
        const elTable = cat && cat.$refs && cat.$refs.elTable
        if (!elTable || !elTable.$el) {
          return
        }
        const headerWrap = elTable.$el.querySelector('.el-table__header-wrapper')
        if (!headerWrap) {
          return
        }
        /* Cat2BugTable 固定列切换等会重建表头 DOM，需重绑 ResizeObserver */
        if (
          this._caseTableHeaderObservedEl &&
          this._caseTableHeaderObservedEl !== headerWrap
        ) {
          this.destroyCaseTableHeaderHeightSync()
          this.initCaseTableHeaderHeightSync()
          return
        }
        const tr = headerWrap.querySelector('thead tr')
        if (!tr) {
          return
        }
        const rect = tr.getBoundingClientRect()
        const h = Math.round(rect.height || tr.offsetHeight || 0)
        if (h > 0 && h !== this.treeToolsToolbarHeight) {
          this.treeToolsToolbarHeight = h
        }
      })
    },
    initCaseTableHeaderHeightSync() {
      this.destroyCaseTableHeaderHeightSync()
      const cat = this.$refs.cat2BugTable
      const elTable = cat && cat.$refs && cat.$refs.elTable
      if (!elTable || !elTable.$el) {
        this.syncTreeToolbarWithTableHeader()
        return
      }
      const headerWrap = elTable.$el.querySelector('.el-table__header-wrapper')
      if (!headerWrap) {
        this.syncTreeToolbarWithTableHeader()
        return
      }
      this._caseTableHeaderObservedEl = headerWrap
      this.syncTreeToolbarWithTableHeader()
      if (typeof ResizeObserver === 'undefined') {
        return
      }
      this._caseTableHeaderResizeObserver = new ResizeObserver(() => {
        this.syncTreeToolbarWithTableHeader()
      })
      this._caseTableHeaderResizeObserver.observe(headerWrap)
    },
    destroyCaseTableHeaderHeightSync() {
      if (this._caseTableHeaderResizeObserver) {
        this._caseTableHeaderResizeObserver.disconnect()
        this._caseTableHeaderResizeObserver = null
      }
      this._caseTableHeaderObservedEl = null
    },
    syncCaseToolsWrapped() {
      const measure = () => {
        const tools = this.$refs.caseTools
        const left = tools && tools.querySelector('.left')
        const right = this.$refs.caseToolsRight
        if (!tools || !left || !right) {
          this.caseToolsWrapped = false
          this.caseRightButtonsWrapped = false
          return false
        }
        // 左侧筛选区高度未稳定时不判定换行，避免首次进入误加 wrapped-tools
        if ((left.offsetHeight || 0) < 8) {
          this.caseToolsWrapped = false
          this.caseRightButtonsWrapped = false
          return false
        }
        // 与报告页一致：比较左右区域纵向偏移，而非 leftBottom 与 rightTop
        this.caseToolsWrapped = (right.offsetTop - left.offsetTop) > 4

        const buttonItems = Array.from(right.children || [])
        if (!this.caseToolsWrapped || !buttonItems.length) {
          this.caseRightButtonsWrapped = false
          return true
        }
        const firstTop = buttonItems[0].offsetTop
        this.caseRightButtonsWrapped = buttonItems.some(
          item => Math.abs((item.offsetTop || 0) - firstTop) > 2
        )
        return true
      }
      const runMeasure = (retryLeft = 2) => {
        this.caseToolsWrapped = false
        this.caseRightButtonsWrapped = false
        this.$nextTick(() => {
          if (measure()) return
          if (retryLeft <= 0) return
          requestAnimationFrame(() => runMeasure(retryLeft - 1))
        })
      }
      this.$nextTick(() => runMeasure())
    },
    /** 处理鼠标在表格点下事件 */
    handleTableMouseDown(e) {
      this.mouseOffset = e.clientX
      this.mouseFlag = true
    },
    /** 处理鼠标在表格点起事件 */
    handleTableMouseUp(e) {
      this.mouseFlag = false
    },
    /** 处理鼠标在表格移动事件 */
    onCaseTableColumnsChange(columns) {
      this.caseColumnPickerRev += 1
      const picker = columns.filter(c => c.showInColumnPicker !== false).map(c => ({ ...c }))
      this.$set(this, 'casePickerColumnList', picker)
      this.columnPickerCheckedKeys = columns.filter(c => c.visible && c.showInColumnPicker !== false).map(c => c.key)
      this.$nextTick(() => this.syncTreeToolbarWithTableHeader())
    },
    onCaseColumnPickerChange(keys) {
      this.$refs.cat2BugTable && this.$refs.cat2BugTable.setColumnsVisible(keys)
    },
    handleTableMouseMove(e) {
      const elTable = this.$refs.cat2BugTable && this.$refs.cat2BugTable.$refs.elTable
      if (!elTable || !elTable.bodyWrapper) return
      const tableBody = elTable.bodyWrapper
      if (this.mouseFlag) {
        tableBody.scrollLeft -= (-this.mouseOffset + (this.mouseOffset = e.clientX))
      }
    },
    strFormat,
    /** 重新加载数据 */
    reloadData() {
      this.getList()
      if (this.$refs.treeModuleRef) {
        this.$refs.treeModuleRef.reloadData()
      }
    },
    /** 初始化排序数据 */
    /** 设置列表显示的属性字段 */
    /** 获取树模型宽度 */
    getTreeModuleWidth() {
      const treeModuleWidth = this.$cache.session.get(TREE_MODULE_WIDTH_CACHE_KEY)
      this.treeModuleStyle['--treeModuleWidth'] = (treeModuleWidth || 300) + 'px'
    },
    /** 设置树模型宽度到本地缓存 */
    cacheTreeModuleWidth() {
      this.$cache.session.set(TREE_MODULE_WIDTH_CACHE_KEY, this.$refs.treeModule.clientWidth)
    },
    /** 拖动事件完成 */
    dragStopHandle(pane, container, size) {
      if (this.showModuleTree) {
        this.cacheTreeModuleWidth()
      }
      this.setDragComponentSize()
    },
    /** 切换左侧交付物列表显示，状态写入本地 */
    toggleModuleTreeVisible() {
      this.showModuleTree = !this.showModuleTree
      this.$cache.local.set(CASE_TREE_MODULE_VISIBLE_CACHE_KEY, this.showModuleTree ? '1' : '0')
      this.$nextTick(() => {
        this.setDragComponentSize()
        if (this.$refs.cat2BugTable) {
          this.$refs.cat2BugTable.doLayout()
        }
        this.$nextTick(() => this.syncTreeToolbarWithTableHeader())
      })
    },
    /** 表格 doLayout + 分隔条手柄位置（竖线 CSS 已随 resizer 100% 置底） */
    setDragComponentSize() {
      this.syncCaseTableBodyMaxHeight()
      this.$nextTick(() => {
        if (this.$refs.cat2BugTable && this.$refs.cat2BugTable.doLayout) {
          this.$refs.cat2BugTable.doLayout()
        }
        this.syncMultipaneTreeTableHeight('cat2BugTable')
        this.scheduleSyncPaneResizerHandle()
      })
    },
    initCaseTableBodyResizeObserver() {
      if (typeof ResizeObserver === 'undefined') return
      this.destroyCaseTableBodyResizeObserver()
      const el = this.$refs.caseContextBody
      if (!el) return
      this._caseTableBodyResizeObserver = new ResizeObserver(() => {
        this.syncCaseTableBodyMaxHeight()
      })
      this._caseTableBodyResizeObserver.observe(el)
    },
    destroyCaseTableBodyResizeObserver() {
      if (this._caseTableBodyResizeObserver) {
        this._caseTableBodyResizeObserver.disconnect()
        this._caseTableBodyResizeObserver = null
      }
    },
    syncCaseTableBodyMaxHeight() {
      this.$nextTick(() => {
        const body = this.$refs.caseContextBody
        if (!body || !body.clientHeight) return
        const pagEl = this.$refs.casePaginationBand
        let reserveBelowTable = 0
        if (this.total > 0 && pagEl && pagEl.offsetParent !== null) {
          const st = window.getComputedStyle(pagEl)
          reserveBelowTable =
            pagEl.offsetHeight +
            parseFloat(st.marginTop || '0') +
            parseFloat(st.marginBottom || '0')
        }
        const next = Math.max(120, Math.floor(body.clientHeight - reserveBelowTable - 2))
        if (this.caseTableBodyMaxHeight !== next) {
          this.caseTableBodyMaxHeight = next
          this.$nextTick(() => {
            const tbl = this.$refs.cat2BugTable
            if (tbl && typeof tbl.doLayout === 'function') tbl.doLayout()
          })
        }
      })
    },
    /** 查询测试用例列表 */
    getList() {
      this.loading = true
      this.queryParams.projectId = this.projectId
      listCase(this.queryParams).then(response => {
        this.loading = false
        this.caseList = response.rows
        this.total = response.total
        this.syncCaseToolsWrapped()
        this.$nextTick(() => {
          this.syncCaseTableBodyMaxHeight()
          this.syncTreeToolbarWithTableHeader()
        })
      })
    },
    /** 搜索按钮操作 */
    handleQuery() {
      if (this.queryParams.caseNum) {
        this.queryParams.caseNum = this.queryParams.caseNum.replace(/[^\d]/g, '')
      }
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.$refs.addCaseDialog.open()
    },
    handleCloudCaseAdd() {
      this.$refs.cloudCaseDialog.open()
    },
    handleCloudCaseAdd2() {
      this.$refs.cloudCaseDialog2.open()
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      if (checkPermi(['system:case:edit'])) {
        this.$refs.addCaseDialog.open(row.caseId, this.queryParams)
      }
    },
    /** 删除按钮操作 */
    handleDelete(e, row) {
      const caseIds = row ? [row.caseId] : this.ids
      const delCaseNames = this.caseList.filter(c => caseIds.indexOf(c.caseId) > -1).map(c => {
        return this.caseNumber(c)
      }).join(', ')
      this.$modal.confirm(strFormat(this.$i18n.t('case.is-delete').toString(), delCaseNames)).then(function() {
        return delCase(caseIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess(this.$i18n.t('delete-success'))
      }).catch(() => {})
      if (e && e.stopPropagation) e.stopPropagation()
    },
    /** 导出按钮操作 */
    handleExport() {
      const host = resolveExportAssetHost()
      const payload = { ...this.queryParams }
      if (host) {
        payload.params = { ...(payload.params || {}), host }
      }
      const columns = getColumnsFromCat2BugTable(this.$refs.cat2BugTable) ||
        mergeTableColumns(this.caseTableColumnDefaults, CASE_FIELD_LIST_KEY, this.$cache.local)
      appendExportColumnParams(payload, columns, 'data', 'case')
      this.download('system/case/export', payload, `${this.$i18n.t('case.export-file-name')}_${new Date().getTime()}.xlsx`)
    },
    /** 点击模块树中的某个模块操作 */
    moduleClickHandle(moduleId) {
      this.queryParams.params.modulePid = moduleId
      this.handleQuery()
    },
    /** 添加缺陷操作 */
    addDefectHandle(e, caseObj) {
      this.$refs.addDefect.openByCase(caseObj)
      e.stopPropagation()
    },
    /** 与当前界面语言同步上传请求头（切换语言后导出/导入须一致） */
    refreshUploadHeaders() {
      const headers = {}
      setHeader('/system/case/importData', headers)
      this.upload.headers = headers
    },
    /** 导入按钮操作 */
    handleImport() {
      this.refreshUploadHeaders()
      this.upload.title = this.$i18n.t('case.import')
      this.upload.open = true
    },
    /** 下载模板操作 */
    importTemplate() {
      const payload = {}
      const columns = mergeTableColumns(this.caseTableColumnDefaults, CASE_FIELD_LIST_KEY, this.$cache.local)
      appendExportColumnParams(payload, columns, 'importTemplate', 'case')
      this.download('system/case/importTemplate?projectId=' + this.projectId, payload,
        this.$i18n.t('case.template-file-name') + this.$i18n.t('excel.import-template-word') + `${new Date().getTime()}.xlsx`)
    },
    /** 文件上传中处理 */
    handleFileUploadProgress(event, file, fileList) {
      this.upload.isUploading = true
    },
    /** 文件上传成功处理 */
    handleFileSuccess(response, file, fileList) {
      this.upload.open = false
      this.upload.isUploading = false
      this.$refs.upload.clearFiles()
      let html = "<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" +
        '<label>' + response.data.message + '</label>'
      response.data.rows.forEach(r => {
        html += '<div style="display: flex;flex-direction: row; align-items: center; padding: 5px 0px; border-top:1px solid #EBEEF5">'
        if (r.rowNum) {
          html += '<h4 style="margin: 0px 10px 0px 0px;">' + strFormat(this.$i18n.t('line'), r.rowNum + '') + '</h4>'
        }
        html += '<div style="display:flex;flex-direction:column;flex:1;overflow: hidden;">'
        r.messages.forEach(m => {
          html += '<span>' + m + '</span>'
        })
        html += '</div></div>'
      })
      html += '</div>'
      this.$alert(html, this.$i18n.t('case.import-result').toString(), { dangerouslyUseHTMLString: true, customClass: 'case-upload-alert' })
      this.getList()
    },
    /** 提交上传文件 */
    submitFileForm() {
      this.refreshUploadHeaders()
      this.$refs.upload.submit()
    },
    /** 跳转到缺陷页面 */
    handleGoDefect(event, testCase, defectStateName, defectStateValues) {
      const params = {
        tabId: new Date().getMilliseconds(),
        tabName: strFormat(this.$i18n.t('case.go-defect-tab-name'), defectStateName),
        config: {
          caseId: testCase.caseId,
          params: {
            defectStates: defectStateValues
          }
        }}
      setDefectTempTab(params)
      const targetRoute = this.$router.resolve({ name: 'Defect', params: {}})
      window.open(targetRoute.href, '_blank')
      event.stopPropagation()
    },
    /** 多选框选中数据 */
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.caseId)
      this.single = selection.length != 1
      this.multiple = !selection.length
    },
    /** 下载附件操作 */
    handleDown(event, file) {
      const a = document.createElement('a')
      const e = new MouseEvent('click')
      a.href = file
      a.dispatchEvent(e)
      event.stopPropagation()
    },
    handleSortChange(column) {
      this.queryParams.isAsc = column.order
      this.queryParams.orderByColumn = column.prop
      this.getList()
    }
  }
}
</script>
<style>
.case-upload-alert {
  width: auto;
}
</style>
<style scoped lang="scss">
@import "~@/assets/styles/multipane-resizer-grip.scss";

.col {
  display: flex;
  flex-direction: column;
}
.row {
  display: flex;
  flex-direction: row;
  align-items: center;
  > * {
    margin: 0px 5px 0px 0px;
  }
}
.case-field-divider {
  margin: 8px 0px;
}
/* ProjectLabel 根为 h3，类名在 h3 上：用 ::v-deep 覆盖子组件 scoped 的 h3 下边距 */
.case-page ::v-deep h3.case-project-label {
  margin-top: 0;
  margin-bottom: 0;
}
/* 与缺陷页 .defect-tools-search 一致：表单项可换行；父级为 .case-view-toolbar（与 defect-view-toolbar 同级 flex） */
.case-tools-search {
  /* 与缺陷页观感一致：左侧随表单项定宽，不把同行剩余空间全部吃掉（缺陷无「表单项 flex:1 1 180px」） */
  flex: 0 1 auto;
  min-width: 0;
  max-width: 100%;
  box-sizing: border-box;
  ::v-deep .el-form.el-form--inline {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    row-gap: var(--cat2bug-toolbar-row-gap, 8px);
    column-gap: var(--cat2bug-toolbar-item-gap, 10px);
    width: 100%;
    max-width: 100%;
    box-sizing: border-box;
  }
  @media screen and (min-width: 577px) {
    ::v-deep .el-form.el-form--inline {
      /* 宽屏：随表单项内容定宽，避免 width:100% 把父级撑满整行 */
      width: auto;
      max-width: 100%;
    }
  }
  ::v-deep .el-form--inline .el-form-item {
    margin-right: 0;
    margin-bottom: 0;
    display: flex;
    align-items: center;
    vertical-align: middle;
  }
  @media screen and (max-width: 576px) {
    flex: 1 1 100%;
    width: 100%;
    max-width: 100%;
    ::v-deep .el-form.el-form--inline {
      flex-direction: column;
      align-items: stretch;
      row-gap: 6px;
      column-gap: 0;
    }
    ::v-deep .el-form--inline .el-form-item {
      width: 100%;
      max-width: 100%;
      min-width: 0;
      /* 列布局下禁止 flex-grow，否则与宽屏表单项 flex 叠加会纵向均分撑满 */
      flex: 0 0 auto;
      align-self: stretch;
    }
    ::v-deep .el-form--inline .el-form-item .el-form-item__content {
      width: 100% !important;
      max-width: 100%;
      min-width: 0;
      box-sizing: border-box;
    }
    ::v-deep .el-form--inline .el-form-item .el-input,
    ::v-deep .el-form--inline .el-form-item .el-select.case-level-input {
      width: 100% !important;
      max-width: 100%;
    }
    ::v-deep .el-form--inline .el-form-item .el-select.case-level-input {
      display: block;
    }
    ::v-deep .el-form--inline .el-form-item .el-select.case-level-input .el-input {
      width: 100% !important;
    }
    ::v-deep .el-form--inline .el-form-item .el-input__inner {
      width: 100% !important;
      box-sizing: border-box;
    }
  }
}
.case-body {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  min-height: 0;
  /* 与缺陷页 defect-page 一致：在 AppMain 内铺满，底边 padding 交给分页区 margin；左右 20px */
  flex: 1 1 auto;
  width: 100%;
  box-sizing: border-box;
  padding: 20px 20px 0;
}
.case-body > .custom-resizer.multipane {
  flex: 1 1 0%;
  min-height: 0;
  height: auto;
  width: 100%;
  min-width: 0;
  align-items: stretch;
  user-select: none;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
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
}
.tree-module ::v-deep > .tree {
  flex: 1 1 0%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.tree-module ::v-deep .el-tree {
  flex: 1 1 0%;
  min-height: 0;
  overflow-y: auto;
}
.case-context {
  flex: 1 1 0%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
/* 仅占 multipane 剩余高度；内部普通块级流式排版，表格 max-height 由 JS 测量写入 el-table */
.case-context-body {
  flex: 1 1 auto;
  min-height: 0;
  min-width: 0;
  width: 100%;
  overflow: hidden;
  box-sizing: border-box;
}
.case-table-x-scroll {
  width: 100%;
  min-width: 0;
  overflow-x: auto;
  overflow-y: hidden;
  -webkit-overflow-scrolling: touch;
}
.case-table-pagination-band {
  flex-shrink: 0;
}
.case-sidebar-expand-trigger {
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
.case-sidebar-expand-trigger:hover {
  color: #409eff;
  background-color: var(--sidebar-expand-trigger-hover-bg, #ecf5ff);
}
.case-sidebar-expand-trigger:focus-visible {
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.35);
}
.case-sidebar-expand-trigger ::v-deep .case-sidebar-expand-svg {
  width: 12px;
  height: 12px;
  vertical-align: middle;
  display: block;
}
::v-deep .case-sidebar-expand-header-cell {
  padding: 0 !important;
  text-align: center;
}
::v-deep .case-sidebar-expand-header-cell .cell {
  padding: 0 !important;
  overflow: visible !important;
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
}
::v-deep .el-table__fixed-header-wrapper .case-sidebar-expand-header-cell .cell {
  overflow: visible !important;
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
}
::v-deep .case-sidebar-expand-body-cell .cell {
  padding: 4px 0 !important;
}
.case-sidebar-expand-body-placeholder {
  display: block;
  width: 1px;
  height: 1px;
  visibility: hidden;
}
/* 查询条上下留白（与缺陷页 --defect-toolbar-v-gap 一致） */
/* 与项目条、列表区间距由 .project-list-page-host gap（--project-list-section-v-gap）承担 */
.case-view-toolbar {
  margin-top: 0;
  margin-bottom: 0;
}
.case-view-toolbar.wrapped-tools .case-right-tools {
  margin-left: 0;
  flex: 1 1 100%;
  width: 100%;
  max-width: 100%;
  display: flex;
  flex-wrap: wrap;
  gap: var(--cat2bug-toolbar-item-gap, 10px);
  /* 换行独占一行时与缺陷观感一致：工具从左排，勿 flex-end 整行贴右 */
  justify-content: flex-start;
  align-items: center;
}
.case-view-toolbar.wrapped-tools .case-right-tools > * {
  flex: 0 0 auto;
  width: auto;
}
.case-view-toolbar.wrapped-tools .case-right-tools ::v-deep .case-field-picker-btn,
.case-view-toolbar.wrapped-tools .case-right-tools > .case-batch-delete-btn {
  flex: 0 0 auto;
  white-space: nowrap;
}
.case-view-toolbar.wrapped-tools .case-right-tools > .case-batch-delete-btn {
  flex: 0 0 auto;
  width: auto;
  display: flex;
}
/* split 下拉整块参与工具栏换行；组内主键+箭头始终同一行 */
.case-view-toolbar.wrapped-tools .case-right-tools > .case-add-dropdown,
.case-view-toolbar.wrapped-tools .case-right-tools > .case-ai-add-dropdown {
  flex: 0 0 auto;
  min-width: 118px;
  max-width: 100%;
  width: auto;
  display: flex;
}
.case-view-toolbar.wrapped-tools .case-right-tools.buttons-wrapped > .case-batch-delete-btn {
  flex: 1 1 0;
  min-width: 92px;
  width: 100%;
  max-width: 100%;
}
.case-view-toolbar.wrapped-tools .case-right-tools.buttons-wrapped > .case-add-dropdown,
.case-view-toolbar.wrapped-tools .case-right-tools.buttons-wrapped > .case-ai-add-dropdown {
  flex: 1 1 0;
  min-width: 118px;
  width: 100%;
  max-width: 100%;
}
.case-view-toolbar.wrapped-tools .case-right-tools ::v-deep .el-button-group {
  display: inline-flex;
  flex-wrap: nowrap;
  align-items: center;
  width: auto;
  max-width: 100%;
  box-sizing: border-box;
}
.case-view-toolbar.wrapped-tools .case-right-tools.buttons-wrapped ::v-deep .el-button-group {
  align-items: stretch;
}
.case-view-toolbar.wrapped-tools .case-right-tools ::v-deep .el-button-group > .el-button:not(.el-dropdown__caret-button) {
  flex: 1 1 auto;
  min-width: 0;
}
.case-view-toolbar.wrapped-tools .case-right-tools ::v-deep .el-button-group > .el-button {
  min-width: 0;
}
.case-view-toolbar.wrapped-tools .case-right-tools ::v-deep .el-button-group > .el-dropdown__caret-button {
  flex: 0 0 auto;
}
.red {
  color: #f56c6c;
}
.custom-resizer--tree-hidden > .case-context {
  flex: 1 1 100%;
  width: 100%;
}
.case-body .custom-resizer > .multipane-resizer {
  flex-shrink: 0;
  /* 与缺陷 list/table.vue：骑缝 + multipane-resizer-grip（含手柄边框） */
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
.table-case-title {
  display: inline-flex;
  flex-direction: column;
}
.case-add-dropdown {
  .title {
    display: inline-flex;
    flex-direction: row;
    gap: 5px;
    > * {
      margin: 0px;
    }
  }
  ::v-deep .el-button-group {
    display: flex;
    flex-direction: row;
    flex-wrap: nowrap;
    align-items: center;
  }
  ::v-deep .el-button-group > .el-button {
    height: 32px;
  }
  ::v-deep button {
    color: #1890ff;
    background: #e8f4ff;
    border-color: #a3d3ff;
  }
  ::v-deep button:hover {
    background: #1890ff;
    border-color: #1890ff;
    color: #FFFFFF;
  }
  ::v-deep .el-dropdown__caret-button::before {
    background-color: #a3d3ff;

    @at-root html.dark & {
      background-color: #141414 !important;
    }
  }
}
.case-ai-add-dropdown {
  .title {
    display: inline-flex;
    flex-direction: row;
    gap: 5px;
    > * {
      margin: 0px;
    }
  }
  ::v-deep .el-button-group {
    display: flex;
    flex-direction: row;
    flex-wrap: nowrap;
    align-items: center;
  }
  ::v-deep .el-button-group > .el-button {
    height: 32px;
  }
  ::v-deep button {
    color: #fff;
    background: #67c23a;
    border-color: #67c23a;
  }
  ::v-deep button:hover {
    background: #85ce61;
    border-color: #85ce61;
    color: #FFFFFF;
  }
  ::v-deep .el-dropdown__caret-button::before {
    background-color: #f0f9eb;

    @at-root html.dark & {
      background-color: #141414 !important;
    }
  }
}
.case-add-dropdown-menu {
  min-width: 120px;
}
.case-add-dropdown-divider {
  margin-top: 5px;
  margin-bottom: 5px;
  background-color: #E4E7ED;
  padding: 0px 5px;
}
.table-font {
  font-size: 0.75rem;
  margin: 0;
  padding: 0px 5px;
}
.table-font:hover {
  text-decoration: underline;
  cursor: pointer;
}
.table-font-red {
  color: rgb(245, 108, 108);
}
.table-font-orange {
  color: rgb(251, 177, 63);
}
.table-font-green {
  color: rgb(103, 194, 58);
}
.case-defect-status-lines {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}
.case-defect-status-lines .table-font {
  line-height: 1.2;
  margin: 0;
  padding: 0 5px;
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
/* 与缺陷 list/table.vue .defect-table-tools 一致：本页工具条为 flex 容器 */
.case-view-toolbar.case-table-tools {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  align-items: center;
  align-content: flex-start;
  row-gap: var(--cat2bug-toolbar-row-gap, 8px);
  column-gap: var(--cat2bug-toolbar-section-gap, 10px);
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
  > * {
    display: block;
    justify-content: flex-start;
    margin-bottom: 0;
    ::v-deep .el-form-item {
      margin-bottom: 0;
    }
  }
  .table-tools {
    align-items: center;
    > * {
      margin-bottom: 0;
    }
  }
}
.case-right-tools {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-start;
  gap: var(--cat2bug-toolbar-item-gap, 10px);
  min-width: 0;
  max-width: 100%;
  box-sizing: border-box;
  > .case-batch-delete-btn {
    min-width: 92px;
  }
  > .case-add-dropdown,
  > .case-ai-add-dropdown {
    min-width: 118px;
  }
  > * {
    flex: 0 0 auto;
    width: auto;
  }
}
@media screen and (max-width: 576px) {
  .case-right-tools {
    width: 100%;
    max-width: 100%;
    row-gap: 8px;
  }
}
</style>

<!-- 与缺陷页 defect/index.vue 中 .defect-page .defect-view-toolbar 对齐（用例根为 .case-page，须单独写选择器） -->
<style lang="scss">
.case-page .case-view-toolbar {
  box-sizing: border-box;
  flex-shrink: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  align-content: flex-start;
  justify-content: space-between;
  column-gap: var(--cat2bug-toolbar-section-gap, 10px);
  row-gap: var(--cat2bug-toolbar-row-gap, 8px);
  width: 100%;
  padding-top: 0;
  padding-bottom: 0;
}
.case-page .case-view-toolbar > *:first-child {
  /* 宽屏左侧查询区不随视口横向拉长；同行两子项时仍由 justify-content: space-between 把右侧工具顶到行末 */
  flex: 0 1 auto;
  min-width: 0;
}
.case-page .case-view-toolbar > *:nth-child(2) {
  /* 0 1 auto：主区变窄时允许收缩，配合内部 flex-wrap 换行（0 0 auto 会整坨溢出裁切） */
  flex: 0 1 auto;
  min-width: 0;
  max-width: 100%;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-start;
}
@media screen and (max-width: 576px) {
  .case-page .case-view-toolbar > *:nth-child(2) {
    flex: 1 1 100%;
    width: 100%;
    max-width: 100%;
    justify-content: flex-start;
    /* 允许多个工具换行，避免窄屏整行 nowrap 裁切 */
    flex-wrap: wrap !important;
  }
  .case-page .case-view-toolbar .table-tools {
    width: 100%;
    max-width: 100%;
    box-sizing: border-box;
    display: flex !important;
    flex-direction: row !important;
    flex-wrap: wrap !important;
    align-items: center !important;
    column-gap: 8px;
    row-gap: 8px;
  }
  .case-page .case-view-toolbar .table-tools > *:first-child {
    flex-shrink: 0;
  }
  .case-page .case-view-toolbar .table-tools .case-add-dropdown,
  .case-page .case-view-toolbar .table-tools .case-ai-add-dropdown {
    /* 整块换行，不在 split 内部拆主键与箭头 */
    flex: 0 0 auto;
    width: auto !important;
    min-width: 118px;
    max-width: 100%;
    box-sizing: border-box;
  }
  .case-page .case-view-toolbar .case-add-dropdown .el-button-group,
  .case-page .case-view-toolbar .case-ai-add-dropdown .el-button-group {
    width: auto;
    max-width: 100%;
    box-sizing: border-box;
    display: inline-flex;
    flex-wrap: nowrap;
    align-items: center;
  }
  .case-page .case-view-toolbar .case-add-dropdown .el-button-group > .el-button:not(.el-dropdown__caret-button),
  .case-page .case-view-toolbar .case-ai-add-dropdown .el-button-group > .el-button:not(.el-dropdown__caret-button) {
    flex: 1 1 auto;
    min-width: 0;
  }
  .case-page .case-view-toolbar .case-add-dropdown .el-button-group > .el-dropdown__caret-button,
  .case-page .case-view-toolbar .case-ai-add-dropdown .el-button-group > .el-dropdown__caret-button {
    flex: 0 0 auto;
  }
}
.case-page .case-view-toolbar .table-tools {
  padding-top: 0 !important;
  align-items: center !important;
  display: flex;
  flex-wrap: wrap;
  row-gap: 8px;
  justify-content: flex-start;
  min-width: 0;
  max-width: 100%;
  box-sizing: border-box;
}
.case-page .case-view-toolbar .table-tools.row > * {
  margin-bottom: 0 !important;
}
.case-page .case-view-toolbar .case-right-tools.row > * {
  margin-right: 0 !important;
}
.case-page .case-view-toolbar .table-tools > .case-batch-delete-btn {
  min-width: 92px;
}
.case-page .case-view-toolbar .table-tools > .case-add-dropdown,
.case-page .case-view-toolbar .table-tools > .case-ai-add-dropdown {
  min-width: 118px;
}
</style>
