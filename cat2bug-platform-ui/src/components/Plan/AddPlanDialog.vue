<template>
  <!-- 新建/编辑测试计划整页（原 el-dialog，现与执行页同为路由整页） -->
  <div class="app-container plan-form-page">
    <div class="plan-form-page-header">
      <div class="plan-form-title">
        <legacy-icon icon="el-icon-arrow-left" class="plan-form-back" @click="requestCloseToolDialog" />
        <span class="plan-form-title-text">{{ title ? $t(title) : '' }}</span>
      </div>
      <div class="plan-form-actions">
        <el-button
          size="default"
          :icon="$legacyIcon('el-icon-close')"
          @click="requestCloseToolDialog"
        >{{ $t('cancel') }}</el-button>
        <el-button
          type="primary"
          size="default"
          class="defect-kbd-hint-host"
          :icon="$legacyIcon('el-icon-finished')"
          @click="submitForm"
        >
          {{ $t('plan.save') }}
          <span v-show="fieldHintsActive" class="cat2bug-field-hint defect-kbd-hint defect-kbd-hint--primary" aria-hidden="true">{{ dialogSaveShortcutLabel }}</span>
        </el-button>
      </div>
    </div>
    <el-form ref="form" class="plan-add-dialog__form plan-form-page__form" :model="form" :rules="rules" label-width="120px" @submit.prevent>
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
                  :prefix-icon="$legacyIcon('el-icon-s-flag')"
                  :placeholder="$t('case.please-enter-id')"
                  clearable
                  @input="handleQuery"
                />
              </el-form-item>
              <el-form-item prop="caseName">
                <el-input
                  v-model="caseQueryParams.caseName"
                  size="small"
                  :prefix-icon="$legacyIcon('el-icon-files')"
                  :placeholder="$t('case.please-enter-title')"
                  clearable
                  @input="handleQuery"
                />
              </el-form-item>
              <el-form-item prop="caseLevel">
                <cat2-bug-select-level v-model:level="caseQueryParams.caseLevel" @change="handleQuery" :icon="$legacyIcon('el-icon-s-data')" :clearable="true" />
              </el-form-item>
              <el-form-item prop="caseType">
                <el-select v-model="caseQueryParams.caseType" clearable filterable size="small" :placeholder="$t('case.type')" @change="handleQuery">
                  <el-option v-for="item in caseTypes" :key="item.typeId" :label="item.typeName" :value="item.typeId" />
                </el-select>
              </el-form-item>
              <el-form-item prop="reviewStatus" class="plan-add-review-status-item">
                <el-select v-model="caseQueryParams.reviewStatus" class="plan-add-review-status-input" clearable size="small" :placeholder="$t('case.review-status')" @change="handleQuery">
                  <el-option :label="$t('case.review.none')" value="none" />
                  <el-option :label="$t('case.review.in_review')" value="in_review" />
                  <el-option :label="$t('case.review.passed')" value="passed" />
                  <el-option :label="$t('case.review.changes')" value="changes" />
                </el-select>
              </el-form-item>
            </el-form>
          </div>
        </div>
        <multipane layout="vertical" ref="multiPane" class="custom-resizer" :class="{ 'custom-resizer--tree-hidden': !moduleTreeOpen }" @paneResizeStop="dragStopHandle">
          <!--      树形模块选择组件-->
          <div v-if="moduleTreeOpen" class="tree-module" ref="treeModule" :style="treeModuleStyle">
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
          <multipane-resizer ref="paneResizer" v-show="moduleTreeOpen" :style="[multipaneStyle, paneResizerRuleVars]"></multipane-resizer>
          <!--      用例列表-->
          <div ref="caseContext" class="case-context">
          <div ref="planAddCaseContextBody" class="case-context-body plan-add-case-context-body">
          <div class="plan-add-case-table-scroll">
          <el-table ref="planItemTable" v-loading="loading" :data="caseList" :max-height="planAddTableBodyMaxHeight || undefined" v-resize="setDragComponentSize" @select="handleSelectionChange" @select-all="handleAllSelectionChange">
            <el-table-column
              v-if="deliverableModuleOn && !showModuleTree"
              key="plan-add-sidebar-expand-col"
              fixed
              width="30"
              align="center"
              label-class-name="tree-sidebar-expand-header-cell"
              class-name="tree-sidebar-expand-body-cell"
            >
              <template #header>
                <el-tooltip :content="$t('case.show-module-tree')" placement="bottom">
                  <span
                    class="tree-sidebar-expand-trigger"
                    role="button"
                    tabindex="0"
                    @click.stop="toggleModuleTreeVisible"
                    @keyup.enter.stop.prevent="toggleModuleTreeVisible"
                  >
                    <svg-icon icon-class="menu" class-name="tree-sidebar-expand-svg" />
                  </span>
                </el-tooltip>
              </template>
              <template #default>
                <span class="tree-sidebar-expand-body-placeholder" />
              </template>
            </el-table-column>
            <el-table-column type="selection" width="50" align="center" fixed />
            <el-table-column :label="$t('id')" align="left" prop="caseNum" width="80" sortable fixed>
              <template #default="scope">
                <span>{{ caseNumber(scope.row) }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="$t('title')" align="left" prop="caseName" min-width="200" sortable fixed>
              <template #default="scope">
                <div class="table-case-title">
<!--                  <focus-member-list-->
<!--                    v-model="scope.row.focusList"-->
<!--                    module-name="case"-->
<!--                    :data-id="scope.row.caseId" />-->
                  <cat2-bug-text :content="scope.row.caseName" :tooltip="scope.row.caseName" />
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="$t('module')" align="left" prop="moduleName" min-width="150" sortable />
            <el-table-column :label="$t('case.type')" align="left" prop="caseTypeName" width="100" />
            <el-table-column :label="$t('case.review-status')" align="left" prop="reviewStatus" width="100">
              <template #default="scope">
                <span>{{ reviewStatusLabel(scope.row.reviewStatus) }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="$t('level')" align="left" prop="caseLevel" sortable width="80">
              <template #default="scope">
                <cat2-bug-level :level="scope.row.caseLevel" />
              </template>
            </el-table-column>
            <el-table-column :label="$t('preconditions')" align="left" prop="casePreconditions" min-width="250" sortable >
              <template #default="scope">
                <cat2-bug-text :content="scope.row.casePreconditions" :tooltip="scope.row.casePreconditions" />
              </template>
            </el-table-column>
            <el-table-column :label="$t('step')" align="left" prop="caseStep" width="300" sortable>
              <template #default="scope">
                <step :steps="scope.row.caseStep" style="width: 300px;" />
              </template>
            </el-table-column>
            <el-table-column :label="$t('data')" align="left" prop="caseData" min-width="250" sortable />
            <el-table-column :label="$t('expect')" align="left" prop="caseExpect" min-width="250" sortable >
              <template #default="scope">
                <cat2-bug-text :content="scope.row.caseExpect" :tooltip="scope.row.caseExpect" />
              </template>
            </el-table-column>
            <el-table-column :label="$t('image')" :key="$t('image')" align="center" prop="imgUrls" width="100">
              <template #default="scope">
                <cat2-bug-preview-image :images="getUrl(scope.row.imgUrls)" />
              </template>
            </el-table-column>
            <el-table-column :label="$t('annex')" :key="$t('annex')" align="left" prop="annexUrls" min-width="300">
              <template #default="scope">
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
              v-model:page="caseQueryParams.pageNum"
              v-model:limit="caseQueryParams.pageSize"
              @pagination="getPlanItemCaseList"
            />
          </div>
          </div>
          </div>
        </multipane>
      </div>
    </el-form>
  </div>
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
import teamModuleGate from '@/mixins/team-module-gate';
import multipaneTreeTableHeightSync from '@/mixins/multipaneTreeTableHeightSync';
import planAddDialogKbd from '@/mixins/plan-add-dialog-kbd';
import {addPlan, getPlan, updatePlan} from "@/api/system/plan";
import {listPlanItemCase} from "@/api/system/PlanItem";
import { listCaseTypes } from "@/api/system/case-type";
import {listCase, listPlanCaseId, listPlanCaseLevel} from "@/api/system/case";
import { storeToRefs } from "pinia";
import { useUserStore } from "@/stores/user";
import { buildProjectModulePath } from "@/utils/project-module-path";
import { checkPermi } from "@/utils/permission";

const TREE_MODULE_WIDTH_CACHE_KEY = 'plan_case_tree_module_width';
/** 新建/编辑计划：左侧树是否展开（与缺陷列表一致用 local） */
const PLAN_ADD_DIALOG_TREE_VISIBLE_CACHE_KEY = 'plan_add_dialog_tree_module_visible';

export default {
  name: "AddPlanDialog",
  dicts: ['plan_item_state'],
  mixins: [paneResizerHandleViewport, multipaneTreeTableHeightSync, planAddDialogKbd, teamModuleGate],
  components: { Cat2BugLevel,Step,TreePlanItemModule,Multipane,MultipaneResizer, FocusMemberList, Cat2BugPreviewImage,Cat2BugText, Cat2BugSelectLevel },
  setup() {
    const userStore = useUserStore()
    const { config } = storeToRefs(userStore)
    return { config }
  },
  data() {
    return {
      multipaneStyle: {},
      treeModuleStyle: {'--treeModuleWidth':'300px'},
      // 遮罩层
      loading: true,
      // 页标题 i18n key
      title: "",
      // 整页视为「已打开」，供快捷键/脏检查 mixin 复用
      open: true,
      navigatingBack: false,
      _planFormAllowLeave: false,
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
        reviewStatus: null,
        projectId: this.projectId,
        params:{}
      },
      selectCaseIdSet: new Set(),
      caseTypes: [],
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
  created() {
    const treeVis = this.$cache.local.get(PLAN_ADD_DIALOG_TREE_VISIBLE_CACHE_KEY);
    this.showModuleTree = !(treeVis === '0' || treeVis === 'false');
  },
  mounted() {
    this.bootstrapFromRoute();
  },
  beforeUnmount() {
    this.destroyPlanAddTableHeaderHeightSync();
    this.destroyPlanAddTableBodyResizeObserver();
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
      return parseInt(this.config.currentProjectId);
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
  methods: {
    /** 按路由进入新建或编辑 */
    bootstrapFromRoute() {
      const name = this.$route && this.$route.name;
      const planId = this.$route && this.$route.params && this.$route.params.planId;
      if (name === 'ProjectScopedPlanEdit' || (planId && String(this.$route.path || '').endsWith('/edit'))) {
        if (!checkPermi(['system:plan:edit'])) {
          this.$modal.msgWarning('没有操作权限');
          this._planFormAllowLeave = true;
          this.goBackToList();
          return;
        }
        this.openUpdate({ planId });
        return;
      }
      if (!checkPermi(['system:plan:add'])) {
        this.$modal.msgWarning('没有操作权限');
        this._planFormAllowLeave = true;
        this.goBackToList();
        return;
      }
      this.openAdd();
    },
    goBackToList() {
      if (this.navigatingBack) return;
      this.navigatingBack = true;
      const { teamKey, projectKey } = this.$route.params;
      const path = buildProjectModulePath(teamKey, projectKey, 'plan');
      const finish = () => { this.navigatingBack = false; };
      if (path) {
        this.$router.push(path).finally(finish);
      } else {
        this.$router.back();
        finish();
      }
    },
    async guardLeavePlanForm(next) {
      if (this._planFormAllowLeave) {
        next();
        return;
      }
      const ok = await this.confirmCloseToolDialog();
      if (ok) {
        this.toolDialogCloseBaseline = null;
        next();
      } else {
        next(false);
      }
    },
    doCloseToolDialog() {
      this.toolDialogCloseBaseline = null;
      this._planFormAllowLeave = true;
      if (typeof this.reset === 'function') this.reset();
      this.goBackToList();
    },
    reviewStatusLabel(status) {
      if (!status) return this.$t('case.review.none')
      const key = 'case.review.' + status
      const text = this.$t(key)
      return text === key ? status : text
    },
    loadCaseTypes() {
      if (!this.projectId) return
      listCaseTypes(this.projectId, { enabledOnly: true }).then(res => { this.caseTypes = res.data || [] }).catch(() => {})
    },
    onPlanAddPageReady() {
      this.open = true;
      this.loadCaseTypes();
      this.onPlanAddDialogOpenedKbd();
      this.$nextTick(() => {
        this.initPlanAddTableHeaderHeightSync();
        this.initPlanAddTableBodyResizeObserver();
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
    /** 取消 / 返回 */
    cancel() {
      this.requestCloseToolDialog();
    },
    /** 进入新建 */
    openAdd() {
      this._planFormAllowLeave = false;
      this.reset();
      this.title = "plan.create";
      this.getTreeModuleWidth();
      this.caseQueryParams.params.planId = 'cat2bug-temp-id';
      this.getPlanItemCaseList();
      this.$nextTick(() => {
        this.$refs.treeModuleRef && this.$refs.treeModuleRef.reloadData();
        this.onPlanAddPageReady();
      });
    },
    /** 进入编辑 */
    openUpdate(plan) {
      this._planFormAllowLeave = false;
      this.reset();
      const planId = plan.planId;
      this.title = "plan.edit";
      this.getTreeModuleWidth();
      this.caseQueryParams.params.planId = planId;
      getPlan(planId).then(response => {
        this.form = response.data;
        this.planTimeRang = [new Date(this.form.planStartTime), new Date(this.form.planEndTime)];
        if (response.data.sysPlanItemList) {
          this.selectCaseIdSet = new Set(response.data.sysPlanItemList.map(i => i.caseId));
        }
        this.getPlanItemCaseList();
        this.$nextTick(() => {
          this.$refs.treeModuleRef && this.$refs.treeModuleRef.reloadData();
          this.onPlanAddPageReady();
        });
      }).catch(() => {
        this._planFormAllowLeave = true;
        this.goBackToList();
      });
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
            updatePlan(this.form).then(() => {
              this.$modal.msgSuccess(this.$i18n.t('modify-success'));
              this._planFormAllowLeave = true;
              this.toolDialogCloseBaseline = null;
              this.goBackToList();
            });
          } else {
            addPlan(this.form).then(() => {
              this.$modal.msgSuccess(this.$i18n.t('create-success'));
              this._planFormAllowLeave = true;
              this.toolDialogCloseBaseline = null;
              this.goBackToList();
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
        const available = Math.max(120, Math.floor(body.clientHeight - reserveBelowTable - 2));
        // 用表头 + 表体内层 table 自然高判断是否需要封顶；短表不设 max-height，分页紧贴列表
        const tbl = this.$refs.planItemTable;
        const tableEl = tbl && tbl.$el;
        let next = available;
        if (tableEl) {
          const header = tableEl.querySelector('.el-table__header-wrapper');
          const bodyTable = tableEl.querySelector('.el-table__body-wrapper table');
          const naturalH =
            (header ? header.offsetHeight : 0) + (bodyTable ? bodyTable.offsetHeight : 0);
          next = naturalH > available ? available : null;
        }
        if (this.planAddTableBodyMaxHeight !== next) {
          this.planAddTableBodyMaxHeight = next;
          this.$nextTick(() => {
            const t = this.$refs.planItemTable;
            if (t && typeof t.doLayout === 'function') {
              t.doLayout();
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
  :deep(.el-form-item){
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
.tree-module :deep(> .tree){
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
/* 高度随表格内容；纵向上限由 planAddTableBodyMaxHeight（父列减分页）交给 el-table max-height。
   短表分页紧贴列表，多余空白在分页下方；长表顶满后分页贴页底（页面 padding-bottom 10px） */
.plan-add-case-table-scroll {
  flex: 0 1 auto;
  align-self: stretch;
  width: 100%;
  min-width: 0;
  overflow-x: auto;
  overflow-y: hidden;
  /* 表格可横向滚动但不显示滚动条（仍可用触控板 / Shift+滚轮） */
  :deep(.el-table__body-wrapper),
  :deep(.el-table__fixed-body-wrapper){
    scrollbar-width: none;
    -ms-overflow-style: none;
    &::-webkit-scrollbar {
      width: 0 !important;
      height: 0 !important;
    }
  }
}
/* 分页贴表格区底部；与页面底间距由 .plan-form-page padding-bottom（通用 10px）承担 */
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
/* 计划新建/编辑整页：与计划列表左右 20px 一致；选用例区占满剩余高度 */
.plan-form-page.app-container {
  display: flex;
  flex-direction: column;
  /* 撑满 app-main（已按是否有 tags 算好 50/84），勿写死 100vh-84 否则无 tags 时底空 34px */
  flex: 1 1 auto;
  min-height: 0;
  height: 100%;
  max-height: 100%;
  /* 左右 20px；上下用通用区块竖向间距 10px */
  padding: var(--project-list-section-v-gap, 10px) 20px;
  box-sizing: border-box;
  overflow: hidden;
}

/* 底距由页面 padding-bottom 承担，避免与 .table-pagination-band margin-bottom 叠成 20px */
.plan-form-page .table-pagination-band {
  margin-bottom: 0;
}

.plan-form-page .plan-form-page-header {
  flex: 0 0 auto;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  min-height: 32px;
}

.plan-form-page .plan-form-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.plan-form-page .plan-form-back {
  font-size: 22px;
  cursor: pointer;
}

.plan-form-page .plan-form-back:hover {
  color: var(--text-color-secondary);
}

.plan-form-page .plan-form-title-text {
  font-size: 20px;
  font-weight: 500;
  color: var(--text-color-primary);
}

.plan-form-page .plan-form-actions {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: var(--cat2bug-toolbar-item-gap, 10px);
}

.plan-form-page .plan-form-actions .el-button + .el-button {
  margin-left: 0;
}

.plan-form-page .plan-add-dialog__form,
.plan-form-page .plan-form-page__form {
  flex: 1 1 auto;
  min-height: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.plan-form-page .plan-add-dialog__form > .el-form-item,
.plan-form-page .plan-add-dialog__form > .el-row {
  flex-shrink: 0;
}

.plan-form-page .plan-add-dialog__case-divider {
  flex-shrink: 0;
  margin: 8px 0;
}

.plan-form-page .plan-add-case-select {
  flex: 1 1 0%;
  min-height: 240px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 短表时表区撑满、分页置底；与页面底间距见 padding-bottom（通用 10px） */
.plan-form-page .plan-add-case-context-body {
  justify-content: flex-start;
}

.plan-form-page .custom-resizer:not(.custom-resizer--tree-hidden) > .tree-module > .tree {
  background-color: transparent !important;
}

.plan-form-page .el-table:not(.el-table--border) {
  border: none !important;
}
</style>
