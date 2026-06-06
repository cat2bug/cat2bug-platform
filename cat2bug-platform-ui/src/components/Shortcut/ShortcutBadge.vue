<template>
  <kbd class="shortcut-badge" :class="{ 'shortcut-badge--lg': large }">{{ display }}</kbd>
</template>

<script>
export default {
  name: 'ShortcutBadge',
  props: {
    letter: { type: [String, Number], default: '' },
    large: { type: Boolean, default: false }
  },
  computed: {
    display() {
      return this.letter === '' || this.letter == null ? '·' : String(this.letter)
    }
  }
}
</script>

<style lang="scss" scoped>
.shortcut-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  color: #e8e8e8;
  background: #2b2b2e;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-bottom-width: 2px;
  border-bottom-color: rgba(255, 255, 255, 0.28);
  border-radius: 5px;
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.4);
  user-select: none;

  &--lg {
    min-width: 28px;
    height: 28px;
    font-size: 14px;
  }
}
</style>
