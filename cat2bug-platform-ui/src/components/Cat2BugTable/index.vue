<template>
  <div class="cat2-bug-table-wrap" :style="operateColumnWrapStyle">
    <el-table ref="elTable"
              :key="tableKey"
              v-loading="loading"
              :data="data"
              :max-height="elTableMaxHeight"
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
                       :sortable="columnSortableMode(col)">
        <template v-slot:header>
          <div v-if="col.pinFixedToggle !== false" :class="['table-header', { 'table-header--required': col.required }]">
            <svg-icon
              :icon-class="col.fixed ? 'header-right' : 'header-left'"
              class="table-header-pin"
              @mousedown.stop
              @click.stop="handleClickColumnsPin(col)"
            ></svg-icon>
            <span :class="['header-title', { 'header-title--required': col.required }]">{{ columnHeaderLabel(col) }}</span>
          </div>
          <span v-else :class="['header-title-only', { 'header-title--required': col.required }]">{{ columnHeaderLabel(col) }}</span>
        </template>
        <template slot-scope="scope">
          <slot name="columns" :scope="scope" :column="col"></slot>
        </template>
      </el-table-column>
      <slot name="append"></slot>
    </el-table>
    <!-- 原生横向条无法在不错位 DOM 的情况下区间显示；自定义条同步 scrollLeft，轨道从左/右固定列内侧起算 -->
    <div
      v-show="customXVisible"
      ref="customXBarTrack"
      class="cat2bug-custom-xbar"
      :style="customXTrackStyle"
      @mousedown.self.prevent="onCustomXTrackMouseDown"
    >
      <div
        ref="customXBarThumb"
        class="cat2bug-custom-xbar__thumb"
        :style="customXThumbStyle"
        @mousedown.stop.prevent="onCustomXThumbMouseDown"
      />
    </div>
  </div>
</template>

<script>
import Sortable from "sortablejs";
import { getColumnById } from "element-ui/packages/table/src/util";

const TABLE_FIELD_LIST_CACHE_KEY = 'defect-table-field-list';
const TABLE_SORT_COLUMN = 'defect_table_sort_column_key';
const TABLE_SORT_TYPE = 'defect_table_sort_type_key';
const DEFAULT_COLUMN_WIDTH = 180;
/** append 操作列 class-name 需含此类名以启用自动列宽 */
const OPERATE_COLUMN_CLASS = 'cat2bug-operate-column';
/** 操作列内容容器 class，用于测量与换行；无此 class 时回退测量 .cell 内可见子节点 */
const OPERATE_TOOLS_CLASS = 'cat2bug-operate-tools';
const DEFAULT_OPERATE_COLUMN_MAX_WIDTH = 450;
const DEFAULT_OPERATE_COLUMN_MIN_WIDTH = 88;
const DEFAULT_OPERATE_COLUMN_HEADER_MIN = 56;
/** 操作列 .cell 左右内边距（单侧 px，列宽计算时按两侧合计） */
const DEFAULT_OPERATE_COLUMN_PADDING_X = 20;

export default {
  name: "Cat2BugTable",
  data() {
    return {
      tableFieldList: [],
      tableKey: (new Date()).getMilliseconds(),
      headerSortable: {},
      orderByColumn: null,
      isAsc: null,
      customXVisible: false,
      customXTrackStyle: {},
      customXThumbStyle: {},
      _customXScrollListener: null,
      _customXWheelListener: null,
      _customXResizeListener: null,
      _customXRo: null,
      _customXBoundBody: null,
      _doLayoutTimer: null,
      _rowSyncRafId: null,
      _lastOperateWidth: null,
      _operateColRo: null,
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
    /** 为 true 时列顺序以父组件 columns 为准，本地缓存仅由父组件合并显隐/固定后再传入 */
    preserveColumnOrder: {
      type: Boolean,
      default: false
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
    },
    /** 为 false 时不展示列头排序、不触发 sort-change（仍保留列拖拽等表头能力） */
    enableHeaderSort: {
      type: Boolean,
      default: true
    },
    /** 传入后由表格内部滚动表体，表头固定（像素值或带单位的字符串，不设则整表随外层滚动） */
    tableMaxHeight: {
      type: [Number, String],
      default: null
    },
    /** 为 true 时，对 class-name 含 cat2bug-operate-column 的 append 列按内容自动设宽 */
    operateColumnAutoWidth: {
      type: Boolean,
      default: true
    },
    operateColumnMaxWidth: {
      type: Number,
      default: DEFAULT_OPERATE_COLUMN_MAX_WIDTH
    },
    operateColumnMinWidth: {
      type: Number,
      default: DEFAULT_OPERATE_COLUMN_MIN_WIDTH
    },
    operateColumnHeaderMin: {
      type: Number,
      default: DEFAULT_OPERATE_COLUMN_HEADER_MIN
    },
    operateColumnPaddingX: {
      type: Number,
      default: DEFAULT_OPERATE_COLUMN_PADDING_X
    }
  },
  computed: {
    operateColumnWrapStyle() {
      return {
        '--cat2bug-operate-col-max-width': `${this.operateColumnMaxWidth}px`,
        '--cat2bug-operate-col-padding-x': `${this.operateColumnPaddingX}px`
      };
    },
    elTableMaxHeight() {
      const v = this.tableMaxHeight;
      if (v === null || v === undefined || v === '') return undefined;
      return v;
    },
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
      this.$nextTick(() => this.scheduleDoLayout());
    },
    tableKey() {
      this.teardownCustomHorizontalScrollbar();
      this.$nextTick(() => {
        this.setupCustomHorizontalScrollbar();
        this.scheduleDoLayout();
      });
    },
    data() {
      this.$nextTick(() => this.scheduleDoLayout());
    },
    loading(val) {
      if (val === false) {
        this.$nextTick(() => this.scheduleDoLayout());
      }
    },
    columns: {
      deep: true,
      handler(cols) {
        if (!this.preserveColumnOrder) return;
        const next = (cols || []).map(c => ({ ...c }));
        this.tableFieldList = next;
        this.$emit('columns-change', next);
      }
    },
  },
  created() {
    const merged = this.preserveColumnOrder
      ? (this.columns || []).map(c => ({ ...c }))
      : this.mergeCachedColumns(this.columns);
    this.tableFieldList = merged;
    this.$emit('columns-change', merged);
    /* 不在 created 写入列缓存：父组件异步加载字段管理顺序前，columns 常为 TableOptions 默认序，会污染本地缓存 */
  },
  mounted() {
    this.initSort();
    this.initColumnDrag();
    this.$nextTick(() => {
      this.patchTableDoLayout();
      this.setupOperateColumnResizeGuard();
      this.setupCustomHorizontalScrollbar();
      this.scheduleDoLayout();
    });
  },
  beforeDestroy() {
    this.teardownOperateColumnResizeGuard();
    if (this._doLayoutTimer) {
      clearTimeout(this._doLayoutTimer);
      this._doLayoutTimer = null;
    }
    if (this._rowSyncRafId != null) {
      cancelAnimationFrame(this._rowSyncRafId);
      this._rowSyncRafId = null;
    }
    this.teardownCustomHorizontalScrollbar();
  },
  methods: {
    /**
     * 固定列区域是独立 table，主表体与固定列行高依赖同一套列宽与 scrollY gutter。
     * 数据/插槽渲染后延迟 doLayout，避免中间列换行高度与固定列不一致。
     */
    scheduleDoLayout() {
      if (this._doLayoutTimer) clearTimeout(this._doLayoutTimer);
      this._doLayoutTimer = setTimeout(() => {
        this._doLayoutTimer = null;
        const t = this.$refs.elTable;
        if (t) {
          t.doLayout();
          this.$nextTick(() => {
            this.$nextTick(() => {
              requestAnimationFrame(() => {
                this.syncOperateColumnWidths();
                this.syncFixedDataRowHeights();
                this.updateCustomHorizontalScrollbar();
              });
            });
          });
        }
      }, 50);
    },

    /** 主表与左右固定表各行 tr 上的行内 height，便于重新测量 */
    clearDataRowHeights(tableRoot) {
      if (!tableRoot) return;
      const sel = "tbody tr.el-table__row";
      const reset = (tr) => {
        tr.style.height = "";
        tr.style.minHeight = "";
      };
      const mainBw = tableRoot.querySelector(".el-table__body-wrapper");
      if (mainBw) {
        mainBw.querySelectorAll(sel).forEach(reset);
      }
      const fixedL = tableRoot.querySelector(".el-table__fixed");
      const fixedR = tableRoot.querySelector(".el-table__fixed-right");
      [fixedL, fixedR].forEach((fx) => {
        if (!fx) return;
        const fbw = fx.querySelector(".el-table__fixed-body-wrapper");
        if (fbw) {
          fbw.querySelectorAll(sel).forEach(reset);
        }
      });
    },

    /** 单行自然内容高度（取该行各单元格最大值） */
    measureDataRowContentHeight(tr) {
      if (!tr) return 0;
      let max = 0;
      tr.querySelectorAll("td.el-table__cell").forEach((td) => {
        max = Math.max(max, td.getBoundingClientRect().height);
      });
      if (max <= 0) {
        max = tr.getBoundingClientRect().height;
      }
      return max;
    },

    /**
     * Element 固定列为独立 table，仅靠 is-hidden + visibility 仍可能出现行高与斑马纹逐行错位。
     * 以主表体行为基准，取主表 / 左固定 / 右固定同一索引行的最大高度，写回三处 tr。
     */
    syncFixedDataRowHeights() {
      const t = this.$refs.elTable;
      if (!t || !t.$el) return;
      const root = t.$el;
      const fixedL = root.querySelector(".el-table__fixed");
      const fixedR = root.querySelector(".el-table__fixed-right");
      if (!fixedL && !fixedR) return;

      const mainBw = root.querySelector(".el-table__body-wrapper");
      if (!mainBw) return;

      const sel = "tbody tr.el-table__row";
      this.clearDataRowHeights(root);

      const mainRows = mainBw.querySelectorAll(sel);
      if (!mainRows.length) return;

      let leftRows = null;
      let rightRows = null;
      if (fixedL) {
        const fbw = fixedL.querySelector(".el-table__fixed-body-wrapper");
        if (fbw) leftRows = fbw.querySelectorAll(sel);
      }
      if (fixedR) {
        const fbw = fixedR.querySelector(".el-table__fixed-body-wrapper");
        if (fbw) rightRows = fbw.querySelectorAll(sel);
      }

      let n = mainRows.length;
      if (leftRows && leftRows.length < n) n = leftRows.length;
      if (rightRows && rightRows.length < n) n = rightRows.length;

      for (let i = 0; i < n; i++) {
        mainRows[i].style.height = "auto";
        if (leftRows && leftRows[i]) leftRows[i].style.height = "auto";
        if (rightRows && rightRows[i]) rightRows[i].style.height = "auto";
      }
      void root.offsetHeight;

      const heights = [];
      for (let i = 0; i < n; i++) {
        let h = this.measureDataRowContentHeight(mainRows[i]);
        if (leftRows && leftRows[i]) {
          h = Math.max(h, this.measureDataRowContentHeight(leftRows[i]));
        }
        if (rightRows && rightRows[i]) {
          h = Math.max(h, this.measureDataRowContentHeight(rightRows[i]));
        }
        heights.push(Math.max(1, Math.ceil(h)));
      }

      for (let i = 0; i < n; i++) {
        const px = `${heights[i]}px`;
        mainRows[i].style.height = px;
        if (leftRows && leftRows[i]) leftRows[i].style.height = px;
        if (rightRows && rightRows[i]) rightRows[i].style.height = px;
      }
    },

    /** ResizeObserver 可能高频触发，合并到单次 rAF */
    requestFixedRowHeightSync() {
      if (this._rowSyncRafId != null) cancelAnimationFrame(this._rowSyncRafId);
      this._rowSyncRafId = requestAnimationFrame(() => {
        this._rowSyncRafId = null;
        this.syncFixedDataRowHeights();
      });
    },
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
      if (!this.enableHeaderSort) return false;
      if (col.sortable === false) return false;
      if (col.customFieldKey) return false;
      if (col.prop && String(col.prop).startsWith('custom_')) return false;
      if (col.sortable === true) return true;
      return 'custom';
    },
    isUnsupportedSortProp(prop) {
      if (prop == null || prop === '') return false;
      const s = String(prop);
      return s.startsWith('custom_') || s.startsWith('custom:');
    },
    columnSortBy(col) {
      const mode = this.columnSortableMode(col);
      return mode === 'custom' || mode === true ? col.prop : undefined;
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
      if (!this.enableHeaderSort) {
        this.orderByColumn = null;
        this.isAsc = null;
        return;
      }
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
      if (this.isUnsupportedSortProp(this.orderByColumn)) {
        this.clearPersistedSort();
        return;
      }
      this.sort(this.orderByColumn, this.isAsc);
    },
    clearPersistedSort() {
      this.orderByColumn = null;
      this.isAsc = null;
      if (!this.persistSort) return;
      const ck = this.sortColumnStorageKey();
      const tk = this.sortTypeStorageKey();
      if (ck) this.$cache.local.remove(ck);
      if (tk) this.$cache.local.remove(tk);
    },
    clearSort() {
      this.clearPersistedSort();
      if (this.$refs.elTable) this.$refs.elTable.clearSort();
    },
    sortChangeHandle(e) {
      if (!this.enableHeaderSort) return;
      if (e.prop != null && this.isUnsupportedSortProp(e.prop)) {
        this.clearSort();
        this.$emit('sort-change', { prop: null, order: null, column: e.column });
        return;
      }
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
    getColumnConfigForExport() {
      return this.tableFieldList.map(col => ({
        key: col.key,
        prop: col.prop,
        visible: col.visible !== false
      }));
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
    /**
     * 与 Element UI util.getColumnByCell 一致：th.className 中含 el-table_*_column_*（可多级 _column_），
     * 不能用 ^el-table_\\d+_column_\\d+$，否则子列 id 或部分构建下无法解析，拖列会整段 return。
     */
    getHeaderThDataProp(th) {
      if (!th || !this.$refs.elTable) return null;
      const matches = (th.className || "").match(/el-table_[^\s]+/);
      if (!matches) return null;
      const storeCol = getColumnById(this.$refs.elTable, matches[0]);
      return storeCol && storeCol.property != null && storeCol.property !== ""
        ? storeCol.property
        : null;
    },
    initColumnDrag() {
      this.bindSortable('.el-table__header-wrapper thead tr', 'normal');
      this.bindSortable('.el-table__fixed-header-wrapper thead tr', 'fixed');
      this.$nextTick(() => {
        this.updateCustomHorizontalScrollbar();
        this.requestFixedRowHeightSync();
      });
    },
    getTableBodyWrapper() {
      const t = this.$refs.elTable;
      return t && t.$el ? t.$el.querySelector('.el-table__body-wrapper') : null;
    },
    setupCustomHorizontalScrollbar() {
      this.teardownCustomHorizontalScrollbar();
      const bw = this.getTableBodyWrapper();
      const wrap = this.$el;
      if (!bw || !wrap) return;

      this._customXBoundBody = bw;
      this._customXScrollListener = () => this.updateCustomHorizontalScrollbar();
      bw.addEventListener('scroll', this._customXScrollListener, { passive: true });

      this._customXWheelListener = (e) => this.handleCustomXWheel(e);
      wrap.addEventListener('wheel', this._customXWheelListener, { passive: false, capture: true });

      this._customXResizeListener = () => {
        this.$nextTick(() => {
          this.updateCustomHorizontalScrollbar();
          this.requestFixedRowHeightSync();
        });
      };
      window.addEventListener('resize', this._customXResizeListener);

      this._customXRo = new ResizeObserver(() => {
        this.updateCustomHorizontalScrollbar();
        this.requestFixedRowHeightSync();
      });
      this._customXRo.observe(wrap);
      this._customXRo.observe(bw);
      const root = this.$refs.elTable.$el;
      const fixedL = root.querySelector('.el-table__fixed');
      const fixedR = root.querySelector('.el-table__fixed-right');
      if (fixedL) this._customXRo.observe(fixedL);
      if (fixedR) this._customXRo.observe(fixedR);

      this.updateCustomHorizontalScrollbar();
    },
    teardownCustomHorizontalScrollbar() {
      const wrap = this.$el;
      if (wrap && wrap.classList) wrap.classList.remove('cat2bug-hide-native-x');
      if (wrap && wrap.parentElement) wrap.parentElement.classList.remove('cat2bug-parent-lock-x');

      const bw = this._customXBoundBody;
      if (bw && this._customXScrollListener) {
        bw.removeEventListener('scroll', this._customXScrollListener);
      }
      if (wrap && this._customXWheelListener) {
        wrap.removeEventListener('wheel', this._customXWheelListener, true);
      }
      this._customXBoundBody = null;
      this._customXScrollListener = null;
      this._customXWheelListener = null;
      if (this._customXResizeListener) {
        window.removeEventListener('resize', this._customXResizeListener);
        this._customXResizeListener = null;
      }
      if (this._customXRo) {
        this._customXRo.disconnect();
        this._customXRo = null;
      }
    },
    updateCustomHorizontalScrollbar() {
      const wrap = this.$el;
      const elTable = this.$refs.elTable;
      if (!wrap || !elTable || !elTable.$el) return;

      const tableRoot = elTable.$el;
      const bw = tableRoot.querySelector('.el-table__body-wrapper');
      if (!bw) return;

      const scrollW = bw.scrollWidth;
      const clientW = bw.clientWidth;
      const needsX = scrollW > clientW + 2;

      wrap.classList.toggle('cat2bug-hide-native-x', needsX);
      const scrollParent = wrap.parentElement;
      if (scrollParent) scrollParent.classList.toggle('cat2bug-parent-lock-x', needsX);

      if (!needsX) {
        this.customXVisible = false;
        return;
      }

      const wrapRect = wrap.getBoundingClientRect();
      const bwRect = bw.getBoundingClientRect();
      const fixedL = tableRoot.querySelector('.el-table__fixed');
      const fixedR = tableRoot.querySelector('.el-table__fixed-right');

      let leftInset = 0;
      if (fixedL) {
        const fr = fixedL.getBoundingClientRect();
        leftInset = Math.max(0, Math.round(fr.right - bwRect.left));
      }

      let rightInset = 0;
      if (fixedR) {
        const fr = fixedR.getBoundingClientRect();
        rightInset = Math.max(0, Math.round(bwRect.right - fr.left));
      }

      const barH = 6;
      const trackLeft = bwRect.left - wrapRect.left + leftInset;
      const trackWidth = Math.max(0, Math.round(bwRect.width - leftInset - rightInset));
      let trackTop = bwRect.bottom - wrapRect.top - barH;
      if (trackTop < 0) trackTop = 0;

      if (trackWidth < 8) {
        this.customXVisible = false;
        wrap.classList.remove('cat2bug-hide-native-x');
        if (scrollParent) scrollParent.classList.remove('cat2bug-parent-lock-x');
        return;
      }

      this.customXTrackStyle = {
        left: `${trackLeft}px`,
        top: `${trackTop}px`,
        width: `${trackWidth}px`,
        height: `${barH}px`,
      };

      const maxScroll = scrollW - clientW;
      const thumbRatio = Math.min(1, clientW / scrollW);
      let thumbW = Math.max(18, Math.floor(trackWidth * thumbRatio));
      if (thumbW > trackWidth) thumbW = trackWidth;
      const travel = Math.max(0, trackWidth - thumbW);
      const thumbLeft = maxScroll <= 0 ? 0 : Math.round((bw.scrollLeft / maxScroll) * travel);

      this.customXThumbStyle = {
        width: `${thumbW}px`,
        height: '100%',
        left: `${thumbLeft}px`,
      };

      this.customXVisible = true;
    },
    /**
     * body-wrapper overflow-x:hidden 后浏览器不再把横滑交给默认横条；在表格外包层捕获 wheel，
     * 仅在「横向意图」下 preventDefault 并写 scrollLeft（竖向滚动照常冒泡）。
     */
    handleCustomXWheel(e) {
      const wrap = this.$el;
      const bw = this._customXBoundBody;
      if (!wrap || !bw || !wrap.classList.contains('cat2bug-hide-native-x')) return;

      const maxScroll = bw.scrollWidth - bw.clientWidth;
      if (maxScroll <= 0) return;

      let dx = e.deltaX;
      if (e.shiftKey && Math.abs(e.deltaY) >= Math.abs(dx)) {
        dx = e.deltaY;
      }
      if (!dx) return;

      if (!e.shiftKey && Math.abs(dx) < Math.abs(e.deltaY)) return;

      e.preventDefault();
      e.stopPropagation();
      bw.scrollLeft = Math.max(0, Math.min(maxScroll, bw.scrollLeft + dx));
      this.updateCustomHorizontalScrollbar();
    },
    onCustomXThumbMouseDown(e) {
      const bw = this.getTableBodyWrapper();
      const track = this.$refs.customXBarTrack;
      const thumb = this.$refs.customXBarThumb;
      if (!bw || !track || !thumb) return;

      const startX = e.clientX;
      const startScroll = bw.scrollLeft;
      const maxScroll = Math.max(0, bw.scrollWidth - bw.clientWidth);
      const travel = Math.max(1, track.clientWidth - thumb.offsetWidth);

      const onMove = (ev) => {
        const dx = ev.clientX - startX;
        const next = startScroll + (dx / travel) * maxScroll;
        bw.scrollLeft = Math.max(0, Math.min(maxScroll, next));
        this.updateCustomHorizontalScrollbar();
      };
      const onUp = () => {
        document.removeEventListener('mousemove', onMove);
        document.removeEventListener('mouseup', onUp);
      };
      document.addEventListener('mousemove', onMove);
      document.addEventListener('mouseup', onUp);
    },
    onCustomXTrackMouseDown(e) {
      const bw = this.getTableBodyWrapper();
      const track = this.$refs.customXBarTrack;
      const thumb = this.$refs.customXBarThumb;
      if (!bw || !track || !thumb) return;

      const rect = track.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const maxScroll = Math.max(0, bw.scrollWidth - bw.clientWidth);
      const trackW = rect.width;
      const thumbW = thumb.offsetWidth;
      const thumbTravel = Math.max(1, trackW - thumbW);
      let ratio = (x - thumbW / 2) / thumbTravel;
      ratio = Math.max(0, Math.min(1, ratio));
      bw.scrollLeft = ratio * maxScroll;
      this.updateCustomHorizontalScrollbar();
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
          filter: 'th.gutter, .el-table__gutter, .no-drag, .cat2bug-operate-column, .table-header, .table-header-pin',
          preventOnFilter: false,
          onStart: () => {
            this.mouseFlag = false;
          },
          onEnd: evt => {
            this.mouseFlag = false;
            const visibleInZone = this.showTableFieldList.filter((col) =>
              zone === 'fixed' ? !!col.fixed : !col.fixed
            );
            const dataLen = visibleInZone.length;
            if (!dataLen || !evt.from || !evt.from.children) return;

            const isGutterTh = (cell) =>
              cell &&
              cell.tagName === 'TH' &&
              (cell.classList.contains('gutter') || cell.classList.contains('el-table__gutter'));

            /**
             * 注意：必须用 tagName !== 'TH' 才跳过非表头；若写成 === 'TH' 会跳过所有列，导致永远无法联动。
             * 按表头 th 从左到右读取本分区 property 顺序，与 model 比较后整体写回（不依赖 oldIndex/newIndex）。
             */
            const zonePropSet = new Set(visibleInZone.map((c) => c.prop).filter(Boolean));
            const domPropsInZone = [];
            for (let i = 0; i < evt.from.children.length; i++) {
              const cell = evt.from.children[i];
              if (!cell || cell.tagName !== 'TH' || isGutterTh(cell)) continue;
              const p = this.getHeaderThDataProp(cell);
              if (p != null && zonePropSet.has(p)) {
                domPropsInZone.push(p);
              }
            }
            if (domPropsInZone.length !== dataLen) return;

            const modelProps = visibleInZone.map((c) => c.prop);
            const sameOrder =
              domPropsInZone.length === modelProps.length &&
              domPropsInZone.every((p, idx) => p === modelProps[idx]);
            if (sameOrder) return;

            const zoneKeySet = new Set(visibleInZone.map((c) => c.key));
            const byProp = new Map(visibleInZone.map((c) => [c.prop, c]));
            const reorderedCols = domPropsInZone.map((p) => byProp.get(p)).filter(Boolean);
            if (reorderedCols.length !== dataLen) return;

            const positions = [];
            this.tableFieldList.forEach((col, idx) => {
              if (
                col.visible &&
                (zone === 'fixed' ? !!col.fixed : !col.fixed) &&
                zoneKeySet.has(col.key)
              ) {
                positions.push(idx);
              }
            });
            if (positions.length !== dataLen) return;

            const newList = this.tableFieldList.slice();
            reorderedCols.forEach((col, j) => {
              newList[positions[j]] = col;
            });
            this.tableFieldList = newList;
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
      const t = this.$refs.elTable;
      if (!t) return;
      t.doLayout();
      this.$nextTick(() => {
        this.$nextTick(() => {
          requestAnimationFrame(() => {
            this.syncOperateColumnWidths();
            this.syncFixedDataRowHeights();
            this.updateCustomHorizontalScrollbar();
          });
        });
      });
    },
    /** 拦截 el-table.doLayout，防止布局重算后操作列被内容再次撑开 */
    patchTableDoLayout() {
      const t = this.$refs.elTable;
      if (!t || t.__cat2bugOperatePatch) return;
      const orig = t.doLayout;
      t.doLayout = (...args) => {
        orig.apply(t, args);
        if (this._lastOperateWidth != null) {
          this.applyOperateColumnDomWidth(this._lastOperateWidth);
        }
      };
      t.__cat2bugOperatePatch = true;
    },
    setupOperateColumnResizeGuard() {
      if (typeof ResizeObserver === 'undefined' || !this.$refs.elTable) return;
      this.teardownOperateColumnResizeGuard();
      const root = this.$refs.elTable.$el;
      if (!root) return;
      const cap = this.operateColumnMaxWidth;
      this._operateColRo = new ResizeObserver(() => {
        const fixedRight = root.querySelector('.el-table__fixed-right');
        if (!fixedRight) return;
        const w = fixedRight.getBoundingClientRect().width;
        if (w > cap + 0.5) {
          this.syncOperateColumnWidths();
        }
      });
      this._operateColRo.observe(root);
      const fixedRight = root.querySelector('.el-table__fixed-right');
      if (fixedRight) this._operateColRo.observe(fixedRight);
    },
    teardownOperateColumnResizeGuard() {
      if (this._operateColRo) {
        this._operateColRo.disconnect();
        this._operateColRo = null;
      }
    },
    resolveOperateColumnClassName(col) {
      if (!col) return '';
      return col.className
        || (col.columnConfig && col.columnConfig.className)
        || '';
    },
    isOperateColumn(col) {
      const className = this.resolveOperateColumnClassName(col);
      if (className.indexOf(OPERATE_COLUMN_CLASS) !== -1) {
        return true;
      }
      // append 操作列常无 prop，且固定在右侧
      return !col.property && col.fixed === 'right';
    },
    getOperateColumns() {
      const t = this.$refs.elTable;
      if (!t || !t.store || !t.store.states || !t.store.states.columns) return [];
      return t.store.states.columns.filter((col) => this.isOperateColumn(col));
    },
    applyOperateColumnDomWidth(widthPx) {
      const t = this.$refs.elTable;
      if (!t || !t.$el || widthPx == null) return;
      let parsed = Number(widthPx);
      if (!Number.isFinite(parsed) && typeof widthPx === 'string') {
        parsed = parseFloat(widthPx);
      }
      if (!Number.isFinite(parsed) || parsed <= 0) {
        parsed = this.operateColumnMinWidth;
      }
      const capPx = Math.min(
        this.operateColumnMaxWidth,
        Math.max(this.operateColumnMinWidth, Math.ceil(parsed))
      );
      const cap = `${capPx}px`;
      t.$el.querySelectorAll(`th.${OPERATE_COLUMN_CLASS}, td.${OPERATE_COLUMN_CLASS}`).forEach((el) => {
        el.style.setProperty('width', cap, 'important');
        el.style.setProperty('max-width', cap, 'important');
        el.style.setProperty('min-width', '0', 'important');
        if (el.tagName === 'TH') {
          el.style.setProperty('overflow', 'visible', 'important');
        } else {
          el.style.setProperty('overflow', 'hidden', 'important');
        }
      });
      const operateCols = this.getOperateColumns();
      operateCols.forEach((col) => {
        if (col && col.id) {
          t.$el.querySelectorAll(`col[name="${col.id}"]`).forEach((colEl) => {
            colEl.setAttribute('width', String(capPx));
            colEl.style.setProperty('width', cap, 'important');
          });
        }
      });
      const fixedRight = t.$el.querySelector('.el-table__fixed-right');
      if (fixedRight && operateCols.length) {
        fixedRight.style.setProperty('width', cap, 'important');
        fixedRight.style.setProperty('max-width', cap, 'important');
        fixedRight.querySelectorAll('table').forEach((tableEl) => {
          tableEl.style.setProperty('width', cap, 'important');
          tableEl.style.setProperty('max-width', cap, 'important');
        });
        fixedRight.querySelectorAll('colgroup col').forEach((colEl) => {
          colEl.setAttribute('width', String(capPx));
          colEl.style.setProperty('width', cap, 'important');
          colEl.style.setProperty('max-width', cap, 'important');
        });
      }
      const patch = t.$el.querySelector('.el-table__fixed-right-patch');
      if (patch) {
        const gutter = Math.ceil((t.layout && t.layout.gutterWidth) || 17);
        const gutterCap = `${gutter}px`;
        patch.style.setProperty('width', gutterCap, 'important');
        patch.style.setProperty('max-width', gutterCap, 'important');
        patch.style.setProperty('min-width', gutterCap, 'important');
      }
      t.$el.querySelectorAll('colgroup col').forEach((colEl) => {
        const name = colEl.getAttribute('name');
        if (!name) return;
        const matched = operateCols.some((col) => col && col.id === name);
        if (matched) {
          colEl.setAttribute('width', String(capPx));
          colEl.style.setProperty('width', cap, 'important');
          colEl.style.setProperty('max-width', cap, 'important');
        }
      });
      if (this.$el) {
        this.$el.style.setProperty('--cat2bug-operate-col-applied-width', cap);
      }
    },
    /** 测量表头文字固有宽度（勿用 .cell 的 getBoundingClientRect，避免已被撑开的列宽反馈放大） */
    measureOperateHeaderContentWidth(headerCell) {
      if (!headerCell) return this.operateColumnHeaderMin;
      const label = (headerCell.textContent || '').replace(/\s+/g, ' ').trim();
      if (!label || label.length > 32) return this.operateColumnHeaderMin;
      const probe = document.createElement('span');
      probe.textContent = label;
      probe.style.cssText = 'position:absolute;left:-9999px;top:-9999px;visibility:hidden;white-space:nowrap;padding:0;margin:0;border:0;';
      const cs = window.getComputedStyle(headerCell);
      probe.style.font = cs.font || `${cs.fontWeight} ${cs.fontSize} ${cs.fontFamily}`;
      document.body.appendChild(probe);
      const width = Math.ceil(probe.getBoundingClientRect().width);
      document.body.removeChild(probe);
      return Math.max(this.operateColumnHeaderMin, width);
    },
    isVisibleOperateNode(node) {
      if (!(node instanceof HTMLElement)) return false;
      const st = window.getComputedStyle(node);
      if (st.display === 'none' || st.visibility === 'hidden') return false;
      return node.offsetWidth > 0 || node.offsetHeight > 0 || node.getClientRects().length > 0;
    },
    resolveOperateMeasureNode(node) {
      if (!(node instanceof HTMLElement)) return null;
      const popover = node.classList.contains('el-popover')
        ? node
        : node.querySelector('.el-popover');
      if (popover) {
        return popover.querySelector('.el-popover__reference')
          || popover.querySelector('svg-icon.button')
          || popover.querySelector('svg-icon')
          || popover.querySelector('.button')
          || null;
      }
      if (node.querySelector('.el-dialog__wrapper, .shard-context, [role="tooltip"]')) {
        return null;
      }
      return node;
    },
    measureNodeNaturalWidth(node) {
      if (!node) return 0;
      if (!(node instanceof HTMLElement)) return 0;
      const target = this.resolveOperateMeasureNode(node) || node;
      const clone = target.cloneNode(true);
      clone.style.cssText = 'position:fixed;left:-9999px;top:-9999px;visibility:hidden;white-space:nowrap;pointer-events:none;margin:0;';
      document.body.appendChild(clone);
      const width = Math.ceil(clone.getBoundingClientRect().width || clone.scrollWidth || 0);
      document.body.removeChild(clone);
      if (width > 0) return width;
      const rect = target.getBoundingClientRect().width;
      const scroll = target.scrollWidth || 0;
      return Math.ceil(Math.max(rect, scroll));
    },
    measureOperateFlexRowNaturalWidth(root) {
      if (!root) return 0;
      const clone = root.cloneNode(true);
      clone.querySelectorAll(
        '.defect-tools__dialogs, .el-dialog__wrapper, [role="tooltip"][aria-hidden="true"]'
      ).forEach((el) => el.remove());
      const cs = window.getComputedStyle(root);
      clone.style.cssText = [
        'position:fixed',
        'left:-9999px',
        'top:0',
        'visibility:hidden',
        'pointer-events:none',
        'margin:0',
        'white-space:nowrap',
        'display:inline-flex',
        'flex-wrap:nowrap',
        'align-items:center',
        `column-gap:${cs.columnGap && cs.columnGap !== 'normal' ? cs.columnGap : '10px'}`,
        'width:auto',
        'max-width:none',
        'min-width:0',
      ].join(';');
      clone.querySelectorAll('*').forEach((node) => {
        if (!(node instanceof HTMLElement)) return;
        node.style.maxWidth = 'none';
        node.style.width = 'auto';
        node.style.minWidth = '0';
        node.style.flex = '0 0 auto';
      });
      document.body.appendChild(clone);
      const width = Math.ceil(clone.getBoundingClientRect().width || clone.scrollWidth || 0);
      document.body.removeChild(clone);
      return width;
    },
    measureOperateRowInnerWidth(container) {
      if (!container) return 0;
      const inner = container.classList.contains(OPERATE_TOOLS_CLASS)
        ? container
        : container.querySelector(`.${OPERATE_TOOLS_CLASS}`) || container;
      const toolsRoot = inner.querySelector('.defect-tools__bar')
        || inner.querySelector('.defect-tools')
        || inner;
      const gapCss = window.getComputedStyle(toolsRoot);
      const gap = gapCss.columnGap && gapCss.columnGap !== 'normal' ? parseFloat(gapCss.columnGap) || 10 : 10;

      let total = 0;
      const bar = inner.querySelector('.defect-tools__bar');
      if (bar) {
        total = this.measureOperateFlexRowNaturalWidth(bar);
      } else {
        total = this.measureOperateFlexRowNaturalWidth(toolsRoot);
      }

      const focus = inner.querySelector('.focus-member-list');
      if (focus && this.isVisibleOperateNode(focus)) {
        if (total > 0) total += gap;
        total += this.measureNodeNaturalWidth(focus);
      }

      if (total > 0) return Math.ceil(total);

      const visible = this.collectOperateMeasureNodes(container);
      if (!visible.length) return 0;
      total = 0;
      visible.forEach((node, index) => {
        if (index > 0) total += gap;
        total += this.measureNodeNaturalWidth(node);
      });
      return Math.ceil(total);
    },
    /** 布局后检测操作列是否发生裁切或单行超宽，必要时强制换行 */
    enforceOperateColumnWrapIfOverflow() {
      const t = this.$refs.elTable;
      if (!t || !t.$el || !this.$el) return false;

      const cap = this.operateColumnMaxWidth;
      const fixedCells = t.$el.querySelectorAll(`.el-table__fixed-right td.${OPERATE_COLUMN_CLASS} .cell`);
      const cells = fixedCells.length
        ? fixedCells
        : t.$el.querySelectorAll(`td.${OPERATE_COLUMN_CLASS}:not(.is-hidden) .cell`);
      let overflow = false;
      let needsCap = false;
      cells.forEach((cell) => {
        const inner = cell.querySelector(`.${OPERATE_TOOLS_CLASS}`) || cell;
        if (inner.scrollWidth > cell.clientWidth + 1) {
          overflow = true;
        }
        const natural = this.measureOperateRowInnerWidth(cell);
        if (Math.ceil(natural + this.operateColumnPaddingX * 2) > cap) {
          needsCap = true;
        }
      });

      if ((!overflow && !needsCap) || this.$el.getAttribute('data-operate-at-cap') === 'true') {
        return false;
      }

      this.$el.setAttribute('data-operate-at-cap', 'true');
      const operateCols = this.getOperateColumns();
      operateCols.forEach((col) => {
        col.width = cap;
        col.minWidth = this.operateColumnMinWidth;
        col.realWidth = cap;
      });
      this._lastOperateWidth = capPx;
      this.applyOperateColumnDomWidth(capPx);
      this.syncFixedDataRowHeights();
      return true;
    },
    /** 累加容器内可见子节点宽度（勿用容器 scrollWidth，会被单元格撑满） */
    measureOperateContentWidth(container) {
      const innerMax = Math.max(0, this.operateColumnMaxWidth - this.operateColumnPaddingX * 2);
      return Math.min(innerMax, this.measureOperateRowInnerWidth(container));
    },
    /** 收集操作列内用于测宽的可见节点（排除 DefectTools 内挂载的对话框等不可见大块节点） */
    collectOperateMeasureNodes(container) {
      if (!container) return [];
      const inner = container.classList.contains(OPERATE_TOOLS_CLASS)
        ? container
        : container.querySelector(`.${OPERATE_TOOLS_CLASS}`) || container;
      const toolSelector = '.el-button, .el-dropdown, .el-link, .share-card, .star-switch, .start-switch, .focus-member-list, .el-popover';
      const defectTools = inner.querySelector('.defect-tools__bar') || inner.querySelector('.defect-tools');
      if (defectTools) {
        const directNodes = [];
        Array.from(defectTools.children).forEach((node) => {
          if (!(node instanceof HTMLElement)) return;
          if (node.classList.contains('el-dialog__wrapper')) return;
          const measureNode = this.resolveOperateMeasureNode(node);
          if (measureNode && this.isVisibleOperateNode(measureNode)) {
            directNodes.push(measureNode);
          }
        });
        if (directNodes.length) return directNodes;

        const toolNodes = Array.from(defectTools.querySelectorAll(toolSelector)).filter((node) =>
          this.isVisibleOperateNode(node)
        );
        if (toolNodes.length) return toolNodes;
      }

      const innerTools = Array.from(inner.querySelectorAll(toolSelector)).filter((node) =>
        this.isVisibleOperateNode(node)
      );
      if (innerTools.length) return innerTools;

      const nodes = [];
      Array.from(inner.children).forEach((node) => {
        if (this.isVisibleOperateNode(node)) nodes.push(node);
      });
      if (nodes.length) return nodes;

      return Array.from(container.querySelectorAll(toolSelector)).filter((node) =>
        this.isVisibleOperateNode(node)
      );
    },
    /** 按当前页各行操作内容同步 append 操作列宽度（列宽全表统一，不超过 operateColumnMaxWidth） */
    syncOperateColumnWidths() {
      const t = this.$refs.elTable;
      if (!t || !t.$el) return;

      const cap = this.operateColumnMaxWidth;
      if (!this.operateColumnAutoWidth) {
        return;
      }

      const operateCols = this.getOperateColumns();
      if (!operateCols.length) return;

      const fixedCells = t.$el.querySelectorAll(`.el-table__fixed-right td.${OPERATE_COLUMN_CLASS} .cell`);
      const cells = fixedCells.length
        ? fixedCells
        : t.$el.querySelectorAll(`td.${OPERATE_COLUMN_CLASS}:not(.is-hidden) .cell`);
      const innerMax = Math.max(0, cap - this.operateColumnPaddingX * 2);
      let maxSingleRowInner = 0;
      cells.forEach((cell) => {
        maxSingleRowInner = Math.max(maxSingleRowInner, this.measureOperateRowInnerWidth(cell));
        const tools = cell.querySelector(`.${OPERATE_TOOLS_CLASS}`);
        if (tools) {
          const liveW = Math.ceil(
            Math.max(tools.scrollWidth || 0, tools.getBoundingClientRect().width || 0)
          );
          maxSingleRowInner = Math.max(maxSingleRowInner, liveW);
        }
      });

      const operateHeader = t.$el.querySelector(
        `.el-table__fixed-right th.${OPERATE_COLUMN_CLASS} .cell, th.${OPERATE_COLUMN_CLASS}:not(.is-hidden) .cell`
      );
      const headerContentW = operateHeader
        ? this.measureOperateHeaderContentWidth(operateHeader)
        : this.operateColumnHeaderMin;

      const contentWithPad = Math.ceil(
        Math.max(maxSingleRowInner, headerContentW) + this.operateColumnPaddingX * 2
      );
      const atCap = contentWithPad > cap;
      if (this.$el) {
        this.$el.setAttribute('data-operate-at-cap', atCap ? 'true' : 'false');
      }

      const next = atCap
        ? cap
        : Math.max(this.operateColumnMinWidth, Math.min(cap, contentWithPad));

      const floor = this.operateColumnMinWidth;
      operateCols.forEach((col) => {
        col.width = next;
        col.minWidth = floor;
        col.realWidth = next;
      });

      this._lastOperateWidth = next;
      this.applyOperateColumnDomWidth(next);
      this.$nextTick(() => {
        requestAnimationFrame(() => {
          this.applyOperateColumnDomWidth(next);
          if (this.enforceOperateColumnWrapIfOverflow()) {
            this.updateCustomHorizontalScrollbar();
            return;
          }
          this.syncFixedDataRowHeights();
          this.updateCustomHorizontalScrollbar();
        });
      });
    },
    handleClickColumnsPin(column) {
      column.fixed = !column.fixed;
      this.reorderColumns();
      this.tableKey = Date.now();
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
        if (!this.$refs.elTable) return;
        this.$refs.elTable.doLayout();
        this.initColumnDrag();
        this.$nextTick(() => {
          requestAnimationFrame(() => {
            this.syncOperateColumnWidths();
            this.syncFixedDataRowHeights();
            this.updateCustomHorizontalScrollbar();
          });
        });
      });
    }
  }
}
</script>

<style lang="scss" scoped>
.cat2-bug-table-wrap {
  position: relative;
  --cat2bug-operate-col-applied-width: 450px;

  /* 覆盖 Element UI 默认 .el-table { background: #FFF } / .el-table tr { background: #FFF } */
  ::v-deep .el-table {
    background-color: var(--table-bg) !important;
  }

  ::v-deep .el-table__fixed-right {
    max-width: var(--cat2bug-operate-col-max-width, 450px) !important;
    width: var(--cat2bug-operate-col-applied-width, 450px) !important;
    box-sizing: border-box;
    z-index: 31 !important;
  }

  ::v-deep .el-table__fixed-right table {
    table-layout: fixed;
    width: 100%;
    box-sizing: border-box;
  }

  /* 滚动条垫片：仅 gutter 宽，禁止与操作列同宽（否则会盖住「操作」表头） */
  ::v-deep .el-table__fixed-right-patch {
    width: auto !important;
    max-width: 20px !important;
    min-width: 0 !important;
    z-index: 29 !important;
    box-sizing: border-box;
    pointer-events: none;
  }

  ::v-deep th.cat2bug-operate-column,
  ::v-deep td.cat2bug-operate-column {
    max-width: var(--cat2bug-operate-col-max-width, 450px) !important;
    width: var(--cat2bug-operate-col-applied-width, 450px) !important;
    min-width: 0 !important;
    box-sizing: border-box;
  }

  ::v-deep th.cat2bug-operate-column .cell {
    overflow: visible !important;
    color: var(--table-header-color, inherit);
  }

  ::v-deep .el-table__body-wrapper,
  ::v-deep .el-table__fixed-body-wrapper {
    background-color: var(--cat2bug-table-row-bg, var(--table-bg));
  }

  /* 表头 tr/th：覆盖 Element UI .el-table tr / th { background:#FFF } */
  ::v-deep thead tr {
    background-color: var(--table-header-bg) !important;
  }

  ::v-deep thead tr > th.el-table__cell,
  ::v-deep .el-table__fixed thead tr > th.el-table__cell,
  ::v-deep .el-table__fixed-right thead tr > th.el-table__cell {
    background-color: var(--table-header-bg) !important;
    border-bottom: 1px solid var(--border-color-light) !important;
    color: var(--table-header-color, inherit) !important;
  }

  /* 表体：非固定列与固定列分层（参考浅色：主体 table-bg，固定列 block 色） */
  ::v-deep tbody tr {
    background-color: var(--cat2bug-table-row-bg, var(--table-bg)) !important;
  }

  ::v-deep .el-table__body-wrapper tbody tr > td.el-table__cell {
    background-color: var(--cat2bug-table-row-bg, var(--table-bg)) !important;
    border-bottom: 1px solid var(--border-color-light) !important;
  }

  ::v-deep .el-table__fixed tbody tr > td.el-table__cell,
  ::v-deep .el-table__fixed-right tbody tr > td.el-table__cell {
    background-color: var(--cat2bug-table-fixed-row-bg, var(--cat2bug-table-row-bg, var(--table-bg))) !important;
    border-bottom: 1px solid var(--border-color-light) !important;
  }

  ::v-deep .el-table__body-wrapper tbody tr:hover > td.el-table__cell,
  ::v-deep .el-table__body-wrapper tbody tr.hover-row > td.el-table__cell {
    background-color: var(--table-row-hover-bg) !important;
  }

  ::v-deep .el-table__fixed tbody tr:hover > td.el-table__cell,
  ::v-deep .el-table__fixed tbody tr.hover-row > td.el-table__cell,
  ::v-deep .el-table__fixed-right tbody tr:hover > td.el-table__cell,
  ::v-deep .el-table__fixed-right tbody tr.hover-row > td.el-table__cell {
    background-color: var(--table-row-hover-bg) !important;
  }

  ::v-deep tbody tr:hover,
  ::v-deep tbody tr.hover-row {
    background-color: var(--table-row-hover-bg) !important;
  }
}

.cat2bug-custom-xbar {
  position: absolute;
  z-index: 45;
  box-sizing: border-box;
  background: var(--cat2bug-xbar-track);
  border-radius: 3px;
  overflow: hidden;
  padding: 0;
}

.cat2bug-custom-xbar__thumb {
  position: absolute;
  top: 0;
  left: 0;
  box-sizing: border-box;
  cursor: pointer;
  background: var(--cat2bug-xbar-thumb);
  border-radius: 3px;
  border: none;
}

.cat2bug-custom-xbar__thumb:hover {
  background: var(--cat2bug-xbar-thumb-hover);
}

.header-title-only {
  display: inline-block;
}

.header-title--required {
  color: #f56c6c;
}

/* 必填列：固定/取消固定图标与标题同色（svg-icon 使用 currentColor） */
.table-header--required {
  color: #f56c6c;
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
  min-width: 0;
  flex: 1 1 auto;

  ::v-deep .svg-icon {
    cursor: pointer;
    flex-shrink: 0;
  }
}

.header-title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 自定义表头 slot 后仍保留 Element 排序箭头，避免被挤没或点不到 */
::v-deep th.is-sortable .cell {
  display: inline-flex !important;
  align-items: center;
  vertical-align: middle;
  max-width: 100%;
}

::v-deep th.is-sortable .caret-wrapper {
  flex-shrink: 0;
  margin-left: 2px;
}

::v-deep thead .no-drag {
  background-color: var(--table-header-bg);
}

::v-deep tbody > .el-table__row > .el-table__cell {
  padding: 5px 0;
}

/* 行高由 syncFixedDataRowHeights 按内容逐行同步三表 tr；单元格内容垂直居中 */
::v-deep .el-table__body-wrapper .el-table__body td.el-table__cell,
::v-deep .el-table__fixed .el-table__fixed-body-wrapper td.el-table__cell,
::v-deep .el-table__fixed-right .el-table__fixed-body-wrapper td.el-table__cell {
  vertical-align: middle;
}

/* append 操作列：class-name 含 cat2bug-operate-column，内容包在 cat2bug-operate-tools 内 */
::v-deep th.cat2bug-operate-column,
::v-deep td.cat2bug-operate-column {
  width: var(--cat2bug-operate-col-applied-width, auto);
  max-width: var(--cat2bug-operate-col-max-width, 450px);
  box-sizing: border-box;
}

::v-deep th.cat2bug-operate-column {
  overflow: visible;
}

::v-deep td.cat2bug-operate-column {
  overflow: hidden;
}

::v-deep th.cat2bug-operate-column .cell {
  overflow: visible;
  text-align: left;
  padding-left: var(--cat2bug-operate-col-padding-x, 20px);
  padding-right: var(--cat2bug-operate-col-padding-x, 20px);
  max-width: 100%;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  min-height: 0;
  line-height: normal;
  white-space: nowrap;
  text-overflow: clip;
  color: var(--table-header-color, inherit);
  position: relative;
  z-index: 1;
}

::v-deep td.cat2bug-operate-column .cell {
  overflow: hidden;
  text-align: left;
  padding-left: var(--cat2bug-operate-col-padding-x, 20px);
  padding-right: var(--cat2bug-operate-col-padding-x, 20px);
  max-width: 100%;
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  flex-wrap: nowrap;
  white-space: nowrap;
  width: 100%;
  position: relative;
  z-index: 1;
}

.cat2-bug-table-wrap[data-operate-at-cap='true'] ::v-deep td.cat2bug-operate-column .cell {
  flex-wrap: wrap;
  align-items: flex-start;
  align-content: flex-start;
  white-space: normal;
  overflow: visible !important;
}

::v-deep td.cat2bug-operate-column .cat2bug-operate-tools,
::v-deep td.cat2bug-operate-column .defect-operate-cell {
  max-width: 100%;
  min-width: 0;
  width: auto;
  flex-wrap: nowrap;
  white-space: nowrap;
  box-sizing: border-box;
}

.cat2-bug-table-wrap[data-operate-at-cap='true'] ::v-deep td.cat2bug-operate-column .cat2bug-operate-tools,
.cat2-bug-table-wrap[data-operate-at-cap='true'] ::v-deep td.cat2bug-operate-column .defect-operate-cell {
  width: 100%;
  flex-wrap: wrap;
  white-space: normal;
}

::v-deep .cat2bug-operate-tools {
  column-gap: var(--cat2bug-operate-tools-gap, 10px);
  row-gap: var(--cat2bug-operate-tools-row-gap, 0);
}
</style>

<!-- 非 scoped：覆盖 Element UI 默认白底，明暗主题均由 CSS 变量驱动 -->
<style lang="scss">
.cat2-bug-table-wrap .el-table thead tr,
.cat2-bug-table-wrap .el-table thead tr > th.el-table__cell,
.cat2-bug-table-wrap .el-table__fixed thead tr > th.el-table__cell,
.cat2-bug-table-wrap .el-table__fixed-right thead tr > th.el-table__cell,
.cat2-bug-table-wrap .el-table__header-wrapper thead th.gutter,
.cat2-bug-table-wrap .el-table__header-wrapper thead th.el-table__gutter,
.cat2-bug-table-wrap .el-table__fixed-header-wrapper thead th.gutter,
.cat2-bug-table-wrap .el-table__fixed-header-wrapper thead th.el-table__gutter,
.cat2-bug-table-wrap .el-table__fixed-right-patch {
  background-color: var(--table-header-bg) !important;
  border-bottom: 1px solid var(--border-color-light) !important;
}

.cat2-bug-table-wrap .el-table__body-wrapper tbody tr > td.el-table__cell {
  background-color: var(--cat2bug-table-row-bg, var(--table-bg)) !important;
  border-bottom: 1px solid var(--border-color-light) !important;
}

.cat2-bug-table-wrap .el-table__fixed tbody tr > td.el-table__cell,
.cat2-bug-table-wrap .el-table__fixed-right tbody tr > td.el-table__cell {
  background-color: var(--cat2bug-table-fixed-row-bg, var(--cat2bug-table-row-bg, var(--table-bg))) !important;
  border-bottom: 1px solid var(--border-color-light) !important;
}

.cat2-bug-table-wrap .el-table__body-wrapper tbody tr:hover > td.el-table__cell,
.cat2-bug-table-wrap .el-table__body-wrapper tbody tr.hover-row > td.el-table__cell,
.cat2-bug-table-wrap .el-table__fixed tbody tr:hover > td.el-table__cell,
.cat2-bug-table-wrap .el-table__fixed tbody tr.hover-row > td.el-table__cell,
.cat2-bug-table-wrap .el-table__fixed-right tbody tr:hover > td.el-table__cell,
.cat2-bug-table-wrap .el-table__fixed-right tbody tr.hover-row > td.el-table__cell {
  background-color: var(--table-row-hover-bg) !important;
}
</style>
