/** 与后端 DefectDefaults 一致；独立模块避免 utils/defect.js 循环依赖导致导出未就绪 */

export const DEFECT_DEFAULT_TYPE = 'BUG'

export const DEFECT_DEFAULT_LEVEL = 'middle'

/** 提交前补全空的类型与优先级 */
export function normalizeDefectTypeAndLevel(defect) {
  if (!defect) {
    return
  }
  if (defect.defectType == null || defect.defectType === '') {
    defect.defectType = DEFECT_DEFAULT_TYPE
  }
  if (defect.defectLevel == null || String(defect.defectLevel).trim() === '') {
    defect.defectLevel = DEFECT_DEFAULT_LEVEL
  }
}
