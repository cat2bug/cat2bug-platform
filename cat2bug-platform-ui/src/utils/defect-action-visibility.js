import { checkPermi } from '@/utils/permission'

/** 处理中 */
export const PROCESSING_STATE = 'PROCESSING'
/** 待审核 */
export const AUDIT_STATE = 'AUDIT'
/** 已解决（遗留态） */
export const RESOLVED_STATE = 'RESOLVED'
/** 已驳回 */
export const REJECTED_STATE = 'REJECTED'
/** 已关闭 */
export const CLOSE_STATE = 'CLOSED'

/** 批量候选动作（菜单展示顺序） */
export const BATCH_ACTIONS = [
  'assign',
  'repair',
  'reject',
  'pass',
  'close',
  'open',
  'delete',
]

/** 混合状态时仅允许的动作 */
export const MIXED_STATE_BATCH_ACTIONS = ['assign', 'close', 'delete']

export function isDefectDeleted(defect) {
  const f = defect && defect.delFlag
  return f === '2' || f === 2
}

function isCurrentHandler(defect, currentUserId) {
  return !!(defect && defect.handleBy && defect.handleBy.indexOf(currentUserId) !== -1)
}

function isCreator(defect, currentUserId) {
  return !!(defect && defect.createById && defect.createById == currentUserId)
}

/**
 * 与行内 DefectTools 一致的单条动作可见性（不含 exclusions 时由调用方传入）。
 * @param {string} action assign|repair|reject|pass|close|open|delete|edit|view|restore
 * @param {object} defect
 * @param {number|string} currentUserId
 * @param {string[]} [exclusions]
 */
export function isDefectActionVisible(action, defect, currentUserId, exclusions = []) {
  if (!defect || exclusions.includes(action)) return false
  const deleted = isDefectDeleted(defect)
  const state = defect.defectStateName

  switch (action) {
    case 'assign':
      return (
        !deleted &&
        state != CLOSE_STATE &&
        state != RESOLVED_STATE &&
        checkPermi(['system:defect:assign']) &&
        (isCreator(defect, currentUserId) || isCurrentHandler(defect, currentUserId))
      )
    case 'repair':
      return (
        !deleted &&
        (state == PROCESSING_STATE || state == REJECTED_STATE) &&
        checkPermi(['system:defect:repair']) &&
        isCurrentHandler(defect, currentUserId)
      )
    case 'reject':
      return (
        !deleted &&
        state == AUDIT_STATE &&
        checkPermi(['system:defect:reject']) &&
        isCurrentHandler(defect, currentUserId)
      )
    case 'pass':
      return (
        !deleted &&
        state == AUDIT_STATE &&
        checkPermi(['system:defect:pass']) &&
        isCurrentHandler(defect, currentUserId)
      )
    case 'close':
      return (
        !deleted &&
        state != CLOSE_STATE &&
        state != RESOLVED_STATE &&
        checkPermi(['system:defect:close'])
      )
    case 'open':
      return !deleted && state == CLOSE_STATE && checkPermi(['system:defect:open'])
    case 'delete':
      return (
        !deleted &&
        (isCreator(defect, currentUserId) || checkPermi(['system:defect:remove']))
      )
    case 'edit':
      return (
        !deleted &&
        (isCreator(defect, currentUserId) || checkPermi(['system:defect:edit']))
      )
    case 'view':
      return isCreator(defect, currentUserId) || checkPermi(['system:defect:query'])
    case 'restore':
      return (
        deleted &&
        (isCreator(defect, currentUserId) || checkPermi(['system:defect:remove']))
      )
    default:
      return false
  }
}

/**
 * 计算批量菜单各动作是否可用（严格：选中每一条都可做）。
 * @param {object[]} defects
 * @param {number|string} currentUserId
 * @returns {{ hasSelection: boolean, mixed: boolean, assign: boolean, repair: boolean, reject: boolean, pass: boolean, close: boolean, open: boolean, delete: boolean }}
 */
export function getBatchActionFlags(defects, currentUserId) {
  const empty = {
    hasSelection: false,
    mixed: false,
    assign: false,
    repair: false,
    reject: false,
    pass: false,
    close: false,
    open: false,
    delete: false,
  }
  if (!defects || !defects.length) return empty

  const states = new Set(defects.map((d) => d && d.defectStateName))
  const mixed = states.size > 1
  const candidates = new Set(mixed ? MIXED_STATE_BATCH_ACTIONS : BATCH_ACTIONS)

  const flags = { hasSelection: true, mixed }
  for (const action of BATCH_ACTIONS) {
    if (!candidates.has(action)) {
      flags[action] = false
    } else {
      flags[action] = defects.every((d) =>
        isDefectActionVisible(action, d, currentUserId)
      )
    }
  }
  return flags
}
