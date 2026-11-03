<template>
  <div class="push-parts">
    <p v-if="pending && !parts.length" class="push-parts__empty">{{ $t('project.push.parts-pending') }}</p>
    <ul v-else class="push-parts__list" role="list">
      <li v-for="(part, index) in parts" :key="part.partId || index" class="push-part" role="listitem">
        <span class="push-part__name">{{ partLabel(part) }}</span>
        <span class="push-part__size">{{ formatByteSize(part.bytes) }}</span>
        <el-tag size="small" :type="tagType(part)">{{ stateLabel(part) }}</el-tag>
        <span v-if="failed(part) && part.error" class="push-part__error">{{ part.error }}</span>
      </li>
    </ul>
    <el-button
      v-if="canRetry || retrying"
      type="primary"
      plain
      :loading="retrying"
      :disabled="!canRetry"
      @click="$emit('retry')"
    >{{ retrying ? $t('project.push.retrying') : $t('project.push.retry-failed') }}</el-button>
  </div>
</template>

<script>
import { formatByteSize, isFailedPart } from './push-estimate'

const DONE = ['OK', 'SUCCESS', 'SUCCEEDED']
const RUNNING = ['UPLOADING', 'RUNNING', 'PACKING', 'RETRYING']

export default {
  name: 'PushPartProgress',
  props: {
    parts: { type: Array, default: () => [] },
    pending: { type: Boolean, default: false },
    canRetry: { type: Boolean, default: false },
    retrying: { type: Boolean, default: false }
  },
  emits: ['retry'],
  methods: {
    formatByteSize,
    failed(part) {
      return isFailedPart(part)
    },
    partLabel(part) {
      const type = String(part.type || '')
      if (type === 'core') return this.$t('project.push.part.core')
      const prefix = type === 'git'
        ? this.$t('project.push.part.git')
        : (type === 'doc' || type === 'document' ? this.$t('project.push.part.doc') : '')
      const name = part.name || part.partId
      return prefix ? (prefix + ' · ' + name) : name
    },
    stateLabel(part) {
      const state = String(part.state || '').toUpperCase()
      if (DONE.includes(state)) return this.$t('project.push.part.ok')
      if (isFailedPart(part)) return this.$t('project.push.part.failed')
      if (state === 'PACKING') return this.$t('project.push.part.packing')
      if (RUNNING.includes(state)) return this.$t('project.push.part.uploading')
      return this.$t('project.push.part.pending')
    },
    tagType(part) {
      const state = String(part.state || '').toUpperCase()
      if (DONE.includes(state)) return 'success'
      if (isFailedPart(part)) return 'danger'
      if (RUNNING.includes(state) || state === 'PACKING') return 'warning'
      return 'info'
    }
  }
}
</script>

<style scoped>
.push-parts__empty {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary, #909399);
}
.push-parts__list {
  margin: 0 0 10px;
  padding: 0;
  list-style: none;
  max-width: 720px;
}
.push-part {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px solid var(--el-border-color-lighter, #ebeef5);
}
.push-part__name {
  flex: 1;
  min-width: 140px;
}
.push-part__size {
  font-size: 12px;
  color: var(--el-text-color-secondary, #909399);
  font-variant-numeric: tabular-nums;
}
.push-part__error {
  flex-basis: 100%;
  font-size: 12px;
  color: var(--el-color-danger);
}
</style>
