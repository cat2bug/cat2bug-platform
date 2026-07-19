import {
  TABLE_TOKEN_RE,
  CARD_TOKEN_RE,
  IMAGE_TOKEN_RE
} from '../dsl/grammar'
import { parseTemplate } from '../dsl/parse'
import { normalizeDslSource } from '../dsl/normalize'

/**
 * @param {import('../dsl/parse').TemplateSegment} segment
 * @returns {object[]}
 */
export function segmentToMdast(segment) {
  switch (segment.type) {
    case 'text':
      return segment.value ? [{ type: 'text', value: segment.value }] : []
    case 'variable':
      return [{ type: 'cat2bugVariable', path: segment.path, raw: segment.raw }]
    case 'planVariable':
      return [{
        type: 'cat2bugPlanVariable',
        path: segment.path,
        planIndex: segment.planIndex,
        raw: segment.raw
      }]
    case 'defectTable':
      return [{
        type: 'cat2bugDefectTable',
        dataSource: segment.dataSource,
        columns: segment.columns,
        raw: segment.raw
      }]
    case 'caseCard':
      return [{
        type: 'cat2bugCaseCard',
        dataSource: segment.dataSource,
        raw: segment.raw
      }]
    case 'dynamicImage':
      return [{
        type: 'cat2bugDynamicImage',
        style: segment.style,
        path: segment.path,
        raw: segment.raw
      }]
    default:
      return segment.raw ? [{ type: 'text', value: segment.raw }] : []
  }
}

/**
 * @param {string} text
 * @returns {{ kind: string, segment: import('../dsl/parse').TemplateSegment }|null}
 */
export function isBlockToken(text) {
  const trimmed = normalizeDslSource(text || '').trim()
  if (!trimmed) {
    return null
  }
  TABLE_TOKEN_RE.lastIndex = 0
  CARD_TOKEN_RE.lastIndex = 0
  IMAGE_TOKEN_RE.lastIndex = 0
  const table = TABLE_TOKEN_RE.exec(trimmed)
  if (table && table[0] === trimmed) {
    return { kind: 'defectTable', segment: parseTemplate(trimmed)[0] }
  }
  const card = CARD_TOKEN_RE.exec(trimmed)
  if (card && card[0] === trimmed) {
    return { kind: 'caseCard', segment: parseTemplate(trimmed)[0] }
  }
  const image = IMAGE_TOKEN_RE.exec(trimmed)
  if (image && image[0] === trimmed) {
    return { kind: 'dynamicImage', segment: parseTemplate(trimmed)[0] }
  }
  return null
}

/**
 * Expand DSL tokens inside a GFM table cell's phrasing children.
 * Variables and $image become custom mdast nodes; $table/$card stay as text.
 *
 * @param {string} text
 * @returns {object[]|null} next children, or null when unchanged
 */
export function expandTableCellDslText(text) {
  const source = String(text || '')
  if (!/\$\{|\$image|\$img/.test(source)) {
    return null
  }
  const segments = parseTemplate(normalizeDslSource(source))
  if (!segments.length || (segments.length === 1 && segments[0].type === 'text')) {
    return null
  }
  const nextChildren = segments.flatMap((segment) => {
    if (
      segment.type === 'defectTable' ||
      segment.type === 'caseCard' ||
      segment.type === 'raw'
    ) {
      return [{ type: 'text', value: segment.raw || segment.value || '' }]
    }
    return segmentToMdast(segment)
  })
  return nextChildren.length ? nextChildren : null
}

/**
 * @param {import('../dsl/parse').TemplateSegment} segment
 * @returns {object}
 */
export function blockMdastFromSegment(segment) {
  const nodes = segmentToMdast(segment)
  const block = nodes[0]
  if (!block || block.type === 'text') {
    return {
      type: 'paragraph',
      children: nodes.length ? nodes : [{ type: 'text', value: segment.raw || '' }]
    }
  }
  // Images are inline atoms — keep them inside a paragraph for PM/GFM.
  if (block.type === 'cat2bugDynamicImage') {
    return {
      type: 'paragraph',
      children: nodes
    }
  }
  return block
}
