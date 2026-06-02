import { checkPermi } from '@/utils/permission'

/** 项目设置卡片：任一权限命中即显示对应入口 */
export function hasAnyPermi(perms) {
  if (!perms || !perms.length) return false
  return perms.some(p => checkPermi([p]))
}

/** 缺陷属性管理（团队/项目创建人、管理员） */
export const DEFECT_FIELD_MANAGE_PERMS = [
  'system:project:defect-field:list',
  'system:project:defect-field:add',
  'system:project:defect-field:edit',
  'system:project:defect-field:remove'
]

export function canManageDefectFields() {
  return hasAnyPermi(DEFECT_FIELD_MANAGE_PERMS)
}
