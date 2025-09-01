<template>
  <div class="plan-item-content">
    <div class="plan-item-query">
      <div class="row">
        <slot name="query"></slot>
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
      </div>
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
    <el-table ref="planItemTable" v-loading="loading" :data="planItemList"
              @sort-change="handleSortChange"
              @selection-change="handleSelectionChange"
              @mousedown.native="handleTableMouseDown"
              @mouseup.native="handleTableMouseUp"
              @mousemove.native="handleTableMouseMove"
              v-resize="setDragComponentSize">
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
          <div class="table-row-full-height">
            <step :steps="scope.row.caseStep" />
          </div>
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
            <plan-item-tools v-model="scope.row" :plan="plan" :project-id="projectId" @change="getPlanItemList" @close="initFloatMenu"></plan-item-tools>
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
    <handle-case-of-plan ref="handleCaseDialog" :module-id="planItem.moduleId" :append-to-body="true" @change="getPlanItemList" @close="initFloatMenu" />
  </div>
</template>

<script>
import HandleCaseOfPlan from "@/components/Plan/HandleCaseOfPlan";
import PlanItemTools from "@/components/Plan/PlanItemTools";
import Cat2BugText from "@/components/Cat2BugText";
import Cat2BugPreviewImage from "@/components/Cat2BugPreviewImage";
import Cat2BugLevel from "@/components/Cat2BugLevel";
import Step from "@/views/system/case/components/step";
import {listPlanItem, updatePlanItem} from "@/api/system/PlanItem";
import {parseTime} from "@/utils/ruoyi";
import {checkPermi} from "@/utils/permission";

/** 需要显示的测试用例字段列表在缓存的key值 */
const PLAN_ITEM_TABLE_FIELD_LIST_CACHE_KEY='plan-item-table-field-list';
/** 测试子项不通过的状态key值 */
const PLAN_ITEM_STATE_NOT_PASS = 'not_pass';
/** 计划项排序的列 */
const PLAN_ITEM_SORT_COLUMN = 'plan_item_sort_column_key';
/** 计划项排序的类型（正序、倒叙） */
const PLAN_ITEM_SORT_TYPE = 'plan_item_sort_type_key';
export default {
  name: "CaseList",
  dicts: ['plan_item_state'],
  components: { HandleCaseOfPlan, PlanItemTools, Cat2BugText, Cat2BugPreviewImage, Cat2BugLevel, Step },
  data() {
    return {
      // 鼠标是否点击
      mouseFlag: false,
      // 鼠标移动的偏移量
      mouseOffset: 0,
      loading: false,
      // 是否多选
      multiple: false,
      // 表格中可以显示的字段列表
      checkedFieldList: [],
      // 所有属性类型
      fieldList: [],
      plan: {},
      planItem: {},
      projectId: null,
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
  watch: {
    "$i18n.locale": function (newVal, oldVal) {
      this.setFieldList();
    },
  },
  computed: {
    /** 字段是否显示 */
    showField: function () {
      return function (field) {
        return this.checkedFieldList.filter(f => f == field).length > 0;
      }
    },
    /** 用于显示的用例编号 */
    caseNumber: function () {
      return function (val) {
        return '#'+val.caseNum;
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
    this.query.isAsc = this.$cache.local.get(PLAN_ITEM_SORT_TYPE)||null;
    this.query.orderByColumn = this.$cache.local.get(PLAN_ITEM_SORT_COLUMN)||null;
    // 设置缺陷列表显示哪些列属性
    this.setFieldList();
  },
  methods: {
    checkPermi,
    parseTime,
    setDragComponentSize() {
      // 组件尺寸改变
      this.$emit('resize');
    },
    initFloatMenu() {
      this.$emit('init-float-menu');
    },
    open(planId, projectId, query) {
      this.projectId = projectId;
      this.query.planId = planId;
      this.query.projectId = projectId;
      this.query = {...this.query, ...query}
      this.$refs.planItemTable.sort(this.query.orderByColumn, this.query.isAsc);
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
    /** 打开编辑用例窗口 */
    handleOpenEditCase(planItem) {
      this.$refs.handleCaseDialog.open(this.plan, planItem, planItem.caseId, this.query);
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
    handleTableMouseMove(e) {
      // 这里面需要注意，通过ref需要那个那个包含table元素的父元素
      let tableBody = this.$refs.planItemTable.bodyWrapper;
      if (this.mouseFlag) {
        // 设置水平方向的元素的位置
        tableBody.scrollLeft -= (- this.mouseOffset + (this.mouseOffset = e.clientX));
      }
    },
  }
}
</script>

<style lang="scss" scoped>
.plan-item-content {
  flex-grow: 1;
  overflow:hidden;
  height: 100%;
  padding-bottom: 30px;
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
.handle-plan-tools-right {
  display: inline-flex;
  flex-direction: row;
  gap: 10px;
}
.plan-item-field-divider {
  margin: 8px 0px;
}
.row {
  display: flex;
  flex-direction: row;
  align-items: center;
  > * {
    margin: 0px 5px 0px 0px;
  }
}
.col {
  display: flex;
  flex-direction: column;
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
</style>
