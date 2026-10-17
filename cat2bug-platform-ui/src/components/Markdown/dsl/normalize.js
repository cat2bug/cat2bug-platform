import { normalizePlanStatisticsTablesInSource } from './complex-blocks'
import { stripMilkdownTableCellBreaks } from './normalize-pipe-table'

/**
 * Strip markdown / editor escape sequences inside DSL source.
 * md-editor-v3 may persist `\$table` and `\[` in the source pane.
 */
export function unescapeDslEscapes(content) {
  if (!content) {
    return ''
  }
  let result = content
  result = result.replace(/\\\$/g, '$')
  result = result.replace(/\\([\[\]{}])/g, '$1')
  return result
}

/**
 * Repair $image["style":"..."]{path} when the closing ] before { is missing.
 * e.g. $image["style":"width:100px;"{api.project.projectIcon}
 */
export function repairImageTokenBrackets(content) {
  if (!content) {
    return ''
  }
  return content.replace(
    /(\$(?:img|image)(?:\[[^\]]*"[^"]*")+)(\{[\w.]+\})/g,
    (full, attrPart, pathPart) => {
      if (attrPart.endsWith(']')) {
        return full
      }
      return `${attrPart}]${pathPart}`
    }
  )
}

/**
 * Collapse soft line breaks inside Cat2Bug DSL block tokens.
 * md-editor / manual edits may wrap long tokens across lines:
 *   $table{api.defect.list\
 *   [projectNum,defectTypeName,...]}
 */
export function normalizeDslSource(content) {
  if (!content) {
    return ''
  }

  let result = unescapeDslEscapes(content)
  result = repairImageTokenBrackets(result)
  // `$image[...] {path}` (stray space) → `$image[...]{path}`
  result = result.replace(/(\$(?:img|image)(?:\[[^\]]*\])*)\s+(\{[\w.]+\})/g, '$1$2')
  // Stray backslash before column brackets after line-wrap edits: list\[] -> list[]
  result = result.replace(/(\$table\{[^}\r\n]*?)\\(?=[\[\]])/g, '$1')
  // Backslash continuation (common in code editors)
  result = result.replace(/\\\r?\n\s*/g, '')
  // $table{api...} newline [columns]}
  result = result.replace(
    /(\$table\{[^}\r\n]*)\r?\n\s*(\[[^\]]*\]\})/g,
    '$1$2'
  )
  // $card{...} / $image{...} split before closing brace
  result = result.replace(
    /(\$(?:card|image|img)(?:\[[^\]]*\])*\{[^}\r\n]*)\r?\n\s*(\})/g,
    '$1$2'
  )
  // Legacy plan-statistics mega-table → GFM so Crepe/Milkdown tables stay editable
  result = normalizePlanStatisticsTablesInSource(result)
  // Milkdown serializes empty cell paragraphs as <br />; strip to keep GFM `||` stable
  result = stripMilkdownTableCellBreaks(result)
  return result
}
