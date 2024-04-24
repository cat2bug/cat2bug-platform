export function MarkdownTools() {
  return [{
    icon: 'mk-weight',
    content: ''
  },{
    icon: 'mk-italic',
    content: ''
  },{
    icon: 'mk-title',
    content: ''
  },{
    type: 'siding'
  },{
    icon: 'md-variable',
    children: [{
      icon: 'mk-table',
      name: '缺陷表格',
      content: '\n$table{api.defect.list}\n',
    },{
      icon: 'md-variable2',
      name: '变量',
      content: '${api.project}'
    }]
  },{
    icon: 'mk-table',
    content: '\n|column1|column2|column3|\n' +
      '|-|-|-|\n' +
      '|content1|content2|content3|\n'
  },
  ]
}
