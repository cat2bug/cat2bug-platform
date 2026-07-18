import { $node } from '@milkdown/kit/utils'
import { serializeTemplate } from '../dsl/serialize'
import { emitPreservedRawMarkdown } from './raw-block-serialize'

function emitRawBlock(state, raw) {
  state.openNode('paragraph')
  state.addNode('text', undefined, raw || '')
  state.closeNode()
}

function columnsToJson(columns) {
  return JSON.stringify(columns || [])
}

function columnsFromJson(json) {
  try {
    return JSON.parse(json || '[]')
  } catch (_e) {
    return []
  }
}

function readMdastField(node, field, fallback = '') {
  if (node?.[field] != null && node[field] !== '') {
    return node[field]
  }
  return node?.data?.[field] ?? fallback
}

function blockFallbackDom(label, raw, className, attrs) {
  return [
    'div',
    {
      ...attrs,
      contenteditable: 'false',
      class: `cat2bug-dsl-block ${className}`
    },
    ['div', { class: 'cat2bug-dsl-block__view-header' }, label],
    ['code', { class: 'cat2bug-dsl-block__view-token' }, raw || '']
  ]
}

export const cat2bugDefectTableNode = $node('cat2bug_defect_table', () => ({
  group: 'block',
  atom: true,
  isolating: true,
  priority: 100,
  attrs: {
    dataSource: { default: 'api.defect.list' },
    columnsJson: { default: '[]' },
    raw: { default: '' }
  },
  parseDOM: [{
    tag: 'div[data-cat2bug-defect-table]',
    getAttrs: (dom) => ({
      dataSource: dom.getAttribute('data-source') || 'api.defect.list',
      columnsJson: dom.getAttribute('data-columns') || '[]',
      raw: dom.getAttribute('data-raw') || ''
    })
  }],
  toDOM: (node) => blockFallbackDom('缺陷表格', node.attrs.raw, 'cat2bug-dsl-block--table', {
    'data-cat2bug-defect-table': 'true',
    'data-source': node.attrs.dataSource,
    'data-columns': node.attrs.columnsJson,
    'data-raw': node.attrs.raw
  }),
  parseMarkdown: {
    match: ({ type }) => type === 'cat2bugDefectTable',
    runner: (state, node, type) => {
      state.addNode(type, {
        dataSource: readMdastField(node, 'dataSource', 'api.defect.list'),
        columnsJson: columnsToJson(readMdastField(node, 'columns', [])),
        raw: readMdastField(node, 'raw', '')
      })
    }
  },
  toMarkdown: {
    match: (node) => node.type.name === 'cat2bug_defect_table',
    runner: (state, node) => {
      const raw = node.attrs.raw || serializeTemplate([{
        type: 'defectTable',
        dataSource: node.attrs.dataSource,
        columns: columnsFromJson(node.attrs.columnsJson)
      }])
      emitRawBlock(state, raw)
    }
  }
}))

export const cat2bugCaseCardNode = $node('cat2bug_case_card', () => ({
  group: 'block',
  atom: true,
  isolating: true,
  priority: 100,
  attrs: {
    dataSource: { default: 'api.case.list' },
    raw: { default: '' }
  },
  parseDOM: [{
    tag: 'div[data-cat2bug-case-card]',
    getAttrs: (dom) => ({
      dataSource: dom.getAttribute('data-source') || 'api.case.list',
      raw: dom.getAttribute('data-raw') || ''
    })
  }],
  toDOM: (node) => blockFallbackDom('用例卡片', node.attrs.raw, 'cat2bug-dsl-block--card', {
    'data-cat2bug-case-card': 'true',
    'data-source': node.attrs.dataSource,
    'data-raw': node.attrs.raw
  }),
  parseMarkdown: {
    match: ({ type }) => type === 'cat2bugCaseCard',
    runner: (state, node, type) => {
      state.addNode(type, {
        dataSource: readMdastField(node, 'dataSource', 'api.case.list'),
        raw: readMdastField(node, 'raw', '')
      })
    }
  },
  toMarkdown: {
    match: (node) => node.type.name === 'cat2bug_case_card',
    runner: (state, node) => {
      emitRawBlock(state, node.attrs.raw || serializeTemplate([{
        type: 'caseCard',
        dataSource: node.attrs.dataSource
      }]))
    }
  }
}))

export const cat2bugDynamicImageNode = $node('cat2bug_dynamic_image', () => ({
  group: 'block',
  atom: true,
  isolating: true,
  priority: 100,
  attrs: {
    style: { default: '' },
    path: { default: '' },
    raw: { default: '' }
  },
  parseDOM: [{
    tag: 'div[data-cat2bug-dynamic-image]',
    getAttrs: (dom) => ({
      style: dom.getAttribute('data-style') || '',
      path: dom.getAttribute('data-path') || '',
      raw: dom.getAttribute('data-raw') || ''
    })
  }],
  toDOM: (node) => blockFallbackDom('动态图片', node.attrs.raw, 'cat2bug-dsl-block--image', {
    'data-cat2bug-dynamic-image': 'true',
    'data-style': node.attrs.style,
    'data-path': node.attrs.path,
    'data-raw': node.attrs.raw
  }),
  parseMarkdown: {
    match: ({ type }) => type === 'cat2bugDynamicImage',
    runner: (state, node, type) => {
      state.addNode(type, {
        style: readMdastField(node, 'style', ''),
        path: readMdastField(node, 'path', ''),
        raw: readMdastField(node, 'raw', '')
      })
    }
  },
  toMarkdown: {
    match: (node) => node.type.name === 'cat2bug_dynamic_image',
    runner: (state, node) => {
      emitRawBlock(state, node.attrs.raw || serializeTemplate([{
        type: 'dynamicImage',
        style: node.attrs.style,
        path: node.attrs.path
      }]))
    }
  }
}))

export const cat2bugVariableNode = $node('cat2bug_variable', () => ({
  group: 'inline',
  inline: true,
  atom: true,
  priority: 100,
  attrs: {
    path: { default: '' },
    raw: { default: '' }
  },
  parseDOM: [{
    tag: 'span[data-cat2bug-variable]',
    getAttrs: (dom) => ({
      path: dom.getAttribute('data-path') || '',
      raw: dom.getAttribute('data-raw') || ''
    })
  }],
  toDOM: (node) => [
    'span',
    {
      'data-cat2bug-variable': 'true',
      'data-path': node.attrs.path,
      'data-raw': node.attrs.raw,
      contenteditable: 'false',
      class: 'cat2bug-dsl-chip cat2bug-dsl-chip--variable'
    },
    node.attrs.raw || `\${${node.attrs.path}}`
  ],
  parseMarkdown: {
    match: ({ type }) => type === 'cat2bugVariable',
    runner: (state, node, type) => {
      state.addNode(type, {
        path: readMdastField(node, 'path', ''),
        raw: readMdastField(node, 'raw', '')
      })
    }
  },
  toMarkdown: {
    match: (node) => node.type.name === 'cat2bug_variable',
    runner: (state, node) => {
      state.addNode('text', undefined, node.attrs.raw || serializeTemplate([{
        type: 'variable',
        path: node.attrs.path
      }]))
    }
  }
}))

export const cat2bugPlanVariableNode = $node('cat2bug_plan_variable', () => ({
  group: 'inline',
  inline: true,
  atom: true,
  priority: 100,
  attrs: {
    path: { default: '' },
    planIndex: { default: 1 },
    raw: { default: '' }
  },
  parseDOM: [{
    tag: 'span[data-cat2bug-plan-variable]',
    getAttrs: (dom) => ({
      path: dom.getAttribute('data-path') || '',
      planIndex: Number(dom.getAttribute('data-plan-index') || 1),
      raw: dom.getAttribute('data-raw') || ''
    })
  }],
  toDOM: (node) => [
    'span',
    {
      'data-cat2bug-plan-variable': 'true',
      'data-path': node.attrs.path,
      'data-plan-index': String(node.attrs.planIndex),
      'data-raw': node.attrs.raw,
      contenteditable: 'false',
      class: 'cat2bug-dsl-chip cat2bug-dsl-chip--plan'
    },
    node.attrs.raw || `\${${node.attrs.path}[${node.attrs.planIndex}]}`
  ],
  parseMarkdown: {
    match: ({ type }) => type === 'cat2bugPlanVariable',
    runner: (state, node, type) => {
      state.addNode(type, {
        path: readMdastField(node, 'path', ''),
        planIndex: readMdastField(node, 'planIndex', 1),
        raw: readMdastField(node, 'raw', '')
      })
    }
  },
  toMarkdown: {
    match: (node) => node.type.name === 'cat2bug_plan_variable',
    runner: (state, node) => {
      state.addNode('text', undefined, node.attrs.raw || serializeTemplate([{
        type: 'planVariable',
        path: node.attrs.path,
        planIndex: node.attrs.planIndex
      }]))
    }
  }
}))

const RAW_BLOCK_LABELS = {
  'plan-statistics': '计划统计大表',
  'multimd-table': '合并单元格表格',
  generic: '保留块'
}

export const cat2bugRawBlockNode = $node('cat2bug_raw_block', () => ({
  group: 'block',
  atom: true,
  isolating: true,
  priority: 120,
  attrs: {
    kind: { default: 'generic' },
    value: { default: '' }
  },
  parseDOM: [{
    tag: 'div[data-cat2bug-raw-block]',
    getAttrs: (dom) => ({
      kind: dom.getAttribute('data-kind') || 'generic',
      value: dom.getAttribute('data-value') || ''
    })
  }],
  toDOM: (node) => blockFallbackDom(
    RAW_BLOCK_LABELS[node.attrs.kind] || RAW_BLOCK_LABELS.generic,
    node.attrs.value,
    'cat2bug-dsl-block--raw',
    {
      'data-cat2bug-raw-block': 'true',
      'data-kind': node.attrs.kind,
      'data-value': node.attrs.value
    }
  ),
  parseMarkdown: {
    match: ({ type }) => type === 'cat2bugRawBlock',
    runner: (state, node, type) => {
      state.addNode(type, {
        kind: readMdastField(node, 'kind', 'generic'),
        value: readMdastField(node, 'value', '')
      })
    }
  },
  toMarkdown: {
    match: (node) => node.type.name === 'cat2bug_raw_block',
    runner: (state, node) => {
      emitPreservedRawMarkdown(state, node.attrs.value)
    }
  }
}))
