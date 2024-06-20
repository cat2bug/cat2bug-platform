import LinkTool from "@/components/Cat2BugMarkdown/components/LinkTool";
import AddImageLinkTool from "@/components/Cat2BugMarkdown/components/AddImageLinkTool";
import UploadImageTool from "@/components/Cat2BugMarkdown/components/UploadImageTool";
import ShopTool from "@/components/Cat2BugMarkdown/components/ShopTool";

export function MarkdownTools() {
  return [{
    icon: 'mk-weight',
    name: 'font-weight',
    content: '**{0}**'
  },{
    icon: 'mk-italic',
    name: 'font-italic',
    content: '*{0}*'
  },{
    icon: 'mk-title',
    name: 'title',
    children: [{
      name: 'title1',
      content: '# {0}'
    },{
      name: 'title2',
      content: '## {0}'
    },{
      name: 'title3',
      content: '### {0}'
    },{
      name: 'title4',
      content: '#### {0}'
    },{
      name: 'title5',
      content: '##### {0}'
    },]
  },{
    icon: 'mk-underline',
    name: 'font-underline',
    content: '++{0}++'
  },{
    icon: 'mk-strikethrough',
    name: 'font-strikethrough',
    content: '~~{0}~~'
  },{
    icon: 'mk-flag',
    name: 'flag',
    content: '=={0}=='
  },{
    icon: 'mk-superscript',
    name: 'font-superscript',
    content: '^{0}^'
  },{
    icon: 'mk-subscript',
    name: 'font-subscript',
    content: '~{0}~'
  },{
    icon: 'mk-left',
    name: 'font-left',
    content: '\n:-- {0}\n'
  },
    {
    icon: 'mk-center',
    name: 'font-center',
    content: '\n-:- {0}\n'
  },{
    icon: 'mk-right',
    name: 'font-right',
    content: '\n--: {0}\n'
  },{
    icon: 'mk-paragraph',
    name: 'paragraph',
    content: '> {0}'
  },{
    icon: 'mk-ordered-list',
    name: 'ordered-list',
    content: '1. {0}'
  },{
    icon: 'mk-unordered-list',
    name: 'unordered-list',
    content: '- {0}'
  },{
    type: 'siding'
  },{
    icon: 'mk-link',
    name: 'link',
    type: 'component',
    content: LinkTool
  },{
    icon: 'mk-image',
    name: 'image',
    children: [{
      icon: 'mk-image-link',
      name: 'mk.add-image-link',
      type: 'component',
      content: AddImageLinkTool
    },{
      icon: 'mk-image-upload',
      name: 'upload-image',
      type: 'component',
      content: UploadImageTool
    }],
  },{
    icon: 'mk-code',
    name: 'code',
    content: '```language\n{0}\n```'
  },{
    icon: 'mk-table',
    name: 'table',
    content: '\n|column1|column2|column3|\n' +
      '|-|-|-|\n' +
      '|content1|content2|content3|\n'
  },{
    type: 'siding',
    name: 'siding-9899'
  },{
    icon: 'mk-project',
    name: 'project',
    children: [{
      icon: 'mk-name',
      name: 'project.name',
      content: '${api.project.projectName}'
    },{
      icon: 'mk-icon',
      name: 'project.icon',
      content: '$image["style":"width:100px;height:100px;"]{api.project.projectIcon}'
    },{
      icon: 'md-create-time',
      name: 'create-time',
      content: '${api.project.createTime}'
    },{
      icon: 'md-update-time',
      name: 'update-time',
      content: '${api.project.updateTime}'
    },{
      icon: 'mk-remark',
      name: 'describe',
      content: '${api.project.projectIntroduce}'
    }]
  },{
    icon: 'mk-case',
    name: 'case',
    children: [{
      icon: 'mk-total',
      name: 'case.total',
      content: '${api.case.total}'
    },{
      type: 'siding'
    },
    //   {
    //   icon: 'mk-table',
    //   name: 'case.table',
    //   content: '$table{api.case.list}'
    // },
      {
      icon: 'mk-card',
      name: 'case.card',
      content: '$card{api.case.list}'
    }]
  },{
    icon: 'mk-bug',
    name: 'defect',
      children: [{
        icon: 'bug',
        name: 'defect.table',
        content: '$table{api.defect.list[projectNum,defectTypeName,defectLevel,defectName,defectStateName,moduleName,moduleVersion,defectDescribe,createBy,createTime,updateBy,updateTime,imgList,annexList]}'
      }]
  },
    // {
    //   icon: 'mk-member',
    //   name: 'member',
    //   children: [{
    //     icon: 'mk-table',
    //     name: 'project.member-manage',
    //     content: '$table{api.member.all}'
    //   },{
    //     type: 'siding'
    //   },{
    //     icon: 'mk-table',
    //     name: 'project.member-manage',
    //     content: '$table{api.member.all}'
    //   },{
    //     icon: 'mk-table',
    //     name: 'project.admin',
    //     content: '$table{api.member.admin}'
    //   },{
    //     icon: 'mk-table',
    //     name: 'project.develop',
    //     content: '$table{api.member.develop}'
    //   },{
    //     icon: 'mk-table',
    //     name: 'project.tester',
    //     content: '$table{api.member.tester}'
    //   },{
    //     icon: 'mk-table',
    //     name: 'project.outsider',
    //     content: '$table{api.member.outsider}'
    //   }]
    // },
    {
    type: 'siding'
  },
  //   {
  //   icon: 'mk-shop',
  //   name: 'shop',
  //   type: 'component',
  //   content: ShopTool
  // },
    {
    icon: 'mk-clear',
    name: 'clear',
    type: 'command',
    content: function (view, tool) {
      view.clear();
    }
  },{
    icon: 'mk-save',
    name: 'save',
    type: 'command',
    content: function (view, tool) {
      view.save();
    }
  }]
}
