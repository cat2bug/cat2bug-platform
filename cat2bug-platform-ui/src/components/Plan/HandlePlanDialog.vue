<template>
  <!-- 测试计划执行整页（原右侧抽屉改为路由页 plan/:planId/run） -->
  <div class="app-container handle-plan-page">
    <div class="plan-run-page-header">
      <div class="plan-run-title">
        <legacy-icon icon="el-icon-arrow-left" class="plan-run-back" @click="cancel" />
        <div class="plan-run-title-line">
          <span class="plan-run-title-num">{{ $t('plan') }}:{{plan.planName}}</span>
          <el-button v-if="plan.planVersion" size="small" type="success">{{ $t('version') }}:{{ plan.planVersion }}</el-button>
          <el-button size="small" type="primary" plain>{{ $t('plan.time') }}:{{ plan.planStartTime }} - {{ plan.planEndTime }}</el-button>
        </div>
      </div>
    </div>
    <div class="plan-run-content">
      <div class="plan-run-statistics-band">
        <div class="plan-statistical" v-loading="loading">
          <div class="plan-statistical-item plan-stat-card plan-stat-card--key plan-stat-card--countdown">
            <div class="plan-statistical-block">
              <!-- Element Plus 倒计时用 el-countdown；旧 el-statistic + time-indices 无效，会把时间戳当数字千分位显示 -->
              <template v-if="planCountdownEnded">
                <div class="plan-stat-label">{{ $t('plan.countdown-time-title') }}</div>
                <div class="plan-stat-countdown-ended">{{ $t('plan.countdown-ended') }}</div>
              </template>
              <el-countdown
                v-else-if="planCountdownTarget > 0"
                :value="planCountdownTarget"
                :format="$t('plan.countdown-time-format')"
                :title="$t('plan.countdown-time-title')"
                @finish="onPlanCountdownFinish"
              />
              <template v-else>
                <div class="plan-stat-label">{{ $t('plan.countdown-time-title') }}</div>
                <div class="plan-stat-countdown-ended">—</div>
              </template>
            </div>
          </div>
          <div class="plan-statistical-item plan-stat-card plan-stat-card--key plan-stat-card--case">
            <div class="plan-statistical-block">
              <div class="plan-stat-label">{{ $t('case') }}</div>
              <div class="plan-stat-case-grid">
                <span class="plan-stat-case-cell click plan-stat-pass" @click.stop="handlePlanItemStateSearch('pass')">
                  <span class="plan-stat-case-num">{{ plan.passCount || 0 }}</span>
                  <span class="plan-stat-case-name">{{ $t('case.pass-tested') }}</span>
                </span>
                <span class="plan-stat-case-cell click plan-stat-fail" @click.stop="handlePlanItemStateSearch('not_pass')">
                  <span class="plan-stat-case-num">{{ plan.failCount || 0 }}</span>
                  <span class="plan-stat-case-name">{{ $t('case.failed-tested') }}</span>
                </span>
                <span class="plan-stat-case-cell click plan-stat-unexec" @click.stop="handlePlanItemStateSearch('unexecuted')">
                  <span class="plan-stat-case-num">{{ plan.unexecutedCount || 0 }}</span>
                  <span class="plan-stat-case-name">{{ $t('unexecuted') }}</span>
                </span>
                <span class="plan-stat-case-cell click plan-stat-total" @click.stop="handlePlanItemStateSearch(null)">
                  <span class="plan-stat-case-num">{{ plan.itemTotal || 0 }}</span>
                  <span class="plan-stat-case-name">{{ $t('total') }}</span>
                </span>
              </div>
              <div class="plan-stat-case-bar" aria-hidden="true">
                <template v-if="caseBarHasData">
                  <span class="plan-stat-case-bar__seg is-pass" :style="{ flexGrow: Math.max(plan.passCount || 0, 0) }"></span>
                  <span class="plan-stat-case-bar__seg is-fail" :style="{ flexGrow: Math.max(plan.failCount || 0, 0) }"></span>
                  <span class="plan-stat-case-bar__seg is-unexec" :style="{ flexGrow: Math.max(plan.unexecutedCount || 0, 0) }"></span>
                </template>
                <span v-else class="plan-stat-case-bar__seg is-empty"></span>
              </div>
            </div>
          </div>
          <div class="plan-statistical-item plan-stat-card plan-stat-card--key plan-stat-card--defect"><defect-total :plan="plan" /></div>
          <div class="plan-statistical-item plan-stat-card plan-stat-card--rate"><defect-discovery-rate :plan="plan" /></div>
          <div class="plan-statistical-item plan-stat-card plan-stat-card--rate"><defect-repair-rate :plan="plan" /></div>
          <div class="plan-statistical-item plan-stat-card plan-stat-card--rate"><defect-detection-rate :plan="plan" /></div>
          <div class="plan-statistical-item plan-stat-card plan-stat-card--rate"><defect-severity-rate :plan="plan" /></div>
          <div class="plan-statistical-item plan-stat-card plan-stat-card--rate"><defect-restart-rate :plan="plan" /></div>
          <div class="plan-statistical-item plan-stat-card plan-stat-card--rate"><defect-escape-rate :plan="plan" /></div>
          <div class="plan-statistical-item plan-stat-card plan-stat-card--metric"><defect-density :plan="plan" /></div>
          <div class="plan-statistical-item plan-stat-card plan-stat-card--metric"><defect-repair-avg-hour :plan="plan" /></div>
        </div>
        <el-divider class="tool-divider"></el-divider>
      </div>
      <div class="plan-run-list-wrap">
        <!-- 查询条 + 树/表布局由 CaseList / DefectList 内部承担（与缺陷 list/table.vue 一致） -->
        <component
          ref="list"
          :is="listTypeComponentName"
          :show-module-tree="showModuleTree"
          @expand-module-tree="toggleModuleTreeVisible"
          @change="handleListChanged"
        >
          <template #tree="{ toolbarSyncHeight }">
            <tree-plan-item-module
              ref="treeModuleRef"
              :project-id="projectId"
              :plan-id="plan.planId"
              :statistic-type="activeMenuStatisticType"
              :show-sidebar-toggle="true"
              :toolbar-sync-height="toolbarSyncHeight"
              @toggle-sidebar="toggleModuleTreeVisible"
              @level-node-click="levelClickHandle"
              @node-click="moduleClickHandle"
              :check-visible="false"
              :edit-visible="false"
              v-resize="onTreePaneResize"
            />
          </template>
          <template #query>
            <el-radio-group class="list-switch" v-model="activeListType" size="small" @change="handleListTypeChanged">
              <el-radio-button v-for="lt in listTypes" :value="String($t(lt))" :key="lt">{{ $t(lt) }}</el-radio-button>
            </el-radio-group>
          </template>
        </component>
      </div>
    </div>
  </div>
</template>

<script>
import Cat2BugLevel from "@/components/Cat2BugLevel";
import Cat2BugText from "@/components/Cat2BugText";
import RowListMember from "@/components/RowListMember";
import Step from "@/views/system/case/components/step";
import TreePlanItemModule from "@/views/system/plan/TreePlanItemModule";
import FocusMemberList from "@/components/FocusMemberList";
import HandleCaseOfPlan from "@/components/Plan/HandleCaseOfPlan";
import Cat2BugPreviewImage from "@/components/Cat2BugPreviewImage";
import CaseList from "@/components/Plan/HandlePlanDialog/CaseList";
import DefectList from "@/components/Plan/HandlePlanDialog/DefectList";
import DefectDiscoveryRate from "@/components/Plan/statistics/DefectDiscoveryRate";
import DefectTotal from "@/components/Plan/statistics/DefectTotal";
import DefectRepairRate from "@/components/Plan/statistics/DefectRepairRate";
import DefectDensity from "@/components/Plan/statistics/DefectDensity";
import DefectDetectionRate from "@/components/Plan/statistics/DefectDetectionRate";
import DefectSeverityRate from "@/components/Plan/statistics/DefectSeverityRate";
import DefectRestartRate from "@/components/Plan/statistics/DefectRestartRate";
import DefectEscapeRate from "@/components/Plan/statistics/DefectEscapeRate";
import DefectRepairAvgHour from "@/components/Plan/statistics/DefectRepairAvgHour";
import { getPlan } from "@/api/system/plan";
import {checkPermi} from "@/utils/permission";
import { buildProjectModulePath } from "@/utils/project-module-path";
import dialogFormShortcuts from '@/mixins/dialog-form-shortcuts';
import planHandleDrawerClose from '@/mixins/plan-handle-drawer-close';
import { usePlanHandleDrawerKbd } from '@/composables/keyboard/usePlanHandleDrawerKbd';
import { storeToRefs } from "pinia";
import { useUserStore } from "@/stores/user";

/** 执行计划页：左侧树是否展开（沿用原抽屉缓存键） */
const PLAN_HANDLE_DRAWER_TREE_VISIBLE_CACHE_KEY = 'plan_handle_drawer_tree_module_visible';

export default {
  name: "HandlePlanDialog",
  // 关闭/表单快捷键走 mixin；kbd API 必须在本组件 setup 显式展开（自身 setup 时 mixin.setup 返回值可能未挂上）
  mixins: [dialogFormShortcuts, planHandleDrawerClose],
  components: { Cat2BugLevel,Step,TreePlanItemModule,
    FocusMemberList, Cat2BugPreviewImage, HandleCaseOfPlan, RowListMember, Cat2BugText, CaseList, DefectList,
    DefectDiscoveryRate, DefectTotal, DefectRepairRate, DefectDensity, DefectDetectionRate, DefectSeverityRate, DefectRestartRate, DefectEscapeRate, DefectRepairAvgHour
  },
  setup() {
    const userStore = useUserStore()
    const { config } = storeToRefs(userStore)
    return { config, ...usePlanHandleDrawerKbd() }
  },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 激活的列表类型
      activeListType: this.$i18n.t('case'),
      // 列表类型
      listTypes: [
        'case',
        'defect'
      ],
      // 查询条件
      query: {
        params: {}
      },
      // 当前计划ID
      planId: null,
      // 当前测试计划
      plan: {
        passCount: 0,
        failCount: 0,
        unexecutedCount: 0,
        itemTotal: 0,
        defectCount: 0,
        defectDiscoveryRate: 0.00,
        defectRepairRate: 0.00,
        defectDetectionRate: 0.00,
        defectSeverityRate: 0.00,
        defectRestartRate: 0.00,
        defectEscapeRate: 0.00,
        defectDensity: 0,
        defectRepairAvgHour: 0
      },
      // 当前测试计划项对象
      planItem: {},
      /** 供表单快捷键 mixin 识别表面已打开（整页常驻为 true） */
      visible: false,
      // 当前显示的数据列表的组件名称
      listTypeComponentName: null,
      // 菜单统计的类型，默认统计测试用例
      activeMenuStatisticType: null,
      /** 是否显示左侧交付物/优先级树 */
      showModuleTree: true,
      /** 避免关闭导航时重复 push */
      navigatingBack: false,
      /** el-countdown @finish 后标记已结束（避免仍显示 00） */
      planCountdownFinished: false,
    }
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
      return parseInt(this.config.currentProjectId);
    },
    getFileName: function () {
      return function (url) {
        if(!url) return null;
        let arr = url.split('\/');
        return arr[arr.length-1];
      }
    },
    /** 用例分段条是否有非零数据（全 0 时显示空轨） */
    caseBarHasData() {
      const p = this.plan || {}
      return (p.passCount || 0) + (p.failCount || 0) + (p.unexecutedCount || 0) > 0
    },
    /** 计划结束时间戳（毫秒）；无效时为 0 */
    planCountdownTarget() {
      const raw = this.plan && this.plan.planEndTime
      if (raw == null || raw === '') return 0
      const t = new Date(raw).getTime()
      return Number.isFinite(t) ? t : 0
    },
    /** 已到/超过结束时间，或倒计时 finish */
    planCountdownEnded() {
      if (this.planCountdownFinished) return true
      const t = this.planCountdownTarget
      return t > 0 && t <= Date.now()
    },
  },
  created() {
    const treeVis = this.$cache.local.get(PLAN_HANDLE_DRAWER_TREE_VISIBLE_CACHE_KEY);
    this.showModuleTree = !(treeVis === '0' || treeVis === 'false');
  },
  watch: {
    '$route.params.planId': {
      immediate: true,
      handler(planId) {
        if (!planId) return
        // 等到实例方法挂齐再打开，避免 immediate 早于 setup 合并
        this.$nextTick(() => this.bootstrapPlanRun(planId))
      }
    }
  },
  methods: {
    checkPermi,
    onPlanCountdownFinish() {
      this.planCountdownFinished = true
    },
    reset() {
      this.planCountdownFinished = false
      this.query = {
        pageNum: 1,
        pageSize: 10,
        moduleId: null,
        planId: null,
        projectId: this.projectId,
        params:{}
      }
      this.resetForm('queryForm');
    },
    /** 返回计划列表 */
    goBackToList() {
      if (this.navigatingBack) return
      this.navigatingBack = true
      const { teamKey, projectKey } = this.$route.params
      const path = buildProjectModulePath(teamKey, projectKey, 'plan')
      if (path) {
        this.$router.push(path).finally(() => {
          this.navigatingBack = false
        })
      } else {
        this.$router.back()
        this.navigatingBack = false
      }
    },
    /** 取消 / 返回 */
    cancel() {
      this.requestClosePlanHandleDrawer();
    },
    /** 关闭执行页：清理状态并回到计划列表（覆盖抽屉 mixin） */
    doClosePlanHandleDrawer() {
      this.visible = false
      this.reset()
      this.$emit('change')
      this.$emit('close')
      this.goBackToList()
    },
    bootstrapPlanRun(planId) {
      if (!checkPermi(['system:plan:run'])) {
        this.$modal.msgWarning('没有操作权限')
        this.goBackToList()
        return
      }
      this.open(planId)
    },
    /** 打开执行页内容（由路由 planId 驱动） */
    open(planId) {
      this.planId = planId;
      this.visible = true;
      if (typeof this.onHandlePlanDrawerOpenedKbd === 'function') {
        this.onHandlePlanDrawerOpenedKbd();
      }
      this.$nextTick(()=>{
        this.loading = true;
        this.getPlanInfo(planId, true);
        this.handleListTypeChanged(this.activeListType);
      });
    },
    /** 获取计划信息 */
    getPlanInfo(planId, isFreshTreeModule) {
      this.loading = true;
      getPlan(planId).then(response => {
        this.loading = false;
        this.plan = response.data;
        if (isFreshTreeModule) {
          this.$nextTick(() => {
            this.$refs.treeModuleRef && this.$refs.treeModuleRef.reloadData();
          });
        }
      }).catch(e=>{this.loading = false;});
    },

    onTreePaneResize() {
      this.$nextTick(() => {
        const list = this.$refs.list;
        if (list && list.setDragComponentSize) {
          list.setDragComponentSize();
        }
      });
    },
    toggleModuleTreeVisible() {
      this.showModuleTree = !this.showModuleTree;
      this.$cache.local.set(PLAN_HANDLE_DRAWER_TREE_VISIBLE_CACHE_KEY, this.showModuleTree ? '1' : '0');
      this.$nextTick(() => {
        this.setDragComponentSize();
      });
    },
    /** 委托子列表计算 multipane 分隔条高度 */
    setDragComponentSize() {
      this.$nextTick(() => {
        const list = this.$refs.list;
        if (list && list.setDragComponentSize) {
          list.setDragComponentSize();
        }
      });
    },
    /** 点击模块树中的某个模块操作 */
    moduleClickHandle(moduleId) {
      this.query.moduleId = moduleId;
      this.query.caseLevel = null;
      this.query.params.caseLevel = null;
      this.getDataList();
    },
    /** 点击模块树中的某个缺陷优先级操作 */
    levelClickHandle(caseLevel) {
      this.query.moduleId=null;
      this.query.caseLevel = caseLevel;
      this.query.params.caseLevel = caseLevel;
      this.getDataList();
    },
    handlePlanItemStateSearch(state) {
      this.$nextTick(()=>{
        this.$refs.list.handlePlanItemStateSearch(state);
      });
    },
    getDataList() {
      this.$nextTick(()=>{
        this.$refs.list.open(this.planId, this.projectId, this.query);
      });
    },
    /** 处理列表类型切换 */
    handleListTypeChanged(name) {
      switch (name) {
        case this.$i18n.t('case'):
          this.activeMenuStatisticType = 'case';
          this.listTypeComponentName = 'CaseList';
          break;
        case this.$i18n.t('defect'):
          this.activeMenuStatisticType = 'defect';
          this.listTypeComponentName = 'DefectList';
          break;
        default:
          this.activeMenuStatisticType = 'case';
          this.listTypeComponentName = 'CaseList';
          break;
      }
      this.getDataList();
    },
    /** 处理列表数据变更 */
    handleListChanged() {
      this.getPlanInfo(this.planId, false);
      this.setDragComponentSize();
      this.$emit('change');
    },
  }
}
</script>
<style>
.handle-plan-page {
  --plan-drawer-title-line-height: 32px;
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

/* 底距由页面 padding-bottom 承担，避免与分页容器 padding-bottom 叠成 20px */
.handle-plan-page .pagination-container {
  padding-bottom: 0 !important;
}
.handle-plan-page .plan-run-page-header {
  flex: 0 0 auto;
  width: 100%;
  box-sizing: border-box;
  padding: 0;
}
.handle-plan-page .plan-run-title-line {
  line-height: var(--plan-drawer-title-line-height);
}
.handle-plan-page .plan-run-content {
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
  padding-left: 0;
  padding-right: 0;
  box-sizing: border-box;
}
.handle-plan-page .plan-run-content > .plan-run-statistics-band {
  flex: 0 0 auto;
  width: 100%;
  min-width: 0;
  padding-top: var(--project-list-section-v-gap, 10px);
}
.handle-plan-page .plan-run-content > .plan-run-list-wrap {
  flex: 1 1 auto;
  min-height: 0;
  min-width: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
}
.handle-plan-page .plan-run-list-wrap > * {
  flex: 1 1 auto;
  min-height: 0;
  min-width: 0;
  width: 100%;
}
.plan-item-defect-list {
  width: 400px;
  overflow: visible !important;
}
.plan-item-defect-list > .el-dropdown-menu__item {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  text-overflow: ellipsis;
  padding: 5px 10px;
}
.plan-item-defect-num {
  margin-left: 5px;
  margin-right: 5px;
  font-weight: 500;
}
</style>
<style lang="scss" scoped>
.plan-run-statistics-band {
  width: 100%;
  box-sizing: border-box;
  padding-top: var(--project-list-section-v-gap, 10px);
  .plan-statistical {
    min-width: 0;
    box-sizing: border-box;
  }
  .tool-divider {
    width: 100%;
    max-width: 100%;
    margin: var(--project-list-section-v-gap, 10px) 0;
    box-sizing: border-box;
  }
}
.report-edit-tools {
  padding: 5px 0px;
}
.plan-run-title {
  display: inline-flex;
  justify-content: flex-start;
  align-items: center;
  flex-direction: row;
  overflow: hidden;
  margin-bottom: 0;
  min-height: var(--plan-drawer-title-line-height, 36px);
  > * {
    float:left;
  }
  /* 与新建计划页 .plan-form-back：legacy-icon 根为 i.el-icon，无 el-icon-arrow-left 类 */
  .plan-run-back {
    font-size: 22px;
    cursor: pointer;
    flex-shrink: 0;
  }
  .plan-run-back:hover {
    color: var(--text-color-secondary);
  }
  .plan-run-title-name {
    //flex: 1;
    //overflow: hidden;
    //white-space: nowrap;
    //text-overflow: ellipsis;
  }
  .plan-run-title-content .defect-type-tag{
    float: left;
    margin-right: 2px;
    margin-top: 5px;
    padding: 1px 10px;
  }
  .plan-run-title-content .project-member-icons{
    float: left;
    margin-right:10px;
  }
  .plan-run-title-content .el-tag--dark.el-tag--danger{
    float: left;
    margin-right:10px;
    margin-top: 5px;
  }
  .plan-run-title-num, .plan-run-title-name {
    font-size: 20px;
    color: var(--text-color-primary);
    font-weight: 500;
    float: left;
    margin-right:10px;
  }
  > * {
    margin-right:10px;
  }
}
//.plan-item-dropdown-link {
//  cursor: pointer;
//  color: var(--cat2bug-primary, #1890ff);
//}
//.plan-item-icon-arrow-down {
//  font-size: 12px;
//}
/* 统计卡布局/视觉见 cat2bug.scss .plan-stat-card*；此处仅保留点击态 */
.click:hover {
  cursor: pointer;
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
.plan-operate {
  display: inline-flex;
  flex-direction: row;
  flex-wrap: wrap;
  column-gap: var(--cat2bug-operate-tools-gap, 10px);
  row-gap: var(--cat2bug-operate-tools-row-gap, 0);
  padding: 0 10px;
  > :deep(button){
    margin: 0;
  }
}
.list-switch {
  margin-left: 0;
  margin-right: 0;
  margin-bottom: 0;
  flex-shrink: 0;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  > * {
    box-shadow: none !important;
  }
}
</style>
