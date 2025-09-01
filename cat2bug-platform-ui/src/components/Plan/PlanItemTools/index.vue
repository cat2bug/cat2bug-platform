<template>
  <div class="plan-item-tools">
    <el-button
      size="small"
      :type="type==='text'?'text':'danger'"
      icon="el-icon-plus"
      @click="handleOpenAddDefect($event, planItem)"
      v-hasPermi="['system:defect:add']"
    >
      {{ $t('defect.create') }}
    </el-button>
    <el-button
      size="small"
      :type="type==='text'?'text':'danger'"
      icon="el-icon-attract"
      @click="handleOpenRelatedDefect($event, planItem)"
      v-hasPermi="['system:defect:add']"
    >
      {{ $t('defect.related') }}
    </el-button>
    <el-button
      v-if="planItem.defectIds && planItem.defectIds.length==1"
      size="small"
      :type="type==='text'?'text':'danger'"
      @click="handleOpenHandleDefect(planItem, planItem.defectIds[0])"
      v-hasPermi="['system:defect:edit']"
    > <svg-icon icon-class="bug"></svg-icon>
      {{ $t('defect') }}
    </el-button>
    <el-dropdown
      class="defect-dropdown"
      size="small"
      :split-button="type!=='text'"
      :type="type!=='text'?'danger':'text'"
      placement="bottom-start"
      @visible-change="handlePlanItemDefectList($event, planItem)"
      @command="handleOpenHandleDefect(planItem, $event)"
      v-else-if="planItem.defectIds && planItem.defectIds.length>1"
      v-hasPermi="['system:defect:edit']">
        <span class="plan-item-dropdown-link">
          <svg-icon icon-class="bug"></svg-icon>{{ $t('defect') }}({{ planItem.defectIds.length }})
        </span>
      <el-dropdown-menu class="plan-item-defect-list" slot="dropdown" v-loading="defectLoading">
        <el-dropdown-item v-for="(defect,defectIndex) in planItem.defectList" :key="defectIndex" :command="defect.defectId">
          <defect-state-flag :defect="defect" /><span class="plan-item-defect-num">#{{ defect.projectNum }}</span>{{ defect.defectName }}</el-dropdown-item>
      </el-dropdown-menu>
    </el-dropdown>
    <el-button
      size="small"
      :type="type"
      icon="el-icon-check"
      @click="handlePlanItemState(planItem, 'pass')"
      v-if="hasPassPermi(planItem)"
    >{{ $t('pass') }}</el-button>
    <el-button
      size="small"
      :type="type"
      icon="el-icon-close"
      @click="handlePlanItemState(planItem, 'unexecuted')"
      v-if="hasCancelPassPermi(planItem)"
    >{{ $t('cancel-pass') }}</el-button>
    <add-defect ref="addDefect" :project-id="projectId" :append-to-body="true" @added="handleAddedDefect" @close="handleComponentClose" />
    <handle-defect ref="handleDefect" :project-id="projectId" :append-to-body="true" @change="handleDefectChange" @delete="handleDeletedDefect" @close="handleComponentClose" />
    <related-defect ref="relatedDefect" :project-id="projectId" :related-defect-ids="planItem.defectIds" @related="handleRelatedDefect" @close="handleComponentClose" ></related-defect>
  </div>
</template>

<script>
import HandleDefect from "@/components/Defect/HandleDefect";
import AddDefect from "@/components/Defect/AddDefect";
import RelatedDefect from "@/components/Defect/RelatedDefect";
import DefectStateFlag from "@/components/Defect/DefectStateFlag";
import {listDefect} from "@/api/system/defect";
import {updatePlanItem} from "@/api/system/PlanItem";
import {checkPermi} from "@/utils/permission";

/** 测试子项不通过的状态key值 */
const PLAN_ITEM_STATE_NOT_PASS = 'not_pass';

export default {
  name: "PlanItemTools",
  components: {HandleDefect, AddDefect, RelatedDefect, DefectStateFlag},
  model: {
    prop: 'planItem',
    event: 'change'
  },
  props: {
    plan: {
      type: Object,
      default: ()=>{}
    },
    projectId: {
      type: [String,Number],
      default: null
    },
    planItem: {
      type: Object,
      default: ()=>{}
    },
    type: {
      type: "primary" | "success" | "warning" | "danger" | "info" | "text",
      default: 'text'
    }
  },
  data() {
    return {
      defectLoading: false,
    }
  },
  computed: {
    /** 是否有通过权限 */
    hasPassPermi: function() {
      return function (item) {
        return checkPermi(['system:plan:edit']) && item.planItemState!='pass';
      }
    },
    /** 是否有取消通过权限 */
    hasCancelPassPermi: function() {
      return function (item) {
        return checkPermi(['system:plan:edit']) && item.planItemState==='pass';
      }
    },
  },
  methods: {
    /** 处理组件关闭事件 */
    handleComponentClose() {
      this.$emit('close');
    },
    /** 处理关联缺陷 */
    handleRelatedDefect(relatedIds, defect) {
      if(relatedIds.filter(id=>id===defect.defectId).length>0) {
        this.handleAddedDefect(defect, relatedIds);
      } else {
        this.handleDeletedDefect(defect, relatedIds);
      }
    },
    /** 处理删除缺陷完成操作 */
    handleDeletedDefect(defect, ids) {
      let data = {
        planItemId: this.planItem.planItemId,
        params: {
          deleteDefectId: defect.defectId
        },
      }
      this.handleUpdatePlanItem(defect, data, ids);
    },
    /** 更新计划项操作 */
    handleUpdatePlanItem(defect, data, ids) {
      updatePlanItem(data).then(()=>{
        if(ids) {
          this.planItem.defectIds = ids;
        } else {
          this.planItem.defectIds.push(defect.defectId);
        }
        this.$emit('change', this.planItem);
      })
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
        this.planItem.planItemState = state;
        this.$message.success(this.$i18n.t('plan.pass-success').toString());
        this.$emit('change', this.planItem, state);
      });
    },
    /** 处理添加缺陷完成操作 */
    handleAddedDefect(defect, ids) {
      let data = {
        planId: this.planItem.planId,
        planItemId: this.planItem.planItemId,
        params: {
          defectId: defect.defectId
        },
        planItemState: PLAN_ITEM_STATE_NOT_PASS,
      }
      this.handleUpdatePlanItem(defect, data, ids);
    },
    /** 处理缺陷变更操作 */
    handleDefectChange() {

    },
    /** 根据ID查找缺陷 */
    handlePlanItemDefectList(event, planItem) {
      if(!event) return;
      this.defectLoading = true;
      let query = {
        pageNum: 1,
        pageSize: 9999,
        orderByColumn: 'updateTime',
        isAsc: 'desc',
        params: {
          defectIds: planItem.defectIds
        }
      }
      listDefect(query).then(res=>{
        this.defectLoading = false;
        planItem.defectList = res.rows;
      }).catch(e=>{
        this.defectLoading = false;
      })
    },
    /** 处理打开关联缺陷窗口操作 */
    handleOpenRelatedDefect(e,item){
      this.$refs.relatedDefect.open(item.defectIds);
      e.stopPropagation();
    },
    /** 打开测试用例 */
    handleOpenHandleDefect(item, defectId) {
      this.$refs.handleDefect.open(defectId);
    },
    /** 处理打开新建缺陷窗口操作 */
    handleOpenAddDefect(e,item){
      const params = {...item, ...{
          defectId: null,
          defectType: null,
          defectName: null,
          defectDescribe: null,
          annexUrls: null,
          imgUrls: null,
          projectId: this.projectId,
          testPlanId: null,
          dataSources: null,
          dataSourcesParams: null,
          createBy: null,
          updateTime: null,
          createTime: null,
          updateBy: null,
          defectState: null,
          caseStepId: 0,
          handleBy: null,
          handleTime: null,
          defectLevel: 'middle',
          moduleVersion: this.plan.planVersion
      }};
      this.$refs.addDefect.openByCase(params);
      e.stopPropagation();
    },
  }
}
</script>

<style lang="scss" scoped>
.plan-item-tools {
  display: inline-flex;
  flex-direction: row;
  gap: 10px;
  flex-wrap: wrap;
  > * {
    margin: 0;
  }
}
.defect-dropdown {
  flex-shrink: 0;
  display: inline-flex;
  justify-content: center;
  align-items: center;
  color: #409EFF;
}
</style>
