import {
  parseMultimdTable,
  MULTIMD_TABLE_MARKER,
  serializeMultimdTable,
  sourceNeedsMultimdSpanApply,
  tableHasSpanAttrs,
  rewriteMarkdownTablesFromDoc,
  buildPmTableFromMultimdGrid
} from '@/components/ReportTemplateEditor/dsl'
import { getTableCellPlainText } from '@/components/ReportTemplateEditor/dsl/serialize-multimd-table'
import { Schema, Fragment } from 'prosemirror-model'
import { tableNodes, TableMap } from 'prosemirror-tables'

const tableNodeSpecs = tableNodes({
  tableGroup: 'block',
  cellContent: 'paragraph',
  cellAttributes: {
    alignment: {
      default: 'left',
      getFromDOM: () => 'left',
      setDOMAttr: () => {}
    }
  }
})

const schema = new Schema({
  nodes: {
    doc: { content: 'block+' },
    paragraph: { group: 'block', content: 'inline*', toDOM: () => ['p', 0], parseDOM: [{ tag: 'p' }] },
    text: { group: 'inline' },
    cat2bug_dynamic_image: {
      group: 'inline',
      inline: true,
      atom: true,
      attrs: {
        style: { default: '' },
        path: { default: '' },
        raw: { default: '' }
      },
      toDOM: () => ['span', 0],
      parseDOM: [{ tag: 'span[data-cat2bug-dynamic-image]' }]
    },
    cat2bug_variable: {
      group: 'inline',
      inline: true,
      atom: true,
      attrs: {
        path: { default: '' },
        raw: { default: '' }
      },
      toDOM: () => ['span', 0],
      parseDOM: [{ tag: 'span[data-cat2bug-variable]' }]
    },
    table: {
      ...tableNodeSpecs.table,
      content: 'table_header_row table_row+',
      tableRole: 'table'
    },
    table_header_row: {
      ...tableNodeSpecs.table_row,
      content: '(table_header)*',
      tableRole: 'row'
    },
    table_row: tableNodeSpecs.table_row,
    table_cell: tableNodeSpecs.table_cell,
    table_header: tableNodeSpecs.table_header
  }
})

describe('parse-multimd-table', () => {
  it('parses colspan from trailing empty cells', () => {
    const grid = parseMultimdTable(`|a|b|c|
|-|-|-|
|x||z|`)
    expect(grid).toBeTruthy()
    expect(grid.rows[1]).toEqual([
      { text: 'x', colspan: 2, rowspan: 1 },
      { text: 'z', colspan: 1, rowspan: 1 }
    ])
  })

  it('parses rowspan from ^^ markers', () => {
    const grid = parseMultimdTable(`| Stage | Direct Products | ATP Yields |
| --- | --- | --- |
| Glycolysis | 2 ATP ||
| ^^ | 2 NADH | 3--5 ATP |`)
    expect(grid).toBeTruthy()
    expect(grid.rows[1][0]).toEqual({ text: 'Glycolysis', colspan: 1, rowspan: 2 })
    expect(grid.rows[1][1]).toEqual({ text: '2 ATP', colspan: 2, rowspan: 1 })
    expect(grid.rows[2]).toEqual([
      { text: '2 NADH', colspan: 1, rowspan: 1 },
      { text: '3--5 ATP', colspan: 1, rowspan: 1 }
    ])
  })

  it('preserves $image / ${} atom raw when serializing spanned table cells', () => {
    const imageNode = schema.nodes.cat2bug_dynamic_image.create({
      style: 'width:100px;height:100px;',
      path: 'api.project.projectIcon',
      raw: '$image["style":"width:100px;height:100px;"]{api.project.projectIcon}'
    })
    const varNode = schema.nodes.cat2bug_variable.create({
      path: 'api.project.projectName',
      raw: '${api.project.projectName}'
    })
    const paragraph = schema.nodes.paragraph.create(null, [imageNode, schema.text(' '), varNode])
    const cell = schema.nodes.table_cell.create({ colspan: 2, rowspan: 1 }, paragraph)
    expect(getTableCellPlainText(cell)).toBe(
      '$image["style":"width:100px;height:100px;"]{api.project.projectIcon} ${api.project.projectName}'
    )
  })

  it('rebuilds ${} / $image from path when atom raw is empty', () => {
    const imageNode = schema.nodes.cat2bug_dynamic_image.create({
      style: 'width:100px;height:100px;',
      path: 'api.project.projectIcon',
      raw: ''
    })
    const varNode = schema.nodes.cat2bug_variable.create({
      path: 'api.project.projectName',
      raw: ''
    })
    const paragraph = schema.nodes.paragraph.create(null, [
      schema.text('前'),
      varNode,
      schema.text('中'),
      imageNode,
      schema.text('后')
    ])
    const cell = schema.nodes.table_cell.create(null, paragraph)
    expect(getTableCellPlainText(cell)).toBe(
      '前${api.project.projectName}中$image["style":"width:100px;height:100px;"]{api.project.projectIcon}后'
    )
  })

  it('buildPmTableFromMultimdGrid promotes $image and ${} to nodes', () => {
    const grid = parseMultimdTable(`|h1|h2|
|-|-|
|$image["style":"width:100px;height:100px;"]{api.project.projectIcon}|\${api.project.projectName}|`)
    const table = buildPmTableFromMultimdGrid(schema, grid)
    expect(table).toBeTruthy()
    const bodyRow = table.child(1)
    const imageCell = bodyRow.child(0)
    const varCell = bodyRow.child(1)
    expect(imageCell.firstChild.firstChild.type.name).toBe('cat2bug_dynamic_image')
    expect(imageCell.firstChild.firstChild.attrs.path).toBe('api.project.projectIcon')
    expect(varCell.firstChild.firstChild.type.name).toBe('cat2bug_variable')
    expect(varCell.firstChild.firstChild.attrs.path).toBe('api.project.projectName')
  })

  it('promotes $image even when a stray space sits before {', () => {
    const grid = parseMultimdTable(`|h|
|-|
|$image["style":"width:100px;height:100px;"] {api.project.projectIcon}|`)
    const table = buildPmTableFromMultimdGrid(schema, grid)
    const node = table.child(1).child(0).firstChild.firstChild
    expect(node.type.name).toBe('cat2bug_dynamic_image')
    expect(node.attrs.raw).toBe(
      '$image["style":"width:100px;height:100px;"]{api.project.projectIcon}'
    )
  })

  it('round-trips colspan/rowspan through serialize → parse', () => {
    const src = `<!--cat2bug-table-spans-->
|h1|h2|h3|
|-|-|-|
|A||C|
|^^|b|c|`
    const grid = parseMultimdTable(src)
    const table = buildPmTableFromMultimdGrid(schema, grid)
    expect(table).toBeTruthy()
    expect(tableHasSpanAttrs(table)).toBe(true)

    const map = TableMap.get(table)
    expect(map.width).toBe(3)
    expect(map.height).toBe(3)

    const serialized = serializeMultimdTable(table)
    expect(serialized.startsWith(MULTIMD_TABLE_MARKER)).toBe(true)
    expect(sourceNeedsMultimdSpanApply(serialized)).toBe(true)

    const again = parseMultimdTable(serialized)
    expect(again.rows[1][0]).toEqual({ text: 'A', colspan: 2, rowspan: 2 })
    expect(getTableCellPlainText(table.child(1).child(0))).toBe('A')
  })

  it('treats Multimd colspan empties as needing span apply (marker may be stripped)', () => {
    expect(sourceNeedsMultimdSpanApply('|a|b|\n|-|-|\n|x||z|')).toBe(true)
    expect(sourceNeedsMultimdSpanApply('|a|b|\n|-|-|\n|x|y|\n|^^|z|')).toBe(true)
    expect(
      sourceNeedsMultimdSpanApply('|column1 column2||column3|\n|-|-|-|\n|content1|content2|content3|')
    ).toBe(true)
  })

  it('rewrite absorbs orphaned cell text lifted by broken GFM serialize', () => {
    const grid = parseMultimdTable(`|column1|column2|column3|
|-|-|-|
|content1|content2|content3|`)
    // Simulate merged header in PM
    grid.rows[0] = [
      { text: 'column1 column2', colspan: 2, rowspan: 1 },
      { text: 'column3', colspan: 1, rowspan: 1 }
    ]
    const table = buildPmTableFromMultimdGrid(schema, grid)
    const doc = schema.nodes.doc.create(null, Fragment.from(table))
    const broken = `|column1||
|-|-|
|||
column2
`
    const fixed = rewriteMarkdownTablesFromDoc(broken, doc)
    expect(fixed).toContain(MULTIMD_TABLE_MARKER)
    expect(fixed).toContain('|column1 column2||column3|')
    expect(fixed).toContain('|content1|content2|content3|')
    expect(fixed.includes('\ncolumn2')).toBe(false)
  })

  it('rewrite keeps $image block that follows a spanned table (not an orphan)', () => {
    const grid = parseMultimdTable(`|column1|column2|
|-|-|
|a|b|`)
    grid.rows[0] = [
      { text: 'column1 column2', colspan: 2, rowspan: 1 }
    ]
    const table = buildPmTableFromMultimdGrid(schema, grid)
    const imagePara = schema.nodes.paragraph.create(
      null,
      schema.text('$image["style":"width:100px;height:100px;"]{api.project.projectIcon}')
    )
    const doc = schema.nodes.doc.create(null, Fragment.from([table, imagePara]))
    const markdown = `${MULTIMD_TABLE_MARKER}

|column1 column2||
|-|-|
|a|b|
$image["style":"width:100px;height:100px;"]{api.project.projectIcon}
`
    const fixed = rewriteMarkdownTablesFromDoc(markdown, doc)
    expect(fixed).toContain('$image["style":"width:100px;height:100px;"]{api.project.projectIcon}')
    expect(fixed).not.toMatch(/\|\$image/)
  })

  it('round-trips merged header through serialize → needs-apply → parse', () => {
    const md = `${MULTIMD_TABLE_MARKER}

|column1 column2||column3|
|-|-|-|
|content1|content2|content3|`
    expect(sourceNeedsMultimdSpanApply(md)).toBe(true)
    const grid = parseMultimdTable(md)
    expect(grid.rows[0]).toEqual([
      { text: 'column1 column2', colspan: 2, rowspan: 1 },
      { text: 'column3', colspan: 1, rowspan: 1 }
    ])
  })
})
