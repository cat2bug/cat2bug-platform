import {
  TABLE_TOKEN_RE,
  CARD_TOKEN_RE,
  IMAGE_TOKEN_RE,
  PLAN_VARIABLE_TOKEN_RE,
  VARIABLE_TOKEN_RE,
  DEFECT_TABLE_DEFAULT_COLUMNS,
  parseTableColumns
} from './grammar'
import { normalizeDslSource } from './normalize'
import { splitSourceWithRawBlocks } from './complex-blocks'

/**
 * @typedef {object} TextSegment
 * @property {'text'} type
 * @property {string} value
 */

/**
 * @typedef {object} VariableSegment
 * @property {'variable'} type
 * @property {string} path
 */

/**
 * @typedef {object} PlanVariableSegment
 * @property {'planVariable'} type
 * @property {string} path
 * @property {number} planIndex
 */

/**
 * @typedef {object} TableColumn
 * @property {string} field
 * @property {string|null} label
 */

/**
 * @typedef {object} DefectTableSegment
 * @property {'defectTable'} type
 * @property {string} dataSource
 * @property {TableColumn[]} columns
 * @property {string} raw
 */

/**
 * @typedef {object} CaseCardSegment
 * @property {'caseCard'} type
 * @property {string} dataSource
 * @property {string} raw
 */

/**
 * @typedef {object} DynamicImageSegment
 * @property {'dynamicImage'} type
 * @property {string} style
 * @property {string} path
 * @property {string} raw
 */

/**
 * @typedef {object} RawSegment
 * @property {'raw'} type
 * @property {string} value
 * @property {string} [kind]
 */

/** @typedef {TextSegment|VariableSegment|PlanVariableSegment|DefectTableSegment|CaseCardSegment|DynamicImageSegment|RawSegment} TemplateSegment */

const BLOCK_RES = [
  { type: 'defectTable', re: TABLE_TOKEN_RE, parse: parseTableToken },
  { type: 'caseCard', re: CARD_TOKEN_RE, parse: parseCardToken },
  { type: 'dynamicImage', re: IMAGE_TOKEN_RE, parse: parseImageToken }
]

const INLINE_RES = [
  { type: 'planVariable', re: PLAN_VARIABLE_TOKEN_RE, parse: parsePlanVariableToken },
  { type: 'variable', re: VARIABLE_TOKEN_RE, parse: parseVariableToken }
]

function parseTableToken(match) {
  const dataSource = match[1]
  let columns = parseTableColumns(match[2])
  if (!columns.length && dataSource === 'api.defect.list') {
    columns = DEFECT_TABLE_DEFAULT_COLUMNS.map((field) => ({ field, label: null }))
  }
  return {
    type: 'defectTable',
    dataSource,
    columns,
    raw: match[0]
  }
}

function parseCardToken(match) {
  return {
    type: 'caseCard',
    dataSource: match[1],
    raw: match[0]
  }
}

function parseImageToken(match) {
  const attrChunk = match[1]
  const path = match[2]
  let style = ''
  const styleMatch = attrChunk.match(/"style"\s*:\s*"([^"]*)"/)
  if (styleMatch) {
    style = styleMatch[1]
  }
  return {
    type: 'dynamicImage',
    style,
    path,
    raw: match[0]
  }
}

function parsePlanVariableToken(match) {
  return {
    type: 'planVariable',
    path: match[1],
    planIndex: parseInt(match[2], 10),
    raw: match[0]
  }
}

function parseVariableToken(match) {
  return {
    type: 'variable',
    path: match[1],
    raw: match[0]
  }
}

function tryMatchAt(src, pos, patterns) {
  const slice = src.slice(pos)
  for (const entry of patterns) {
    entry.re.lastIndex = 0
    const match = entry.re.exec(slice)
    if (match && match.index === 0) {
      return {
        segment: entry.parse(match),
        length: match[0].length
      }
    }
  }
  return null
}

/**
 * Parse template markdown into ordered segments.
 * @param {string} content
 * @returns {TemplateSegment[]}
 */
export function parseTemplate(content) {
  const source = normalizeDslSource(content)
  if (!source) {
    return []
  }

  /** @type {TemplateSegment[]} */
  const segments = []
  let pos = 0
  let textStart = 0

  while (pos < source.length) {
    const atLineStart = pos === 0 || source[pos - 1] === '\n'
    const patterns = atLineStart ? [...BLOCK_RES, ...INLINE_RES] : INLINE_RES
    const hit = tryMatchAt(source, pos, patterns)

    if (hit) {
      if (pos > textStart) {
        segments.push({ type: 'text', value: source.slice(textStart, pos) })
      }
      segments.push(hit.segment)
      pos += hit.length
      textStart = pos
      continue
    }

    pos += 1
  }

  if (textStart < source.length) {
    segments.push({ type: 'text', value: source.slice(textStart) })
  }

  return expandRawPreservedSegments(segments)
}

/**
 * @param {TemplateSegment[]} segments
 * @returns {TemplateSegment[]}
 */
function expandRawPreservedSegments(segments) {
  /** @type {TemplateSegment[]} */
  const expanded = []

  for (const segment of segments) {
    if (segment.type !== 'text') {
      expanded.push(segment)
      continue
    }

    const parts = splitSourceWithRawBlocks(segment.value)
    if (parts.length === 1 && parts[0].type === 'markdown') {
      expanded.push(segment)
      continue
    }

    for (const part of parts) {
      if (part.type === 'raw') {
        expanded.push({
          type: 'raw',
          value: part.value,
          kind: part.kind
        })
        continue
      }
      if (!part.value) {
        continue
      }
      expanded.push(...parseTemplate(part.value))
    }
  }

  return expanded
}
