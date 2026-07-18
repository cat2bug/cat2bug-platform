import { PLAN_VARIABLE_TOKEN_RE, VARIABLE_TOKEN_RE } from '../dsl/grammar'
import { mountInlineVariableUnit } from './mount-inline-variable-unit'

const COMBINED_VAR_RE = /\$\{(?:(?:\w|\.)+)(?:\[(\d+)\])?\}/g

/**
 * Split a markdown table line into cells (tolerates missing leading/trailing pipes).
 * @param {string} line
 * @returns {string[]}
 */
export function splitPipeTableCells(line) {
  let text = String(line || '')
  if (text.startsWith('|')) {
    text = text.slice(1)
  }
  if (text.endsWith('|')) {
    text = text.slice(0, -1)
  }
  return text.split('|').map((cell) => cell.trim())
}

/**
 * @param {string} line
 */
export function isPipeSeparatorRow(line) {
  const cells = splitPipeTableCells(line)
  if (!cells.length) {
    return false
  }
  return cells.every((cell) => !cell || /^:?-+:?$/.test(cell))
}

/**
 * @param {string} source
 * @returns {string[][]}
 */
export function parsePipeTableRows(source) {
  const lines = String(source || '')
    .split(/\r?\n/)
    .map((line) => line.trimEnd())
    .filter((line) => line.trim().length > 0)

  /** @type {string[][]} */
  const rows = []
  for (const line of lines) {
    if (isPipeSeparatorRow(line)) {
      continue
    }
    // Keep rows that look like table rows (contain |) even if missing leading |
    if (!line.includes('|')) {
      continue
    }
    rows.push(splitPipeTableCells(line))
  }
  return rows
}

/**
 * @typedef {{ type: 'text', value: string }|{ type: 'variable', path: string, raw: string }|{ type: 'planVariable', path: string, planIndex: number, raw: string }} CellToken
 */

/**
 * @param {string} cell
 * @returns {CellToken[]}
 */
export function tokenizeTableCell(cell) {
  const text = String(cell || '')
  /** @type {CellToken[]} */
  const tokens = []
  let last = 0
  COMBINED_VAR_RE.lastIndex = 0
  let match
  while ((match = COMBINED_VAR_RE.exec(text)) != null) {
    if (match.index > last) {
      tokens.push({ type: 'text', value: text.slice(last, match.index) })
    }
    const raw = match[0]
    const planMatch = raw.match(PLAN_VARIABLE_TOKEN_RE)
    if (planMatch) {
      tokens.push({
        type: 'planVariable',
        path: planMatch[1],
        planIndex: Number(planMatch[2]) || 1,
        raw
      })
    } else {
      const varMatch = raw.match(VARIABLE_TOKEN_RE)
      if (varMatch) {
        tokens.push({
          type: 'variable',
          path: varMatch[1],
          raw
        })
      } else {
        tokens.push({ type: 'text', value: raw })
      }
    }
    last = match.index + raw.length
  }
  if (last < text.length) {
    tokens.push({ type: 'text', value: text.slice(last) })
  }
  if (!tokens.length) {
    tokens.push({ type: 'text', value: '' })
  }
  return tokens
}

/**
 * Classify a cell for visual styling: label (plain title text) vs value (has variables) vs empty.
 * @param {CellToken[]} tokens
 * @returns {'label'|'value'|'empty'}
 */
export function classifyPlanStatisticsCell(tokens) {
  const list = Array.isArray(tokens) ? tokens : []
  const hasVariable = list.some((token) => token.type === 'variable' || token.type === 'planVariable')
  if (hasVariable) {
    return 'value'
  }
  const text = list
    .filter((token) => token.type === 'text')
    .map((token) => token.value || '')
    .join('')
    .trim()
  if (!text) {
    return 'empty'
  }
  return 'label'
}

/**
 * Dispose interactive variable units mounted under a plan-statistics preview root.
 * @param {HTMLElement|null|undefined} root
 */
export function disposePlanStatisticsPreview(root) {
  if (!root || typeof root.__disposePlanStats !== 'function') {
    return
  }
  try {
    root.__disposePlanStats()
  } finally {
    root.__disposePlanStats = null
  }
}

/**
 * Render plan-statistics preserved markdown as an HTML table.
 * Cell `${}` tokens are interactive variable units (hover chrome + 文/${} toggle).
 * @param {string} source
 * @returns {HTMLElement}
 */
export function renderPreviewPlanStatistics(source) {
  const wrap = document.createElement('div')
  wrap.className = 'cat2bug-dsl-preview-plan-stats'

  /** @type {Array<() => void>} */
  const disposers = []
  wrap.__disposePlanStats = () => {
    while (disposers.length) {
      const dispose = disposers.pop()
      try {
        dispose()
      } catch (_e) {
        // ignore dispose errors
      }
    }
  }

  const rows = parsePipeTableRows(source)
  if (!rows.length) {
    const hint = document.createElement('div')
    hint.className = 'cat2bug-dsl-preview-plan-stats__hint'
    hint.textContent = '无法解析计划统计表'
    wrap.append(hint)
    return wrap
  }

  const scroll = document.createElement('div')
  scroll.className = 'cat2bug-dsl-preview-plan-stats__scroll'

  const table = document.createElement('table')
  table.className = 'cat2bug-dsl-preview-plan-stats__table'

  const tbody = document.createElement('tbody')
  rows.forEach((cells) => {
    const tr = document.createElement('tr')
    cells.forEach((cell) => {
      const td = document.createElement('td')
      const tokens = tokenizeTableCell(cell)
      const kind = classifyPlanStatisticsCell(tokens)
      td.className = `cat2bug-dsl-preview-plan-stats__cell cat2bug-dsl-preview-plan-stats__cell--${kind}`
      tokens.forEach((token) => {
        if (token.type === 'text') {
          if (token.value) {
            td.appendChild(document.createTextNode(token.value))
          }
          return
        }
        const unit = mountInlineVariableUnit({
          path: token.path,
          raw: token.raw,
          planIndex: token.type === 'planVariable' ? token.planIndex : undefined,
          isPlan: token.type === 'planVariable'
        })
        disposers.push(unit.destroy)
        td.appendChild(unit.dom)
      })
      tr.appendChild(td)
    })
    tbody.appendChild(tr)
  })

  table.appendChild(tbody)
  scroll.appendChild(table)
  wrap.appendChild(scroll)
  return wrap
}
