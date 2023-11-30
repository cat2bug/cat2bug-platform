<template>
  <div class="defect-tools">
    <el-button :icon="isShowIcon?'el-icon-refresh':''" :size="size" :type="isText?'text':'info'" @click="assignHandle">{{$i18n.t('assign')}}</el-button>
    <el-button :icon="isShowIcon?'el-icon-document-delete':''" :size="size" :type="isText?'text':'warning'">{{$i18n.t('reject')}}</el-button>
    <el-button :icon="isShowIcon?'el-icon-document-checked':''" :size="size" :type="isText?'text':'primary'">{{$i18n.t('repair')}}</el-button>
    <el-button :icon="isShowIcon?'el-icon-finished':''" :size="size" :type="isText?'text':'success'">{{$i18n.t('pass')}}</el-button>
    <el-button :icon="isShowIcon?'el-icon-takeaway-box':''" :size="size" :type="isText?'text':'danger'">{{$i18n.t('close')}}</el-button>
    <assign-dialog ref="assignDialog" :project-id="defect.projectId" :defect-id="defect.defectId" />
  </div>
</template>

<script>
import AssignDialog from "@/components/DefectTools/AssignDialog";
export default {
  name: "DefectTools",
  components: { AssignDialog },
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
  methods:{
    /** 指派 */
    assignHandle(){
      this.$refs.assignDialog.open();
      // this.$emit('update',{});
    }
  }
}
</script>

<style lang="scss" scoped>
  .defect-tools {
    display: flex;
    flex-wrap: wrap;
    flex-direction: row;
    font-size: 12px;
    margin-bottom: 0px !important;
  }
</style>
