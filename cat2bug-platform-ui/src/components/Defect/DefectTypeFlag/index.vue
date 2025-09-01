<template>
  <div class="defect-type-tag bug" v-if="defect.defectTypeName=='BUG'">
    <el-image :src="require('@/assets/images/bug.png')" />
    <span>{{$i18n.t(defect.defectTypeName)}}</span>
  </div>
  <div class="defect-type-tag task" v-else-if="defect.defectTypeName=='TASK'">
    <el-image :src="require('@/assets/images/task.png')" />
    <span>{{$i18n.t(defect.defectTypeName)}}</span>
  </div>
  <div class="defect-type-tag demand" v-else-if="defect.defectTypeName=='DEMAND'">
    <el-image :src="require('@/assets/images/demand.png')" />
    <span>{{$i18n.t(defect.defectTypeName)}}</span>
  </div>
  <div class="defect-type-tag" v-else>
    <span>{{$i18n.t(defect.defectTypeName)}}</span>
  </div>
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

<style lang="scss" scoped>
  .defect-type-tag {
    display: inline-flex;
    flex-direction: row;
    justify-content: center;
    align-items: center;
    border-width: 1px;
    border-style: solid;
    border-radius: 3px;
    gap: 2px;
    font-size: 13px;
    padding: 0px 5px;
    background-color: #FFFFFF;
    .el-image {
      width: 15px;
      height: 15px;
    }
    span {
      line-height: 15px;
      margin: 5px 0px;
    }
  }
  .bug {
    border-color: #ff4949;
    color: #ff4949;
  }
  .task {
    border-color: #13ce66;
    color: #13ce66;
  }
  .demand {
    border-color: #ffba00;
    color: #ffba00;
  }
</style>
