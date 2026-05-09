/**
 * 测试计划添加/编辑弹窗 — 可选用例列表列配置（原 AddPlanDialog el-table），供 Cat2BugTable。
 * sortable: true 表示表头排序为前端当前页排序，与原先 el-table-column sortable 行为一致。
 */
export const AddPlanDialogCaseTableOptions = [
  {
    key: 'id',
    prop: 'caseNum',
    fixed: true,
    visible: true,
    align: 'left',
    width: 100,
    sortable: true
  },
  {
    key: 'title',
    prop: 'caseName',
    fixed: true,
    visible: true,
    align: 'left',
    minWidth: 200,
    className: 'defect-name-col',
    sortable: true
  },
  {
    key: 'module',
    prop: 'moduleName',
    fixed: false,
    visible: true,
    align: 'left',
    minWidth: 150,
    sortable: true
  },
  {
    key: 'level',
    prop: 'caseLevel',
    fixed: false,
    visible: true,
    align: 'left',
    width: 120,
    sortable: true
  },
  {
    key: 'preconditions',
    prop: 'casePreconditions',
    fixed: false,
    visible: true,
    align: 'left',
    minWidth: 250,
    className: 'defect-name-col',
    sortable: true
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
    className: 'fixed-width',
    sortable: true
  },
  {
    key: 'expect',
    prop: 'caseExpect',
    fixed: false,
    visible: true,
    align: 'left',
    minWidth: 250,
    className: 'defect-name-col',
    sortable: true
  },
  {
    key: 'image',
    prop: 'imgUrls',
    fixed: false,
    visible: true,
    align: 'center',
    width: 150,
    sortable: false
  },
  {
    key: 'annex',
    prop: 'annexUrls',
    fixed: false,
    visible: true,
    align: 'left',
    minWidth: 300,
    sortable: false
  }
]
