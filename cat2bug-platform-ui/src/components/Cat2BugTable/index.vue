<template>
  <el-table ref="defectTable"
            :key="tableKey"
            v-loading="loading"
            :data="data"
            @selection-change="handleSelectionChange"
            @sort-change="sortChangeHandle"
            @row-click="handleClickTableRow">
    <el-table-column v-for="(col, colIndex) in showTableFieldList"
                     :label="$t(col.key)"
                     :key="$t(col.key)+colIndex"
                     :min-width="columnWidth(col)"
                     :prop="col.prop"
                     :class-name="col.fixed ? 'no-drag' : ''"
                     :fixed="col.fixed ? 'left' : false"
                     :align="col.align || 'left'"
                     :sort-by="col.prop"
                     :sort-orders="col.prop===orderByColumn?[isAsc]:[null]"
                     sortable="custom"  >
      <template v-slot:header>
        <div class="table-header">
          <svg-icon :icon-class="col.fixed ? 'header-right' : 'header-left'" @click.stop="handleClickColumnsPin(col)"></svg-icon>
          <span class="header-title">{{ $t(col.key) }}</span>
        </div>
      </template>
      <!-- 数据行 -->
      <template slot-scope="scope">
        <slot name="columns" :scope="scope" :column="col"></slot>
      </template>
    </el-table-column>
    <slot></slot>
  </el-table>
</template>

<script>
import Sortable from "sortablejs";

/** 需要显示的字段列表在缓存的key值 */
const TABLE_FIELD_LIST_CACHE_KEY='defect-table-field-list';
/** 表排序的列key值 */
const TABLE_SORT_COLUMN = 'defect_table_sort_column_key';
/** 表排序的类型（正序、倒叙）key值 */
const TABLE_SORT_TYPE = 'defect_table_sort_type_key';
/** 默认列宽 */
const DEFAULT_COLUMN_WIDTH = 180;

export default {
  name: "index",
  data() {
    return {
      loading: false,
      tableFieldList: [],
      tableKey: (new Date()).getMilliseconds(),
      headerSortable: {},
      orderByColumn: null,
      isAsc: null
    }
  },
  props: {
    /** 缓存Key */
    cacheKey: {
      type: String,
      default: null
    },
    /** 表列集合 */
    columns: {
      type: Array,
      default: []
    },
    /** 表数据集合 */
    data: {
      type: Array,
      default: []
    }
  },
  computed: {
    /** 显示的列集合 */
    showTableFieldList: function () {
      return this.tableFieldList.map((col,index)=>{
        col.index = index;
        return col;
      }).filter(col=>col.visible);
    },
    /** 列宽度 */
    columnWidth: function () {
      return function (column) {
        return column['width_'+this.$i18n.locale] || column.width || DEFAULT_COLUMN_WIDTH;
      }
    }
  },
  watch: {
    "$i18n.locale": function (newVal, oldVal) {
      this.$nextTick(() => {
        this.$refs.defectTable.doLayout();
      });
    },
  },
  created() {
    this.setColumns(this.getShowColumns() || this.columns);
  },
  mounted() {
    this.initSort();
    this.initColumnDrag();
  },
  methods: {
    /** 初始化排序数据 */
    initSort() {
      this.isAsc = this.$cache.local.get(this.cacheKey+TABLE_SORT_TYPE)||null;
      this.orderByColumn = this.$cache.local.get(this.cacheKey+TABLE_SORT_COLUMN)||null;
      this.sort(this.orderByColumn, this.isAsc);
    },
    /** 排序改变的处理 */
    sortChangeHandle(e) {
      this.isAsc = e.order;
      this.orderByColumn = e.prop;
      this.$cache.local.set(this.cacheKey+TABLE_SORT_COLUMN, e.prop);
      this.$cache.local.set(this.cacheKey+TABLE_SORT_TYPE, e.order);
      this.$emit('sort-change', e);
    },
    /** 保存表格显示哪些属性 */
    saveShowColumns(columns) {
      this.$cache.local.setJSON(this.cacheKey+TABLE_FIELD_LIST_CACHE_KEY, columns);
    },
    /** 获取表格显示哪些属性 */
    getShowColumns() {
      return this.$cache.local.getJSON(this.cacheKey+TABLE_FIELD_LIST_CACHE_KEY);
    },
    /** 设置列显示属性 */
    setColumnsVisible(showColumns) {
      this.setColumns(this.tableFieldList.map(col=>{
        col.visible = showColumns.filter(key=>key===col.key).length>0;
        return col;
      }));
      this.reorderColumns();
    },
    /** 设置列 */
    setColumns(columns) {
      this.tableFieldList = columns;
      this.$emit('columns-change', columns);
      this.saveShowColumns(columns);
    },
    /** 出事化列拖动 */
    initColumnDrag() {
      this.bindSortable('.el-table__header-wrapper thead tr', 'normal');
      this.bindSortable('.el-table__fixed-header-wrapper thead tr', 'fixed');
    },
    /** 绑定拖动功能 */
    bindSortable(selector, zone) {
      this.$nextTick(()=>{
        const el = this.$refs.defectTable.$el.querySelector(selector);
        if (!el) return;
        // 防止重复绑定
        if (this.headerSortable[zone]) {
          this.headerSortable[zone].destroy();
        }

        this.headerSortable[zone] = Sortable.create(el, {
          ghostClass: 'table-header-ghost',
          // filter: '.no-drag',
          group: zone,
          onStart: () => {
            this.mouseFlag = false;
          },
          // onMove: evt => !evt.related.classList.contains('no-drag'),
          onEnd: evt => {
            const oldField = this.showTableFieldList[evt.oldIndex];
            const newField = this.showTableFieldList[evt.newIndex];
            this.mouseFlag = false;
            const moved = this.tableFieldList.splice(oldField.index, 1)[0];
            this.tableFieldList.splice(newField.index, 0, moved);
            this.reorderColumns();
          }
        });
      });
    },
    /** 表格排序 */
    sort(prop, order) {
      this.$refs.defectTable.sort(prop, order);
    },
    /** 刷新表格 */
    doLayout() {
      this.$refs.defectTable.doLayout();
    },
    /** 处理点击表格列中的图钉按钮 */
    handleClickColumnsPin(column) {
      column.fixed = !column.fixed;
      this.saveShowColumns(this.tableFieldList);
      this.tableKey = (new Date()).getMilliseconds();
      this.initColumnDrag();
    },
    /** 多选框选中数据 */
    handleSelectionChange(selection) {
      this.$emit('selection-change', selection);
    },
    /** 处理点击了表格中的某一行 */
    handleClickTableRow(row) {
      this.$emit('row-click', row);
    },
    /** 处理鼠标在表格点下事件 */
    handleTableMouseDown(e) {
      this.mouseOffset = e.clientX;
      this.mouseFlag = true;
    },
    /** 处理鼠标在表格点起事件 */
    handleTableMouseUp(e) {
      this.mouseFlag = false;
    },
    /** 处理鼠标在表格移动事件 */
    handleTableMouseMove(e) {
      // 这里面需要注意，通过ref需要那个那个包含table元素的父元素
      let tableBody = this.$refs.defectTable.bodyWrapper;
      if (this.mouseFlag) {
        // 设置水平方向的元素的位置
        tableBody.scrollLeft -= (- this.mouseOffset + (this.mouseOffset = e.clientX));
      }
    },
    /** 重新加载列 */
    reorderColumns() {
      // 重排 tableFieldList
      const fixedCols = this.tableFieldList.filter(c => c.fixed);
      const normalCols = this.tableFieldList.filter(c => !c.fixed);
      this.setColumns([...fixedCols, ...normalCols]);

      // 同步 el-table 内部 columns
      const storeCols = this.$refs.defectTable.store.states.columns;
      const dataCols = storeCols.filter(c => c.property); // 排除 selection / 操作列
      const fixedStore = dataCols.filter(c => c.fixed === 'left');
      const normalStore = dataCols.filter(c => !c.fixed);

      this.$refs.defectTable.store.states.columns = [
        ...fixedStore,
        ...normalStore,
        ...storeCols.filter(c => !c.property) // 操作列
      ];
      this.$nextTick(() => {
        this.$refs.defectTable.doLayout();
      });
    }
  }
}
</script>

<style scoped>
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
