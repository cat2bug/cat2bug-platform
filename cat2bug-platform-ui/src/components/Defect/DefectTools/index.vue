<template>
  <div class="defect-tools">
    <slot name="left"></slot>
    <star-switch v-model="defect.collect" @change="clickCollectHandle($event, defect, false)"></star-switch>

    <el-button v-show="assignVisible" :icon="isShowIcon?'el-icon-refresh':''" :size="size" :type="isText?'text':'info'" @click="assignHandle" v-hasPermi="['system:defect:assign']">{{$i18n.t('assign')}}</el-button>
    <el-button v-show="repairVisible" :icon="isShowIcon?'el-icon-document-checked':''" :size="size" :type="isText?'text':'primary'" @click="repairDialogHandle" v-hasPermi="['system:defect:repair']">{{$i18n.t('repair')}}</el-button>
    <el-button v-show="rejectVisible" :icon="isShowIcon?'el-icon-document-delete':''" :size="size" :type="isText?'text':'warning'" @click="rejectHandle" v-hasPermi="['system:defect:reject']">{{$i18n.t('reject')}}</el-button>
    <el-button v-show="passVisible" :icon="isShowIcon?'el-icon-finished':''" :size="size" :type="isText?'text':'success'" @click="passDialogHandle" v-hasPermi="['system:defect:pass']">{{$i18n.t('pass')}}</el-button>
    <el-button v-show="openVisible" :icon="isShowIcon?'el-icon-document-copy':''" :size="size" :type="isText?'text':'danger'" @click="openDialogHandle" v-hasPermi="['system:defect:open']">{{$i18n.t('open')}}</el-button>
    <el-button v-show="closeVisible" :icon="isShowIcon?'el-icon-takeaway-box':''" :size="size" :type="isText?'text':'danger'" @click="closeDialogHandle" v-hasPermi="['system:defect:close']">{{$i18n.t('close')}}</el-button>
    <el-button v-show="editVisible" :icon="isShowIcon?'el-icon-delete':''" :size="size" :type="isText?'text':'primary'" :plain="!isShowIcon" @click="editDialogHandle" >{{ $t('modify') }}</el-button>
    <el-button v-show="deleteVisible" :icon="isShowIcon?'el-icon-delete':''" :size="size" :type="isText?'text':'danger'" :class="isText?'red':''" :plain="!isShowIcon" @click="handleDelete">{{$i18n.t('delete')}}</el-button>
    <!--          <el-button-->
    <!--            size="mini"-->
    <!--            type="text"-->
    <!--            icon="el-icon-edit"-->
    <!--            @click="handleUpdate(scope.row)"-->
    <!--            v-hasPermi="['system:defect:edit']"-->
    <!--          >修改</el-button>-->
    <assign-dialog ref="assignDialog" :project-id="defect.projectId" :defect-id="defect.defectId" @log="logHandle" />
    <repair-dialog ref="repairDialog" :project-id="defect.projectId" :defect-id="defect.defectId" @log="logHandle" />
    <reject-dialog ref="rejectDialog" :project-id="defect.projectId" :defect-id="defect.defectId" @log="logHandle" />
    <pass-dialog ref="passDialog" :project-id="defect.projectId" :defect-id="defect.defectId" @log="logHandle" />
    <open-dialog ref="openDialog" :project-id="defect.projectId" :defect-id="defect.defectId" @log="logHandle" />
    <close-dialog ref="closeDialog" :project-id="defect.projectId" :defect-id="defect.defectId" @log="logHandle" />
    <edit-defect-dialog ref="editDialog"  :project-id="defect.projectId" :defect-id="defect.defectId" @log="logHandle" />
    <slot name="right"></slot>
  </div>
</template>

<script>
import AssignDialog from "@/components/Defect/DefectTools/AssignDialog";
import RejectDialog from "@/components/Defect/DefectTools/RejectDialog";
import RepairDialog from "@/components/Defect/DefectTools/RepairDialog";
import PassDialog from "@/components/Defect/DefectTools/PassDialog";
import CloseDialog from "@/components/Defect/DefectTools/CloseDialog";
import OpenDialog from "@/components/Defect/DefectTools/OpenDialog";
import EditDefectDialog from "@/components/Defect/EditDefectDialog";
import StarSwitch from "@/components/StarSwitch";
import {delDefect, updateUserDefect} from "@/api/system/defect";
import {checkPermi} from "@/utils/permission";
import i18n from "@/utils/i18n/i18n";

/** 处理中状态 */
const PROCESSING_STATE = 'PROCESSING';
/** 待审核 */
const AUDIT_STATE = 'AUDIT';
/** 已解决 */
const RESOLVED_STATE = 'RESOLVED';
/** 已驳回 */
const REJECTED_STATE = 'REJECTED';
/** 关闭状态 */
const CLOSE_STATE = 'CLOSED';
export default {
  name: "DefectTools",
  components: {PassDialog, AssignDialog, RejectDialog, RepairDialog,CloseDialog,OpenDialog,EditDefectDialog,StarSwitch },
  model: {
    prop: 'defect',
    event: 'update'
  },
  props:{
    defect: {
      type: Object,
      default: {}
    },
    isText: {
      type: Boolean,
      default: null
    },
    isShowIcon: {
      type: Boolean,
      default: null
    },
    size: {
      type: String,
      default: null
    }
  },
  computed: {
    /** 获取当前用户id */
    currentUserId: function() {
      return this.$store.state.user.id;
    },
    // 指派
    assignVisible: function () {
      return this.defect.defectStateName!=CLOSE_STATE && this.defect.defectStateName!=RESOLVED_STATE;
    },
    // 修复
    repairVisible: function () {
      return this.defect.defectStateName==PROCESSING_STATE || this.defect.defectStateName==REJECTED_STATE;
    },
    // 驳回
    rejectVisible: function () {
      return this.defect.defectStateName==AUDIT_STATE;
    },
    // 通过
    passVisible: function () {
      return this.defect.defectStateName==AUDIT_STATE;
    },
    // 关闭
    closeVisible: function () {
      return this.defect.defectStateName!=CLOSE_STATE && this.defect.defectStateName!=RESOLVED_STATE;
    },
    // 开启
    openVisible: function () {
      return this.defect.defectStateName==CLOSE_STATE;
    },
    deleteVisible: function () {
      return this.defect.createById==this.currentUserId || checkPermi(['system:defect:remove']);
    },
    editVisible: function () {
      return this.defect.createById==this.currentUserId || checkPermi(['system:defect:edit']);
    },
  },
  methods:{
    /** 指派 */
    assignHandle(event){
      this.$refs.assignDialog.open();
      event.stopPropagation();
    },
    rejectHandle(event) {
      this.$refs.rejectDialog.open();
      event.stopPropagation();
    },
    repairDialogHandle(event) {
      this.$refs.repairDialog.open();
      event.stopPropagation();
    },
    passDialogHandle(event) {
      this.$refs.passDialog.open();
      event.stopPropagation();
    },
    closeDialogHandle(event){
      this.$refs.closeDialog.open();
      event.stopPropagation();
    },
    openDialogHandle(event){
      this.$refs.openDialog.open();
      event.stopPropagation();
    },
    /** 打开编辑窗体 */
    editDialogHandle(event) {
      this.$refs.editDialog.open(this.defect.defectId);
      event.stopPropagation();
    },
    logHandle(log) {
      this.$emit('log',log);
    },
    /** 点击收藏操作 */
    clickCollectHandle(collect, defect,isRefreshList) {
      // 保存收藏状态
      updateUserDefect(defect.defectId, {
        defectId: defect.defectId,
        userId: this.currentUserId,
        collect: collect?1:0
      }).then(res=>{
        this.$set(defect, 'collect', collect);
        // 如果collect参数存在，表明是点击头部的收藏按钮触发
        if(isRefreshList) {
          this.$emit('update',this.defect);
        }
        if(collect) {
          this.$message.success(this.$i18n.t('collect-success').toString());
        } else {
          this.$message.success(this.$i18n.t('cancel-success').toString());
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(event) {
      const defectIds = this.defect.defectId;
      this.$modal.confirm(
        this.$i18n.t('defect.delete-defect'),
        this.$i18n.t('prompted').toString(),
        {
          confirmButtonText: i18n.t('delete').toString(),
          cancelButtonText: i18n.t('cancel').toString(),
          confirmButtonClass: 'delete-button',
          type: "warning"
        }).then(function() {
        return delDefect(defectIds);
      }).then(() => {
        this.$emit('delete',this.defect);
        this.$modal.msgSuccess(this.$i18n.t('delete.success'));
      }).catch(() => {});
      event.stopPropagation();
    },
  }
}
</script>

<style lang="scss" scoped>
  .defect-tools {
    display: flex;
    flex-wrap: wrap;
    flex-direction: row;
    gap: 10px;
    align-items: center;
    justify-content: flex-start !important;
    font-size: 12px;
    margin-bottom: 0px !important;
    > * {
      margin: 0;
    }
  }
  .red {
    color: #f56c6c;
  }
</style>
