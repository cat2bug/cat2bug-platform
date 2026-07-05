/** 文档列表动态列（首列为图标，不参与「显示字段」勾选） */
export const DocumentTableColumnDefaults = [
  { key: 'doc.name', prop: 'docName', fixed: false, visible: true, align: 'left', flex: true, minWidth: 200, className: 'doc-name-col' },
  { key: 'doc.type', prop: 'fileExtension', fixed: false, visible: true, align: 'center', width: 100 },
  { key: 'updateBy', prop: 'updateByName', fixed: false, visible: true, align: 'center', width: 100 },
  { key: 'update-time', prop: 'updateTime', fixed: false, visible: true, align: 'center', width: 120 }
]
