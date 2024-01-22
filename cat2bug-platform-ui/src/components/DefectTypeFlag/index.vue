<template>
  <el-tag v-if="defect.defectTypeName=='BUG'" type="danger"><svg-icon icon-class="bug" style="color:#F56C6C;" />{{$i18n.t(defect.defectTypeName)}}</el-tag>
  <el-tag v-else-if="defect.defectTypeName=='TASK'" type="success"><svg-icon icon-class="task" style="color:#67C23A;" />{{$i18n.t(defect.defectTypeName)}}</el-tag>
  <el-tag v-else-if="defect.defectTypeName=='DEMAND'" type="warning"><svg-icon icon-class="demand" style="color:#FBB13F;" />{{$i18n.t(defect.defectTypeName)}}</el-tag>
  <el-tag v-else type="info">{{$i18n.t('unknown')}}</el-tag>
</template>

<script>
export default {
  name: "DefectTypeFlag",
  props:{
    defect: {
      type: Object,
      default: ()=>{}
    }
  },
}
</script>

<style scoped>
  .svg-icon {
    margin-right: 3px;
  }
</style>
