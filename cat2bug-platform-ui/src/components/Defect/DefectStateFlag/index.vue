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
  background-color: var(--defect-state-processing-bg, #e04e4e);
  border-color: var(--defect-state-processing-border, #e04e4e);
  color: var(--defect-state-processing-color, #ffffff);
}

.defect-state-tag.audit {
  background-color: var(--defect-state-audit-bg, #337ecc);
  border-color: var(--defect-state-audit-border, #337ecc);
  color: var(--defect-state-audit-color, #ffffff);
}

.defect-state-tag.resolved {
  background-color: var(--defect-state-resolved-bg, #5daf34);
  border-color: var(--defect-state-resolved-border, #5daf34);
  color: var(--defect-state-resolved-color, #ffffff);
}

.defect-state-tag.rejected {
  background-color: var(--defect-state-rejected-bg, #cf9236);
  border-color: var(--defect-state-rejected-border, #cf9236);
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
