<template>
  <div class="app-container case-body">
    <project-label />
    <div ref="caseTools" class="case-tools" :class="{ 'wrapped-tools': caseToolsWrapped }">
      <el-form class="left" :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="0px">
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
          <cat2-bug-select-level v-model="queryParams.caseLevel" @change="handleQuery" icon="el-icon-s-data" :clearable="true" />
        </el-form-item>
      </el-form>

      <div ref="caseToolsRight" class="case-right-tools" :class="{ 'buttons-wrapped': caseRightButtonsWrapped }">
        <el-popover placement="top" trigger="click">
          <div class="row">
            <i class="el-icon-s-fold"></i>
            <h4>{{ $t('display-field') }}</h4>
          </div>
          <el-divider class="case-field-divider"></el-divider>
          <el-checkbox-group v-model="columnPickerCheckedKeys" class="col" @change="onCaseColumnPickerChange">
            <el-checkbox v-for="c in caseColumnPickerOptions" :key="c.key" :label="c.key">{{ $t(c.key) }}</el-checkbox>
          </el-checkbox-group>
          <el-button class="case-field-picker-btn" style="padding: 9px;" plain slot="reference" icon="el-icon-s-fold" size="small"></el-button>
        </el-popover>
        <el-button
          class="case-batch-delete-btn"
          type="danger"
          plain
          icon="el-icon-delete"
          size="small"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:case:remove']"
        >{{ $t('batch-delete') }}</el-button>
        <el-dropdown class="case-add-dropdown"
                     split-button
                     size="small"
                     type="primary"
                     v-hasPermi="['system:case:add']"
                     @click="handleAdd">
          <div class="title">
            <i class="el-icon-plus" />
            <span>{{$i18n.t('case.create')}}</span>
          </div>
          <el-dropdown-menu slot="dropdown" class="case-add-dropdown-menu">
            <el-dropdown-item @click.native="handleAdd"><i class="el-icon-plus" />{{$i18n.t('case.create')}}</el-dropdown-item>
            <el-divider class="case-add-dropdown-divider"></el-divider>
            <el-dropdown-item @click.native="handleImport"><i class="el-icon-upload2" />{{ $t('case.import') }}</el-dropdown-item>
            <el-dropdown-item @click.native="handleExport"><i class="el-icon-download" />{{ $t('case.export') }}</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
        <el-dropdown class="case-ai-add-dropdown"
                     split-button
                     size="small"
                     type="success"
                     v-hasPermi="['system:case:add']"
                     @click="handleCloudCaseAdd">
          <div class="title">
            <svg-icon icon-class="robot" />
            <span>{{$i18n.t('case.ai-create')}}</span>
          </div>
          <el-dropdown-menu slot="dropdown" class="case-add-dropdown-menu">
            <el-dropdown-item @click.native="handleCloudCaseAdd"><svg-icon icon-class="robot" />{{ $t('case.ai-create') }}</el-dropdown-item>
            <!--              <el-dropdown-item @click.native="handleCloudCaseAdd2"><svg-icon icon-class="robot" />{{ $t('case.ai-create') }}2</el-dropdown-item>-->
          </el-dropdown-menu>
        </el-dropdown>
      </div>
    </div>
    <!--    模块树和用例列表区域-->
    <multipane layout="vertical" ref="multiPane" class="custom-resizer" :class="{ 'custom-resizer--tree-hidden': !showModuleTree }" @paneResizeStop="dragStopHandle">
      <!--      树形模块选择组件-->
      <div class="tree-module" ref="treeModule" :style="treeModuleStyle" v-if="showModuleTree">
        <tree-module
          ref="treeModuleRef"
          :project-id="projectId"
          :show-sidebar-toggle="true"
          @toggle-sidebar="toggleModuleTreeVisible"
          @node-click="moduleClickHandle"
          v-resize="setDragComponentSize"
        />
      </div>
      <multipane-resizer v-show="showModuleTree" :style="multipaneStyle"></multipane-resizer>
      <!--      用例列表-->
      <div ref="caseContext" class="case-context">
        <div class="case-context-body">
        <cat2-bug-table
          ref="cat2BugTable"
          cache-key="case-table"
          field-list-cache-key="case-table-field-list"
          sort-column-cache-key="case_table_sort_column_key"
          sort-type-cache-key="case_table_sort_type_key"
          :columns="caseTableColumnDefaults"
          :data="caseList"
          :loading="loading"
          @columns-change="onCaseTableColumnsChange"
          @sort-change="handleSortChange"
          @selection-change="handleSelectionChange"
          @native-mousedown="handleTableMouseDown"
          @native-mouseup="handleTableMouseUp"
          @native-mousemove="handleTableMouseMove"
          v-resize="setDragComponentSize"
        >
          <template #prepend>
            <el-table-column
              v-if="!showModuleTree"
              key="case-sidebar-expand-col"
              fixed
              width="30"
              align="center"
              label-class-name="case-sidebar-expand-header-cell"
              class-name="case-sidebar-expand-body-cell">
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
              <cat2-bug-text :content="file" type="down" :tooltip="file" v-for="(file,index) in getUrl(scope.row.annexUrls)" :key="index" />
            </div>
            <span v-else-if="column.prop==='updateTime'">{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
            <template v-else-if="column.prop==='defectProcessingCount'">
              <p class="table-font table-font-red" @click="handleGoDefect($event, scope.row, $t('PENDING'),[0,3])">{{ $t('PENDING') }}:{{ scope.row.defectProcessingCount }}</p>
              <p class="table-font table-font-orange" @click="handleGoDefect($event, scope.row, $t('PROCESSING'), [1])">{{ $t('PROCESSING') }}:{{ scope.row.defectAuditCount }}</p>
              <p class="table-font table-font-green" @click="handleGoDefect($event, scope.row, $t('CLOSED'),[4])">{{ $t('CLOSED') }}:{{ scope.row.defectCloseCount }}</p>
            </template>
            <span v-else>{{ scope.row[column.prop] }}</span>
          </template>
          <template #append>
            <el-table-column :label="$t('operate')" align="left" class-name="small-padding fixed-width no-drag" fixed="right" min-width="150">
              <template slot-scope="scope">
                <div class="case-table-operate">
                  <el-button
                    size="small"
                    type="text"
                    @click="handleUpdate(scope.row)"
                    icon="el-icon-edit"
                    v-hasPermi="['system:case:edit']">
                    {{ $t('modify') }}</el-button>
                  <el-button
                    size="small"
                    type="text"
                    class="red"
                    icon="el-icon-delete"
                    @click="handleDelete($event,scope.row)"
                    v-hasPermi="['system:case:remove']"
                  >{{ $t('delete') }}</el-button>
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
          @pagination="getList"
        />
        </div>
      </div>
    </multipane>
    <add-case ref="addCaseDialog" :module-id="queryParams.params.modulePid" @added="reloadData" @close="initFloatMenu" />
    <add-defect ref="addDefect" :project-id="projectId" @added="reloadData" @close="initFloatMenu" />
    <cloud-case ref="cloudCaseDialog" @added="reloadData" @close="initFloatMenu" />
    <cloud-case2 ref="cloudCaseDialog2" @added="reloadData" @close="initFloatMenu" />
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
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">{{ $t('case.import-prompt') }}<em> {{ $t('click.upload') }}</em></div>
        <div class="el-upload__tip text-center" slot="tip">
          <span>{{ strFormat($t('case.import-file-format'), 'xls、xlsx') }}</span>
          <el-link type="primary" :underline="false" style="font-size:12px;vertical-align: baseline;" @click="importTemplate">
            {{ $t('download.template') }}</el-link>
        </div>
      </el-upload>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitFileForm">{{ $t('import' )}}</el-button>
        <el-button @click="upload.open = false">{{ $t('cancel') }}</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import ProjectLabel from "@/components/Project/ProjectLabel";
import Cat2BugLevel from "@/components/Cat2BugLevel";
import Cat2BugText from "@/components/Cat2BugText";
import Cat2BugSelectLevel from "@/components/Cat2BugSelectLevel";
import Step from "@/views/system/case/components/step";
import TreeModule from "@/components/Module/TreeModule";
import { Multipane, MultipaneResizer } from 'vue-multipane';
import { listCase, delCase } from "@/api/system/case";
import AddCase from "@/components/Case/AddCase";
import AddDefect from "@/components/Defect/AddDefect";
import CloudCase from "@/components/Cloud/CloudCase";
import CloudCase2 from "@/components/Cloud/CloudCase2";
import FocusMemberList from "@/components/FocusMemberList";
import Cat2BugPreviewImage from "@/components/Cat2BugPreviewImage";
import Cat2BugTable from "@/components/Cat2BugTable";
import { CaseTableColumnDefaults } from "@/views/system/case/case-table-options";
import {checkPermi} from "@/utils/permission";
import {strFormat} from "@/utils";
import { resolveExportAssetHost } from "@/utils/ruoyi";
import {getToken} from "@/utils/auth";
import {setDefectTempTab} from "@/utils/defect";
import {setHeader} from "@/utils/request";

const TREE_MODULE_WIDTH_CACHE_KEY = 'case_tree_module_width';
/** 用例页左侧交付物树是否展开（本地缓存） */
const CASE_TREE_MODULE_VISIBLE_CACHE_KEY = 'case_tree_module_visible';

export default {
  name: "Case",
  components: {ProjectLabel,AddCase,Cat2BugLevel,Step,TreeModule,Multipane,MultipaneResizer,AddDefect,CloudCase,CloudCase2,FocusMemberList,Cat2BugPreviewImage,Cat2BugSelectLevel,Cat2BugText,Cat2BugTable},
  data() {
    return {
      // 鼠标是否点击
      mouseFlag: false,
      // 鼠标移动的偏移量
      mouseOffset: 0,
      multipaneStyle: {'--marginTop':'0px'},
      treeModuleStyle: {'--treeModuleWidth':'300px'},
      /** 是否显示左侧交付物列表 */
      showModuleTree: true,
      caseTableColumnDefaults: CaseTableColumnDefaults.map(c => ({ ...c })),
      columnPickerCheckedKeys: [],
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
      // 弹出层标题
      title: "",
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
        params:{}
      },
      // 用例导入参数
      upload: {
        // 是否显示弹出层（用户导入）
        open: false,
        // 弹出层标题（用户导入）
        title: "",
        // 是否禁用上传
        isUploading: false,
        // 设置上传的请求头部
        headers: {},
        // 上传的地址
        url: process.env.VUE_APP_BASE_API + "/system/case/importData"
      },
      observer: null,
      caseToolsWrapped: false,
      caseRightButtonsWrapped: false,
    };
  },
  watch: {
    "$i18n.locale": function () {
      this.$nextTick(() => {
        this.$refs.cat2BugTable && this.$refs.cat2BugTable.doLayout();
      });
    },
    showSearch() {
      this.syncCaseToolsWrapped();
    },
  },
  computed: {
    caseColumnPickerOptions() {
      return CaseTableColumnDefaults.filter(c => c.showInColumnPicker !== false);
    },
    /** 用于显示的用例编号 */
    caseNumber: function () {
      return function (val) {
        return '#'+val.caseNum;
      }
    },
    /** 项目ID */
    projectId: function () {
      return parseInt(this.$store.state.user.config.currentProjectId);
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
  },
  created() {
    const treeVis = this.$cache.local.get(CASE_TREE_MODULE_VISIBLE_CACHE_KEY);
    this.showModuleTree = !(treeVis === '0' || treeVis === 'false');
    this.queryParams.orderByColumn = this.$cache.local.get('case_table_sort_column_key') || null;
    this.queryParams.isAsc = this.$cache.local.get('case_table_sort_type_key') || null;
    this.handleQuery();
    let headers = {};
    setHeader('/system/case/importData', headers);
    this.upload.headers = headers;
  },
  mounted() {
    this.queryParams.projectId=this.projectId;
    this.getTreeModuleWidth();
    this.initFloatMenu();
    this.$nextTick(() => {
      this.syncCaseToolsWrapped();
      this.initCaseToolsObserver();
    });
    window.addEventListener("resize", this.syncCaseToolsWrapped);
  },
  destroyed() {
    // 移除滚动条监听
    this.$floatMenu.windowsDestory();
    window.removeEventListener("resize", this.syncCaseToolsWrapped);
    this.destroyCaseToolsObserver();
  },
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
  methods: {
    initCaseToolsObserver() {
      if (typeof ResizeObserver === "undefined") return;
      this.destroyCaseToolsObserver();
      const tools = this.$refs.caseTools;
      if (!tools) return;
      this.observer = new ResizeObserver(() => {
        this.syncCaseToolsWrapped();
      });
      this.observer.observe(tools);
    },
    destroyCaseToolsObserver() {
      if (this.observer && typeof this.observer.disconnect === "function") {
        this.observer.disconnect();
      }
      this.observer = null;
    },
    syncCaseToolsWrapped() {
      const measure = () => {
        const tools = this.$refs.caseTools;
        const left = tools && tools.querySelector(".left");
        const right = this.$refs.caseToolsRight;
        if (!tools || !left || !right) {
          this.caseToolsWrapped = false;
          this.caseRightButtonsWrapped = false;
          return;
        }
        const leftTop = left.offsetTop || 0;
        const leftBottom = leftTop + (left.offsetHeight || 0);
        const rightTop = right.offsetTop || 0;
        // 仅当右侧工具条真正落到左侧筛选区域“下一行”时，才进入 wrapped 态
        this.caseToolsWrapped = rightTop >= leftBottom - 1;

        // 仅当右侧按钮组自身出现多行时，才让主按钮等分填充。
        const buttonItems = Array.from(right.children || []);
        if (!buttonItems.length) {
          this.caseRightButtonsWrapped = false;
          return;
        }
        const firstTop = buttonItems[0].offsetTop;
        this.caseRightButtonsWrapped = buttonItems.some(item => Math.abs((item.offsetTop || 0) - firstTop) > 2);
      };
      this.$nextTick(() => {
        if (this.caseToolsWrapped) {
          this.caseToolsWrapped = false;
          this.$nextTick(measure);
          return;
        }
        measure();
      });
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
    onCaseTableColumnsChange(columns) {
      this.columnPickerCheckedKeys = columns.filter(c => c.visible && c.showInColumnPicker !== false).map(c => c.key);
    },
    onCaseColumnPickerChange(keys) {
      this.$refs.cat2BugTable && this.$refs.cat2BugTable.setColumnsVisible(keys);
    },
    handleTableMouseMove(e) {
      const elTable = this.$refs.cat2BugTable && this.$refs.cat2BugTable.$refs.elTable;
      if (!elTable || !elTable.bodyWrapper) return;
      let tableBody = elTable.bodyWrapper;
      if (this.mouseFlag) {
        tableBody.scrollLeft -= (- this.mouseOffset + (this.mouseOffset = e.clientX));
      }
    },
    strFormat,
    /** 重新加载数据 */
    reloadData() {
      this.getList();
      if (this.$refs.treeModuleRef) {
        this.$refs.treeModuleRef.reloadData();
      }
    },
    /** 初始化排序数据 */
    /** 初始化浮动菜单 */
    initFloatMenu() {
      this.$floatMenu.windowsInit(document.querySelector('.main-container'));
      this.$floatMenu.resetMenus([{
        id: 'addCase',
        name: 'case.create',
        visible: true,
        plain: true,
        type: 'primary',
        icon: 'add-tab',
        prompt: 'case.create',
        permissions: ['system:case:add'],
        click : this.handleAdd
      },{
        id: 'importCase',
        name: 'case.import',
        visible: true,
        plain: true,
        type: 'info',
        icon: 'import',
        prompt: 'case.import',
        permissions: ['system:case:add'],
        click : this.handleImport
      },{
        id: 'exportCase',
        name: 'case.export',
        visible: true,
        plain: true,
        type: 'info',
        icon: 'export',
        prompt: 'case.export',
        permissions: ['system:case:add'],
        click : this.handleExport
      },{
        id: 'aiAddCase',
        name: 'case.ai-create',
        visible: true,
        plain: false,
        type: 'success',
        icon: 'robot',
        prompt: 'case.ai-create',
        permissions: ['system:case:add'],
        click : this.handleCloudCaseAdd
      }]);
    },
    /** 设置列表显示的属性字段 */
    /** 获取树模型宽度 */
    getTreeModuleWidth() {
      let treeModuleWidth = this.$cache.session.get(TREE_MODULE_WIDTH_CACHE_KEY);
      this.treeModuleStyle['--treeModuleWidth'] = (treeModuleWidth?treeModuleWidth:300)+'px';
    },
    /** 设置树模型宽度到本地缓存 */
    cacheTreeModuleWidth() {
      this.$cache.session.set(TREE_MODULE_WIDTH_CACHE_KEY,this.$refs.treeModule.clientWidth);
    },
    /** 拖动事件完成 */
    dragStopHandle(pane, container, size) {
      if (this.showModuleTree) {
        this.cacheTreeModuleWidth();
      }
    },
    /** 切换左侧交付物列表显示，状态写入本地 */
    toggleModuleTreeVisible() {
      this.showModuleTree = !this.showModuleTree;
      this.$cache.local.set(CASE_TREE_MODULE_VISIBLE_CACHE_KEY, this.showModuleTree ? '1' : '0');
      this.$nextTick(() => {
        this.setDragComponentSize();
        if (this.$refs.cat2BugTable) {
          this.$refs.cat2BugTable.doLayout();
        }
      });
    },
    /** 设置模块与用例列表中间拖动块的尺寸 */
    setDragComponentSize() {
      this.multipaneStyle['--marginTop'] = '0px';
      this.$nextTick(()=> {
        const treeH = this.$refs.treeModule ? (this.$refs.treeModule.scrollHeight || 0) : 0;
        let pageHeight = Math.max(treeH, this.$refs.caseContext.scrollHeight || 0, document.body.scrollHeight - 170)
        this.multipaneStyle['--marginTop'] = pageHeight + 'px';
      })
    },
    /** 查询测试用例列表 */
    getList() {
      this.loading = true;
      this.queryParams.projectId = this.projectId;
      listCase(this.queryParams).then(response => {
        this.loading = false;
        this.caseList = response.rows;
        this.total = response.total;
        this.syncCaseToolsWrapped();
      });
    },
    /** 搜索按钮操作 */
    handleQuery() {
      if(this.queryParams.caseNum) {
        this.queryParams.caseNum=this.queryParams.caseNum.replace(/[^\d]/g,'');
      }
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.$refs.addCaseDialog.open();
    },
    handleCloudCaseAdd() {
      this.$refs.cloudCaseDialog.open();
    },
    handleCloudCaseAdd2() {
      this.$refs.cloudCaseDialog2.open();
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      if(checkPermi(['system:case:edit'])) {
        this.$refs.addCaseDialog.open(row.caseId, this.queryParams);
      }
    },
    /** 删除按钮操作 */
    handleDelete(e,row) {
      const caseIds = row?[row.caseId]:this.ids;
      const delCaseNames = this.caseList.filter(c=>caseIds.indexOf(c.caseId)>-1).map(c=>{
        return this.caseNumber(c);
      }).join(', ');
      this.$modal.confirm(strFormat(this.$i18n.t('case.is-delete').toString(), delCaseNames)).then(function() {
        return delCase(caseIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess(this.$i18n.t('delete-success'));
      }).catch(() => {});
      if (e && e.stopPropagation) e.stopPropagation();
    },
    /** 导出按钮操作 */
    handleExport() {
      const host = resolveExportAssetHost()
      const payload = { ...this.queryParams }
      if (host) {
        payload.params = { ...(payload.params || {}), host }
      }
      this.download('system/case/export', payload, `${ this.$i18n.t('case.export') }_${new Date().getTime()}.xlsx`)
    },
    /** 点击模块树中的某个模块操作 */
    moduleClickHandle(moduleId) {
      this.queryParams.params.modulePid = moduleId;
      this.handleQuery();
    },
    /** 添加缺陷操作 */
    addDefectHandle(e,caseObj){
      this.$refs.addDefect.openByCase(caseObj);
      e.stopPropagation();
    },
    /** 导入按钮操作 */
    handleImport() {
      this.upload.title = this.$i18n.t('case.import');
      this.upload.open = true;
    },
    /** 下载模板操作 */
    importTemplate() {
      this.download('system/case/importTemplate?projectId='+this.projectId, {
      }, this.$i18n.t('case.template-file-name')+`${new Date().getTime()}.xlsx`)
    },
    /** 文件上传中处理 */
    handleFileUploadProgress(event, file, fileList) {
      this.upload.isUploading = true;
    },
    /** 文件上传成功处理 */
    handleFileSuccess(response, file, fileList) {
      this.upload.open = false;
      this.upload.isUploading = false;
      this.$refs.upload.clearFiles();
      let html = "<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>"
        + "<label>"+response.data.message +"</label>";
      response.data.rows.forEach(r=>{
        html+='<div style="display: flex;flex-direction: row; align-items: center; padding: 5px 0px; border-top:1px solid #EBEEF5">';
        if(r.rowNum) {
          html += '<h4 style="margin: 0px 10px 0px 0px;">' + strFormat(this.$i18n.t('line'), r.rowNum + '') + '</h4>';
        }
        html+='<div style="display:flex;flex-direction:column;flex:1;overflow: hidden;">'
        r.messages.forEach(m=>{
          html+='<span>'+m+'</span>'
        });
        html+='</div></div>'
      });
      html+="</div>"
      this.$alert(html, this.$i18n.t('case.import-result').toString(), { dangerouslyUseHTMLString: true, customClass: 'case-upload-alert' });
      this.getList();
    },
    /** 提交上传文件 */
    submitFileForm() {
      this.$refs.upload.submit();
    },
    /** 跳转到缺陷页面 */
    handleGoDefect(event,testCase,defectStateName, defectStateValues) {
      const params = {
        tabId: new Date().getMilliseconds(),
        tabName: strFormat(this.$i18n.t('case.go-defect-tab-name'), defectStateName),
        config: {
          caseId: testCase.caseId,
          params: {
            defectStates: defectStateValues
          }
        }}
      setDefectTempTab(params);
      const targetRoute = this.$router.resolve({ name:'Defect', params: {}});
      window.open(targetRoute.href, '_blank');
      event.stopPropagation();
    },
    /** 多选框选中数据 */
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.caseId);
      this.single = selection.length != 1;
      this.multiple = !selection.length;
    },
    /** 下载附件操作 */
    handleDown(event, file) {
      const a = document.createElement("a");
      const e = new MouseEvent("click");
      a.href = file;
      a.dispatchEvent(e);
      event.stopPropagation();
    },
    handleSortChange(column) {
      this.queryParams.isAsc = column.order;
      this.queryParams.orderByColumn = column.prop;
      this.getList();
    }
  }
};
</script>
<style>
.case-upload-alert {
  width: auto;
}
</style>
<style scoped lang="scss">
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
.tree-module {
  width: var(--treeModuleWidth);
  max-width: 75%;
  flex-shrink: 0;
}
.case-context {
  flex-grow: 1;
  min-width: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: stretch;
}
.case-context-body {
  flex: 1;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
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
  background-color: #ecf5ff;
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
.case-tools {
  width: 100%;
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  justify-content: flex-start;
  align-items: center;
  align-content: flex-start;
  column-gap: 12px;
  row-gap: 8px;
  margin-top: 10px;
  margin-bottom: 10px;
  .el-form-item {
    margin-bottom: 0;
  }
}
.case-tools > .left {
  flex: 1 1 auto;
  min-width: 0;
  max-width: 100%;
  width: auto;
  display: inline-flex;
  flex-wrap: nowrap;
  align-items: center;
  row-gap: 8px;
  column-gap: 8px;
  box-sizing: border-box;
  ::v-deep .el-form-item {
    flex: 1 1 0;
    min-width: 180px;
    margin-right: 0;
  }
  ::v-deep .el-form-item .el-form-item__content,
  ::v-deep .el-form-item .el-input,
  ::v-deep .el-form-item .el-select,
  ::v-deep .el-form-item .cat2-bug-select-level {
    width: 100%;
  }
}
.case-tools.wrapped-tools {
  > .left {
    display: inline-flex;
    flex-wrap: wrap;
    flex: 1 1 100%;
  }
  > .left ::v-deep .el-form-item {
    display: inline-flex;
    flex: 1 1 0;
    min-width: 180px;
    margin-right: 0;
    margin-bottom: 0;
  }
  > .left ::v-deep .el-form-item .el-form-item__content,
  > .left ::v-deep .el-form-item .el-input,
  > .left ::v-deep .el-form-item .el-select,
  > .left ::v-deep .el-form-item .el-select .el-input,
  > .left ::v-deep .el-form-item .cat2-bug-select-level {
    width: 100% !important;
    max-width: 100%;
    box-sizing: border-box;
  }
  > .left ::v-deep .el-form-item .el-select .el-input__inner {
    width: 100% !important;
  }
  > .case-right-tools {
    margin-left: 0;
    flex: 1 1 100%;
    width: 100%;
    display: inline-flex;
    flex-wrap: wrap;
    gap: 10px;
    justify-content: flex-start;
    align-items: center;
  }
  > .case-right-tools > * {
    flex: 0 0 auto;
    width: auto;
    min-width: 0;
  }
  /* 字段显示 + 批量删除始终同一行 */
  > .case-right-tools ::v-deep .case-field-picker-btn,
  > .case-right-tools > .case-batch-delete-btn {
    flex: 0 0 auto;
    white-space: nowrap;
  }

  /* 按钮组没换行时保持自然宽度 */
  > .case-right-tools > .case-batch-delete-btn,
  > .case-right-tools > .case-add-dropdown,
  > .case-right-tools > .case-ai-add-dropdown {
    flex: 0 0 auto;
    width: auto;
    display: flex;
  }
  /* 右侧按钮组自身换行后，三个主按钮才等宽填充 */
  > .case-right-tools.buttons-wrapped > .case-batch-delete-btn,
  > .case-right-tools.buttons-wrapped > .case-add-dropdown,
  > .case-right-tools.buttons-wrapped > .case-ai-add-dropdown {
    flex: 1 1 0;
    min-width: 0;
    width: 100%;
  }
  > .case-right-tools ::v-deep .el-button-group {
    width: 100%;
    display: flex;
    flex-wrap: nowrap;
  }
  > .case-right-tools ::v-deep .el-button-group > .el-button:not(.el-dropdown__caret-button) {
    flex: 1 1 auto;
  }
  > .case-right-tools ::v-deep .el-button-group > .el-button {
    min-width: 0;
  }
  > .case-right-tools ::v-deep .el-button-group > .el-dropdown__caret-button {
    flex: 0 0 auto;
  }
}
.red {
  color: #f56c6c;
}
.custom-resizer {
  width: 100%;
  height: auto;
  user-select: none;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
}
.custom-resizer--tree-hidden > .case-context {
  flex: 1 1 100%;
  width: 100%;
}
.custom-resizer > .multipane-resizer {
  margin: 0; left: 0;
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
    background-color: #DCDFE6;
  }
  &:after {
    content: "";
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 4px;
    height: 30px;
    border-left: 1px solid #C0C4CC;
    border-right: 1px solid #C0C4CC;
    border-radius: 2px;
  }
  &:hover {
    &:before {
      background-color: #B0B0B0;
    }
    &:after {
      border-color: #909399;
    }
  }
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
.case-table-operate {
  padding-left: 10px;
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
.case-right-tools {
  flex: 0 0 auto;
  display: inline-flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
  flex-wrap: nowrap;
  gap: 10px;
  margin-left: auto;
  > * {
    flex: 0 0 auto;
    width: auto;
    min-width: 0;
  }
}
</style>
