<template>
  <!-- 添加或修改测试计划对话框 -->
  <el-drawer
    custom-class="handle-plan-dialog"
    size="90%"
    :visible.sync="visible"
    direction="rtl"
    :before-close="closePlanDrawer">
    <!-- 仅标题与关闭钮同区；统计条移到正文，避免与 el-drawer__close-btn 同一 flex 行挤压 -->
    <template slot="title">
      <div class="plan-run-drawer-title">
        <div class="plan-run-title">
          <i class="el-icon-arrow-left" @click="cancel"></i>
          <div class="plan-run-title-line">
            <span class="plan-run-title-num">{{ $t('plan') }}:{{plan.planName}}</span>
            <el-button size="mini" type="success">{{ $t('version') }}:{{ plan.planVersion }}</el-button>
            <el-button size="mini" type="primary" plain>{{ $t('plan.time') }}:{{ plan.planStartTime }} - {{ plan.planEndTime }}</el-button>
          </div>
        </div>
      </div>
    </template>
    <div class="plan-run-content">
      <div class="plan-run-statistics-band">
        <div class="plan-statistical" v-loading="loading">
          <div class="plan-statistical-item">
            <div class="plan-statistical-block">
              <div style="width: 100%; display: inline-block;">
                <el-statistic
                  :format="$t('plan.countdown-time-format')"
                  :value="new Date(plan.planEndTime).getTime()"
                  :time-indices="true"
                  :title="`${$t('plan.countdown-time-title')}: `"
                >
                </el-statistic>
              </div>
            </div>
          </div>
          <div class="plan-statistical-item">
            <div class="plan-statistical-block">
              <el-statistic
                group-separator=",">
                <template slot="title">
                  <span>{{$t('case')}}: </span>
                  <span class="click" style="color: rgb(19, 206, 102);" @click.stop="handlePlanItemStateSearch('pass')">{{$t('case.pass-tested')}}</span>
                  <span>/</span>
                  <span class="click" style="color: #f56c6c;" @click.stop="handlePlanItemStateSearch('not_pass')">{{$t('case.failed-tested')}}</span>
                  <span>/</span>
                  <span class="click" style="color: #909399;" @click.stop="handlePlanItemStateSearch('unexecuted')">{{$t('unexecuted')}}</span>
                  <span>/</span>
                  <span class="click" style="font-weight: 500;" @click.stop="handlePlanItemStateSearch(null)">{{$t('total')}}{{ $t('a') }}</span>
                </template>
                <template slot="formatter">
                  <span class="click" style="color: rgb(19, 206, 102);" @click.stop="handlePlanItemStateSearch('pass')">{{plan.passCount}}</span>
                  <span>/</span>
                  <span class="click" style="color: #f56c6c;" @click.stop="handlePlanItemStateSearch('not_pass')">{{plan.failCount}}</span>
                  <span>/</span>
                  <span class="click" style="color: #909399;" @click.stop="handlePlanItemStateSearch('unexecuted')">{{plan.unexecutedCount}}</span>
                  <span>/</span>
                  <span class="click" style="font-weight: 500;" @click.stop="handlePlanItemStateSearch(null)">{{plan.itemTotal}}{{ $t('a') }}</span>
                </template>
              </el-statistic>
            </div>
          </div>
          <div class="plan-statistical-item"><defect-total :plan="plan" /></div>
          <div class="plan-statistical-item"><defect-discovery-rate :plan="plan" /></div>
          <div class="plan-statistical-item"><defect-repair-rate :plan="plan" /></div>
          <div class="plan-statistical-item"><defect-detection-rate :plan="plan" /></div>
          <div class="plan-statistical-item"><defect-severity-rate :plan="plan" /></div>
          <div class="plan-statistical-item"><defect-restart-rate :plan="plan" /></div>
          <div class="plan-statistical-item"><defect-escape-rate :plan="plan" /></div>
          <div class="plan-statistical-item"><defect-density :plan="plan" /></div>
          <div class="plan-statistical-item"><defect-repair-avg-hour :plan="plan" /></div>
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
            <el-radio-group class="list-switch" v-model="activeListType" size="small" @input="handleListTypeChanged">
              <el-radio-button v-for="lt in listTypes" :label="$t(lt).toString()" :key="lt"></el-radio-button>
            </el-radio-group>
          </template>
        </component>
      </div>
    </div>
  </el-drawer>
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

/** 执行计划抽屉：左侧树是否展开 */
const PLAN_HANDLE_DRAWER_TREE_VISIBLE_CACHE_KEY = 'plan_handle_drawer_tree_module_visible';

export default {
  name: "HandlePlanDialog",
  components: { Cat2BugLevel,Step,TreePlanItemModule,
    FocusMemberList, Cat2BugPreviewImage, HandleCaseOfPlan, RowListMember, Cat2BugText, CaseList, DefectList,
    DefectDiscoveryRate, DefectTotal, DefectRepairRate, DefectDensity, DefectDetectionRate, DefectSeverityRate, DefectRestartRate, DefectEscapeRate, DefectRepairAvgHour
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
      // 是否显示弹出层
      visible: false,
      // 当前显示的数据列表的组件名称
      listTypeComponentName: null,
      // 菜单统计的类型，默认统计测试用例
      activeMenuStatisticType: null,
      /** 是否显示左侧交付物/优先级树 */
      showModuleTree: true,
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
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    getFileName: function () {
      return function (url) {
        if(!url) return null;
        let arr = url.split('\/');
        return arr[arr.length-1];
      }
    },
  },
  created() {
    const treeVis = this.$cache.local.get(PLAN_HANDLE_DRAWER_TREE_VISIBLE_CACHE_KEY);
    this.showModuleTree = !(treeVis === '0' || treeVis === 'false');
  },
  methods: {
    checkPermi,
    reset() {
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
    /** 取消按钮 */
    cancel() {
      this.visible = false;
      this.reset();
      this.$emit('close');
    },
    /** 打开窗口 */
    open(planId) {
      this.planId = planId;
      this.visible = true;
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
    /** 关闭缺陷抽屉窗口 */
    closePlanDrawer(done) {
      done();
      this.cancel();
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
    },
  }
}
</script>
<style>
.handle-plan-dialog {
  border-left: 4px solid #ffb700;
  /* 与 el-drawer 默认结构配合：整列 flex，正文区占满标题以下直到抽屉底 */
  display: flex;
  flex-direction: column;
  height: 100%;
  box-sizing: border-box;
}
.handle-plan-dialog.el-drawer .el-drawer__body {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding: 0;
}
.handle-plan-dialog .plan-run-content {
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
  /* 与标题区统计卡片左右对齐：正文（查询条 + 树表）统一 10px 边距 */
  padding-left: 10px;
  padding-right: 10px;
  box-sizing: border-box;
}
/* 统计带占内容高度，列表区独占剩余空间（勿再对 >* 统一 flex:1，会把统计条撑满屏） */
.handle-plan-dialog .plan-run-content > .plan-run-statistics-band {
  flex: 0 0 auto;
  width: 100%;
  min-width: 0;
}
.handle-plan-dialog .plan-run-content > .plan-run-list-wrap {
  flex: 1 1 auto;
  min-height: 0;
  min-width: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
}
.handle-plan-dialog .plan-run-list-wrap > * {
  flex: 1 1 auto;
  min-height: 0;
  min-width: 0;
  width: 100%;
}
.handle-plan-dialog > .el-drawer__header {
  /* 与 .plan-run-title-line 高度一致，供关闭按钮垂直对齐 */
  --plan-drawer-title-line-height: 36px;
  /* 右侧为关闭钮预留宽度 + 与标题文案的间隔（长标题换行时不贴按钮） */
  --plan-drawer-header-close-reserve: 48px;
  margin-bottom: 0px;
  flex-shrink: 0;
  padding: 16px var(--plan-drawer-header-close-reserve) 8px 10px;
  box-sizing: border-box;
  position: relative;
}
.handle-plan-dialog .plan-run-title-line {
  line-height: var(--plan-drawer-title-line-height);
}
/* 与左侧标题首行垂直居中 */
.handle-plan-dialog > .el-drawer__header > .el-drawer__close-btn {
  position: absolute;
  right: 10px;
  top: calc(16px + var(--plan-drawer-title-line-height) / 2);
  transform: translateY(-50%);
  margin: 0;
}
.plan-item-defect-list {
  width: 400px;
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
.plan-run-drawer-title {
  width: 100%;
  box-sizing: border-box;
}
.plan-run-statistics-band {
  width: 100%;
  box-sizing: border-box;
  padding-top: 0;
  .plan-statistical {
    min-width: 0;
    box-sizing: border-box;
  }
  .tool-divider {
    width: 100%;
    max-width: 100%;
    margin: 10px 0 10px;
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
  .el-icon-arrow-left {
    font-size: 22px;
  }
  .el-icon-arrow-left:hover {
    cursor: pointer;
    color: #909399;
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
    color: #303133;
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
//  color: #409EFF;
//}
//.plan-item-icon-arrow-down {
//  font-size: 12px;
//}
/*
 * 统计块布局参考仪表盘 PlanStatisticsChart .plan-statistics；
 * 统计区在 .plan-run-content 内，与查询条同为左右 10px。
 */
.plan-statistical {
  width: 100%;
  max-width: 100%;
  padding: 0;
  box-sizing: border-box;
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 10px;
  align-content: flex-start;
}
/* flex-basis 用 0 + flex-grow:1，同一行内卡片均分宽度，避免行尾留空（1 1 120px 易算出不铺满） */
.plan-statistical-item {
  flex: 1 1 0;
  min-width: 100px;
  max-width: 100%;
  box-sizing: border-box;
  border-radius: 4px;
  border: 1px solid #ebeef5;
  background-color: #fff;
  overflow: hidden;
  color: #303133;
  transition: 0.3s;
  /* 与仪表盘项 padding 左右 5px 接近；保留略大的上下留白 */
  padding: 10px 5px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}
/* 首格（倒计时）略宽下限，仍参与同行均分 */
.plan-statistical-item:first-child {
  flex: 1 1 0;
  min-width: 180px;
}
.plan-statistical-item:hover {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}
/* 缺陷统计组件根节点多为 el-tooltip，在格子内居中撑满 */
.plan-statistical-item ::v-deep(.el-tooltip) {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
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
  > ::v-deep button {
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
