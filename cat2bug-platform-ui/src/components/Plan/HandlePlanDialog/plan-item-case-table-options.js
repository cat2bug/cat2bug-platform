/**
 * 测试计划「执行」抽屉-用例列表列配置（原 CaseList el-table），供 Cat2BugTable。
 */
export const PlanItemCaseTableOptions = [
  {
    key: 'id',
    prop: 'caseNum',
    fixed: true,
    visible: true,
    align: 'center',
    width: 100
  },
  {
    key: 'state',
    prop: 'planItemState',
    fixed: true,
    visible: true,
    align: 'center',
    width: 140
  },
  {
    key: 'title',
    prop: 'caseName',
    fixed: true,
    visible: true,
    align: 'left',
    minWidth: 300,
    className: 'defect-name-col'
  },
  {
    key: 'module',
    prop: 'moduleName',
    fixed: false,
    visible: true,
    align: 'left',
    minWidth: 150
  },
  {
    key: 'level',
    prop: 'caseLevel',
    fixed: false,
    visible: true,
    align: 'left',
    width: 120
  },
  {
    key: 'preconditions',
    prop: 'casePreconditions',
    fixed: false,
    visible: true,
    align: 'left',
    minWidth: 250,
    className: 'defect-name-col'
  },
  {
    key: 'step',
    prop: 'caseStep',
    fixed: false,
    visible: true,
    align: 'left',
    width: 300,
    className: 'plan-item-step-cell',
    sortable: true
  },
  {
    key: 'data',
    prop: 'caseData',
    fixed: false,
    visible: true,
    align: 'left',
    minWidth: 250,
    className: 'fixed-width'
  },
  {
    key: 'expect',
    prop: 'caseExpect',
    fixed: false,
    visible: true,
    align: 'left',
    minWidth: 250,
    className: 'defect-name-col'
  },
  {
    key: 'image',
    prop: 'imgUrls',
    fixed: false,
    visible: true,
    align: 'center',
    width: 120
  },
  {
    key: 'annex',
    prop: 'annexUrls',
    fixed: false,
    visible: true,
    align: 'left',
    minWidth: 300
  },
  {
    key: 'updateBy',
    prop: 'updateBy',
    fixed: false,
    visible: true,
    align: 'center',
    width: 130,
    sortable: true
  },
  {
    key: 'update-time',
    prop: 'updateTime',
    fixed: false,
    visible: true,
    align: 'center',
    width: 180,
    sortable: false
  }
]
