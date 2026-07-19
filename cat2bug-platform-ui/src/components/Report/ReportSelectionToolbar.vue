<template>
  <div
    v-if="visible"
    class="report-selection-toolbar"
    :style="positionStyle"
    @mousedown.stop.prevent
  >
    <el-button
      size="small"
      type="primary"
      :icon="$legacyIcon('el-icon-chat-line-round')"
      @click="$emit('add-comment')"
    >{{ $t('report.add-comment') }}</el-button>
  </div>
</template>

<script>
export default {
  name: 'ReportSelectionToolbar',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    top: {
      type: Number,
      default: 0
    },
    left: {
      type: Number,
      default: 0
    }
  },
  emits: ['add-comment'],
  computed: {
    positionStyle() {
      return {
        top: this.top + 'px',
        left: this.left + 'px'
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.report-selection-toolbar {
  position: fixed;
  z-index: 3000;
  transform: translate(-50%, -110%);
  filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.15));
}
</style>
