<template>
  <el-tag v-if="report.reportKey" type="success" effect="dark" size="default">{{$i18n.t('report.type-runtime')}}</el-tag>
  <el-tag v-else type="info" effect="dark" size="default">{{$i18n.t('report.type-text')}}</el-tag>
</template>

<script>
export default {
  name: "ReportTypeFlag",
  props:{
    report: {
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
