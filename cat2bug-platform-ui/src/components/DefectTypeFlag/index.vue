<template>
  <el-tag v-if="defect.defectTypeName=='BUG'" type="danger">{{$i18n.t(defect.defectTypeName)}}</el-tag>
  <el-tag v-else-if="defect.defectTypeName=='TASK'" type="success">{{$i18n.t(defect.defectTypeName)}}</el-tag>
  <el-tag v-else-if="defect.defectTypeName=='DEMAND'">{{$i18n.t(defect.defectTypeName)}}</el-tag>
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

</style>
