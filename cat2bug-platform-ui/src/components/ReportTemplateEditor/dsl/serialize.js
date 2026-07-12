import { formatTableColumns } from './grammar'

/**
 * @param {import('./parse').TemplateSegment[]} segments
 * @returns {string}
 */
export function serializeTemplate(segments) {
  if (!segments || segments.length === 0) {
    return ''
  }

  return segments.map(serializeSegment).join('')
}

/**
 * @param {import('./parse').TemplateSegment} segment
 */
function serializeSegment(segment) {
  switch (segment.type) {
    case 'text':
      return segment.value
    case 'variable':
      return `\${${segment.path}}`
    case 'planVariable':
      return `\${${segment.path}[${segment.planIndex}]}`
    case 'defectTable': {
      const colSpec = formatTableColumns(segment.columns)
      if (colSpec) {
        return `$table{${segment.dataSource}[${colSpec}]}`
      }
      return `$table{${segment.dataSource}}`
    }
    case 'caseCard':
      return `$card{${segment.dataSource}}`
    case 'dynamicImage': {
      const stylePart = segment.style ? `["style":"${segment.style}"]` : ''
      return `$image${stylePart}{${segment.path}}`
    }
    case 'raw':
      return segment.value
    default:
      return segment.raw || ''
  }
}

import { parseTemplate } from './parse'

/**
 * Round-trip helper for tests and editor save.
 * @param {string} content
 */
export function roundTripTemplate(content) {
  return serializeTemplate(parseTemplate(content))
}
