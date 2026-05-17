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
const DEFAULT_OPERATE_COLUMN_MAX_WIDTH = 200;
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
    this.$nextTick(() => {
      this.setupCustomHorizontalScrollbar();
      this.scheduleDoLayout();
    });
  },
  beforeDestroy() {
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
      const mainBw = tableRoot.querySelector(".el-table__body-wrapper");
      if (mainBw) {
        mainBw.querySelectorAll(sel).forEach((tr) => {
          tr.style.height = "";
        });
      }
      const fixedL = tableRoot.querySelector(".el-table__fixed");
      const fixedR = tableRoot.querySelector(".el-table__fixed-right");
      [fixedL, fixedR].forEach((fx) => {
        if (!fx) return;
        const fbw = fx.querySelector(".el-table__fixed-body-wrapper");
        if (fbw) {
          fbw.querySelectorAll(sel).forEach((tr) => {
            tr.style.height = "";
          });
        }
      });
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
      void root.offsetHeight;

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
        let h = mainRows[i].offsetHeight;
        if (leftRows && leftRows[i]) h = Math.max(h, leftRows[i].offsetHeight);
        if (rightRows && rightRows[i]) h = Math.max(h, rightRows[i].offsetHeight);
        h = Math.ceil(h);
        mainRows[i].style.height = h + "px";
        if (leftRows && leftRows[i]) leftRows[i].style.height = h + "px";
        if (rightRows && rightRows[i]) rightRows[i].style.height = h + "px";
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
      this.sort(this.orderByColumn, this.isAsc);
    },
    sortChangeHandle(e) {
      if (!this.enableHeaderSort) return;
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
          filter: 'th.gutter, .el-table__gutter',
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
    isOperateColumn(col) {
      return col && col.className && col.className.indexOf(OPERATE_COLUMN_CLASS) !== -1;
    },
    getOperateColumns() {
      const t = this.$refs.elTable;
      if (!t || !t.store || !t.store.states || !t.store.states.columns) return [];
      return t.store.states.columns.filter((col) => this.isOperateColumn(col));
    },
    /** 累加容器内可见子节点宽度（勿用容器 scrollWidth，会被单元格撑满） */
    measureOperateContentWidth(container) {
      if (!container) return 0;
      const inner = container.classList.contains(OPERATE_TOOLS_CLASS)
        ? container
        : container.querySelector(`.${OPERATE_TOOLS_CLASS}`) || container;
      const gap = 10;
      const visible = Array.from(inner.children).filter((node) => {
        if (!(node instanceof HTMLElement)) return false;
        if (node.offsetWidth <= 0) return false;
        const st = window.getComputedStyle(node);
        return st.display !== "none" && st.visibility !== "hidden";
      });
      if (!visible.length) return 0;
      let total = 0;
      visible.forEach((node, index) => {
        if (index > 0) total += gap;
        total += node.getBoundingClientRect().width;
      });
      return Math.ceil(total);
    },
    /** 按当前页各行操作内容同步 append 操作列宽度（列宽全表统一） */
    syncOperateColumnWidths() {
      if (!this.operateColumnAutoWidth) return;
      const t = this.$refs.elTable;
      if (!t || !t.$el) return;

      const operateCols = this.getOperateColumns();
      if (!operateCols.length) return;

      const cells = t.$el.querySelectorAll(`td.${OPERATE_COLUMN_CLASS} .cell`);
      let maxContent = 0;
      cells.forEach((cell) => {
        maxContent = Math.max(maxContent, this.measureOperateContentWidth(cell));
      });

      const next = Math.min(
        this.operateColumnMaxWidth,
        Math.max(
          this.operateColumnHeaderMin,
          this.operateColumnMinWidth,
          Math.ceil(maxContent + this.operateColumnPaddingX * 2)
        )
      );

      let changed = false;
      operateCols.forEach((col) => {
        if (col.width !== next || col.minWidth !== next) {
          col.width = next;
          col.minWidth = next;
          changed = true;
        }
      });
      if (!changed) return;

      t.doLayout();
      this.$nextTick(() => {
        requestAnimationFrame(() => {
          this.syncFixedDataRowHeights();
          this.updateCustomHorizontalScrollbar();
        });
      });
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
}

.cat2bug-custom-xbar {
  position: absolute;
  z-index: 45;
  box-sizing: border-box;
  background: #f2f3f5;
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
  background: #e3e6eb;
  border-radius: 3px;
  border: none;
}

.cat2bug-custom-xbar__thumb:hover {
  background: #d9dde4;
}

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

/* 行高由 syncFixedDataRowHeights 统一三表 tr 高度；单元格内容垂直居中 */
::v-deep .el-table__body-wrapper .el-table__body td.el-table__cell,
::v-deep .el-table__fixed .el-table__fixed-body-wrapper td.el-table__cell,
::v-deep .el-table__fixed-right .el-table__fixed-body-wrapper td.el-table__cell {
  vertical-align: middle;
}

/* append 操作列：class-name 含 cat2bug-operate-column，内容包在 cat2bug-operate-tools 内 */
::v-deep th.cat2bug-operate-column .cell,
::v-deep td.cat2bug-operate-column .cell {
  overflow: visible;
  text-align: left;
  padding-left: var(--cat2bug-operate-col-padding-x, 20px);
  padding-right: var(--cat2bug-operate-col-padding-x, 20px);
  max-width: var(--cat2bug-operate-col-max-width, 200px);
}

::v-deep .cat2bug-operate-tools {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  row-gap: 4px;
  width: 100%;
  max-width: 100%;
  >* {
    margin: 0;
  }
}
</style>
