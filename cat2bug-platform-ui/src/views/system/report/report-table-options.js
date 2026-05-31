/** 报告列表动态列 */
export const ReportTableColumnDefaults = [
  { key: 'report.type', prop: 'reportKey', fixed: false, visible: true, align: 'center', width: 100, sortable: false },
  { key: 'report.title', prop: 'reportTitle', fixed: false, visible: true, align: 'left', minWidth: 200 },
  { key: 'report.time', prop: 'reportTime', fixed: false, visible: true, align: 'center', width: 180 },
  { key: 'report.source', prop: 'reportSource', fixed: false, visible: true, align: 'center', width: 200 },
  { key: 'pusher', prop: 'createBy', fixed: false, visible: true, align: 'center', width: 150 }
]
