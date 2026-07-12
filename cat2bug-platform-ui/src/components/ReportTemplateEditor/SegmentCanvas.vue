<template>
  <div class="report-segment-canvas">
    <template v-for="(segment, index) in segments" :key="index">
      <div v-if="segment.type === 'text'" class="report-segment-canvas__text">
        <milkdown-editor
          v-if="isMultiline(segment.value)"
          :model-value="segment.value"
          @update:model-value="updateSegment(index, { ...segment, value: $event })"
        />
        <el-input
          v-else
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 4 }"
          :model-value="segment.value"
          @update:model-value="updateSegment(index, { ...segment, value: $event })"
        />
      </div>

      <variable-chip
        v-else-if="segment.type === 'variable'"
        :path="segment.path"
        @remove="removeSegment(index)"
      />

      <plan-variable-chip
        v-else-if="segment.type === 'planVariable'"
        :path="segment.path"
        :plan-index="segment.planIndex"
        :plan-options="planOptions"
        @update:plan-index="updateSegment(index, { ...segment, planIndex: $event })"
        @remove="removeSegment(index)"
      />

      <defect-table-block
        v-else-if="segment.type === 'defectTable'"
        :data-source="segment.dataSource"
        :columns="segment.columns"
        @update:columns="updateSegment(index, { ...segment, columns: $event })"
        @remove="removeSegment(index)"
      />

      <case-card-block
        v-else-if="segment.type === 'caseCard'"
        :data-source="segment.dataSource"
        @remove="removeSegment(index)"
      />

      <dynamic-image-block
        v-else-if="segment.type === 'dynamicImage'"
        :path="segment.path"
        :style="segment.style"
        @update:path="updateSegment(index, { ...segment, path: $event })"
        @update:style="updateSegment(index, { ...segment, style: $event })"
        @remove="removeSegment(index)"
      />

      <raw-block
        v-else-if="segment.type === 'raw'"
        :value="segment.value"
      />
    </template>
  </div>
</template>

<script>
import MilkdownEditor from './MilkdownEditor.vue'
import VariableChip from './nodes/VariableChip.vue'
import PlanVariableChip from './nodes/PlanVariableChip.vue'
import DefectTableBlock from './nodes/DefectTableBlock.vue'
import CaseCardBlock from './nodes/CaseCardBlock.vue'
import DynamicImageBlock from './nodes/DynamicImageBlock.vue'
import RawBlock from './nodes/RawBlock.vue'

export default {
  name: 'SegmentCanvas',
  components: {
    MilkdownEditor,
    VariableChip,
    PlanVariableChip,
    DefectTableBlock,
    CaseCardBlock,
    DynamicImageBlock,
    RawBlock
  },
  props: {
    segments: {
      type: Array,
      default: () => []
    },
    planOptions: {
      type: Array,
      default: () => []
    }
  },
  emits: ['update:segments'],
  methods: {
    isMultiline(value) {
      return (value || '').includes('\n')
    },
    updateSegment(index, segment) {
      const next = [...this.segments]
      next[index] = segment
      this.$emit('update:segments', next)
    },
    removeSegment(index) {
      const next = [...this.segments]
      next.splice(index, 1)
      this.$emit('update:segments', next)
    }
  }
}
</script>

<style scoped>
.report-segment-canvas__text {
  margin: 4px 0;
}

.report-segment-canvas {
  padding: 8px;
}
</style>
