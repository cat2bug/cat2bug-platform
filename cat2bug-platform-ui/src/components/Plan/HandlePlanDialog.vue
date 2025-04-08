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

        <div class="plan-statistical">
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
          <div class="plan-statistical-block">
            <el-statistic
              :title="$t('plan.defect-count')"
            >
              <template slot="formatter">
                {{`${plan.defectCount}`}}{{ $t('a') }}
              </template>
            </el-statistic>
          </div>
          <div class="plan-statistical-block">
            <el-statistic
              group-separator=","
              :title="$t('plan.defect-discovery-rate')"
            >
              <template slot="formatter">
                {{`${plan.defectDiscoveryRate}`}}
              </template>
            </el-statistic>
          </div>
          <div class="plan-statistical-block">
            <el-statistic
              group-separator=","
              :title="$t('plan.defect-repair-rate')"
            >
              <template slot="formatter">
                {{`${plan.defectRepairRate}`}}
              </template>
            </el-statistic>
          </div>
          <div class="plan-statistical-block">
            <el-statistic
              group-separator=","
              :title="$t('plan.defect-density')"
            >
              <template slot="formatter">
                {{`${plan.defectDensity}`}}
              </template>
            </el-statistic>
          </div>
          <div class="plan-statistical-block">
            <el-statistic
              group-separator=","
              :title="$t('plan.defect-detection-rate')"
            >
              <template slot="formatter">
                {{`${plan.defectDetectionRate}`}}
              </template>
            </el-statistic>
          </div>
          <div class="plan-statistical-block">
            <el-statistic
              group-separator=","
              :title="$t('plan.defect-severity-rate')"
            >
              <template slot="formatter">
                {{`${plan.defectSeverityRate}`}}
              </template>
            </el-statistic>
          </div>
          <div class="plan-statistical-block">
            <el-statistic
              group-separator=","
              :title="$t('plan.defect-restart-rate')"
            >
              <template slot="formatter">
                {{`${plan.defectRestartRate}`}}
              </template>
            </el-statistic>
          </div>
          <div class="plan-statistical-block">
            <el-statistic
              group-separator=","
              :title="$t('plan.defect-escape-rate')"
            >
              <template slot="formatter">
                {{`${plan.defectEscapeRate}`}}
              </template>
            </el-statistic>
          </div>
          <div class="plan-statistical-block">
            <el-statistic
              group-separator=","
              :title="$t('plan.defect-repair-avg-hour')"
            >
              <template slot="formatter">
                {{`${plan.defectRepairAvgHour}`}}h
              </template>
            </el-statistic>
          </div>
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
                                 @level-node-click="levelClickHandle"
                                 @node-click="moduleClickHandle" :check-visible="false" :edit-visible="false" v-resize="setDragComponentSize" />
        </div>
        <multipane-resizer :style="multipaneStyle"></multipane-resizer>
        <!--      用例列表-->
        <div ref="caseContext" class="plan-item-content">
          <div class="plan-item-query">
            <el-form :model="query" ref="queryForm" size="small" :inline="true">
              <el-form-item label="" prop="caseName">
                <el-input
                  size="small"
                  v-model="query.params.caseName"
                  :placeholder="$t('case.please-enter-title')"
                  prefix-icon="el-icon-search"
                  clearable
                  @input="getPlanItemList()"
                />
              </el-form-item>
              <el-form-item label="" prop="planItemState">
                <el-select v-model="query.planItemState" :placeholder="$t('plan.enter-item-state')" clearable @change="getPlanItemList">
                  <el-option
                    v-for="dict in dict.type.plan_item_state"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  />
                </el-select>
              </el-form-item>
<!--              <el-form-item label="" prop="updateById">-->
<!--                <el-input-->
<!--                  v-model="query.updateById"-->
<!--                  placeholder="请输入更新人"-->
<!--                  clearable-->
<!--                  @keyup.enter.native="handleQuery"-->
<!--                />-->
<!--              </el-form-item>-->
            </el-form>
            <div class="handle-plan-tools-right">
              <el-col :span="1.5">
                <el-button
                  size="small"
                  type="primary"
                  icon="el-icon-check"
                  :disabled="!multiple"
                  v-hasPermi="['system:plan:edit']"
                  @click="handlePlanItemState(null, 'pass')"
                >{{ $t('batch-pass') }}</el-button>
              </el-col>
              <el-popover
                placement="top"
                trigger="click">
                <div class="row">
                  <i class="el-icon-s-fold"></i>
                  <h4>{{$t('defect.display-field')}}</h4>
                </div>
                <el-divider class="plan-item-field-divider"></el-divider>
                <el-checkbox-group v-model="checkedFieldList" class="col" @change="checkedFieldListChange">
                  <el-checkbox v-for="field in fieldList" :label="field" :key="field">{{$t(field)}}</el-checkbox>
                </el-checkbox-group>
                <el-button
                  style="padding: 9px;"
                  plain
                  slot="reference"
                  icon="el-icon-s-fold"
                  size="small"
                ></el-button>
              </el-popover>
            </div>
          </div>
          <el-table ref="planItemTable" v-loading="loading" :data="planItemList" v-resize="setDragComponentSize" @sort-change="handleSortChange" @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="50" align="center" />
            <el-table-column v-if="showField('id')" :label="$t('id')" align="left" prop="caseNum" width="80" sortable="custom" fixed>
              <template slot-scope="scope">
                <span>{{ caseNumber(scope.row) }}</span>
              </template>
            </el-table-column>
            <el-table-column v-if="showField('state')" :label="$t('state')" align="center" prop="planItemState" sortable="custom" fixed>
              <template slot-scope="scope">
                <dict-tag :options="dict.type.plan_item_state" :value="scope.row.planItemState"/>
              </template>
            </el-table-column>
            <el-table-column v-if="showField('title')" :label="$t('title')" align="left" prop="caseName" min-width="300" sortable="custom" fixed>
              <template slot-scope="scope">
                <div class="table-case-title">
                  <cat2-bug-text :type="checkPermi(['system:case:edit'])?'link':'text'" v-model="scope.row.caseName+''" :tooltip="scope.row.caseName"  @click="handleOpenEditCase(scope.row)" />
                </div>
              </template>
            </el-table-column>
            <el-table-column v-if="showField('module')" :label="$t('module')" align="left" prop="moduleName" sortable="custom" min-width="150" />
            <el-table-column v-if="showField('level')" :label="$t('level')" align="left" prop="caseLevel" sortable="custom" width="80">
              <template slot-scope="scope">
                <cat2-bug-level :level="scope.row.caseLevel" />
              </template>
            </el-table-column>
            <el-table-column v-if="showField('preconditions')" :label="$t('preconditions')" align="left" prop="casePreconditions" min-width="250" sortable="custom">
              <template slot-scope="scope">
                <cat2-bug-text v-model="scope.row.casePreconditions" :tooltip="scope.row.casePreconditions" />
              </template>
            </el-table-column>
            <el-table-column v-if="showField('step')" :label="$t('step')" align="left" prop="caseStep" width="300" sortable="custom">
              <template slot-scope="scope">
                <step :steps="scope.row.caseStep" style="width: 300px;" />
              </template>
            </el-table-column>
            <el-table-column v-if="showField('data')" :label="$t('data')" class-name="fixed-width" align="left" prop="caseData" min-width="250" sortable="custom" />
            <el-table-column v-if="showField('expect')" :label="$t('expect')" align="left" prop="caseExpect" min-width="250" sortable="custom">
              <template slot-scope="scope">
                <cat2-bug-text v-model="scope.row.caseExpect" :tooltip="scope.row.caseExpect" />
              </template>
            </el-table-column>
            <el-table-column v-if="showField('image')" :label="$t('image')" :key="$t('image')" align="center" prop="imgUrls" width="100">
              <template slot-scope="scope">
                <cat2-bug-preview-image :images="getUrl(scope.row.imgUrls)" />
              </template>
            </el-table-column>
            <el-table-column v-if="showField('annex')" :label="$t('annex')" :key="$t('annex')" align="left" prop="annexUrls" min-width="300">
              <template slot-scope="scope">
                <div class="annex-list">
                  <cat2-bug-text :content="file" type="down" :tooltip="file" v-for="(file,index) in getUrl(scope.row.annexUrls)" :key="index" />
                </div>
              </template>
            </el-table-column>
            <el-table-column v-if="showField('updateBy')" :label="$t('updateBy')" align="center" prop="updateBy" sortable="custom" width="120">
              <template slot-scope="scope">
                <row-list-member :members="member(scope.row)"></row-list-member>
              </template>
            </el-table-column>
            <el-table-column v-if="showField('update-time')" :label="$t('updateTime')" align="center" prop="updateTime" width="180">
              <template slot-scope="scope">
                <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="$t('operate')" align="start" class-name="small-padding fixed-width" fixed="right" width="270">
              <template slot-scope="scope">
                <div class="plan-operate">
                  <plan-item-tools v-model="scope.row" :plan="plan" :project-id="projectId" @change="handlePlanItemChange" @close="initFloatMenu"></plan-item-tools>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <pagination
            v-show="total>0"
            :total="total"
            :page.sync="query.pageNum"
            :limit.sync="query.pageSize"
            @pagination="getPlanItemList"
          />
        </div>
      </multipane>
      <handle-case-of-plan ref="handleCaseDialog" :module-id="planItem.moduleId" :append-to-body="true" @change="getPlanItemList" @close="initFloatMenu" />
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

import { Multipane, MultipaneResizer } from 'vue-multipane';
import { getPlan } from "@/api/system/plan";
import {listPlanItem, updatePlanItem} from "@/api/system/PlanItem";
import {checkPermi} from "@/utils/permission";
import {listDefect} from "@/api/system/defect";
import PlanItemTools from "@/components/Plan/PlanItemTools";

const TREE_MODULE_WIDTH_CACHE_KEY = 'plan_case_tree_module_width';
/** 需要显示的测试用例字段列表在缓存的key值 */
const PLAN_ITEM_TABLE_FIELD_LIST_CACHE_KEY='plan-item-table-field-list';
/** 测试子项不通过的状态key值 */
const PLAN_ITEM_STATE_NOT_PASS = 'not_pass';
/** 计划项排序的列 */
const PLAN_ITEM_SORT_COLUMN = 'plan_item_sort_column_key';
/** 计划项排序的类型（正序、倒叙） */
const PLAN_ITEM_SORT_TYPE = 'plan_item_sort_type_key';
export default {
  name: "AddPlanDialog",
  dicts: ['plan_item_state'],
  components: { Cat2BugLevel,Step,TreePlanItemModule,Multipane,MultipaneResizer, FocusMemberList, Cat2BugPreviewImage, HandleCaseOfPlan, RowListMember, Cat2BugText, PlanItemTools },
  data() {
    return {
      // 动态样式
      multipaneStyle: {'--marginTop':'0px'},
      treeModuleStyle: {'--treeModuleWidth':'300px'},
      // 遮罩层
      loading: true,
      // 表格中可以显示的字段列表
      checkedFieldList: [],
      // 所有属性类型
      fieldList: [],
      // 当前计划
      plan: {},
      planItem: {},
      // 是否显示弹出层
      visible: false,
      // 查询参数
      query: {
        pageNum: 1,
        pageSize: 10,
        moduleId: null,
        planId: null,
        projectId: this.projectId,
        params:{}
      },
      // 测试计划子项表格数据
      planItemList: [],
      // 总条数
      total: 0,
      // 表格的多选项
      ids: [],
      // 是否多选
      multiple: false
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
    /** 字段是否显示 */
    showField: function () {
      return function (field) {
        return this.checkedFieldList.filter(f=>f==field).length>0;
      }
    },
    /** 成员结构 */
    member: function () {
      return function (planItem) {
        return [{
          nickName: planItem.updateBy
        }]
      }
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
    getFileName: function () {
      return function (url) {
        if(!url) return null;
        let arr = url.split('\/');
        return arr[arr.length-1];
      }
    },
  },
  watch: {
    "$i18n.locale": function (newVal, oldVal) {
      this.setFieldList();
    },
  },
  created() {
    this.query.isAsc = this.$cache.local.get(PLAN_ITEM_SORT_TYPE)||null;
    this.query.orderByColumn = this.$cache.local.get(PLAN_ITEM_SORT_COLUMN)||null;
    // 设置缺陷列表显示哪些列属性
    this.setFieldList();
  },
  methods: {
    checkPermi,
    /** 初始化浮动菜单 */
    initFloatMenu() {
      this.$floatMenu.windowsInit(document.querySelector('.main-container'));
      this.$floatMenu.resetMenus([]);
    },
    /** 处理计划项改变的事件 */
    handlePlanItemChange() {
      this.getPlanInfo(this.plan.planId);
      this.getPlanItemList();
    },
    /** 设置列表显示的属性字段 */
    setFieldList() {
      this.fieldList = [
        'id','title','module','level', 'preconditions','step','data','expect','image','annex', 'state', 'updateBy','update-time'
      ];

      const fieldList = this.$cache.local.get(PLAN_ITEM_TABLE_FIELD_LIST_CACHE_KEY);
      if(fieldList) {
        this.checkedFieldList = JSON.parse(fieldList);
      } else {
        this.checkedFieldList = [];
        this.fieldList.forEach(f=>{
          this.checkedFieldList.push(f);
        });
      }
    },
    /** 测试用例列表属性字段改变操作 */
    checkedFieldListChange(field) {
      this.$cache.local.set(PLAN_ITEM_TABLE_FIELD_LIST_CACHE_KEY,JSON.stringify(field));
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
      this.visible = true;
      this.query.planId = planId;
      this.query.projectId = this.projectId;

      this.$nextTick(()=>{
        this.$refs.planItemTable.sort(this.query.orderByColumn, this.query.isAsc);
        this.loading = true;
        this.getPlanInfo(planId);
        this.getTreeModuleWidth();
        this.getPlanItemList();
        this.initFloatMenu();
      });
    },
    /** 获取计划信息 */
    getPlanInfo(planId) {
      getPlan(planId).then(response => {
        this.plan = response.data;
        this.$nextTick(()=>{
          this.$refs.treeModuleRef.reloadData();
        })
      });
    },
    /** 查询测试用例列表 */
    getPlanItemList() {
      this.loading = true;
      listPlanItem(this.query).then(response => {
        this.loading = false;
        this.planItemList = response.rows.map(i=>{
          i.defectList=[];
          return i;
        });
        this.total = response.total;
      });
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
      this.getPlanItemList();
    },
    /** 点击模块树中的某个缺陷优先级操作 */
    levelClickHandle(caseLevel) {
      this.query.caseLevel = caseLevel;
      this.getPlanItemList();
    },
    /** 处理添加缺陷完成操作 */
    handleAddedDefect(defect) {
      let data = {
        planId: this.planItem.planId,
        planItemId: this.planItem.planItemId,
        params: {
          defectId: defect.defectId
        },
        planItemState: PLAN_ITEM_STATE_NOT_PASS,
      }
      updatePlanItem(data).then(res=>{
        this.getPlanInfo(this.plan.planId);
        this.getPlanItemList();
        this.$emit('change');
      })
    },
    /** 处理缺陷日志添加完成操作 */
    handleDefectLogAdded(log) {
      let data = {
        planItemId: this.planItem.planItemId,
        params: {
          defectId: log.defectId
        },
        planItemState: PLAN_ITEM_STATE_NOT_PASS,
      }
      updatePlanItem(data).then(res=>{
        this.getPlanInfo(this.plan.planId);
        this.getPlanItemList();
        this.$emit('change');
      });
    },
    /** 打开编辑用例窗口 */
    handleOpenEditCase(planItem) {
      this.$refs.handleCaseDialog.open(this.plan, planItem, planItem.caseId);
    },
    /** 查询计划项 */
    handlePlanItemStateSearch(state) {
      this.query.planItemState = state;
      this.query.pageNum = 1;
      this.query.moduleId = null;
      this.getPlanItemList();
    },
    /** 下载附件操作 */
    handleDown(event, file) {
      const a = document.createElement("a");
      const e = new MouseEvent("click");
      a.href = file;
      a.dispatchEvent(e);
      event.stopPropagation();
    },
    /** 多选框选中数据 */
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.planItemId);
      this.multiple = selection.length;
    },
    /** 更改子项状态操作 */
    handlePlanItemState(planItem, state) {
      let data = {
        planItemState: state,
      }
      if(planItem) {
        data.planItemId = planItem.planItemId;
      } else if(this.ids.length>0) {
        data.params = {
          planItemIds:this.ids
        }
      }
      updatePlanItem(data).then(()=>{
        this.$message.success(this.$i18n.t('plan.pass-success').toString());
        this.getPlanItemList();
        this.$emit('change');
      });
    },
    handleSortChange(column) {
      this.query.isAsc = column.order;
      this.query.orderByColumn = column.prop;
      this.$cache.local.set(PLAN_ITEM_SORT_COLUMN, column.prop);
      this.$cache.local.set(PLAN_ITEM_SORT_TYPE, column.order);
      this.getPlanItemList();
    }
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
.red {
  color: #f56c6c;
}
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
.plan-item-field-divider {
  margin: 8px 0px;
}
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
.plan-item-content {
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
.plan-item-query {
  display: inline-flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: flex-start;
  width: 100%;
  .el-form-item {
    margin-bottom: 5px;
  }
}
.plan-run-content {
  //height: 100%;
}
.plan-item-dropdown-link {
  cursor: pointer;
  color: #409EFF;
}
.plan-item-icon-arrow-down {
  font-size: 12px;
}
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
.handle-plan-tools-right {
  display: inline-flex;
  flex-direction: row;
  gap: 10px;
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
</style>
