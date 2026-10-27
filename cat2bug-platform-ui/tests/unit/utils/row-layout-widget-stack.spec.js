/**
 * @jest-environment node
 */
import {
  normalizeCol,
  normalizeRow,
  placeWidgetInCol,
  moveWidgetInstance,
  removeWidgetInstance,
  columnHasWidgetType,
  colWidgets,
  GRID_SPAN
} from '@/views/system/dashboard/workbench/row-layout'
import { rowsFromPreset } from '@/views/system/dashboard/workbench/presets'

function sampleRows() {
  return [
    normalizeRow({
      cols: [
        { id: 'c1', ratio: 12, widgets: [{ instanceId: 'i-a', type: 'todo', props: {} }] },
        { id: 'c2', ratio: 12 }
      ]
    })
  ]
}

describe('row-layout widget stack', () => {
  it('normalizeCol maps legacy singular widget into widgets[]', () => {
    const col = normalizeCol({
      id: 'legacy',
      ratio: GRID_SPAN,
      widget: { instanceId: 'old-1', type: 'defect-list', props: { x: 1 } }
    })
    expect(col.widgets).toHaveLength(1)
    expect(col.widgets[0].type).toBe('defect-list')
    expect(col.widgets[0].instanceId).toBe('old-1')
    expect(col.widget.type).toBe('defect-list')
  })

  it('placeWidgetInCol appends instead of replacing', () => {
    const rows = sampleRows()
    const next = placeWidgetInCol(rows, 'c1', 'document-pin', {})
    expect(next).not.toBe(rows)
    const list = colWidgets(next[0].cols[0])
    expect(list.map(w => w.type)).toEqual(['todo', 'document-pin'])
    expect(list[0].instanceId).toBe('i-a')
  })

  it('placeWidgetInCol rejects duplicate type in same column', () => {
    const rows = sampleRows()
    const next = placeWidgetInCol(rows, 'c1', 'todo', {})
    expect(next).toBe(rows)
    expect(columnHasWidgetType(rows[0].cols[0], 'todo')).toBe(true)
  })

  it('placeWidgetInCol can insert at index', () => {
    let rows = sampleRows()
    rows = placeWidgetInCol(rows, 'c1', 'document-pin', {})
    rows = placeWidgetInCol(rows, 'c1', 'source-code', {}, 1)
    expect(colWidgets(rows[0].cols[0]).map(w => w.type)).toEqual([
      'todo',
      'source-code',
      'document-pin'
    ])
  })

  it('moveWidgetInstance reorders within column', () => {
    let rows = sampleRows()
    rows = placeWidgetInCol(rows, 'c1', 'document-pin', {})
    const idDoc = colWidgets(rows[0].cols[0])[1].instanceId
    rows = moveWidgetInstance(rows, idDoc, 'c1', 0)
    expect(colWidgets(rows[0].cols[0]).map(w => w.type)).toEqual(['document-pin', 'todo'])
  })

  it('moveWidgetInstance moves upper widget onto lower index (swap-like)', () => {
    let rows = sampleRows()
    rows = placeWidgetInCol(rows, 'c1', 'document-pin', {})
    // [todo, document-pin] — 上面拖到下面（落到下标 1）
    const idTodo = colWidgets(rows[0].cols[0])[0].instanceId
    const next = moveWidgetInstance(rows, idTodo, 'c1', 1)
    expect(next).not.toBe(rows)
    expect(colWidgets(next[0].cols[0]).map(w => w.type)).toEqual(['document-pin', 'todo'])
  })

  it('moveWidgetInstance moves across columns and preserves instanceId', () => {
    const rows = sampleRows()
    const next = moveWidgetInstance(rows, 'i-a', 'c2')
    expect(colWidgets(next[0].cols[0])).toHaveLength(0)
    expect(colWidgets(next[0].cols[1]).map(w => w.type)).toEqual(['todo'])
    expect(colWidgets(next[0].cols[1])[0].instanceId).toBe('i-a')
  })

  it('moveWidgetInstance rejects cross-column duplicate type', () => {
    let rows = sampleRows()
    rows = placeWidgetInCol(rows, 'c2', 'todo', {})
    const blocked = moveWidgetInstance(rows, 'i-a', 'c2')
    expect(blocked).toBe(rows)
  })

  it('removeWidgetInstance leaves remaining widgets', () => {
    let rows = sampleRows()
    rows = placeWidgetInCol(rows, 'c1', 'document-pin', {})
    const idDoc = colWidgets(rows[0].cols[0])[1].instanceId
    rows = removeWidgetInstance(rows, idDoc)
    expect(colWidgets(rows[0].cols[0]).map(w => w.type)).toEqual(['todo'])
  })

  it('manager preset stack column supports append and reorder', () => {
    const rows = rowsFromPreset('manager')
    let stacked = null
    for (const row of rows) {
      for (const col of row.cols) {
        if (colWidgets(col).length > 1) {
          stacked = col
          break
        }
      }
      if (stacked) break
    }
    expect(stacked).toBeTruthy()
    expect(colWidgets(stacked).map(w => w.type)).toEqual([
      'git-repo-stats',
      'document-list'
    ])
    let next = placeWidgetInCol(rows, stacked.id, 'my-kpi', {})
    expect(colWidgets(next.flatMap(r => r.cols).find(c => c.id === stacked.id)).map(w => w.type)).toEqual([
      'git-repo-stats',
      'document-list',
      'my-kpi'
    ])
    const list = colWidgets(next.flatMap(r => r.cols).find(c => c.id === stacked.id))
    next = moveWidgetInstance(next, list[2].instanceId, stacked.id, 0)
    expect(colWidgets(next.flatMap(r => r.cols).find(c => c.id === stacked.id)).map(w => w.type)[0]).toBe(
      'my-kpi'
    )
  })
})
