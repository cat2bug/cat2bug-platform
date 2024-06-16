<template>
  <div class="markdown">
    <div class="markdown-tools">
      <tools-menu :tools="tools" @select="toolsHandle" />
      <tools-menu v-model="viewTools" @select="viewToolsHandle" />
    </div>
    <multipane layout="vertical" ref="multiPane" class="markdown-body" @pane-resize-start="dragStopHandle">
      <textarea
        class="markdown-body-edit"
        :mode="viewVisible?'':'single'"
        ref="markdownEdit"
        v-resize="setDragComponentSize"
        v-model="markdownContent"
        @input="inputHandle"
        :placeholder="placeholder"
      />
      <multipane-resizer v-show="viewVisible" class="markdown-body-resizer" :style="multipaneStyle"></multipane-resizer>
      <div
        v-show="viewVisible"
        class="markdown-body-view"
        v-resize="setDragComponentSize"
        ref="markdownViewParent">
        <markdown-it-vue
          ref="markdownView"
          :content="markdownContent"
          :options="options"
          />
      </div>
    </multipane>
    <keep-alive>
      <component :is="currentToolView" ref="toolView"></component>
    </keep-alive>
  </div>
</template>

<script>
import { Multipane, MultipaneResizer } from 'vue-multipane';
import MarkdownItVue from "markdown-it-vue"
import { TablePlugin } from "./plugins/TablePlugin"
import { CardPlugin } from "@/components/Cat2BugMarkdown/plugins/CardPlugin";
import {CaseCardPlugin} from "@/components/Cat2BugMarkdown/plugins/CaseCardPlugin";
import {VariablePlugin} from "./plugins/VariablePlugin"
import {CenterPointPlugin} from "@/components/Cat2BugMarkdown/plugins/CenterPointPlugin";
import {LeftPointPlugin} from "@/components/Cat2BugMarkdown/plugins/LeftPointPlugin";
import {RightPointPlugin} from "@/components/Cat2BugMarkdown/plugins/RightPointPlugin";
import { MarkdownTools } from "./MarkdownTools"
import 'markdown-it-vue/dist/markdown-it-vue.css'
import {listDefect} from "@/api/system/defect";
import {listCase} from "@/api/system/case";
import {getProject} from "@/api/system/project";
import ToolsMenu from "@/components/Cat2BugMarkdown/components/ToolsMenu";
import {ImagePlugin} from "@/components/Cat2BugMarkdown/plugins/ImagePlugin";
import html2canvas from 'html2canvas';
import {strFormat} from "@/utils";
import Vue from "vue";
import {upload} from "@/api/common/upload";
import {updateTemplate} from "@/api/system/ReportTemplate";
import {markdownData} from "@/api/markdown/markdown";

export default {
  name: "Cat2BugMarkdown",
  components: { Multipane, MultipaneResizer, MarkdownItVue, ToolsMenu },
  model: {
    prop: 'content',
    event: 'input'
  },
  data() {
    return {
      multipaneStyle: {'--marginTop':'0px'},
      activeToolsIndex: null,
      viewVisible: true,
      markdownContent: '',
      currentToolView: null,
      viewTools: [{
        id: 'mk-look',
        name: 'edit-mode',
        type: 'check',
        icon: 'mk-look',
        activeIcon: 'mk-not-look',
        check: false,
        content: this.markdownView
      },{
        id: 'help',
        name: 'markdown-explanation',
        icon: 'mk-help',
        content: function (view, tool) {
          return '';
        }
      }],
      options: {
        markdownIt: {
          html: true,
          linkify: true
        },
      }
    }
  },
  props: {
    content: {
      type: String,
      default: ''
    },
    template: {
      type: Object,
      default: ()=>{}
    },
    placeholder: {
      type: String,
      default: null
    },
    excludeTools: {
      type: Array,
      default: ()=>[]
    }
  },
  directives: {
    resize: {
      // 指令的名称
      bind(el, binding) {
        // el为绑定的元素，binding为绑定给指令的对象
        let width = ''
        let height = ''
        function isResize() {
          const style = document.defaultView.getComputedStyle(el)
          if (width !== style.width || height !== style.height) {
            binding.value({ width: style.width, height: style.height }) // 关键(这传入的是函数,所以执行此函数)
          }
          width = style.width
          height = style.height
        }
        el.__vueSetInterval__ = setInterval(isResize, 300)
      },
      unbind(el) {
        clearInterval(el.__vueSetInterval__)
      }
    }
  },
  watch: {
    content(v) {
      if(this.markdownContent!=v) {
        this.markdownContent = v;
      }
    },
    markdownContent(v) {
      // console.log('markdownContent',v)
    }
  },
  computed: {
    projectId() {
      return this.$store.state.user.config.currentProjectId
    },
    tools() {
      return MarkdownTools().filter(t=>this.excludeTools.indexOf(t.name)===-1)
    }
  },
  mounted() {
    this.initMarkdownPlug();
    this._registerComponentTools(MarkdownTools());
  },
  methods: {
    /** 工具数组 */
    MarkdownTools,
    /** 注册工具组件 */
    _registerComponentTools(tools) {
      let arr = [];
      if(tools) {
        tools.forEach(t=>{
          if(t.type=='component') {
            arr.push(t.content);
            Vue.component(t.content.name ,t.content);
          }
          if(t.children) {
            let childrenArr = this._registerComponentTools(t.children);
            arr = [...arr, ...childrenArr];
          }
        })
      }
      return arr;
    },
    /** 控制markdown组件是否显示 */
    markdownView(view, tool) {
      this.viewVisible = !tool.check;
    },
    /** 保存文档 */
    async save() {
      if(!this.template || Object.keys(this.template).length==0) return;
      let template = {
        templateId: this.template.templateId,
        templateTitle: this.template.templateTitle,
        moduleType: this.template.moduleType,
        projectId: this.template.projectId,
        templateContent: this.markdownContent,
        majorVersion: this.template.majorVersion,
        minorVersion: this.template.minorVersion,
      }
      const blob = await this.getMarkdownImage();
      if(blob && blob.type=='image/png') {
        const formData = new FormData();
        formData.append('file', blob);
        let res = await upload(formData);
        template.templateIconUrl = res.fileName;
      }
      updateTemplate(template).then(res=>{
        this.$message.success(this.$i18n.t('save-success').toString())
      });
    },
    /** 清除文档 */
    clear() {
      this.markdownContent = '';
    },
    /** 编辑文档时触发事件 */
    inputHandle() {
      this.$emit('input',this.markdownContent);
    },
    /** 初始化markdown插件 */
    initMarkdownPlug() {
      // 可以合并行列的表格
      this.$refs.markdownView.use(require('markdown-it-multimd-table'), {
        multiline:  true,
        rowspan:    true,
        headerless: true,
        multibody:  true,
        aotolabel:  true,
      });
      this.$refs.markdownView.use(LeftPointPlugin);
      this.$refs.markdownView.use(RightPointPlugin);
      this.$refs.markdownView.use(CenterPointPlugin);
      // 缺陷操作符
      const defectList = listDefect().then(res=>{
        // this.$refs.markdownView.use(TablePlugin,{
        //   name: 'api.defect.list',
        //   value: res.rows,
        //   defaultTitles: ["projectNum","defectTypeName","defectLevel","defectName","defectStateName","moduleName","moduleVersion","defectDescribe","createBy","createTime",
        //     "updateBy","updateTime","imgList","annexList"]
        // });
        this.$refs.markdownView.use(CardPlugin,{
          name: 'api.defect.list',
          value: res.rows
        });
        return res;
      });
      // 测试用例操作符
      // const caseList = listCase().then(res=>{
      //   this.$refs.markdownView.use(TablePlugin,{
      //     name: 'api.case.list',
      //     value: res.rows
      //   });
      //   return res;
      // });
      // 项目操作符
      const project = getProject(this.projectId).then(res=>{
        this.$refs.markdownView.use(ImagePlugin,{
          name: 'api.project',
          value: res.data
        });
        this.$refs.markdownView.use(VariablePlugin,{
          name: 'api.project',
          value: res.data
        });
        return res;
      });
      markdownData(this.projectId).then(res=>{
        this.$refs.markdownView.use(TablePlugin,{
          name: 'api.defect.list',
          value: res.data.defect?res.data.defect.list:[],
          defaultTitles: ["projectNum","defectTypeName","defectLevel","defectName","defectStateName","moduleName","moduleVersion","defectDescribe","createBy","createTime",
            "updateBy","updateTime","imgList","annexList"]
        });
        this.$refs.markdownView.use(TablePlugin,{
          name: 'api.case.list',
          value: res.data.case?res.data.case.list:[],
          defaultTitles: ["caseNum","caseName","moduleName","casePreconditions","caseExpect","caseStep","createBy","createTime",
            "updateBy","updateTime"]
        });
        this.$refs.markdownView.use(CaseCardPlugin,{
          name: 'api.case.list',
          value: res.data.case?res.data.case.list:[]
        });
        this.$refs.markdownView.use(VariablePlugin,{
          name: 'api.case.total',
          value: res.data.case?res.data.case.total:0
        });
      });
      Promise.all([defectList,project]).then(res=>{
        this.markdownContent = this.content?this.content:'';
      }).catch(e=>{
        this.$message.error(e);
      })
    },
    /** 拖动事件完成 */
    dragStopHandle(pane, container, size) {
    },
    /** 设置模块与用例列表中间拖动块的尺寸 */
    setDragComponentSize() {
      this.multipaneStyle['--marginTop'] = '0px';
      this.$nextTick(()=> {
        let pageHeight = Math.max(this.$refs.markdownEdit.scrollHeight || 0, this.$refs.markdownView.scrollHeight || 0, document.body.scrollHeight - 170)
        this.multipaneStyle['--marginTop'] = pageHeight + 'px';
      })
    },
    /** 右侧工具栏点击处理事件 */
    viewToolsHandle(tools,tool) {
      if(tool.content instanceof Function || tool.content instanceof Promise) {
        tool.content(this, tool);
      }
    },
    /** 插入文本内容到编辑框 */
    async insertText(text) {
      if(!text) return;
      const markdownEdit = this.$refs.markdownEdit;
      if (markdownEdit.selectionStart || markdownEdit.selectionStart === 0) {
        let startPos = markdownEdit.selectionStart
        let endPos = markdownEdit.selectionEnd
        let content =text;
        this.markdownContent = markdownEdit.value.substring(0, startPos) + '\n' + content + '\n' + markdownEdit.value.substring(endPos, markdownEdit.value.length);
        await this.$nextTick() // 这句是重点, 圈起来
        markdownEdit.focus()
        markdownEdit.setSelectionRange(endPos + text.length+1, endPos + text.length+1);
      } else {
        this.markdownContent += '\n' + text + '\n';
      }
    },
    /** 插入内容到编辑框 */
    async insertContent(tool) {
      const markdownEdit = this.$refs.markdownEdit;
      let content;
      if (tool.content instanceof Function || tool.content instanceof Promise) {
        content = await tool.content(this, tool);
      } else {
        content = this.$i18n.t(tool.content);
      }
      if (markdownEdit.selectionStart || markdownEdit.selectionStart === 0) {
        if (markdownEdit.selectionStart == markdownEdit.selectionEnd) {
          content = strFormat(content, tool.name);
        } else {
          content = strFormat(content, markdownEdit.value.substring(startPos, endPos));
        }
      }
      await this.insertText(content);
    },
    /** 左侧工具栏处理事件 */
    async toolsHandle(tools, tool) {
      if(tool.type=='command') {
        tool.content(this, tool);
      } else if(tool.type=='component'){
        this.currentToolView = tool.content.name;
        this.$nextTick(()=>{
          this.$refs.toolView.run(this,tools,tool);
        });
      } else {
        await this.insertContent(tool);
      }
    },
    /** 获取Markdown文档的图片 */
    async getMarkdownImage() {
      if(this.viewVisible) {
        let canvas = await html2canvas(this.$refs.markdownViewParent);
        const base64Img = canvas.toDataURL("image/png");
        let blob = this.base64ToBlob(base64Img.replace("data:image/png;base64,", ""), "image/png", 512);
        return blob;
      } else {
        return null;
      }
    },
    /** 图片文件格式转换 */
    base64ToBlob(b64Data, contentType, sliceSize) {
      contentType = contentType || "";
      sliceSize = sliceSize || 512;
      let byteCharacters = window.atob(b64Data);
      // var byteCharacters  = b64Data;
      // 该atob函数将base64编码的字符串解码为一个新字符串，其中包含二进制数据每个字节的字符。
      let byteArrays = [];
      for (let offset = 0; offset < byteCharacters.length; offset += sliceSize) {
        let slice = byteCharacters.slice(offset, offset + sliceSize);
        let byteNumbers = new Array(slice.length);
        // 通过使用.charCodeAt字符串中每个字符的方法应用它来创建一个新的数组。
        for (let i = 0; i < slice.length; i++) {
          byteNumbers[i] = slice.charCodeAt(i);
        }
        // 将这个数组转换为实际类型的数组，方法是将其传递给Uint8Array构造函数。
        let byteArray = new Uint8Array(byteNumbers);
        byteArrays.push(byteArray);
      }
      // 创建一个blob：包含这条数据的URL，返回去。
      let blob = new Blob(byteArrays, { type: contentType });
      return blob;
    },
  }
}
</script>
<style lang="scss" scoped>
.markdown {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 4px;
  box-shadow: rgba(0, 0, 0, 0.1) 0px 2px 12px 0px;
}
.markdown-tools {
  width: 100%;
  display: inline-flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  padding: 5px 10px;
  border-bottom: 1px solid #f2f6fc;
  ::v-deep .el-submenu__icon-arrow {
    display: none !important;
  }
}

.markdown-body {
  width: 100%;
  display: inline-flex;
  flex-direction: row;
  flex: 1;
  background-color: #f6f6f6;
  height: 100%;
  > * {
    min-height: 100px;
    overflow-y: auto;
  }
  > .markdown-body-edit, > .markdown-body-view {
    padding: 15px 20px;
  }
  > .markdown-body-view {
    background-color: #FCFCFC;
  }
  > .markdown-body-edit {
    background-color: #FFFFFF;
  }
  > .markdown-body-edit[mode='single'] {
    flex: 1;
  }
  > .markdown-body-resizer {
    flex-shrink: 1;
  }
  > .markdown-body-edit {
    width: calc( 50% - 5px );
    margin-right: 5px;
    font-size: 16px;
    border: none;
    outline:none;
    letter-spacing: 1px;
  }
  > .markdown-body-view {
    flex: 1;
    overflow: hidden;
    overflow-y: auto;
  }
}
</style>
