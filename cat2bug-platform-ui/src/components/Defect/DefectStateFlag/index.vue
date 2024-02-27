<template>
  <el-tag v-if="defect.defectStateName=='PROCESSING'" type="danger" effect="dark">{{$i18n.t(defect.defectStateName)}}</el-tag>
  <el-tag v-else-if="defect.defectStateName=='AUDIT'" type="" effect="dark">{{$i18n.t(defect.defectStateName)}}</el-tag>
  <el-tag v-else-if="defect.defectStateName=='RESOLVED'" effect="success">{{$i18n.t(defect.defectStateName)}}</el-tag>
  <el-tag v-else-if="defect.defectStateName=='REJECTED'" type="warning" effect="dark">{{$i18n.t(defect.defectStateName)}}</el-tag>
  <el-tag v-else-if="defect.defectStateName=='CLOSED'" type="info" effect="plain">{{$i18n.t(defect.defectStateName)}}</el-tag>
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
