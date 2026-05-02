<template>
  <div class="cat2-bug-table-wrap">
    <el-table ref="elTable"
              :key="tableKey"
              v-loading="loading"
              :data="data"
              @selection-change="handleSelectionChange"
              @sort-change="sortChangeHandle"
              @row-click="handleClickTableRow"
              @mousedown.native="e => $emit('native-mousedown', e)"
              @mouseup.native="e => $emit('native-mouseup', e)"
              @mousemove.native="e => $emit('native-mousemove', e)">
      <slot name="prepend"></slot>
      <el-table-column v-for="(col, colIndex) in showTableFieldList"
                       :label="columnHeaderLabel(col)"
                       :key="columnRowKey(col, colIndex)"
                       :min-width="columnMinWidth(col)"
                       :prop="col.prop"
                       :class-name="columnClassName(col)"
                       :fixed="col.fixed ? 'left' : false"
                       :align="col.align || 'left'"
                       :sort-by="columnSortBy(col)"
                       :sort-orders="columnSortOrders(col)"
                       :sortable="columnSortableMode(col)">
        <template v-slot:header>
          <div v-if="col.pinFixedToggle !== false" class="table-header">
            <svg-icon :icon-class="col.fixed ? 'header-right' : 'header-left'" @click.stop="handleClickColumnsPin(col)"></svg-icon>
            <span class="header-title">{{ columnHeaderLabel(col) }}</span>
          </div>
          <span v-else class="header-title-only">{{ columnHeaderLabel(col) }}</span>
        </template>
        <template slot-scope="scope">
          <slot name="columns" :scope="scope" :column="col"></slot>
        </template>
      </el-table-column>
      <slot name="append"></slot>
    </el-table>
  </div>
</template>

<script>
import Sortable from "sortablejs";

const TABLE_FIELD_LIST_CACHE_KEY = 'defect-table-field-list';
const TABLE_SORT_COLUMN = 'defect_table_sort_column_key';
const TABLE_SORT_TYPE = 'defect_table_sort_type_key';
const DEFAULT_COLUMN_WIDTH = 180;

export default {
  name: "Cat2BugTable",
  data() {
    return {
      tableFieldList: [],
      tableKey: (new Date()).getMilliseconds(),
      headerSortable: {},
      orderByColumn: null,
      isAsc: null
    }
  },
  props: {
    cacheKey: {
      type: String,
      default: null
    },
    fieldListCacheKey: {
      type: String,
      default: null
    },
    columns: {
      type: Array,
      default: () => []
    },
    data: {
      type: Array,
      default: () => []
    },
    loading: {
      type: Boolean,
      default: false
    },
    sortColumnCacheKey: {
      type: String,
      default: null
    },
    sortTypeCacheKey: {
      type: String,
      default: null
    },
    persistSort: {
      type: Boolean,
      default: true
    }
  },
  computed: {
    showTableFieldList: function () {
      return this.tableFieldList.map((col, index) => {
        col.index = index;
        return col;
      }).filter(col => col.visible);
    },
    columnWidth: function () {
      return function (column) {
        return column['width_' + this.$i18n.locale] || column.width || DEFAULT_COLUMN_WIDTH;
      }
    }
  },
  watch: {
    "$i18n.locale": function () {
      this.$nextTick(() => {
        this.$refs.elTable && this.$refs.elTable.doLayout();
      });
    },
  },
  created() {
    const merged = this.mergeCachedColumns(this.columns);
    this.tableFieldList = merged;
    this.$emit('columns-change', merged);
    if (this.columnsStorageKey()) {
      this.saveShowColumns(merged);
    }
  },
  mounted() {
    this.initSort();
    this.initColumnDrag();
  },
  methods: {
    columnsStorageKey() {
      if (this.fieldListCacheKey) return this.fieldListCacheKey;
      if (this.cacheKey != null && this.cacheKey !== '') return this.cacheKey + TABLE_FIELD_LIST_CACHE_KEY;
      return null;
    },
    sortColumnStorageKey() {
      if (this.sortColumnCacheKey) return this.sortColumnCacheKey;
      if (this.cacheKey != null && this.cacheKey !== '') return this.cacheKey + TABLE_SORT_COLUMN;
      return null;
    },
    sortTypeStorageKey() {
      if (this.sortTypeCacheKey) return this.sortTypeCacheKey;
      if (this.cacheKey != null && this.cacheKey !== '') return this.cacheKey + TABLE_SORT_TYPE;
      return null;
    },
    columnHeaderLabel(col) {
      if (col.label !== undefined && col.label !== null) return col.label;
      return this.$t(col.key);
    },
    columnRowKey(col, colIndex) {
      return (col.key || col.prop) + '_' + colIndex;
    },
    columnMinWidth(col) {
      return col.minWidth || this.columnWidth(col);
    },
    columnClassName(col) {
      const parts = [];
      if (col.fixed) parts.push('no-drag');
      if (col.className) parts.push(col.className);
      return parts.join(' ');
    },
    columnSortableMode(col) {
      if (col.sortable === false) return false;
      if (col.sortable === true) return true;
      return 'custom';
    },
    columnSortBy(col) {
      const mode = this.columnSortableMode(col);
      return mode === 'custom' || mode === true ? col.prop : undefined;
    },
    columnSortOrders(col) {
      if (this.columnSortableMode(col) !== 'custom') return undefined;
      return col.prop === this.orderByColumn ? [this.isAsc] : [null];
    },
    mergeCachedColumns(defaults) {
      const list = defaults && defaults.length ? defaults.map(d => ({ ...d })) : [];
      const storageKey = this.columnsStorageKey();
      if (!storageKey || !list.length) {
        return list;
      }
      const cached = this.getShowColumns(storageKey);
      if (!cached || !Array.isArray(cached) || cached.length === 0) {
        return list;
      }
      if (typeof cached[0] === 'string') {
        const visibleKeys = new Set(cached);
        return list.map(d => ({ ...d, visible: visibleKeys.has(d.key) }));
      }
      const defaultByKey = {};
      list.forEach(d => {
        defaultByKey[d.key] = d;
      });
      const merged = [];
      const used = new Set();
      cached.forEach(c => {
        const base = defaultByKey[c.key];
        if (!base) return;
        merged.push({
          ...base,
          fixed: !!c.fixed,
          visible: c.visible !== false
        });
        used.add(c.key);
      });
      list.forEach(d => {
        if (!used.has(d.key)) merged.push({ ...d });
      });
      return merged;
    },
    initSort() {
      if (!this.persistSort) {
        this.orderByColumn = null;
        this.isAsc = null;
        return;
      }
      const ck = this.sortColumnStorageKey();
      const tk = this.sortTypeStorageKey();
      if (!ck || !tk) {
        this.orderByColumn = null;
        this.isAsc = null;
        return;
      }
      this.isAsc = this.$cache.local.get(tk) || null;
      this.orderByColumn = this.$cache.local.get(ck) || null;
      this.sort(this.orderByColumn, this.isAsc);
    },
    sortChangeHandle(e) {
      this.isAsc = e.order;
      this.orderByColumn = e.prop;
      if (this.persistSort) {
        const ck = this.sortColumnStorageKey();
        const tk = this.sortTypeStorageKey();
        if (ck && tk) {
          if (e.prop != null) this.$cache.local.set(ck, e.prop);
          else this.$cache.local.remove(ck);
          if (e.order != null) this.$cache.local.set(tk, e.order);
          else this.$cache.local.remove(tk);
        }
      }
      this.$emit('sort-change', e);
    },
    saveShowColumns(columns) {
      const key = this.columnsStorageKey();
      if (!key) return;
      this.$cache.local.setJSON(key, columns);
    },
    getShowColumns(storageKey) {
      const key = storageKey || this.columnsStorageKey();
      if (!key) return null;
      let cached = this.$cache.local.getJSON(key);
      if (cached == null) {
        const raw = this.$cache.local.get(key);
        if (raw) {
          try {
            cached = typeof raw === 'string' ? JSON.parse(raw) : raw;
          } catch (e) {
            cached = null;
          }
        }
      }
      return cached;
    },
    /** 按勾选的可显示列 key 更新显隐（不参与列设置的列保持可见） */
    setColumnsVisible(showColumns) {
      const showSet = new Set(showColumns);
      this.setColumns(this.tableFieldList.map(col => ({
        ...col,
        visible: col.showInColumnPicker === false ? true : showSet.has(col.key)
      })));
      this.reorderColumns();
    },
    setColumns(columns) {
      this.tableFieldList = columns;
      this.$emit('columns-change', columns);
      if (this.columnsStorageKey()) this.saveShowColumns(columns);
    },
    initColumnDrag() {
      this.bindSortable('.el-table__header-wrapper thead tr', 'normal');
      this.bindSortable('.el-table__fixed-header-wrapper thead tr', 'fixed');
    },
    bindSortable(selector, zone) {
      this.$nextTick(() => {
        const el = this.$refs.elTable.$el.querySelector(selector);
        if (!el) return;
        if (this.headerSortable[zone]) {
          this.headerSortable[zone].destroy();
        }
        this.headerSortable[zone] = Sortable.create(el, {
          ghostClass: 'table-header-ghost',
          group: zone,
          onStart: () => {
            this.mouseFlag = false;
          },
          onEnd: evt => {
            const oldField = this.showTableFieldList[evt.oldIndex];
            const newField = this.showTableFieldList[evt.newIndex];
            if (!oldField || !newField) return;
            this.mouseFlag = false;
            const moved = this.tableFieldList.splice(oldField.index, 1)[0];
            this.tableFieldList.splice(newField.index, 0, moved);
            this.reorderColumns();
          }
        });
      });
    },
    sort(prop, order) {
      if (!this.$refs.elTable || prop == null) return;
      this.$refs.elTable.sort(prop, order);
    },
    doLayout() {
      this.$refs.elTable && this.$refs.elTable.doLayout();
    },
    handleClickColumnsPin(column) {
      column.fixed = !column.fixed;
      if (this.columnsStorageKey()) this.saveShowColumns(this.tableFieldList);
      this.tableKey = (new Date()).getMilliseconds();
      this.initColumnDrag();
    },
    handleSelectionChange(selection) {
      this.$emit('selection-change', selection);
    },
    handleClickTableRow(row) {
      this.$emit('row-click', row);
    },
    reorderColumns() {
      if (!this.$refs.elTable) return;
      const fixedCols = this.tableFieldList.filter(c => c.fixed);
      const normalCols = this.tableFieldList.filter(c => !c.fixed);
      this.setColumns([...fixedCols, ...normalCols]);

      const storeCols = this.$refs.elTable.store.states.columns;
      const dataCols = storeCols.filter(c => c.property);
      const fixedStore = dataCols.filter(c => c.fixed === 'left');
      const normalStore = dataCols.filter(c => !c.fixed);

      this.$refs.elTable.store.states.columns = [
        ...fixedStore,
        ...normalStore,
        ...storeCols.filter(c => !c.property)
      ];
      this.$nextTick(() => {
        this.$refs.elTable.doLayout();
      });
    }
  }
}
</script>

<style lang="scss" scoped>
.header-title-only {
  display: inline-block;
}

::v-deep .table-header-ghost {
  background-color: #ecf5ff !important;
  border-radius: 3px;
}

.table-header {
  display: inline-flex;
  justify-content: flex-start;
  align-items: center;
  flex-direction: row;
  gap: 5px;
}

::v-deep thead .no-drag {
  background-color: #f0f0f1;
}

::v-deep tbody > .el-table__row > .el-table__cell {
  padding: 5px 0;
}
</style>
