/**
 * Session-scoped expand state for report-template variable NodeViews.
 * expanded=true → content mode (resolved value); false → variable mode (raw token).
 * Toolbar setAll clears per-unit overrides via generation bump.
 */

/** @typedef {'content'|'variable'} VariableDisplayMode */

let variablesExpanded = true
let generation = 0
/** @type {Map<symbol, VariableDisplayMode>} */
const overrides = new Map()
/** @type {Set<(snapshot: { expanded: boolean, generation: number }) => void>} */
const listeners = new Set()

function notify() {
  const snapshot = { expanded: variablesExpanded, generation }
  listeners.forEach((fn) => {
    try {
      fn(snapshot)
    } catch (_e) {
      // ignore subscriber errors
    }
  })
}

export function getVariablesExpanded() {
  return variablesExpanded
}

export function getVariableExpandGeneration() {
  return generation
}

/**
 * Create a stable handle for a NodeView instance (local override key).
 * @returns {symbol}
 */
export function createVariableUnitKey() {
  return Symbol('report-template-variable-unit')
}

/**
 * @param {symbol} unitKey
 * @returns {VariableDisplayMode}
 */
export function getVariableDisplayMode(unitKey) {
  if (unitKey && overrides.has(unitKey)) {
    return overrides.get(unitKey)
  }
  return variablesExpanded ? 'content' : 'variable'
}

/**
 * Local override until next setVariablesExpanded / resetAllVariableOverrides.
 * @param {symbol} unitKey
 * @param {VariableDisplayMode} mode
 */
export function setVariableDisplayModeOverride(unitKey, mode) {
  if (!unitKey) return
  if (mode !== 'content' && mode !== 'variable') return
  overrides.set(unitKey, mode)
  notify()
}

/**
 * Toggle local override between content and variable.
 * @param {symbol} unitKey
 * @returns {VariableDisplayMode}
 */
export function toggleVariableDisplayMode(unitKey) {
  const next = getVariableDisplayMode(unitKey) === 'content' ? 'variable' : 'content'
  setVariableDisplayModeOverride(unitKey, next)
  return next
}

export function clearVariableUnitOverride(unitKey) {
  if (unitKey && overrides.has(unitKey)) {
    overrides.delete(unitKey)
  }
}

/**
 * Toolbar: set global expanded flag and clear all local overrides.
 * @param {boolean} expanded
 */
export function setVariablesExpanded(expanded) {
  variablesExpanded = !!expanded
  generation += 1
  overrides.clear()
  notify()
}

export function resetAllVariableOverrides() {
  generation += 1
  overrides.clear()
  notify()
}

/**
 * @param {(snapshot: { expanded: boolean, generation: number }) => void} fn
 * @returns {() => void}
 */
export function subscribeVariableExpandState(fn) {
  if (typeof fn !== 'function') {
    return () => {}
  }
  listeners.add(fn)
  return () => {
    listeners.delete(fn)
  }
}

/** Test helper — reset module state between cases. */
export function __resetVariableExpandStateForTests() {
  variablesExpanded = true
  generation = 0
  overrides.clear()
  listeners.clear()
}
