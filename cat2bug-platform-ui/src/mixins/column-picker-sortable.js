/**
 * 「显示字段」popover 内拖排序（与缺陷列表同源交互）。
 * 页面需：
 * - checkbox-group 内 `.defect-column-picker-sort-root` + `.defect-column-picker-row[data-field-key]` + `.defect-column-picker-drag`
 * - 实现 `getColumnPickerSortableContext()` 返回：
 *   { tableRef, liveListKey, checkedKeysKey, visibleKey, fallbackColumns }
 * - @show 末尾调 `bindColumnPickerSortableOnShow()`，@hide 调 `destroyColumnPickerSortable()`
 * - beforeUnmount / destroyed 调 `destroyColumnPickerSortable()`
 */
import Sortable from 'sortablejs'
import { getVisibleColumnPickerPopperEl } from '@/utils/column-picker-popover-kbd'

function checkedKeysFromColumns(columns) {
  return (columns || [])
    .filter((c) => c && c.visible !== false && c.showInColumnPicker !== false)
    .map((c) => c.key)
}

export default {
  data() {
    return {
      _columnPickerSortable: null,
      _columnPickerSortableRetry: 0
    }
  },
  destroyed() {
    this.destroyColumnPickerSortable()
  },
  beforeUnmount() {
    this.destroyColumnPickerSortable()
  },
  methods: {
    /**
     * @returns {{
     *   tableRef?: string,
     *   liveListKey: string,
     *   checkedKeysKey?: string,
     *   visibleKey?: string,
     *   fallbackColumns: Array
     * }}
     */
    getColumnPickerSortableContext() {
      return {
        tableRef: 'cat2BugTable',
        liveListKey: '',
        checkedKeysKey: 'columnPickerCheckedKeys',
        visibleKey: '',
        fallbackColumns: []
      }
    },
    isColumnPickerSortableOpen() {
      const ctx = this.getColumnPickerSortableContext() || {}
      if (ctx.visibleKey) return !!this[ctx.visibleKey]
      return true
    },
    destroyColumnPickerSortable() {
      if (this._columnPickerSortable) {
        try {
          this._columnPickerSortable.destroy()
        } catch (_e) {
          /* ignore */
        }
        this._columnPickerSortable = null
      }
    },
    resolveColumnPickerSortRoot() {
      const refRoot = this.$refs.columnPickerSortRoot
      const fromRef = Array.isArray(refRoot) ? refRoot[0] : refRoot
      if (fromRef && fromRef.querySelector && fromRef.querySelector('.defect-column-picker-row')) {
        return fromRef
      }
      const popper = getVisibleColumnPickerPopperEl()
      if (popper) {
        const inPopper = popper.querySelector('.defect-column-picker-sort-root')
        if (inPopper && inPopper.querySelector('.defect-column-picker-row')) return inPopper
        const fallback = popper.querySelector('.defect-column-picker')
        if (fallback && fallback.querySelector('.defect-column-picker-row')) return fallback
      }
      return (
        document.querySelector('.defect-column-picker-popover .defect-column-picker-sort-root') ||
        document.querySelector('.defect-column-picker-popover .defect-column-picker')
      )
    },
    bindColumnPickerSortableOnShow() {
      this._columnPickerSortableRetry = 0
      this._columnPickerSortableForceBind = true
      this.$nextTick(() => {
        requestAnimationFrame(() => this.bindColumnPickerSortable())
      })
    },
    bindColumnPickerSortable() {
      if (!this._columnPickerSortableForceBind && !this.isColumnPickerSortableOpen()) return
      this.destroyColumnPickerSortable()
      const root = this.resolveColumnPickerSortRoot()
      if (!root || !root.querySelector('.defect-column-picker-row')) {
        if ((this._columnPickerSortableRetry || 0) < 12) {
          this._columnPickerSortableRetry = (this._columnPickerSortableRetry || 0) + 1
          setTimeout(() => this.bindColumnPickerSortable(), 40)
        } else {
          this._columnPickerSortableForceBind = false
        }
        return
      }
      this._columnPickerSortableForceBind = false
      this._columnPickerSortableRetry = 0
      this._columnPickerSortable = Sortable.create(root, {
        animation: 150,
        handle: '.defect-column-picker-drag',
        draggable: '.defect-column-picker-row',
        filter: '.el-checkbox, .el-checkbox__input, .el-checkbox__inner, .el-checkbox__original',
        preventOnFilter: false,
        ghostClass: 'defect-column-picker-row--ghost',
        chosenClass: 'defect-column-picker-row--chosen',
        forceFallback: true,
        fallbackOnBody: true,
        fallbackTolerance: 3,
        onEnd: () => {
          const keys = Array.from(root.querySelectorAll('.defect-column-picker-row'))
            .map((n) => n.getAttribute('data-field-key'))
            .filter(Boolean)
          this.applyColumnPickerOrder(keys)
        }
      })
    },
    applyColumnPickerOrder(orderedKeys) {
      const ctx = this.getColumnPickerSortableContext() || {}
      const tableRef = ctx.tableRef || 'cat2BugTable'
      const liveListKey = ctx.liveListKey
      const checkedKeysKey = ctx.checkedKeysKey || 'columnPickerCheckedKeys'
      const table = this.$refs[tableRef]
      if (!table || typeof table.setColumns !== 'function' || !liveListKey) return
      const live = this[liveListKey]
      const current = (
        live && live.length ? live : table.tableFieldList || ctx.fallbackColumns || []
      )
        .filter((c) => c && c.showInColumnPicker !== false)
        .map((c) => ({ ...c }))
      if (!current.length || !orderedKeys || !orderedKeys.length) return
      const byKey = new Map(current.map((c) => [String(c.key), c]))
      const seen = new Set()
      const next = []
      orderedKeys.forEach((k) => {
        const col = byKey.get(String(k))
        if (col) {
          next.push(col)
          seen.add(String(k))
        }
      })
      current.forEach((c) => {
        if (!seen.has(String(c.key))) next.push(c)
      })
      const full =
        Array.isArray(table.tableFieldList) && table.tableFieldList.length
          ? table.tableFieldList.map((c) => ({ ...c }))
          : next.map((c) => ({ ...c }))
      const fullByKey = new Map(full.map((c) => [String(c.key), c]))
      const orderedFull = []
      const fullSeen = new Set()
      next.forEach((c) => {
        const col = fullByKey.get(String(c.key))
        if (col) {
          orderedFull.push(col)
          fullSeen.add(String(c.key))
        }
      })
      full.forEach((c) => {
        if (!fullSeen.has(String(c.key))) orderedFull.push(c)
      })
      /* 始终 setColumns：顺带 normalizeFixedPrefix，避免 fixed 列钉左导致面板序≠表头序 */
      table.setColumns(orderedFull)
      this.$nextTick(() => {
        requestAnimationFrame(() => this.bindColumnPickerSortable())
      })
    }
  }
}
