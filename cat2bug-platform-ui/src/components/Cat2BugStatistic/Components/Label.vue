<template>
  <div class="cat2bug-statistic-label">
    <svg-icon class="cat2bug-statistic-label__icon" :icon-class="icon" :style="iconStyle" />
    <h5 class="cat2bug-statistic-label__name">{{ label }}</h5>
    <div class="cat2bug-statistic-label__values">
      <slot name="content">
        <span>{{ content }}</span>
      </slot>
    </div>
  </div>
</template>

<script>
export default {
  name: "Label",
  props: {
    icon: {
      type: String,
      default: null
    },
    iconColor: {
      type: String,
      default: null
    },
    label: {
      type: String,
      default: null
    },
    content: {
      type: [String, Number],
      default: null
    }
  },
  computed: {
    iconStyle() {
      return { color: this.iconColor || 'var(--text-color-secondary, #909399)' };
    }
  }
}
</script>

<style scoped>
  .cat2bug-statistic-label {
    margin: 0;
    padding: var(--statistic-label-padding-y, 2px) 0;
    display: grid;
    grid-template-columns: 16px minmax(48px, 1fr) 28px 36px 36px;
    column-gap: 6px;
    align-items: center;
    min-height: var(--statistic-label-min-height, 18px);
    line-height: 1.2;
  }

  .cat2bug-statistic-label__icon {
    width: 14px !important;
    height: 14px !important;
    flex-shrink: 0;
  }

  .cat2bug-statistic-label__name {
    font-size: 12px;
    font-weight: 500;
    margin: 0;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: var(--text-color-regular, #606266);
  }

  .cat2bug-statistic-label__values {
    display: contents;
  }

  .cat2bug-statistic-label__values ::v-deep span {
    font-size: 12px;
    font-variant-numeric: tabular-nums;
    text-align: right;
    color: var(--text-color-primary, #303133);
  }

  .cat2bug-statistic-label__values ::v-deep span:nth-child(1) {
    font-weight: 600;
  }
</style>
