/** 测试用例列表动态列（key 与旧版显示字段缓存一致） */
export const CaseTableColumnDefaults = [
  { key: 'id', prop: 'caseNum', fixed: true, visible: true, align: 'center', width: 100 },
  { key: 'case.name', prop: 'caseName', fixed: true, visible: true, align: 'left', minWidth: 200 },
  { key: 'module', prop: 'moduleName', fixed: false, visible: true, align: 'left', minWidth: 140 },
  { key: 'level', prop: 'caseLevel', fixed: false, visible: true, align: 'left', width: 100 },
  { key: 'preconditions', prop: 'casePreconditions', fixed: false, visible: true, align: 'left', minWidth: 250 },
  { key: 'step', prop: 'caseStep', fixed: false, visible: true, align: 'left', width: 300 },
  { key: 'data', prop: 'caseData', fixed: false, visible: true, align: 'left', minWidth: 250 },
  { key: 'expect', prop: 'caseExpect', fixed: false, visible: true, align: 'left', minWidth: 250 },
  { key: 'image', prop: 'imgUrls', fixed: false, visible: true, align: 'center', width: 120 },
  { key: 'annex', prop: 'annexUrls', fixed: false, visible: true, align: 'left', minWidth: 300 },
  { key: 'update-time', prop: 'updateTime', fixed: false, visible: true, align: 'left', width: 150 },
  { key: 'defect.state', prop: 'defectProcessingCount', fixed: false, visible: true, align: 'left', width: 150 }
]
