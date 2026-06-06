<template>
  <div class="defect-tools" :class="{ 'defect-tools--compact': isCompact }">
    <!-- 仅操作按钮参与表格列宽；对话框/弹层挂到隔离层，避免撑开操作列 -->
    <div class="defect-tools__bar">
      <slot name="left"></slot>
      <share-card v-show="!isDeleted" :params="defect" />
      <star-switch v-show="!isDeleted" v-model="defect.collect" @change="clickCollectHandle($event, defect, false)"></star-switch>
      <el-button v-show="assignVisible" data-defect-tool="assign" :icon="isShowIcon?'el-icon-refresh':''" :size="size" :type="isText?'text':'info'" :class="isText?'orange':''" @click="assignHandle">{{$i18n.t('assign')}}</el-button>
      <el-button v-show="repairVisible" data-defect-tool="repair" :icon="isShowIcon?'el-icon-document-checked':''" :size="size" :type="isText?'text':'primary'" :class="isText?'orange':''" @click="repairDialogHandle">{{$i18n.t('repair')}}</el-button>
    <el-button v-show="rejectVisible" data-defect-tool="reject" :icon="isShowIcon?'el-icon-document-delete':''" :size="size" :type="isText?'text':'warning'" :class="isText?'orange':''" @click="rejectHandle">{{$i18n.t('reject')}}</el-button>
      <el-button v-show="passVisible" data-defect-tool="pass" :icon="isShowIcon?'el-icon-finished':''" :size="size" :type="isText?'text':'success'" :class="isText?'orange':''" @click="passDialogHandle">{{$i18n.t('pass')}}</el-button>
      <el-button v-show="openVisible" data-defect-tool="open" :icon="isShowIcon?'el-icon-document-copy':''" :size="size" :type="isText?'text':'danger'" :class="isText?'orange':''" @click="openDialogHandle" v-hasPermi="['system:defect:open']">{{$i18n.t('open')}}</el-button>
      <el-button v-show="closeVisible" data-defect-tool="close" :icon="isShowIcon?'el-icon-takeaway-box':''" :size="size" :type="isText?'text':'danger'" :class="isText?'orange':''" @click="closeDialogHandle">{{$i18n.t('close')}}</el-button>
      <el-button v-show="viewVisible" data-defect-tool="view" :icon="isShowIcon?'el-icon-view':''" :size="size" :type="isText?'text':'warning'" @click="viewHandle">{{$i18n.t('view')}}</el-button>
      <el-button v-show="editVisible" data-defect-tool="edit" :icon="isShowIcon?'el-icon-edit':''" :size="size" :type="isText?'text':'success'" :class="isText?'green':''" :plain="!isShowIcon" @click="editDialogHandle" >{{ $t('modify') }}</el-button>
      <el-button v-show="deleteVisible" data-defect-tool="delete" :icon="isShowIcon?'el-icon-delete':''" :size="size" :type="isText?'text':'danger'" :class="isText?'red':''" :plain="!isShowIcon" @click="handleDelete">{{$i18n.t('delete')}}</el-button>
      <el-button v-show="restoreVisible" data-defect-tool="restore" :icon="isShowIcon?'el-icon-refresh-left':''" :size="size" :type="isText?'text':'warning'" :class="isText?'restore-orange':''" :plain="!isShowIcon" @click="handleRestore">{{$i18n.t('defect.restore')}}</el-button>
      <slot name="right"></slot>
    </div>
    <div class="defect-tools__dialogs" aria-hidden="true">
      <assign-dialog ref="assignDialog" :project-id="defect.projectId" :defect-id="defect.defectId" @log="logHandle" />
      <repair-dialog ref="repairDialog" :project-id="defect.projectId" :defect-id="defect.defectId" @log="logHandle" />
      <reject-dialog ref="rejectDialog" :project-id="defect.projectId" :defect-id="defect.defectId" @log="logHandle" />
      <pass-dialog ref="passDialog" :project-id="defect.projectId" :defect-id="defect.defectId" @log="logHandle" />
      <open-dialog ref="openDialog" :project-id="defect.projectId" :defect-id="defect.defectId" @log="logHandle" />
      <close-dialog ref="closeDialog" :project-id="defect.projectId" :defect-id="defect.defectId" @log="logHandle" />
      <edit-defect-dialog ref="editDialog"  :project-id="defect.projectId" :defect-id="defect.defectId" @log="logHandle" />
    </div>
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
import ShareCard from "@/components/ShareCard";
import {delDefect, restoreDefect, updateUserDefect} from "@/api/system/defect";
import {checkPermi} from "@/utils/permission";
import i18n from "@/utils/i18n/i18n";

/** 处理中状态 */
const PROCESSING_STATE = 'PROCESSING';
/** 待验证 */
const AUDIT_STATE = 'AUDIT';
/** 已解决 */
const RESOLVED_STATE = 'RESOLVED';
/** 已驳回 */
const REJECTED_STATE = 'REJECTED';
/** 关闭状态 */
const CLOSE_STATE = 'CLOSED';
export default {
  name: "DefectTools",
  components: {PassDialog, AssignDialog, RejectDialog, RepairDialog,CloseDialog,OpenDialog,EditDefectDialog,StarSwitch, ShareCard },
  model: {
    prop: 'defect',
    event: 'update'
  },
  props:{
    exclusions: {
      type: Array,
      default: ()=>[]
    },
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
    },
    /** 表格行内紧凑模式：操作栏单行展示，弹层/对话框不参与列宽 */
    compact: {
      type: Boolean,
      default: null
    },
  },
  computed: {
    isCompact() {
      if (this.compact != null) return this.compact;
      return this.isText === true;
    },
    /** 获取当前用户id */
    currentUserId: function() {
      return this.$store.state.user.id;
    },
    isDeleted() {
      const f = this.defect && this.defect.delFlag;
      return f === '2' || f === 2;
    },
    // 指派
    assignVisible: function () {
      return !this.isDeleted && !this.exclusions.includes('assign') && (this.defect.defectStateName!=CLOSE_STATE && this.defect.defectStateName!=RESOLVED_STATE && checkPermi(['system:defect:assign']) &&
        (
          (this.defect.createById && this.defect.createById == this.currentUserId) ||
          (this.defect.handleBy && this.defect.handleBy.indexOf(this.currentUserId)!==-1)
        ));
    },
    // 修复
    repairVisible: function () {
      return !this.isDeleted && !this.exclusions.includes('repair') && ((this.defect.defectStateName==PROCESSING_STATE || this.defect.defectStateName==REJECTED_STATE) && checkPermi(['system:defect:repair']) &&
        (this.defect.handleBy && this.defect.handleBy.indexOf(this.currentUserId)!==-1));
    },
    // 驳回
    rejectVisible: function () {
      return !this.isDeleted && !this.exclusions.includes('reject') && (this.defect.defectStateName==AUDIT_STATE && checkPermi(['system:defect:reject']) &&
        (this.defect.handleBy && this.defect.handleBy.indexOf(this.currentUserId)!==-1));
    },
    // 通过
    passVisible: function () {
      return !this.isDeleted && !this.exclusions.includes('pass') && (this.defect.defectStateName==AUDIT_STATE && checkPermi(['system:defect:pass']) &&
        (this.defect.handleBy && this.defect.handleBy.indexOf(this.currentUserId)!==-1));
    },
    // 关闭
    closeVisible: function () {
      return !this.isDeleted && !this.exclusions.includes('close') && (this.defect.defectStateName!=CLOSE_STATE && this.defect.defectStateName!=RESOLVED_STATE && checkPermi(['system:defect:close']));
    },
    // 开启
    openVisible: function () {
      return !this.isDeleted && !this.exclusions.includes('open') && (this.defect.defectStateName==CLOSE_STATE && checkPermi(['system:defect:open']));
    },
    deleteVisible: function () {
      return !this.isDeleted && !this.exclusions.includes('delete') && (this.defect.createById==this.currentUserId || checkPermi(['system:defect:remove']));
    },
    editVisible: function () {
      return !this.isDeleted && !this.exclusions.includes('edit') && (this.defect.createById==this.currentUserId || checkPermi(['system:defect:edit']));
    },
    viewVisible: function () {
      return !this.exclusions.includes('view') && (this.defect.createById==this.currentUserId || checkPermi(['system:defect:query']));
    },
    restoreVisible: function () {
      return this.isDeleted && !this.exclusions.includes('restore') && (this.defect.createById==this.currentUserId || checkPermi(['system:defect:remove']));
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
    viewHandle() {
      this.$emit('view', this.defect);
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
    handleRestore(event) {
      const defectId = this.defect.defectId;
      this.$modal.confirm(
        this.$i18n.t('defect.restore'),
        this.$i18n.t('prompted').toString(),
        {
          confirmButtonText: i18n.t('defect.restore').toString(),
          cancelButtonText: i18n.t('cancel').toString(),
          type: 'warning'
        }).then(function() {
        return restoreDefect(defectId);
      }).then(() => {
        this.$emit('restore', this.defect);
        this.$emit('log', this.defect);
        this.$modal.msgSuccess(this.$i18n.t('defect.restore-success'));
      }).catch(() => {});
      if (event) event.stopPropagation();
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
    display: inline-flex;
    flex-wrap: nowrap;
    flex-direction: row;
    white-space: nowrap;
    column-gap: var(--cat2bug-operate-tools-gap, 10px);
    row-gap: var(--cat2bug-operate-tools-row-gap, 0);
    align-items: center;
    justify-content: flex-start !important;
    font-size: 12px;
    margin-bottom: 0px !important;
    max-width: 100%;
    vertical-align: middle;
    box-sizing: border-box;
  }

  .defect-tools__bar {
    display: inline-flex;
    flex-wrap: nowrap;
    flex-direction: row;
    align-items: center;
    column-gap: var(--cat2bug-operate-tools-gap, 10px);
    row-gap: var(--cat2bug-operate-tools-row-gap, 0);
    max-width: 100%;
    min-width: 0;
    box-sizing: border-box;

    > * {
      margin: 0;
      flex-shrink: 0;
    }
  }

  /* 对话框容器脱离文档流，避免 append 前撑开表格列宽 */
  .defect-tools__dialogs {
    position: fixed;
    left: -10000px;
    top: 0;
    width: 0;
    height: 0;
    overflow: hidden;
    clip: rect(0, 0, 0, 0);
    white-space: nowrap;
    pointer-events: none;
    z-index: -1;
  }

  .defect-tools--compact {
    display: block;
    max-width: 100%;
    overflow: hidden;

    .defect-tools__bar {
      overflow: hidden;
    }
  }
  .red {
    color: var(--cat2bug-operate-text-danger, #f56c6c);
  }
  .red:hover,
  .red:focus {
    color: var(--cat2bug-operate-text-danger-hover, #f78989);
  }
  .green {
    color: var(--cat2bug-operate-text-success, #67c23a);
  }
  .green:hover,
  .green:focus {
    color: var(--cat2bug-operate-text-success-hover, #85ce61);
  }
  .orange {
    color: var(--cat2bug-operate-text-muted, #606266);
  }
  .orange:hover,
  .orange:focus {
    color: var(--cat2bug-operate-text-muted-hover, #409eff);
  }
  .restore-orange {
    color: var(--cat2bug-operate-text-warning, #ffba00);
  }
  .restore-orange:hover,
  .restore-orange:focus {
    color: var(--cat2bug-operate-text-warning-hover, #ffc933);
  }
</style>
