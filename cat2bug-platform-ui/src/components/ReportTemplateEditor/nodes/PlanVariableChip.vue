<template>
  <div class="cat2bug-plan-variable-chip">
    <el-tag type="warning" size="small" closable @close="$emit('remove')">
      <span class="cat2bug-plan-variable-chip__label">{{ fieldLabel }}</span>
    </el-tag>
    <el-select
      v-if="planOptions.length"
      :model-value="planIndex"
      size="small"
      class="cat2bug-plan-variable-chip__plan"
      @update:model-value="$emit('update:planIndex', $event)"
    >
      <el-option
        v-for="plan in planOptions"
        :key="plan.planNumber"
        :label="plan.planName"
        :value="plan.planNumber"
      />
    </el-select>
    <span v-else class="cat2bug-plan-variable-chip__index">[{{ planIndex }}]</span>
  </div>
</template>

<script>
export default {
  name: 'PlanVariableChip',
  props: {
    path: {
      type: String,
      required: true
    },
    planIndex: {
      type: Number,
      required: true
    },
    planOptions: {
      type: Array,
      default: () => []
    }
  },
  emits: ['remove', 'update:planIndex'],
  computed: {
    fieldLabel() {
      const field = this.path.replace(/^api\.plan\./, '')
      return field || this.path
    }
  }
}
</script>

<style scoped>
.cat2bug-plan-variable-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin: 2px 4px;
  vertical-align: middle;
}

.cat2bug-plan-variable-chip__plan {
  width: 140px;
}

.cat2bug-plan-variable-chip__index {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
