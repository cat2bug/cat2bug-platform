<template>
  <div class="defect-type-tag" :class="typeClass">
    <img v-if="typeIcon" class="defect-type-tag__icon" :src="typeIcon" alt="" />
    <span>{{ typeLabel }}</span>
  </div>
</template>

<script>
import bugIcon from '@/assets/images/bug.png'
import taskIcon from '@/assets/images/task.png'
import demandIcon from '@/assets/images/demand.png'

const KNOWN_TYPES = new Set(['BUG', 'TASK', 'DEMAND'])

export default {
  name: "DefectTypeFlag",
  props: {
    defect: {
      type: Object,
      default: () => ({})
    }
  },
  computed: {
    typeName() {
      const name = this.defect && this.defect.defectTypeName
      return name == null || name === '' ? '' : String(name)
    },
    typeClass() {
      if (this.typeName === 'BUG') return 'bug'
      if (this.typeName === 'TASK') return 'task'
      if (this.typeName === 'DEMAND') return 'demand'
      return ''
    },
    typeIcon() {
      if (this.typeName === 'BUG') return bugIcon
      if (this.typeName === 'TASK') return taskIcon
      if (this.typeName === 'DEMAND') return demandIcon
      return null
    },
    typeLabel() {
      if (!this.typeName) return ''
      if (KNOWN_TYPES.has(this.typeName)) {
        return this.$t(this.typeName)
      }
      return this.typeName
    }
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
