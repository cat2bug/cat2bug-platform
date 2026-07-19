jest.mock('@/components/ReportTemplateEditor/composables/toolbarMenuLoader', () => ({
  loadReportToolbarMenus: jest.fn(async () => [])
}))

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

import {
  extractAddMenuGroupsFromToolbar,
  splitToolbarToolsBySiding
} from '@/components/ReportTemplateEditor/composables/blockEditBusinessMenu'
import { toolbarAddMenuToOverlayGroups } from '@/components/ReportTemplateEditor/milkdown/default-block-menu'

const mockI18n = {
  locale: 'zh_CN',
  te: () => false,
  t: (key) => key,
  getLocaleMessage: () => ({
    project: '项目',
    case: '测试用例',
    defect: '缺陷',
    plan: '测试计划',
    'project.name': '项目名称',
    'case.total': '测试用例总数',
    'case.card': '用例卡片',
    'defect.table': '缺陷表',
    title1: '标题1',
    title2: '标题2',
    image: '图片',
    code: '代码',
    table: '表格',
    'ordered-list': '有序列表',
    'unordered-list': '无序列表',
    'font-weight': '加粗'
  })
}

function sampleToolbar() {
  return [
    { name: 'import-markdown', type: 'command' },
    { name: 'save', type: 'command' },
    { type: 'siding' },
    { name: 'variables-expand-collapse', type: 'check', check: true },
    { type: 'siding' },
    { name: 'font-weight', content: '**{0}**' },
    {
      name: 'title',
      children: [
        { name: 'title1', content: '# {0}' },
        { name: 'title2', content: '## {0}' }
      ]
    },
    { name: 'ordered-list', content: '1. {0}' },
    { name: 'unordered-list', content: '- {0}' },
    { type: 'siding' },
    { name: 'link', type: 'component', content: {} },
    {
      name: 'image',
      children: [
        { name: 'mk.add-image-link', type: 'component' },
        { name: 'upload-image', type: 'component' }
      ]
    },
    { name: 'code', content: '```language\n{0}\n```' },
    { name: 'table', content: '|a|b|\n|-|-|\n|1|2|' },
    { type: 'siding' },
    {
      name: 'project',
      children: [{ name: 'project.name', content: 'Name: ${api.project.projectName}' }]
    },
    {
      name: 'case',
      children: [
        { name: 'case.total', content: 'Total: ${api.case.total}' },
        { name: 'case.card', content: '$card{api.case.list}' }
      ]
    },
    {
      name: 'defect',
      children: [{ name: 'defect.table', content: '$table{api.defect.list}' }]
    }
  ]
}

describe('add menu from toolbar', () => {
  it('drops import/save/variables-expand when splitting by siding', () => {
    const segments = splitToolbarToolsBySiding(sampleToolbar())
    const names = segments.map((seg) => seg.map((t) => t.name))
    expect(names[0]).toEqual(['font-weight', 'title', 'ordered-list', 'unordered-list'])
    expect(names[1]).toEqual(['link', 'image', 'code', 'table'])
    expect(names[2]).toEqual(['project', 'case', 'defect'])
  })

  it('builds groups in toolbar order with 文本 / 高级 / business tabs', () => {
    const groups = extractAddMenuGroupsFromToolbar(sampleToolbar(), mockI18n)
    expect(groups.map((g) => g.key)).toEqual([
      'toolbar-segment-0',
      'toolbar-segment-1',
      'project',
      'case',
      'defect'
    ])
    expect(groups.map((g) => g.label)).toEqual(['文本', '高级', '项目', '测试用例', '缺陷'])

    expect(groups[0].items.map((i) => i.key)).toEqual([
      'font-weight',
      'title1',
      'title2',
      'ordered-list',
      'unordered-list'
    ])
    expect(groups[1].items.map((i) => i.key)).toEqual(['link', 'image', 'code', 'table'])
    expect(groups[1].items[0].insertMode).toBe('link')
    expect(groups[1].items[1].insertMode).toBe('image-block')
    expect(groups[2].items.map((i) => i.key)).toEqual(['project.name'])
  })

  it('does not expose non-insert actions in overlay groups', () => {
    const groups = extractAddMenuGroupsFromToolbar(sampleToolbar(), mockI18n)
    const allKeys = groups.flatMap((g) => g.items.map((i) => i.key))
    expect(allKeys).not.toContain('import-markdown')
    expect(allKeys).not.toContain('save')
    expect(allKeys).not.toContain('variables-expand-collapse')
    expect(allKeys).toContain('link')
  })

  it('maps toolbar groups to overlay onSelect handlers', () => {
    const groups = extractAddMenuGroupsFromToolbar(sampleToolbar(), mockI18n)
    const inserted = []
    const overlay = toolbarAddMenuToOverlayGroups(groups, {
      onInsertContent: (_ctx, content) => inserted.push(content),
      onInsertImageBlock: () => inserted.push('IMAGE_BLOCK')
    })
    // advanced: link, image, code
    overlay[1].items[1].onSelect({})
    overlay[1].items[2].onSelect({})
    expect(inserted[0]).toBe('IMAGE_BLOCK')
    expect(String(inserted[1])).toContain('```')
  })
})
