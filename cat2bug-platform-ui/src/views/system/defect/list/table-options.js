export const TableOptions=[{
  key: 'id',
  prop: 'projectNum',
  fixed: false,
  visible: true,
  align: 'center',
  width: 100,
  width_ja_JP: 100,
  width_ar: 120,
  width_ru: 180
},{
  key: 'type',
  prop: 'defectTypeName',
  fixed: false,
  visible: true,
  align: 'center',
  width: 100,
  width_ja_JP: 120,
  width_ar: 120
},{
  key: 'defect.name',
  prop: 'defectName',
  fixed: false,
  visible: true,
  required: true,
  width: 400,
  width_ru: 500,
  /** 列表格内展示标题中的换行（见 table.vue .defect-name-col） */
  className: 'defect-name-col',
},{
  key: 'handle-by',
  prop: 'handleBy',
  fixed: false,
  visible: true,
  required: true,
  width: 170,
  width_ja_JP: 170,
  width_ar: 170,
  className: 'defect-handle-by-col',
},{
  key: 'priority',
  prop: 'defectLevel',
  fixed: false,
  visible: true,
  align: 'center',
  width: 120,
  width_ja_JP: 140,
  width_ar: 140,
  width_ru: 140
},{
  key: 'state',
  prop: 'defectState',
  fixed: false,
  visible: true,
  align: 'center',
  width: 100,
  width_en_US: 120,
  width_ja_JP: 140,
  width_ar: 140,
  width_ru: 160,
  className: 'defect-state-col',
},{
  key: 'module',
  prop: 'moduleName',
  fixed: false,
  visible: true,
  width: 200,
  width_ja_JP: 200,
  width_ar: 200
},{
  key: 'version',
  prop: 'moduleVersion',
  fixed: false,
  visible: true,
  width: 130,
  width_ja_JP: 130,
  width_ar: 130
},{
  key: 'case',
  prop: 'caseId',
  fixed: false,
  visible: true,
  showInColumnPicker: true,
  width: 180,
  sortable: false
},{
  key: 'describe',
  prop: 'defectDescribe',
  fixed: false,
  visible: true,
  align: 'left',
  width: 200,
  width_ja_JP: 200,
  width_ar: 200,
  /** 与缺陷名称列一致：单元格内换行与 .cell 样式（见 table.vue .defect-name-col） */
  className: 'defect-name-col',
},{
  key: 'image',
  prop: 'imgUrls',
  fixed: false,
  visible: true,
  align: 'center',
  width: 160,
  width_ja_JP: 160,
  width_ar: 160,
  sortable: false
},{
  key: 'annex',
  prop: 'annexUrls',
  fixed: false,
  visible: true,
  width: 400,
  sortable: false
},{
  key: 'update-time',
  prop: 'updateTime',
  fixed: false,
  visible: true,
  width: 170,
  width_ja_JP: 170,
  width_ar: 170,
  width_ru: 200
},{
  key: 'plan-start-time',
  prop: 'planStartTime',
  fixed: false,
  visible: true,
  width: 170,
  width_ja_JP: 170,
  width_ar: 170,
  width_ru: 250
},{
  key: 'plan-end-time',
  prop: 'planEndTime',
  fixed: false,
  visible: true,
  width: 170,
  width_ja_JP: 170,
  width_ar: 170,
  width_ru: 260
},{
  key: 'createBy',
  prop: 'createMember',
  fixed: false,
  visible: true,
  width: 170,
  width_ja_JP: 170,
  width_ar: 170
}]
