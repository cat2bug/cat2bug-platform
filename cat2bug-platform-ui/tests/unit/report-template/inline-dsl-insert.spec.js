/** @jest-environment jsdom */
import { Schema } from 'prosemirror-model'
import { EditorState, NodeSelection, TextSelection } from 'prosemirror-state'
import { Fragment } from 'prosemirror-model'
import {
  resolveInlineInsertCaret,
  insertInlineDslAtCaret,
  insertInlineFragmentAtCaret,
  extractInlineFragmentFromParsedDoc,
  cleanupEphemeralEmptyParagraph
} from '@/components/ReportTemplateEditor/milkdown/inline-dsl-insert'

const schema = new Schema({
  nodes: {
    doc: { content: 'block+' },
    text: { group: 'inline' },
    paragraph: { group: 'block', content: 'inline*', toDOM: () => ['p', 0] },
    table: { group: 'block', content: 'table_row+', tableRole: 'table', isolating: true },
    table_row: { content: '(table_cell | table_header)+', tableRole: 'row' },
    table_cell: { content: 'block+', tableRole: 'cell', isolating: true },
    table_header: { content: 'block+', tableRole: 'header_cell', isolating: true }
  }
})

function makeCellView(text = 'hello world') {
  const paragraph = schema.nodes.paragraph.create(null, schema.text(text))
  const cell = schema.nodes.table_cell.create(null, paragraph)
  const row = schema.nodes.table_row.create(null, cell)
  const table = schema.nodes.table.create(null, row)
  const doc = schema.nodes.doc.create(null, table)

  let paraPos = null
  doc.descendants((node, pos) => {
    if (node.type.name === 'paragraph' && node.textContent === text) {
      paraPos = pos
      return false
    }
    return undefined
  })

  let state = EditorState.create({
    doc,
    schema,
    selection: NodeSelection.create(doc, paraPos)
  })
  const view = {
    state,
    dispatch(tr) {
      state = state.apply(tr)
      view.state = state
    },
    focus() {}
  }
  return { view, paraPos, text }
}

describe('resolveInlineInsertCaret', () => {
  it('collapses NodeSelection on a cell paragraph to a caret inside it', () => {
    const { view, text } = makeCellView()
    const caret = resolveInlineInsertCaret(view.state)
    expect(caret).toBeTruthy()
    expect(caret.empty).toBe(true)
    expect(view.state.doc.textBetween(caret.from, caret.from + text.length)).toBe(text)
  })
})

describe('insertInlineDslAtCaret', () => {
  it('inserts $image at caret without replacing other text on the line', () => {
    const { view, text } = makeCellView('ab cd')
    const image = '$image["style":"width:100px;height:100px;"]{api.project.projectIcon}'
    // caret after "ab "
    const mid = view.state.selection.from + 1 + 3
    insertInlineDslAtCaret(view, image, mid)
    expect(view.state.doc.textContent).toBe(`ab ${image}cd`)
    expect(view.state.doc.textContent).toContain('ab ')
    expect(view.state.doc.textContent).toContain('cd')
  })

  it('does not wipe paragraph when NodeSelection is active', () => {
    const { view, text } = makeCellView('keep me')
    const image = '$image{api.project.projectIcon}'
    insertInlineDslAtCaret(view, image, null)
    expect(view.state.doc.textContent).toContain('keep me')
    expect(view.state.doc.textContent).toContain(image)
  })
})

describe('insertInlineFragmentAtCaret', () => {
  it('keeps surrounding text when inserting an inline fragment mid-line', () => {
    const { view } = makeCellView('ab cd')
    // Build a fake "parsed" doc: paragraph with image-like text atom as plain text node
    const imageText = schema.text('$image{api.project.projectIcon}')
    const parsed = schema.nodes.doc.create(
      null,
      schema.nodes.paragraph.create(null, imageText)
    )
    const frag = extractInlineFragmentFromParsedDoc(parsed)
    expect(frag).toBeTruthy()

    let mid = null
    view.state.doc.descendants((node, pos) => {
      if (node.isText && node.text === 'ab cd') {
        mid = pos + 3 // after "ab "
        return false
      }
      return undefined
    })
    insertInlineFragmentAtCaret(view, frag, mid)
    expect(view.state.doc.textContent).toBe('ab $image{api.project.projectIcon}cd')
  })

  it('extractInlineFragmentFromParsedDoc returns null for empty doc', () => {
    expect(extractInlineFragmentFromParsedDoc(null)).toBeNull()
    expect(extractInlineFragmentFromParsedDoc(schema.nodes.doc.create(null, [
      schema.nodes.paragraph.create()
    ]))).toBeNull()
  })
})

describe('cleanupEphemeralEmptyParagraph', () => {
  it('removes the empty paragraph created after the content line', () => {
    const paragraph = schema.nodes.paragraph.create(null, schema.text('line'))
    const empty = schema.nodes.paragraph.create()
    const doc = schema.nodes.doc.create(null, [paragraph, empty])
    let state = EditorState.create({
      doc,
      schema,
      selection: TextSelection.near(doc.resolve(1), 1)
    })
    const view = {
      state,
      dispatch(tr) {
        state = state.apply(tr)
        view.state = state
      },
      focus() {}
    }
    const emptyPos = paragraph.nodeSize
    expect(cleanupEphemeralEmptyParagraph(view, emptyPos, 1, 0)).toBe(true)
    expect(view.state.doc.childCount).toBe(1)
    expect(view.state.doc.firstChild.textContent).toBe('line')
  })
})
