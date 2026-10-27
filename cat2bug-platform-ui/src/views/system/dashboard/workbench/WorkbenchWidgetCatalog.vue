<template>
  <aside class="wb-catalog">
    <div class="wb-catalog__hd">
      <div class="wb-catalog__title">{{ $t('workbench.editor.catalog') }}</div>
      <el-input
        v-model="query"
        class="wb-catalog__search"
        clearable
        size="small"
        :placeholder="$t('workbench.catalog.searchPlaceholder')"
      />
    </div>
    <div class="wb-catalog__body">
    <div v-if="isSearching && !filteredGroups.length" class="wb-catalog__empty">
      {{ $t('workbench.catalog.searchEmpty') }}
    </div>
    <div
      v-for="g in filteredGroups"
      :key="g.group"
      class="wb-catalog__group"
      :class="{ 'is-open': isOpen(g.group) }"
    >
      <button
        type="button"
        class="wb-catalog__group-hd"
        :aria-expanded="isOpen(g.group) ? 'true' : 'false'"
        @click="toggle(g.group)"
      >
        <span class="wb-catalog__group-name">{{ $t(g.titleKey) }}</span>
        <span class="wb-catalog__group-count">{{ g.widgets.length }}</span>
        <span class="wb-catalog__chev" aria-hidden="true" />
      </button>
      <!-- v-if：折叠卸载，避免整库组件同时打接口；搜索时命中组全开 -->
      <div v-if="isOpen(g.group)" class="wb-catalog__group-bd">
        <div
          v-for="w in g.widgets"
          :key="w.id"
          class="wb-catalog__card"
          :class="{ 'is-used': isUsed(w.id) }"
          draggable="true"
          @dragstart="onDragStart($event, w.id)"
        >
          <catalog-live-thumb>
            <component
              :is="componentOf(w)"
              v-if="componentOf(w)"
              v-bind="propsOf(w)"
            />
            <div v-else class="wb-catalog__placeholder">
              <span>{{ placeholderLetter(w) }}</span>
            </div>
          </catalog-live-thumb>
          <div class="wb-catalog__meta">
            <span class="wb-catalog__name">{{ $t(w.titleKey) }}</span>
            <span v-if="isUsed(w.id)" class="wb-catalog__used">{{ $t('workbench.catalog.used') }}</span>
          </div>
        </div>
      </div>
    </div>
    </div>
  </aside>
</template>

<script>
import { listCatalogGroups, getWidgetDef } from './widget-registry'
import CatalogLiveThumb from './CatalogLiveThumb.vue'

export default {
  name: 'WorkbenchWidgetCatalog',
  components: { CatalogLiveThumb },
  props: {
    /** 当前编辑面上已出现的 widget type 列表 */
    usedTypes: { type: Array, default: () => [] }
  },
  data() {
    return {
      query: '',
      openGroups: []
    }
  },
  computed: {
    groups() {
      return listCatalogGroups()
    },
    queryNorm() {
      return String(this.query || '').trim().toLowerCase()
    },
    isSearching() {
      return !!this.queryNorm
    },
    filteredGroups() {
      const q = this.queryNorm
      if (!q) return this.groups
      return this.groups
        .map(g => ({
          ...g,
          widgets: (g.widgets || []).filter(w => this.widgetMatches(w, q))
        }))
        .filter(g => g.widgets.length)
    },
    usedSet() {
      return new Set((this.usedTypes || []).filter(Boolean))
    }
  },
  watch: {
    groups: {
      immediate: true,
      handler() {
        this.ensureDefaultOpen()
      }
    },
    queryNorm(q, prev) {
      if (q) return
      if (prev) this.restoreSingleOpen()
    }
  },
  methods: {
    widgetMatches(w, q) {
      const title = String(this.$t(w.titleKey) || '').toLowerCase()
      const id = String(w.id || '').toLowerCase()
      return title.includes(q) || id.includes(q)
    },
    ensureDefaultOpen() {
      if (this.isSearching) return
      if (this.openGroups.length) return
      this.restoreSingleOpen()
    },
    restoreSingleOpen() {
      const gs = this.groups
      if (!gs?.length) {
        this.openGroups = []
        return
      }
      const withUsed = gs.find(g => g.widgets.some(w => this.usedSet.has(w.id)))
      this.openGroups = [withUsed ? withUsed.group : gs[0].group]
    },
    isOpen(group) {
      if (this.isSearching) return true
      return this.openGroups.includes(group)
    },
    toggle(group) {
      if (this.isSearching) return
      this.openGroups = this.openGroups.includes(group) ? [] : [group]
    },
    isUsed(id) {
      return this.usedSet.has(id)
    },
    componentOf(w) {
      return getWidgetDef(w.id)?.component || null
    },
    propsOf(w) {
      const def = getWidgetDef(w.id)
      if (!def) return {}
      const props = {
        widgetId: w.id,
        ...(def.props || {})
      }
      if (def.boardChart) {
        props.boardChart = def.boardChart
        props.embedded = true
      }
      if (w.id === 'team-weekly-report') props.showRange = false
      if (w.id === 'document-pin') {
        props.docId = null
        props.editing = false
      }
      return props
    },
    placeholderLetter(w) {
      const t = this.$t(w.titleKey)
      return (t && String(t).charAt(0)) || '?'
    },
    onDragStart(e, type) {
      this.$emit('catalog-drag-start', type)
      if (e.dataTransfer) {
        e.dataTransfer.setData('text/plain', 'catalog:' + type)
        e.dataTransfer.effectAllowed = 'copy'
      }
    }
  }
}
</script>

<style scoped>
.wb-catalog {
  position: relative;
  display: flex;
  flex-direction: column;
  align-self: stretch;
  min-height: 0;
  height: 100%;
  padding: 12px 10px;
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  border-radius: 10px;
  background: var(--el-bg-color, var(--bg-color-base, #fff));
  color: var(--text-color-primary, var(--el-text-color-primary, #303133));
  overflow: hidden;
  box-sizing: border-box;
}
@media (max-width: 900px) {
  .wb-catalog {
    height: auto;
    max-height: min(60vh, 520px);
  }
}
.wb-catalog__hd {
  flex-shrink: 0;
  margin-bottom: 8px;
}
.wb-catalog__title {
  margin: 0 2px 8px;
  font-size: 13px;
  font-weight: 650;
  letter-spacing: 0.02em;
}
.wb-catalog__search {
  margin: 0;
}
.wb-catalog__body {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
}
.wb-catalog__empty {
  margin: 8px 2px 12px;
  padding: 10px 8px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--el-text-color-secondary, #909399);
  text-align: center;
}
.wb-catalog__group + .wb-catalog__group {
  margin-top: 4px;
  padding-top: 4px;
  border-top: 1px solid var(--el-border-color-extra-light, rgba(255, 255, 255, 0.06));
}
.wb-catalog__group-hd {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  margin: 0;
  padding: 8px 4px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-align: left;
}
.wb-catalog__group-hd:hover {
  background: var(--el-fill-color-light, rgba(255, 255, 255, 0.04));
}
.wb-catalog__group-name {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-color-regular, var(--el-text-color-regular, #606266));
}
.wb-catalog__group-count {
  font-size: 11px;
  font-variant-numeric: tabular-nums;
  color: var(--el-text-color-placeholder, #a8abb2);
}
.wb-catalog__chev {
  margin-left: auto;
  width: 6px;
  height: 6px;
  border-right: 1.5px solid currentColor;
  border-bottom: 1.5px solid currentColor;
  opacity: 0.55;
  transform: rotate(-45deg);
  transition: transform 0.18s ease;
}
.wb-catalog__group.is-open .wb-catalog__chev {
  transform: rotate(45deg);
}
.wb-catalog__group-bd {
  padding: 2px 0 8px;
}
.wb-catalog__card {
  margin-bottom: 8px;
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  border-radius: 8px;
  background: var(--el-bg-color, var(--bg-color-base, #1a1a1a));
  cursor: grab;
  user-select: none;
  overflow: hidden;
}
.wb-catalog__card:hover {
  border-color: var(--el-color-primary-light-5, #a0cfff);
}
.wb-catalog__card:active {
  cursor: grabbing;
}
.wb-catalog__card.is-used {
  border-color: var(--el-color-success-light-5, #95d475);
}
.wb-catalog__placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 96px;
  font-size: 22px;
  font-weight: 700;
  color: var(--el-text-color-secondary, #909399);
  opacity: 0.55;
}
.wb-catalog__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  padding: 6px 8px;
  border-top: 1px solid var(--el-border-color-extra-light, rgba(255, 255, 255, 0.06));
}
.wb-catalog__name {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.wb-catalog__used {
  flex-shrink: 0;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 11px;
  line-height: 1.4;
  color: var(--el-color-success, #67c23a);
  background: rgba(103, 194, 58, 0.12);
}
</style>
