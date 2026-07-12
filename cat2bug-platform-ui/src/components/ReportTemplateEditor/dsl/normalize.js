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
  return result
}
