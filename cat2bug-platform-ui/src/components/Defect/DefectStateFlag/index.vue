<template>
  <span class="defect-state-tag" :class="stateClass">{{ $i18n.t(displayStateKey) }}</span>
</template>

<script>
const STATE_CLASS_MAP = {
  PROCESSING: 'processing',
  AUDIT: 'audit',
  RESOLVED: 'resolved',
  REJECTED: 'rejected',
  CLOSED: 'closed'
}

export default {
  name: 'DefectStateFlag',
  props: {
    defect: {
      type: Object,
      default: () => ({})
    }
  },
  computed: {
    stateClass() {
      const name = this.defect && this.defect.defectStateName
      return STATE_CLASS_MAP[name] || 'unknown'
    },
    displayStateKey() {
      const name = this.defect && this.defect.defectStateName
      return name || 'unknown'
    }
  }
}
</script>

<style lang="scss" scoped>
.defect-state-tag {
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  border-width: 1px;
  border-style: solid;
  border-radius: var(--cat2bug-border-radius, 3px);
  gap: 2px;
  font-size: 13px;
  line-height: 15px;
  padding: 5px;
  white-space: nowrap;
  vertical-align: middle;
  background-color: var(--defect-state-unknown-bg, #909399);
  border-color: var(--defect-state-unknown-border, #909399);
  color: var(--defect-state-unknown-color, #ffffff);
}

.defect-state-tag.processing {
  background-color: var(--defect-state-processing-bg, #f56c6c);
  border-color: var(--defect-state-processing-border, #f56c6c);
  color: var(--defect-state-processing-color, #ffffff);
}

.defect-state-tag.audit {
  background-color: var(--defect-state-audit-bg, #409eff);
  border-color: var(--defect-state-audit-border, #409eff);
  color: var(--defect-state-audit-color, #ffffff);
}

.defect-state-tag.resolved {
  background-color: var(--defect-state-resolved-bg, #67c23a);
  border-color: var(--defect-state-resolved-border, #67c23a);
  color: var(--defect-state-resolved-color, #ffffff);
}

.defect-state-tag.rejected {
  background-color: var(--defect-state-rejected-bg, #e6a23c);
  border-color: var(--defect-state-rejected-border, #e6a23c);
  color: var(--defect-state-rejected-color, #ffffff);
}

.defect-state-tag.closed {
  background-color: var(--defect-state-closed-bg, #ffffff);
  border-color: var(--defect-state-closed-border, #d3d4d6);
  color: var(--defect-state-closed-color, #909399);
}

.defect-state-tag.unknown {
  background-color: var(--defect-state-unknown-bg, #909399);
  border-color: var(--defect-state-unknown-border, #909399);
  color: var(--defect-state-unknown-color, #ffffff);
}
</style>
