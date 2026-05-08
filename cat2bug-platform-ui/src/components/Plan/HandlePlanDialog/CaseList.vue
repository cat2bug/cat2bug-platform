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
        <el-popover
          placement="top"
          trigger="click">
          <div class="row">
            <i class="el-icon-s-fold"></i>
            <h4>{{$t('defect.display-field')}}</h4>
          </div>
          <el-divider class="plan-item-field-divider"></el-divider>
          <el-checkbox-group v-model="columnPickerCheckedKeys" class="col" @change="onPlanCaseColumnPickerChange">
            <el-checkbox v-for="c in planCaseColumnPickerOptions" :key="c.key" :label="c.key">{{ $t(c.key) }}</el-checkbox>
          </el-checkbox-group>
          <el-button
            style="padding: 9px;"
            plain
            slot="reference"
            icon="el-icon-s-fold"
            size="small"
          ></el-button>
        </el-popover>
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
      </div>
    </div>
    <cat2-bug-table
      ref="cat2BugTable"
      cache-key="plan-item-case"
      field-list-cache-key="plan-item-table-field-list"
      :sort-column-cache-key="planItemSortColumnKey"
      :sort-type-cache-key="planItemSortTypeKey"
      :columns="planCaseTableColumns"
      :data="planItemList"
      :loading="loading"
      v-resize="setDragComponentSize"
      @sort-change="handleSortChange"
      @selection-change="handleSelectionChange"
      @columns-change="onPlanCaseColumnsChange"
      @native-mousedown="handleTableMouseDown"
      @native-mouseup="handleTableMouseUp"
      @native-mousemove="handleTableMouseMove"
    >
      <template #prepend>
        <el-table-column type="selection" width="50" align="center" />
      </template>
      <template #columns="{ scope, column }">
        <span v-if="column.prop === 'caseNum'">{{ caseNumber(scope.row) }}</span>
        <dict-tag v-else-if="column.prop === 'planItemState'" :options="dict.type.plan_item_state" :value="scope.row.planItemState" />
        <div v-else-if="column.prop === 'caseName'" class="table-case-title">
          <cat2-bug-text
            :type="checkPermi(['system:case:edit']) ? 'link' : 'text'"
            v-model="scope.row.caseName + ''"
            :tooltip="scope.row.caseName"
            @click="handleOpenEditCase(scope.row)"
          />
        </div>
        <cat2-bug-level v-else-if="column.prop === 'caseLevel'" :level="scope.row.caseLevel" />
        <cat2-bug-text v-else-if="column.prop === 'casePreconditions'" v-model="scope.row.casePreconditions" :tooltip="scope.row.casePreconditions" />
        <div v-else-if="column.prop === 'caseStep'" class="table-row-full-height">
          <step :steps="scope.row.caseStep" />
        </div>
        <cat2-bug-text v-else-if="column.prop === 'caseExpect'" v-model="scope.row.caseExpect" :tooltip="scope.row.caseExpect" />
        <cat2-bug-preview-image v-else-if="column.prop === 'imgUrls'" :images="getUrl(scope.row.imgUrls)" />
        <div v-else-if="column.prop === 'annexUrls'" class="annex-list">
          <cat2-bug-text :content="file" type="down" :tooltip="file" v-for="(file, index) in getUrl(scope.row.annexUrls)" :key="index" />
        </div>
        <row-list-member v-else-if="column.prop === 'updateBy'" :members="member(scope.row)" />
        <span v-else-if="column.prop === 'updateTime'">{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
        <span v-else>{{ scope.row[column.prop] }}</span>
      </template>
      <template #append>
        <el-table-column :label="$t('operate')" align="start" class-name="no-drag small-padding fixed-width" fixed="right" width="270">
          <template slot-scope="scope">
            <div class="plan-operate">
              <plan-item-tools v-model="scope.row" :plan="parentPlan" :project-id="projectId" @change="getPlanItemList" @close="initFloatMenu" />
            </div>
          </template>
        </el-table-column>
      </template>
    </cat2-bug-table>
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
import Cat2BugTable from "@/components/Cat2BugTable";
import DictTag from "@/components/DictTag";
import RowListMember from "@/components/RowListMember";
import Step from "@/views/system/case/components/step";
import { PlanItemCaseTableOptions } from "@/components/Plan/HandlePlanDialog/plan-item-case-table-options";
import {listPlanItem, updatePlanItem} from "@/api/system/PlanItem";
import {parseTime} from "@/utils/ruoyi";
import {checkPermi} from "@/utils/permission";

/** 测试子项不通过的状态key值 */
const PLAN_ITEM_STATE_NOT_PASS = 'not_pass';
/** 计划项排序的列 */
const PLAN_ITEM_SORT_COLUMN = 'plan_item_sort_column_key';
/** 计划项排序的类型（正序、倒叙） */
const PLAN_ITEM_SORT_TYPE = 'plan_item_sort_type_key';
export default {
  name: "CaseList",
  dicts: ['plan_item_state'],
  components: { HandleCaseOfPlan, PlanItemTools, Cat2BugText, Cat2BugPreviewImage, Cat2BugLevel, Cat2BugTable, DictTag, RowListMember, Step },
  data() {
    return {
      planItemSortColumnKey: PLAN_ITEM_SORT_COLUMN,
      planItemSortTypeKey: PLAN_ITEM_SORT_TYPE,
      planCaseTableColumns: PlanItemCaseTableOptions.map((c) => ({ ...c })),
      columnPickerCheckedKeys: [],
      planCasePickerColumnList: null,
      // 鼠标是否点击
      mouseFlag: false,
      // 鼠标移动的偏移量
      mouseOffset: 0,
      loading: false,
      // 是否多选
      multiple: false,
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
    "$i18n.locale": function () {
      this.$nextTick(() => {
        this.$refs.cat2BugTable && this.$refs.cat2BugTable.doLayout();
      });
    },
  },
  computed: {
    planCaseColumnPickerOptions() {
      const ordered = this.planCasePickerColumnList;
      if (ordered && ordered.length) {
        return ordered.map((c) => ({ ...c }));
      }
      return PlanItemCaseTableOptions.filter((c) => c.showInColumnPicker !== false);
    },
    parentPlan() {
      return this.$parent && this.$parent.plan ? this.$parent.plan : this.plan;
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
    this.query.isAsc = this.$cache.local.get(PLAN_ITEM_SORT_TYPE) || null;
    this.query.orderByColumn = this.$cache.local.get(PLAN_ITEM_SORT_COLUMN) || null;
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
      this.query = { ...this.query, ...query };
      this.$nextTick(() => {
        this.$refs.cat2BugTable && this.$refs.cat2BugTable.sort(this.query.orderByColumn, this.query.isAsc);
      });
      this.getPlanItemList();
    },
    onPlanCaseColumnsChange(columns) {
      this.planCasePickerColumnList = columns.filter((c) => c.showInColumnPicker !== false).map((c) => ({ ...c }));
      this.columnPickerCheckedKeys = columns
        .filter((c) => c.visible && c.showInColumnPicker !== false)
        .map((c) => c.key);
    },
    onPlanCaseColumnPickerChange(keys) {
      this.$refs.cat2BugTable && this.$refs.cat2BugTable.setColumnsVisible(keys);
    },
    refreshPlanHeader() {
      if (this.$parent && typeof this.$parent.getPlanInfo === 'function' && this.query.planId) {
        this.$parent.getPlanInfo(this.query.planId, false);
      }
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
        this.refreshPlanHeader();
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
        this.refreshPlanHeader();
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
      this.$refs.handleCaseDialog.open(this.parentPlan, planItem, planItem.caseId, this.query);
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
      const elTable = this.$refs.cat2BugTable && this.$refs.cat2BugTable.$refs.elTable;
      if (!elTable) return;
      let tableBody = elTable.bodyWrapper;
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
