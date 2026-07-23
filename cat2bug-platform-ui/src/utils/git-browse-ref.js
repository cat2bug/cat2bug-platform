/**
 * Pick tree/API ref: explicit ?ref= (branch/tag/SHA), else ?branch=, else default branch.
 *
 * @param {unknown} queryRef
 * @param {unknown} queryBranch
 * @param {Array<{ name?: string, default?: boolean, isDefault?: boolean }>|null|undefined} branches
 * @returns {string|null}
 */
export function resolveBrowseCurrentRef(queryRef, queryBranch, branches) {
  const qRef = queryRef != null && String(queryRef).trim() !== '' ? String(queryRef).trim() : ''
  if (qRef) return qRef
  const list = Array.isArray(branches) ? branches : []
  const names = list.map(b => b && b.name).filter(Boolean)
  const qBranch = queryBranch != null && String(queryBranch).trim() !== ''
    ? String(queryBranch).trim()
    : ''
  if (qBranch && names.includes(qBranch)) return qBranch
  return pickDefaultBranchName(list)
}

/**
 * @param {Array<{ name?: string, default?: boolean, isDefault?: boolean }>} list
 * @returns {string|null}
 */
export function pickDefaultBranchName(list) {
  const branches = Array.isArray(list) ? list : []
  const current = branches.find(b => b && (b.default === true || b.isDefault === true)) || branches[0]
  return current && current.name ? String(current.name) : null
}

/**
 * Pick the browse page git ref after branches load.
 * Prefer explicit query.ref (branch, tag, or commit SHA); otherwise default branch.
 *
 * @param {unknown} queryRef
 * @param {Array<{ name?: string, default?: boolean, isDefault?: boolean }>|null|undefined} branches
 * @returns {string|null}
 */
export function pickBrowseRef(queryRef, branches) {
  return resolveBrowseCurrentRef(queryRef, null, branches)
}

/**
 * When currentRef is not a known branch (e.g. commit SHA), return short label metadata.
 *
 * @param {string|null|undefined} currentRef
 * @param {Array<{ name?: string }>|null|undefined} branches
 * @returns {{ value: string, label: string }|null}
 */
export function detachedBrowseRefOption(currentRef, branches) {
  if (currentRef == null || String(currentRef).trim() === '') return null
  const ref = String(currentRef)
  const names = (Array.isArray(branches) ? branches : [])
    .map(b => b && b.name)
    .filter(Boolean)
  if (names.includes(ref)) return null
  const label = /^[0-9a-f]{7,40}$/i.test(ref) && ref.length > 7 ? ref.slice(0, 7) : ref
  return { value: ref, label }
}

/**
 * Branch name shown in the branch select while tree may be pinned to a commit (?ref=SHA).
 * Prefer ?branch= when it is a real branch; else default branch.
 *
 * @param {unknown} queryBranch
 * @param {Array<{ name?: string, default?: boolean, isDefault?: boolean }>|null|undefined} branches
 * @returns {string|null}
 */
export function pickBrowseDisplayBranch(queryBranch, branches) {
  const list = Array.isArray(branches) ? branches : []
  const names = list.map(b => b && b.name).filter(Boolean)
  const qBranch = queryBranch != null && String(queryBranch).trim() !== ''
    ? String(queryBranch).trim()
    : ''
  if (qBranch && names.includes(qBranch)) return qBranch
  return pickDefaultBranchName(list)
}

/**
 * Resolve which branch to keep while browsing a commit / history.
 * Tries explicit candidates first, then default / first branch.
 *
 * @param {Array<unknown>} candidates
 * @param {Array<{ name?: string, default?: boolean, isDefault?: boolean }>|null|undefined} branches
 * @returns {string|null}
 */
export function resolveBrowseBranchName(candidates, branches) {
  const list = Array.isArray(branches) ? branches : []
  const names = list.map(b => b && b.name).filter(Boolean)
  const tried = Array.isArray(candidates) ? candidates : []
  for (let i = 0; i < tried.length; i++) {
    const c = tried[i]
    if (c == null || String(c).trim() === '') continue
    const name = String(c).trim()
    if (names.includes(name)) return name
  }
  // Branch list not loaded yet — still keep an explicit candidate in the URL
  if (!names.length) {
    for (let i = 0; i < tried.length; i++) {
      const c = tried[i]
      if (c == null || String(c).trim() === '') continue
      return String(c).trim()
    }
  }
  return pickDefaultBranchName(list)
}

/**
 * Apply route query + branch list → { currentRef, selectedBranchName }.
 * Never lets a missing ?ref= wipe ?branch= back to the default branch.
 *
 * @param {{ ref?: unknown, branch?: unknown }} query
 * @param {Array<{ name?: string, default?: boolean, isDefault?: boolean }>|null|undefined} branches
 * @param {string|null|undefined} fallbackBranch remembered branch from component state
 * @returns {{ currentRef: string|null, selectedBranchName: string|null }}
 */
export function applyBrowseRefState(query, branches, fallbackBranch) {
  const q = query || {}
  const qRef = q.ref
  const qBranch = q.branch != null && String(q.branch).trim() !== ''
    ? String(q.branch).trim()
    : (fallbackBranch || '')
  const currentRef = resolveBrowseCurrentRef(qRef, qBranch, branches)
  const selectedBranchName = detachedBrowseRefOption(currentRef, branches)
    ? resolveBrowseBranchName([qBranch, fallbackBranch], branches)
    : currentRef
  return { currentRef, selectedBranchName }
}
