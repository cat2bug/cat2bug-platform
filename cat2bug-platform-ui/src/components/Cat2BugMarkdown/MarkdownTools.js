import {DefectTable} from "@/components/Cat2BugMarkdown/tools/DefectTable";
import LinkTool from "@/components/Cat2BugMarkdown/tools/LinkTool";
import AddImageLinkTool from "@/components/Cat2BugMarkdown/tools/AddImageLinkTool";
import UploadImageTool from "@/components/Cat2BugMarkdown/tools/UploadImageTool";

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
  },
    // TODO 段落功能没有实现
  //   {
  //   icon: 'mk-left',
  //   name: '居左',
  //   content: '::: hljs-left\n\n{0}\n\n:::'
  // },
  //   {
  //   icon: 'mk-center',
  //   name: '居中',
  //   content: '::: hljs-center\n\n{0}\n\n:::'
  // },{
  //   icon: 'mk-right',
  //   name: '居右',
  //   content: '::: hljs-right\n\n{0}\n\n:::'
  // },
    {
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
    type: 'siding'
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
      content: '${api.project.remark}'
    }]
  },{
    icon: 'mk-case',
    name: 'case',
    children: []
  },{
    icon: 'mk-bug',
    name: 'defect',
      children: [{
        icon: 'bug',
        name: 'defect.table',
        content: DefectTable(),
      }]
  },{
    type: 'siding'
  },{
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
