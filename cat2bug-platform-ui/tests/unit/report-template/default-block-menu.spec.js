jest.mock('@milkdown/kit/core', () => ({
  commandsCtx: {},
  editorViewCtx: {}
}), { virtual: true })
jest.mock('@milkdown/kit/preset/commonmark', () => ({
  paragraphSchema: { type: () => ({}) },
  clearTextInCurrentBlockCommand: { key: 'clear' },
  setBlockTypeCommand: { key: 'setBlock' },
  headingSchema: { type: () => ({}) },
  blockquoteSchema: { type: () => ({}) },
  wrapInBlockTypeCommand: { key: 'wrap' },
  hrSchema: { type: () => ({}) },
  addBlockTypeCommand: { key: 'addBlock' },
  bulletListSchema: { type: () => ({}) },
  orderedListSchema: { type: () => ({}) },
  codeBlockSchema: { type: () => ({}) },
  selectTextNearPosCommand: { key: 'selectNear' }
}), { virtual: true })
jest.mock('@milkdown/kit/preset/gfm', () => ({
  createTable: () => ({})
}), { virtual: true })
jest.mock('@milkdown/kit/component/image-block', () => ({
  imageBlockSchema: { type: () => ({ name: 'image-block' }) }
}), { virtual: true })

import { buildDefaultBlockMenuGroups } from '@/components/ReportTemplateEditor/milkdown/default-block-menu'

describe('buildDefaultBlockMenuGroups', () => {
  it('includes Crepe-style advanced items: image, code block, table', () => {
    const groups = buildDefaultBlockMenuGroups()
    const advanced = groups.find((g) => g.key === 'advanced')
    expect(advanced).toBeTruthy()
    expect(advanced.label).toBe('高级')
    expect(advanced.items.map((item) => item.key)).toEqual(['image', 'code', 'table'])
    expect(advanced.items.map((item) => item.label)).toEqual(['图片', '代码块', '表格'])
  })
})
