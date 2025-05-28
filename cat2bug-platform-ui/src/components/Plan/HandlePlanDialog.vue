<template>
  <!-- 添加或修改测试计划对话框 -->
  <el-drawer
    custom-class="handle-plan-dialog"
    size="90%"
    :visible.sync="visible"
    direction="rtl"
    :before-close="closePlanDrawer">
    <template slot="title">
      <div class="plan-run-header">
        <div class="plan-run-title">
          <i class="el-icon-arrow-left" @click="cancel"></i>
          <div style="line-height: 36px;">
            <span class="plan-run-title-num">{{ $t('plan') }}:{{plan.planName}}</span>
            <el-button size="mini" type="success">{{ $t('version') }}:{{ plan.planVersion }}</el-button>
            <el-button size="mini" type="primary" plain>{{ $t('plan.time') }}:{{ plan.planStartTime }} - {{ plan.planEndTime }}</el-button>
          </div>
        </div>

        <div class="plan-statistical" v-loading="loading">
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
<!--          缺陷总数-->
          <defect-total :plan="plan"></defect-total>
<!--          缺陷发现率-->
          <defect-discovery-rate :plan="plan"></defect-discovery-rate>
<!--          缺陷修复率-->
          <defect-repair-rate :plan="plan"></defect-repair-rate>
<!--          缺陷探测率-->
          <defect-detection-rate :plan="plan"></defect-detection-rate>
<!--          缺陷严重率-->
          <defect-severity-rate :plan="plan"></defect-severity-rate>
<!--          缺陷重开率-->
          <defect-restart-rate :plan="plan"></defect-restart-rate>
<!--          缺陷逃逸率-->
          <defect-escape-rate :plan="plan"></defect-escape-rate>
<!--          缺陷密度-->
          <defect-density :plan="plan"></defect-density>
<!--          缺陷平均时长-->
          <defect-repair-avg-hour :plan="plan"></defect-repair-avg-hour>
        </div>
        <el-divider class="tool-divider"></el-divider>
      </div>
    </template>
    <div class="plan-run-content">
      <!--    模块树和用例列表区域-->
      <multipane layout="vertical" ref="multiPane" class="custom-resizer" @pane-resize-start="dragStopHandle">
        <!--      树形模块选择组件-->
        <div class="tree-module" ref="treeModule" :style="treeModuleStyle">
          <tree-plan-item-module ref="treeModuleRef" :project-id="projectId" :plan-id="plan.planId"
                                 :statistic-type="activeMenuStatisticType"
                                 @level-node-click="levelClickHandle"
                                 @node-click="moduleClickHandle" :check-visible="false" :edit-visible="false" v-resize="setDragComponentSize" />
        </div>
        <multipane-resizer :style="multipaneStyle"></multipane-resizer>
        <!--      用例列表-->
        <div ref="caseContext" class="plan-item-content-list">
          <component ref="list" :is="listTypeComponentName" @change="handleListChanged">
            <template #query>
              <el-radio-group class="list-switch" v-model="activeListType" size="small" @input="handleListTypeChanged">
                <el-radio-button v-for="lt in listTypes" :label="$t(lt).toString()" :key="lt"></el-radio-button>
              </el-radio-group>
            </template>
          </component>
        </div>
      </multipane>
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
import { Multipane, MultipaneResizer } from 'vue-multipane';
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

const TREE_MODULE_WIDTH_CACHE_KEY = 'plan_case_tree_module_width';

export default {
  name: "HandlePlanDialog",
  components: { Cat2BugLevel,Step,TreePlanItemModule,Multipane,MultipaneResizer,
    FocusMemberList, Cat2BugPreviewImage, HandleCaseOfPlan, RowListMember, Cat2BugText, CaseList, DefectList,
    DefectDiscoveryRate, DefectTotal, DefectRepairRate, DefectDensity, DefectDetectionRate, DefectSeverityRate, DefectRestartRate, DefectEscapeRate, DefectRepairAvgHour
  },
  data() {
    return {
      // 动态样式
      multipaneStyle: {'--marginTop':'0px'},
      treeModuleStyle: {'--treeModuleWidth':'300px'},
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
      activeMenuStatisticType: null
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

  },
  methods: {
    checkPermi,
    /** 初始化浮动菜单 */
    initFloatMenu() {
      this.$floatMenu.windowsInit(document.querySelector('.main-container'));
      this.$floatMenu.resetMenus([]);
    },
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
        this.getTreeModuleWidth();
        this.handleListTypeChanged(this.activeListType);
        this.initFloatMenu();
      });
    },
    /** 获取计划信息 */
    getPlanInfo(planId, isFreshTreeModule) {
      this.loading = true;
      getPlan(planId).then(response => {
        this.loading = false;
        this.plan = response.data;
        if(isFreshTreeModule) {
          this.$nextTick(() => {
            this.$refs.treeModuleRef.reloadData();
          });
        }
      }).catch(e=>{this.loading = false;});
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
      this.cacheTreeModuleWidth();
    },
    /** 设置模块与用例列表中间拖动块的尺寸 */
    setDragComponentSize() {
      this.multipaneStyle['--marginTop'] = '0px';
      this.$nextTick(()=> {
        let pageHeight = Math.max(this.$refs.treeModule.scrollHeight || 0, this.$refs.caseContext.scrollHeight || 0)
        this.multipaneStyle['--marginTop'] = pageHeight + 'px';
      })
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
}
.handle-plan-dialog>.el-drawer__header {
  margin-bottom: 0px;
}
.handle-plan-dialog>.el-drawer__header>.el-drawer__close-btn {
  position: absolute;
  right: 20px;
  top: 30px;
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
.custom-resizer {
  width: 100%;
  height: 100%;
  padding-left: 10px;
  padding-right: 10px;
}
.custom-resizer > .multipane-resizer {
  margin: 0; left: 0;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  height: 100%;
  text-align: center;
  &:before {
     display: block;
     content: "";
     width: 3px;
     height: var(--marginTop);
     margin-top: 0px;
     margin-left: -1.5px;
     border-left: 1px solid #DCDFE6;
     border-right: 1px solid #DCDFE6;
  }
  &:hover {
    &:before {
      border-color: #CCC;
    }
  }
}
.plan-item-content-list {
  flex-grow: 1;
  overflow:hidden;
  height: 100%;
  padding-bottom: 30px;
}
.plan-run-header {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-direction: row;
  flex-wrap: wrap;
  .report-edit-tools {
    padding: 5px 0px;
  }
  .plan-run-title {
    display: inline-flex;
    justify-content: flex-start;
    align-items: center;
    flex-direction: row;
    overflow: hidden;
    margin-bottom: 20px;
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
}
//.plan-run-content {
//  //height: 100%;
//}
//.plan-item-dropdown-link {
//  cursor: pointer;
//  color: #409EFF;
//}
//.plan-item-icon-arrow-down {
//  font-size: 12px;
//}
.tool-divider {
  margin: 10px;
  width: calc(100% - 20px);
}

@media screen and (min-width: 1650px) {
  .plan-statistical {
    > div:first-child {
      min-width: 210px;
      flex: 1
    }
  }
}

@media screen and (max-width: 1650px) {
  .plan-statistical {
    > div {
      width: calc((100% - 50px) / 6);
    }
    > div:first-child, > div:nth-child(2) {
      width: calc((100% - 50px) / 6 * 1.5 + 5px);
    }
  }
}

@media screen and (max-width: 780px) {
  .plan-statistical {
    > div {
      width: calc((100% - 30px) / 4);
    }
    > div:first-child, > div:nth-child(2) {
      width: calc((100% - 30px) / 4 * 1.5 + 5px);
    }
  }
}

.plan-statistical {
  width: 100%;
  padding: 0px 10px;
  display: inline-flex;
  flex-direction: row;
  gap: 10px;
  flex-wrap: wrap;
  > div {
    padding: 0px 10px;
    display: inline-flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    border-radius: 4px;
    border: 1px solid #ebeef5;
    background-color: #fff;
    overflow: hidden;
    color: #303133;
    transition: .3s;
    padding: 10px 15px;
  }
  > div:hover {
    box-shadow: 0 2px 12px 0 rgba(0,0,0,.1);
  }
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
  gap: 10px;
  padding: 0 10px;
  > ::v-deep button {
    margin: 0;
  }
}
.list-switch {
  margin-right: 10px;
  margin-bottom: 5px;
  > * {
    box-shadow: none !important;
  }
}
</style>
