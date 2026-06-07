<template>
  <aside v-if="items.length >= 2" class="doc-page-outline">
    <div class="doc-page-outline__head">
      <span class="doc-page-outline__title doc-hint-outline-title">{{ title }}</span>
      <el-button
        type="text"
        class="doc-page-outline__print doc-hint-print"
        icon="el-icon-printer"
        :title="printLabel"
        :aria-label="printLabel"
        @click="$emit('print')"
      />
    </div>
    <nav class="doc-page-outline__nav" aria-label="outline">
      <a
        v-for="item in items"
        :key="item.id"
        href="#"
        class="doc-page-outline__link"
        :class="[
          `doc-page-outline__link--h${item.level}`,
          {
            'is-active': item.id === activeId,
            'is-kbd-focus': item.id === kbdFocusId
          }
        ]"
        @click.prevent="$emit('click', item.id)"
      >{{ item.text }}</a>
    </nav>
  </aside>
</template>

<script>
export default {
  name: 'DocPageOutline',
  props: {
    title: {
      type: String,
      required: true
    },
    printLabel: {
      type: String,
      default: ''
    },
    items: {
      type: Array,
      default: () => []
    },
    activeId: {
      type: String,
      default: ''
    },
    kbdFocusId: {
      type: String,
      default: ''
    }
  }
}
</script>

<style scoped lang="scss">
.doc-page-outline {
  width: 220px;
  flex-shrink: 0;
  padding: 16px 24px 24px 16px;
  align-self: flex-start;
  position: sticky;
  top: 16px;
  box-sizing: border-box;
  background-color: #fff;

  @media (max-width: 1279px) {
    display: none;
  }

  &__head {
    display: flex;
    align-items: center;
    gap: 4px;
    margin-bottom: 12px;
  }

  &__title {
    flex: 1;
    min-width: 0;
    font-size: 13px;
    font-weight: 600;
    color: #303133;
    line-height: 1.4;
  }

  &__print {
    flex-shrink: 0;
    padding: 4px;
    font-size: 16px;
    color: #606266;

    &:hover {
      color: #409eff;
    }
  }

  &__nav {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  &__link {
    display: block;
    font-size: 13px;
    line-height: 1.5;
    color: #606266;
    text-decoration: none;
    border-left: 2px solid transparent;
    padding: 4px 0 4px 10px;
    transition: color 0.2s, border-color 0.2s;

    &:hover {
      color: #409eff;
    }

    &.is-active {
      color: #409eff;
      border-left-color: #409eff;
      font-weight: 700;
    }

    &.is-kbd-focus {
      color: #409eff;
      border-left-color: #409eff;
      background: rgba(64, 158, 255, 0.08);
    }

    &--h3 {
      padding-left: 22px;
      font-size: 12px;
    }
  }
}
</style>
