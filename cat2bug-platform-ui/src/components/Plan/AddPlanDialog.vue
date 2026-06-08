<template>
  <!-- 添加或修改测试计划对话框 -->
  <el-dialog :title="$t(title)" :visible.sync="open" width="80%" custom-class="plan-add-dialog" append-to-body :close-on-press-escape="false" :before-close="onToolDialogBeforeClose" @closed="cancel" @opened="onPlanAddDialogOpened">
    <el-form ref="form" class="plan-add-dialog__form" :model="form" :rules="rules" label-width="120px">
      <el-form-item :label="$t('plan.name')" prop="planName">
        <el-input v-model="form.planName" :placeholder="$t('plan.enter-name')" maxlength="255" />
      </el-form-item>
      <el-row>
        <el-col :span="12">
          <el-form-item :label="$t('plan.version')" prop="planVersion">
            <el-input v-model="form.planVersion" :placeholder="$t('plan.enter-version')" maxlength="255" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="$t('plan.time')" prop="planStartTime">
            <el-date-picker
              v-model="planTimeRang"
              type="datetimerange"
              value-format="yyyy-MM-dd HH:mm:ss"
              :range-separator="$t('time-to')"
              :start-placeholder="$t('plan.start-time')"
              :end-placeholder="$t('plan.end-time')">
            </el-date-picker>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('remark')" prop="remark">
            <el-input
              type="textarea"
              :placeholder="$t('please-enter-remark')"
              v-model="form.remark"
              :autosize="{  minRows: 3, maxRows: 6 }"
              maxlength="1000"
              show-word-limit
            ></el-input>
          </el-form-item>
        </el-col>
      </el-row>
      <el-divider class="plan-add-dialog__case-divider" content-position="center">{{ $t('plan.select-case') }}</el-divider>
      <!-- 与缺陷 list/table.vue：查询条单独一行，下方再树 + 表 -->
      <div ref="planAddCaseSelect" class="plan-add-case-select">
        <div class="plan-add-case-toolbar defect-table-tools defect-table-tools-bar">
          <div class="case-tools">
            <el-form :model="caseQueryParams" ref="queryForm" size="small" :inline="true" label-width="68px">
              <el-form-item prop="caseNum" class="plan-add-hint-query">
                <el-input
                  v-model="caseQueryParams.caseNum"
                  size="small"
                  prefix-icon="el-icon-s-flag"
                  :placeholder="$t('case.please-enter-id')"
                  clearable
                  @input="handleQuery"
                />
              </el-form-item>
              <el-form-item prop="caseName">
                <el-input
                  v-model="caseQueryParams.caseName"
                  size="small"
                  prefix-icon="el-icon-files"
                  :placeholder="$t('case.please-enter-title')"
                  clearable
                  @input="handleQuery"
                />
              </el-form-item>
              <el-form-item prop="caseLevel">
                <cat2-bug-select-level v-model="caseQueryParams.caseLevel" @change="handleQuery" icon="el-icon-s-data" :clearable="true" />
              </el-form-item>
            </el-form>
          </div>
        </div>
        <multipane layout="vertical" ref="multiPane" class="custom-resizer" :class="{ 'custom-resizer--tree-hidden': !showModuleTree }" @paneResizeStop="dragStopHandle">
          <!--      树形模块选择组件-->
          <div v-if="showModuleTree" class="tree-module" ref="treeModule" :style="treeModuleStyle">
            <tree-plan-item-module
              ref="treeModuleRef"
              :project-id="projectId"
              :show-sidebar-toggle="true"
              :toolbar-sync-height="planAddTreeToolbarSyncHeight"
              @toggle-sidebar="toggleModuleTreeVisible"
              @node-click="moduleClickHandle"
              @check-change="moduleCheckChangeHandle"
              @level-node-click="levelClickHandle"
              @level-check-change="levelCheckChangeHandle"
              :check-visible="true"
              :edit-visible="false"
              :show-all="true"
              :show-count="false"
              v-resize="setDragComponentSize" />
          </div>
          <multipane-resizer ref="paneResizer" v-show="showModuleTree" :style="[multipaneStyle, paneResizerRuleVars]"></multipane-resizer>
          <!--      用例列表-->
          <div ref="caseContext" class="case-context">
          <div ref="planAddCaseContextBody" class="case-context-body plan-add-case-context-body">
          <div class="plan-add-case-table-scroll">
          <el-table ref="planItemTable" v-loading="loading" :data="caseList" :max-height="planAddTableBodyMaxHeight" v-resize="setDragComponentSize" @select="handleSelectionChange" @select-all="handleAllSelectionChange">
            <el-table-column
              v-if="!showModuleTree"
              key="plan-add-sidebar-expand-col"
              fixed
              width="30"
              align="center"
              label-class-name="plan-add-sidebar-expand-header-cell"
              class-name="plan-add-sidebar-expand-body-cell"
            >
              <template slot="header">
                <el-tooltip :content="$t('case.show-module-tree')" placement="bottom">
                  <span
                    class="plan-add-sidebar-expand-trigger"
                    role="button"
                    tabindex="0"
                    @click.stop="toggleModuleTreeVisible"
                    @keyup.enter.stop.prevent="toggleModuleTreeVisible"
                  >
                    <svg-icon icon-class="menu" class-name="plan-add-sidebar-expand-svg" />
                  </span>
                </el-tooltip>
              </template>
              <template slot-scope>
                <span class="plan-add-sidebar-expand-body-placeholder" />
              </template>
            </el-table-column>
            <el-table-column type="selection" width="50" align="center" fixed />
            <el-table-column :label="$t('id')" align="left" prop="caseNum" width="80" sortable fixed>
              <template slot-scope="scope">
                <span>{{ caseNumber(scope.row) }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="$t('title')" align="left" prop="caseName" min-width="200" sortable fixed>
              <template slot-scope="scope">
                <div class="table-case-title">
<!--                  <focus-member-list-->
<!--                    v-model="scope.row.focusList"-->
<!--                    module-name="case"-->
<!--                    :data-id="scope.row.caseId" />-->
                  <cat2-bug-text v-model="scope.row.caseName" :tooltip="scope.row.caseName" />
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="$t('module')" align="left" prop="moduleName" min-width="150" sortable />
            <el-table-column :label="$t('level')" align="left" prop="caseLevel" sortable width="80">
              <template slot-scope="scope">
                <cat2-bug-level :level="scope.row.caseLevel" />
              </template>
            </el-table-column>
            <el-table-column :label="$t('preconditions')" align="left" prop="casePreconditions" min-width="250" sortable >
              <template slot-scope="scope">
                <cat2-bug-text v-model="scope.row.casePreconditions" :tooltip="scope.row.casePreconditions" />
              </template>
            </el-table-column>
            <el-table-column :label="$t('step')" align="left" prop="caseStep" width="300" sortable>
              <template slot-scope="scope">
                <step :steps="scope.row.caseStep" style="width: 300px;" />
              </template>
            </el-table-column>
            <el-table-column :label="$t('data')" align="left" prop="caseData" min-width="250" sortable />
            <el-table-column :label="$t('expect')" align="left" prop="caseExpect" min-width="250" sortable >
              <template slot-scope="scope">
                <cat2-bug-text v-model="scope.row.caseExpect" :tooltip="scope.row.caseExpect" />
              </template>
            </el-table-column>
            <el-table-column :label="$t('image')" :key="$t('image')" align="center" prop="imgUrls" width="100">
              <template slot-scope="scope">
                <cat2-bug-preview-image :images="getUrl(scope.row.imgUrls)" />
              </template>
            </el-table-column>
            <el-table-column :label="$t('annex')" :key="$t('annex')" align="left" prop="annexUrls" min-width="300">
              <template slot-scope="scope">
                <div class="annex-list">
                  <cat2-bug-text :content="file" type="down" :tooltip="file" v-for="(file,index) in getUrl(scope.row.annexUrls)" :key="index" />
                </div>
              </template>
            </el-table-column>
          </el-table>
          </div>

          <div v-show="total>0" ref="planAddPaginationBand" class="plan-add-table-pagination-band table-pagination-band">
            <pagination
              class="plan-add-table-pagination"
              :total="total"
              :page.sync="caseQueryParams.pageNum"
              :limit.sync="caseQueryParams.pageSize"
              @pagination="getPlanItemCaseList"
            />
          </div>
          </div>
          </div>
        </multipane>
      </div>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button type="primary" class="defect-kbd-hint-host" @click="submitForm">
        {{ $t('plan.save') }}
        <span v-show="fieldHintsActive" class="cat2bug-field-hint defect-kbd-hint defect-kbd-hint--primary" aria-hidden="true">{{ dialogSaveShortcutLabel }}</span>
      </el-button>
      <el-button @click="requestCloseToolDialog">{{ $t('cancel') }}</el-button>
    </div>
  </el-dialog>
</template>

<script>
import Cat2BugLevel from "@/components/Cat2BugLevel";
import Cat2BugText from "@/components/Cat2BugText";
import Step from "@/views/system/case/components/step";
import TreePlanItemModule from "@/views/system/plan/TreePlanItemModule";
import FocusMemberList from "@/components/FocusMemberList";
import Cat2BugSelectLevel from "@/components/Cat2BugSelectLevel";
import Cat2BugPreviewImage from "@/components/Cat2BugPreviewImage";
import { Multipane, MultipaneResizer } from 'vue-multipane';
import paneResizerHandleViewport from '@/mixins/paneResizerHandleViewport';
import multipaneTreeTableHeightSync from '@/mixins/multipaneTreeTableHeightSync';
import planAddDialogKbd from '@/mixins/plan-add-dialog-kbd';
import {addPlan, getPlan, updatePlan} from "@/api/system/plan";
import {listPlanItemCase} from "@/api/system/PlanItem";
import {listCase, listPlanCaseId, listPlanCaseLevel} from "@/api/system/case";

const TREE_MODULE_WIDTH_CACHE_KEY = 'plan_case_tree_module_width';
/** 新建/编辑计划弹窗：左侧树是否展开（与缺陷列表一致用 local） */
const PLAN_ADD_DIALOG_TREE_VISIBLE_CACHE_KEY = 'plan_add_dialog_tree_module_visible';

export default {
  name: "AddPlanDialog",
  dicts: ['plan_item_state'],
  mixins: [paneResizerHandleViewport, multipaneTreeTableHeightSync, planAddDialogKbd],
  components: { Cat2BugLevel,Step,TreePlanItemModule,Multipane,MultipaneResizer, FocusMemberList, Cat2BugPreviewImage,Cat2BugText, Cat2BugSelectLevel },
  data() {
    return {
      multipaneStyle: {},
      treeModuleStyle: {'--treeModuleWidth':'300px'},
      // 遮罩层
      loading: true,
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      planTimeRang: [new Date(), new Date()],
      // 查询参数
      caseQueryParams: {
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
        params:{}
      },
      selectCaseIdSet: new Set(),
      // 表单参数
      form: {
        params: {
          modules: [],
          caseIds: []
        }
      },
      // 表单校验
      rules: {
        planName: [
          { required: true, message: this.$i18n.t('plan.name-not-empty'), trigger: "blur" }
        ],
      },
      // 测试计划子项表格数据
      sysPlanItemList: [],
      // 总条数
      total: 0,
      // 测试用例表格数据
      caseList: [],
      /** 是否显示左侧交付物/优先级树 */
      showModuleTree: true,
      /** 与右侧 el-table 表头行高对齐（px） */
      planAddTreeToolbarSyncHeight: null,
      /** 弹窗内用例表 max-height（为底部分页预留空间） */
      planAddTableBodyMaxHeight: null,
    }
  },
  watch: {
    open(val) {
      if (val) {
        this.$nextTick(() => {
          this.initPlanAddTableHeaderHeightSync();
          this.initPlanAddTableBodyResizeObserver();
          this.$nextTick(() => {
            this.setDragComponentSize();
          });
        });
      } else {
        this.destroyPlanAddTableHeaderHeightSync();
        this.destroyPlanAddTableBodyResizeObserver();
      }
    },
  },
  created() {
    const treeVis = this.$cache.local.get(PLAN_ADD_DIALOG_TREE_VISIBLE_CACHE_KEY);
    this.showModuleTree = !(treeVis === '0' || treeVis === 'false');
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
  computed: {
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
    paneResizerRuleVars() {
      const h = this.planAddTreeToolbarSyncHeight;
      return {
        '--multipane-header-rule-offset': h != null && h > 0 ? `${h}px` : '48px',
      };
    },
  },
  destroyed() {
    this.destroyPlanAddTableHeaderHeightSync();
    this.destroyPlanAddTableBodyResizeObserver();
  },
  methods: {
    onPlanAddDialogOpened() {
      this.onPlanAddDialogOpenedKbd();
      this.$nextTick(() => {
        this.syncPlanAddTableBodyMaxHeight();
        this.setDragComponentSize();
        requestAnimationFrame(() => {
          this.syncPlanAddTableBodyMaxHeight();
          this.setDragComponentSize();
        });
      });
    },
    syncPlanAddTreeToolbarWithTableHeader() {
      this.$nextTick(() => {
        const tbl = this.$refs.planItemTable;
        if (!tbl || !tbl.$el) {
          return;
        }
        const headerWrap = tbl.$el.querySelector('.el-table__header-wrapper');
        if (!headerWrap) {
          return;
        }
        if (
          this._planAddTableHeaderObservedEl &&
          this._planAddTableHeaderObservedEl !== headerWrap
        ) {
          this.destroyPlanAddTableHeaderHeightSync();
          this.initPlanAddTableHeaderHeightSync();
          return;
        }
        const rect = headerWrap.getBoundingClientRect();
        const h = Math.round(rect.height || headerWrap.offsetHeight || 0);
        if (h < 1) {
          return;
        }
        if (h > 0 && h !== this.planAddTreeToolbarSyncHeight) {
          this.planAddTreeToolbarSyncHeight = h;
        }
      });
    },
    initPlanAddTableHeaderHeightSync() {
      this.destroyPlanAddTableHeaderHeightSync();
      const tbl = this.$refs.planItemTable;
      if (!tbl || !tbl.$el) {
        this.syncPlanAddTreeToolbarWithTableHeader();
        return;
      }
      const headerWrap = tbl.$el.querySelector('.el-table__header-wrapper');
      if (!headerWrap) {
        this.syncPlanAddTreeToolbarWithTableHeader();
        return;
      }
      this._planAddTableHeaderObservedEl = headerWrap;
      this.syncPlanAddTreeToolbarWithTableHeader();
      if (typeof ResizeObserver === 'undefined') {
        return;
      }
      this._planAddTableHeaderResizeObserver = new ResizeObserver(() => {
        this.syncPlanAddTreeToolbarWithTableHeader();
      });
      this._planAddTableHeaderResizeObserver.observe(headerWrap);
    },
    destroyPlanAddTableHeaderHeightSync() {
      if (this._planAddTableHeaderResizeObserver) {
        this._planAddTableHeaderResizeObserver.disconnect();
        this._planAddTableHeaderResizeObserver = null;
      }
      this._planAddTableHeaderObservedEl = null;
    },
    /** 表单重置 */
    reset() {
      this.selectCaseIdSet.clear();
      this.form = {
        params: {
          modules: [],
          caseIds: []
        },
        planId: null,
        planName: null,
        planVersion: null,
        planStartTime: null,
        planEndTime: null,
        createById: null,
        createTime: null,
        updateById: null,
        updateTime: null,
        projectId: this.projectId,
        reportId: null
      };
      this.caseQueryParams = {
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
        params:{
          planId: 'temp'
        }
      }
      this.planTimeRang = [new Date(), new Date()];
      this.sysPlanItemList = [];
      this.resetForm("form");
    },
    handleQuery() {
      this.caseQueryParams.pageNum = 1;
      this.getPlanItemCaseList();
    },
    /** 取消按钮 */
    cancel() {
      this.open = false;
      this.reset();
      this.$emit('close')
    },
    /** 新增按钮操作 */
    openAdd() {
      this.reset();
      this.open = true;
      this.title = "plan.create";
      this.getTreeModuleWidth();
      this.caseQueryParams.params.planId= 'cat2bug-temp-id';
      this.getPlanItemCaseList();
      this.$nextTick(() => {
        this.$refs.treeModuleRef && this.$refs.treeModuleRef.reloadData();
      });
    },
    /** 修改按钮操作 */
    openUpdate(plan) {
      this.reset();
      const planId = plan.planId;
      getPlan(planId).then(response => {
        this.form = response.data;
        this.planTimeRang = [new Date(this.form.planStartTime), new Date(this.form.planEndTime)];
        if(response.data.sysPlanItemList) {
          this.selectCaseIdSet = new Set(response.data.sysPlanItemList.map(i=>i.caseId));
        }
        this.open = true;
        this.title = "plan.edit";
      });
      this.getTreeModuleWidth();
      this.caseQueryParams.params.planId= planId;
      this.getPlanItemCaseList();
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.form.sysPlanItemList = [...this.selectCaseIdSet].map(id=>{
            return {
              caseId: id
            }
          });
          this.form.projectId = this.projectId;
          this.form.planStartTime = new Date(this.planTimeRang[0]).getTime();
          this.form.planEndTime = new Date(this.planTimeRang[1]).getTime();
          if (this.form.planId != null) {
            updatePlan(this.form).then(response => {
              this.$modal.msgSuccess(this.$i18n.t('modify-success'));
              this.open = false;
              this.$emit('add', this.form);
            });
          } else {
            addPlan(this.form).then(response => {
              this.$modal.msgSuccess(this.$i18n.t('create-success'));
              this.open = false;
              this.$emit('update', this.form);
            });
          }
        }
      });
    },
    /** 查询测试用例列表 */
    getPlanItemCaseList() {
      this.loading = true;
      this.caseQueryParams.projectId = this.projectId;
      listPlanItemCase(this.caseQueryParams).then(response => {
        this.loading = false;
        this.caseList = response.rows;
        // 设置是否选中
        this.$nextTick(()=>{
          this.caseList.forEach(c=>{
            if(this.selectCaseIdSet.has(c.caseId)) {
              this.$refs.planItemTable.toggleRowSelection(c, true);
            }
          });
        });
        this.total = response.total;
        this.$nextTick(() => {
          this.initPlanAddTableHeaderHeightSync();
          this.syncPlanAddTreeToolbarWithTableHeader();
          this.setDragComponentSize();
        });
      });
    },
    /** 搜索按钮操作 */
    handleCaseQuery() {
      this.caseQueryParams.pageNum = 1;
      this.getPlanItemCaseList();
    },
    /** 点击模块树中的某个模块前的check操作 */
    moduleCheckChangeHandle(data, checked, indeterminate) {
      if(indeterminate) return;
      listPlanCaseId(data.id).then(res=>{
        if(!res.rows || res.rows.lenght==0) return;
        // 刷新选择的缓存
        res.rows.forEach(id=>{
          if(checked) {
            this.selectCaseIdSet.add(id);
          } else {
            this.selectCaseIdSet.delete(id);
          }
        });
        // 刷新用例列表中的check状态
        this.caseList.forEach(c=>{
          this.$refs.planItemTable.toggleRowSelection(c, this.selectCaseIdSet.has(c.caseId));
        })
      });
    },
    /** 点击模块树中的某个模块操作 */
    moduleClickHandle(moduleId) {
      this.caseQueryParams.moduleId = moduleId;
      this.handleCaseQuery();
    },
    /** 点击模块树中的某个缺陷优先级前的check操作 */
    levelCheckChangeHandle(data, checked, indeterminate) {
      if(indeterminate) return;
      listPlanCaseLevel(data.id).then(res=>{
        if(!res.rows || res.rows.lenght==0) return;
        // 刷新选择的缓存
        res.rows.forEach(id=>{
          if(checked) {
            this.selectCaseIdSet.add(id);
          } else {
            this.selectCaseIdSet.delete(id);
          }
        });
        // 刷新用例列表中的check状态
        this.caseList.forEach(c=>{
          this.$refs.planItemTable.toggleRowSelection(c, this.selectCaseIdSet.has(c.caseId));
        })
      });
    },
    /** 点击模块树中的某个缺陷优先级操作 */
    levelClickHandle(caseLevel) {
      this.caseQueryParams.caseLevel = caseLevel;
      this.handleCaseQuery();
    },
    /** 复选框选中数据 */
    handleSysPlanItemSelectionChange(selection) {
      this.checkedSysPlanItem = selection.map(item => item.index)
    },
    /** 测试计划子项序号 */
    rowSysPlanItemIndex({ row, rowIndex }) {
      row.index = rowIndex + 1;
    },
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
      if (this.showModuleTree && this.$refs.treeModule) {
        this.cacheTreeModuleWidth();
      }
      this.setDragComponentSize();
    },
    /** 收起/展开左侧树（与缺陷 list/table.vue 一致） */
    toggleModuleTreeVisible() {
      this.showModuleTree = !this.showModuleTree;
      this.$cache.local.set(PLAN_ADD_DIALOG_TREE_VISIBLE_CACHE_KEY, this.showModuleTree ? '1' : '0');
      this.$nextTick(() => {
        this.setDragComponentSize();
        this.$nextTick(() => this.syncPlanAddTreeToolbarWithTableHeader());
      });
    },
    syncPlanAddTableBodyMaxHeight() {
      this.$nextTick(() => {
        const body = this.$refs.planAddCaseContextBody;
        if (!body || !body.clientHeight) {
          return;
        }
        const pagEl = this.$refs.planAddPaginationBand;
        let reserveBelowTable = 0;
        if (this.total > 0 && pagEl && pagEl.offsetParent !== null) {
          const st = window.getComputedStyle(pagEl);
          reserveBelowTable =
            pagEl.offsetHeight +
            parseFloat(st.marginTop || '0') +
            parseFloat(st.marginBottom || '0');
        }
        const next = Math.max(120, Math.floor(body.clientHeight - reserveBelowTable - 2));
        if (this.planAddTableBodyMaxHeight !== next) {
          this.planAddTableBodyMaxHeight = next;
          this.$nextTick(() => {
            const tbl = this.$refs.planItemTable;
            if (tbl && typeof tbl.doLayout === 'function') {
              tbl.doLayout();
            }
          });
        }
      });
    },
    initPlanAddTableBodyResizeObserver() {
      this.destroyPlanAddTableBodyResizeObserver();
      const pane = this.$refs.planAddCaseSelect || this.$refs.planAddCaseContextBody;
      if (!pane) {
        this.syncPlanAddTableBodyMaxHeight();
        return;
      }
      if (typeof ResizeObserver === 'undefined') {
        this.syncPlanAddTableBodyMaxHeight();
        return;
      }
      this._planAddTableBodyResizeObserver = new ResizeObserver(() => {
        this.syncPlanAddTableBodyMaxHeight();
      });
      this._planAddTableBodyResizeObserver.observe(pane);
      this.syncPlanAddTableBodyMaxHeight();
    },
    destroyPlanAddTableBodyResizeObserver() {
      if (this._planAddTableBodyResizeObserver) {
        this._planAddTableBodyResizeObserver.disconnect();
        this._planAddTableBodyResizeObserver = null;
      }
    },
    /** 表格 doLayout + 分隔条手柄 */
    setDragComponentSize() {
      this.syncPlanAddTableBodyMaxHeight();
      this.$nextTick(() => {
        this.$refs.planItemTable && this.$refs.planItemTable.doLayout && this.$refs.planItemTable.doLayout();
        this.syncMultipaneTreeTableHeight('planItemTable');
        this.scheduleSyncPaneResizerHandle();
      });
    },
    handleAllSelectionChange(selection) {
      if(selection.length==0) {
        this.caseList.forEach(c=>{
          this.selectCaseIdSet.delete(c.caseId);
        })
      } else {
        this.caseList.forEach(c=>{
          this.selectCaseIdSet.add(c.caseId);
        })
      }
    },
    handleSelectionChange(selection, row) {
      if(selection.filter(c=>c.caseId==row.caseId).length>0) {
        this.selectCaseIdSet.add(row.caseId);
      } else {
        this.selectCaseIdSet.delete(row.caseId);
      }
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/assets/styles/multipane-resizer-grip.scss";

.plan-add-case-select {
  display: flex;
  flex-direction: column;
  width: 100%;
  min-height: 0;
  flex: 1 1 0%;
}
.plan-add-case-toolbar {
  flex-shrink: 0;
  width: 100%;
  box-sizing: border-box;
}
.plan-add-case-toolbar.defect-table-tools.defect-table-tools-bar {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  align-items: center;
  align-content: flex-start;
  row-gap: var(--cat2bug-toolbar-row-gap, 8px);
  column-gap: var(--cat2bug-toolbar-section-gap, 10px);
  margin: 5px 0 10px 0;
}
.plan-add-case-toolbar .case-tools {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  row-gap: var(--cat2bug-toolbar-row-gap, 8px);
  column-gap: var(--cat2bug-toolbar-item-gap, 10px);
  ::v-deep .el-form-item {
    margin-bottom: 0;
  }
}
.custom-resizer {
  flex: 1 1 0%;
  min-height: 0;
  width: 100%;
  height: auto;
  align-items: stretch;
  box-sizing: border-box;
  user-select: none;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
}
.custom-resizer--tree-hidden > .case-context {
  flex: 1 1 100%;
  width: 100%;
}
.plan-add-sidebar-expand-trigger {
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
.plan-add-sidebar-expand-trigger:hover {
  color: #409eff;
  background-color: var(--sidebar-expand-trigger-hover-bg, #ecf5ff);
}
.plan-add-sidebar-expand-trigger:focus-visible {
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.35);
}
.plan-add-sidebar-expand-trigger ::v-deep .plan-add-sidebar-expand-svg {
  width: 12px;
  height: 12px;
  vertical-align: middle;
  display: block;
}
::v-deep .plan-add-sidebar-expand-header-cell {
  padding: 0 !important;
  text-align: center;
}
::v-deep .plan-add-sidebar-expand-header-cell .cell {
  padding: 0 !important;
  overflow: visible !important;
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
}
::v-deep .el-table__fixed-header-wrapper .plan-add-sidebar-expand-header-cell .cell {
  overflow: visible !important;
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
}
::v-deep .plan-add-sidebar-expand-body-cell .cell {
  padding: 4px 0 !important;
}
.plan-add-sidebar-expand-body-placeholder {
  display: block;
  width: 1px;
  height: 1px;
  visibility: hidden;
}
.custom-resizer > .multipane-resizer {
  flex-shrink: 0;
  margin: 0 0 0 -8px;
  left: 4px;
  width: 8px;
  cursor: col-resize;
  position: relative;
  box-sizing: border-box;
  /* 弹窗内 el-table 固定列等层级较高，与 vendor 100 叠加保证可点 */
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
.case-context {
  flex: 1 1 0%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.plan-add-case-context-body {
  flex: 1 1 0%;
  min-height: 0;
  min-width: 0;
  width: 100%;
  overflow: hidden;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}
.plan-add-case-table-scroll {
  flex: 1 1 0%;
  align-self: stretch;
  width: 100%;
  min-width: 0;
  min-height: 0;
  overflow-x: auto;
  overflow-y: hidden;
  /* 表格可横向滚动但不显示滚动条（仍可用触控板 / Shift+滚轮） */
  ::v-deep .el-table__body-wrapper,
  ::v-deep .el-table__fixed-body-wrapper {
    scrollbar-width: none;
    -ms-overflow-style: none;
    &::-webkit-scrollbar {
      width: 0 !important;
      height: 0 !important;
    }
  }
}
.plan-add-table-pagination-band {
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

<style lang="scss">
/* append-to-body：弹窗在 body 下，需非 scoped */
.plan-add-dialog.el-dialog {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 12vh);
  max-height: calc(100vh - 12vh);
  min-height: 560px;
  margin-top: 6vh !important;
  margin-bottom: 6vh !important;

  .el-dialog__header {
    flex-shrink: 0;
  }

  .el-dialog__body {
    flex: 1 1 auto;
    min-height: 0;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }

  .el-dialog__footer {
    flex-shrink: 0;
  }

  .plan-add-dialog__form {
    flex: 1 1 auto;
    min-height: 0;
    height: 100%;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  .plan-add-dialog__form > .el-form-item,
  .plan-add-dialog__form > .el-row {
    flex-shrink: 0;
  }

  .plan-add-dialog__case-divider {
    flex-shrink: 0;
    margin: 8px 0;
  }

  .plan-add-case-select {
    flex: 1 1 0%;
    min-height: 240px;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  /* 左侧交付物/优先级树：不要外层边框盒子，与弹框背景一致 */
  .custom-resizer:not(.custom-resizer--tree-hidden) > .tree-module > .tree {
    background-color: transparent !important;
  }

  /* 右侧用例表格：与左侧一致，不要外框 */
  .el-table:not(.el-table--border) {
    border: none !important;
  }
}
</style>
