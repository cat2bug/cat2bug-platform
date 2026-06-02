<template>
  <div class="defect-type-tag bug" v-if="defect.defectTypeName=='BUG'">
    <img class="defect-type-tag__icon" :src="bugIcon" alt="" />
    <span>{{$i18n.t(defect.defectTypeName)}}</span>
  </div>
  <div class="defect-type-tag task" v-else-if="defect.defectTypeName=='TASK'">
    <img class="defect-type-tag__icon" :src="taskIcon" alt="" />
    <span>{{$i18n.t(defect.defectTypeName)}}</span>
  </div>
  <div class="defect-type-tag demand" v-else-if="defect.defectTypeName=='DEMAND'">
    <img class="defect-type-tag__icon" :src="demandIcon" alt="" />
    <span>{{$i18n.t(defect.defectTypeName)}}</span>
  </div>
  <div class="defect-type-tag" v-else>
    <span>{{$i18n.t(defect.defectTypeName)}}</span>
  </div>
</template>

<script>
const bugIcon = require('@/assets/images/bug.png')
const taskIcon = require('@/assets/images/task.png')
const demandIcon = require('@/assets/images/demand.png')

export default {
  name: "DefectTypeFlag",
  props:{
    defect: {
      type: Object,
      default: ()=>{}
    }
  },
  data() {
    return { bugIcon, taskIcon, demandIcon }
  }
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
    border-radius: var(--cat2bug-border-radius, 3px);
    gap: 2px;
    font-size: 13px;
    padding: 0px 5px;
    background-color: var(--defect-type-default-bg, #ffffff);
    border-color: var(--defect-type-default-border, #dcdfe6);
    color: var(--defect-type-default-color, #606266);
    .defect-type-tag__icon {
      width: 15px;
      height: 15px;
      display: block;
      flex-shrink: 0;
      object-fit: contain;
      background: transparent;
    }
    span {
      line-height: 15px;
      margin: 5px 0px;
    }
  }
  .bug {
    background-color: var(--defect-type-bug-bg, #ffffff);
    border-color: var(--defect-type-bug-border, #ff4949);
    color: var(--defect-type-bug-color, #ff4949);
  }
  .task {
    background-color: var(--defect-type-task-bg, #ffffff);
    border-color: var(--defect-type-task-border, #13ce66);
    color: var(--defect-type-task-color, #13ce66);
  }
  .demand {
    background-color: var(--defect-type-demand-bg, #ffffff);
    border-color: var(--defect-type-demand-border, #ffba00);
    color: var(--defect-type-demand-color, #ffba00);
  }
</style>
