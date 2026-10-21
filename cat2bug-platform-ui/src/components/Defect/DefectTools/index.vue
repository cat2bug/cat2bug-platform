<template>
  <div class="defect-tools" :class="{ 'defect-tools--compact': isCompact }">
    <!-- 仅操作按钮参与表格列宽；对话框/弹层挂到隔离层，避免撑开操作列 -->
    <div class="defect-tools__bar">
      <slot name="left"></slot>
      <span v-show="!isDeleted" class="defect-tools-share-wrap">
        <share-card :params="defect" />
      </span>
      <star-switch v-show="!isDeleted" v-model="defect.collect" @change="clickCollectHandle($event, defect, false)"></star-switch>
      <el-button v-show="assignVisible" data-defect-tool="assign" :icon="$legacyIcon(isShowIcon?'el-icon-refresh':'')" :size="buttonSize" :link="!!isText" :type="isText ? undefined : 'info'" :class="isText?'orange':''" @click="assignHandle">{{$i18n.t('assign')}}</el-button>
      <el-button v-show="repairVisible" data-defect-tool="repair" :icon="$legacyIcon(isShowIcon?'el-icon-document-checked':'')" :size="buttonSize" :link="!!isText" :type="isText ? undefined : 'primary'" :class="isText?'orange':''" @click="repairDialogHandle">{{$i18n.t('repair')}}</el-button>
    <el-button v-show="rejectVisible" data-defect-tool="reject" :icon="$legacyIcon(isShowIcon?'el-icon-document-delete':'')" :size="buttonSize" :link="!!isText" :type="isText ? undefined : 'warning'" :class="isText?'orange':''" @click="rejectHandle">{{$i18n.t('reject')}}</el-button>
      <el-button v-show="passVisible" data-defect-tool="pass" :icon="$legacyIcon(isShowIcon?'el-icon-finished':'')" :size="buttonSize" :link="!!isText" :type="isText ? undefined : 'success'" :class="isText?'orange':''" @click="passDialogHandle">{{$i18n.t('pass')}}</el-button>
      <el-button v-show="openVisible" data-defect-tool="open" :icon="$legacyIcon(isShowIcon?'el-icon-document-copy':'')" :size="buttonSize" :link="!!isText" :type="isText ? undefined : 'danger'" :class="isText?'orange':''" @click="openDialogHandle" v-hasPermi="['system:defect:open']">{{$i18n.t('open')}}</el-button>
      <el-button v-show="closeVisible" data-defect-tool="close" :icon="$legacyIcon(isShowIcon?'el-icon-takeaway-box':'')" :size="buttonSize" :link="!!isText" :type="isText ? undefined : 'danger'" :class="isText?'orange':''" @click="closeDialogHandle">{{$i18n.t('close')}}</el-button>
      <el-button v-show="viewVisible" data-defect-tool="view" :icon="$legacyIcon(isShowIcon?'el-icon-view':'')" :size="buttonSize" :link="!!isText" :type="isText ? undefined : 'warning'" @click="viewHandle">{{$i18n.t('view')}}</el-button>
      <el-button v-show="editVisible" data-defect-tool="edit" :icon="$legacyIcon(isShowIcon?'el-icon-edit':'')" :size="buttonSize" :link="!!isText" :type="isText ? undefined : 'success'" :class="isText?'green':''" :plain="!isShowIcon" @click="editDialogHandle" >{{ $t('modify') }}</el-button>
      <el-button v-show="deleteVisible" data-defect-tool="delete" :icon="$legacyIcon(isShowIcon?'el-icon-delete':'')" :size="buttonSize" :link="!!isText" :type="isText ? undefined : 'danger'" :class="isText?'red':''" :plain="!isShowIcon" @click="handleDelete">{{$i18n.t('delete')}}</el-button>
      <el-button v-show="restoreVisible" data-defect-tool="restore" :icon="$legacyIcon(isShowIcon?'el-icon-refresh-left':'')" :size="buttonSize" :link="!!isText" :type="isText ? undefined : 'warning'" :class="isText?'restore-orange':''" :plain="!isShowIcon" @click="handleRestore">{{$i18n.t('defect.restore')}}</el-button>
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
import i18n from "@/utils/i18n/i18n";
import { storeToRefs } from "pinia";
import { useUserStore } from "@/stores/user";
import {
  isDefectDeleted,
  isDefectActionVisible,
} from "@/utils/defect-action-visibility";

export default {
  emits: ['update:defect', 'update'],
  name: "DefectTools",
  components: {PassDialog, AssignDialog, RejectDialog, RepairDialog,CloseDialog,OpenDialog,EditDefectDialog,StarSwitch, ShareCard },
  setup() {
    const userStore = useUserStore()
    const { id } = storeToRefs(userStore)
    return { id }
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
    /** Element Plus: mini/medium → small/default */
    buttonSize() {
      const size = this.size
      if (!size) return undefined
      if (size === 'mini') return 'small'
      if (size === 'medium') return 'default'
      return size
    },
    /** 获取当前用户id */
    currentUserId: function() {
      return this.id;
    },
    isDeleted() {
      return isDefectDeleted(this.defect);
    },
    assignVisible() {
      return isDefectActionVisible('assign', this.defect, this.currentUserId, this.exclusions);
    },
    repairVisible() {
      return isDefectActionVisible('repair', this.defect, this.currentUserId, this.exclusions);
    },
    rejectVisible() {
      return isDefectActionVisible('reject', this.defect, this.currentUserId, this.exclusions);
    },
    passVisible() {
      return isDefectActionVisible('pass', this.defect, this.currentUserId, this.exclusions);
    },
    closeVisible() {
      return isDefectActionVisible('close', this.defect, this.currentUserId, this.exclusions);
    },
    openVisible() {
      return isDefectActionVisible('open', this.defect, this.currentUserId, this.exclusions);
    },
    deleteVisible() {
      return isDefectActionVisible('delete', this.defect, this.currentUserId, this.exclusions);
    },
    editVisible() {
      return isDefectActionVisible('edit', this.defect, this.currentUserId, this.exclusions);
    },
    viewVisible() {
      return isDefectActionVisible('view', this.defect, this.currentUserId, this.exclusions);
    },
    restoreVisible() {
      return isDefectActionVisible('restore', this.defect, this.currentUserId, this.exclusions);
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
        defect.collect = collect;
        if (isRefreshList) {
          this.$emit('update:defect', this.defect); this.$emit('update', this.defect);
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

  .defect-tools-share-wrap {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex: 0 0 auto;
    line-height: 1;
    vertical-align: middle;
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
    color: var(--cat2bug-operate-text-muted-hover, #1890ff);
  }
  .restore-orange {
    color: var(--cat2bug-operate-text-warning, #ffba00);
  }
  .restore-orange:hover,
  .restore-orange:focus {
    color: var(--cat2bug-operate-text-warning-hover, #1890ff);
  }
</style>
