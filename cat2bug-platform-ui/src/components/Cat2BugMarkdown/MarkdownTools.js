export function MarkdownTools() {
  return [{
    icon: 'mk-weight',
    name: '粗体',
    content: ''
  },{
    icon: 'mk-italic',
    name: '斜体',
    content: ''
  },{
    icon: 'mk-title',
    name: '标题1',
    content: ''
  },{
    type: 'siding'
  },{
    icon: 'md-variable',
    name: '变量',
    children: [{
        icon: 'list2',
        name: '项目',
      children: [{
        icon: 'mk-name',
        name: '项目名称',
        content: '${api.project.projectName}'
      },{
        icon: 'md-create-time',
        name: '项目创建日期',
        content: '${api.project.createTime}'
      },{
        icon: 'md-update-time',
        name: '项目更新日期',
        content: '${api.project.updateTime}'
      },{
        icon: 'mk-remark',
        name: '项目备注',
        content: '${api.project.remark}'
      }]},{
        icon: 'bug',
        name: '缺陷',
        children: [{
          icon: 'mk-table',
          name: '缺陷表格',
          content: '$table{api.defect.list}',
        }],
      }]
  },{
    icon: 'mk-table',
    name: '表格',
    content: '\n|column1|column2|column3|\n' +
      '|-|-|-|\n' +
      '|content1|content2|content3|\n'
  },
  ]
}
