<template>
  <el-tag v-if="defect.defectTypeName=='BUG'" type="danger" effect="dark"><svg-icon icon-class="bug" />{{$i18n.t(defect.defectTypeName)}}</el-tag>
  <el-tag v-else-if="defect.defectTypeName=='TASK'" type="success" effect="dark"><svg-icon icon-class="task" />{{$i18n.t(defect.defectTypeName)}}</el-tag>
  <el-tag v-else-if="defect.defectTypeName=='DEMAND'" type="warning" effect="dark"><svg-icon icon-class="demand" />{{$i18n.t(defect.defectTypeName)}}</el-tag>
  <el-tag v-else type="info" effect="dark">{{$i18n.t('unknown')}}</el-tag>
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
    color: #FFFFFF;
  }
</style>
