<template>
  <div class="markdown">
    <div class="markdown-tools">
      <tools-menu :tools="toolMenus" @select="toolsHandle" />
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
import {PlanVariablePlugin} from "./plugins/PlanVariablePlugin"
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
      tools: [],
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
      },
      //   {
      //   id: 'help',
      //   name: 'markdown-explanation',
      //   icon: 'mk-help',
      //   content: function (view, tool) {
      //     return '';
      //   }
      // }
      ],
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
        this.markdownContent = v||'';
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
    toolMenus() {
      return this.tools.filter(t=>this.excludeTools.indexOf(t.name)===-1)
    }
  },
  mounted() {
    this.tools = MarkdownTools();
    this.initMarkdownPlug();
    this._registerComponentTools(this.tools);
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
        this.$refs.markdownView.use(PlanVariablePlugin,{
          name: 'api.plan',
          value: res.data.plan?res.data.plan.list:[]
        });

        this.addDynamicToolMenu(res); // 添加动态菜单
      });
      Promise.all([defectList,project]).then(res=>{
        this.markdownContent = '';
        this.$nextTick(()=>{
          this.markdownContent = this.content?this.content:'';
        });
      }).catch(e=>{
        this.$message.error(e);
      })
    },
    /** 添加动态菜单 */
    addDynamicToolMenu(res) {
      // 动态添加计划菜单
      if(res.data.plan && res.data.plan.list && res.data.plan.list.length>0) {
        let planMenu = {
          icon: 'md-create-time',
          name: 'plan',
        };
        planMenu.children = res.data.plan.list.map(p=>{
          return {
            icon: 'md-create-time',
            name: p.planName,
            children: [{
              icon: 'mk-table',
              name: 'plan.statistics',
              content: `|项目名称|\${api.project.projectName}|||||||||||||||||
|测试计划|\${api.plan.planName[${p.planNumber}]}|||||||||||||||||
测试版本号|\${api.plan.planVersion[${p.planNumber}]}||用例通过数量|\${api.plan.passCount[${p.planNumber}]} 个|用例未通过数量|\${api.plan.failCount[${p.planNumber}]} 个|用例未执行数量|\${api.plan.unexecutedCount[${p.planNumber}]} 个||||||||||
|||||||
|-|-|-|-|
|度量指标数据如下:||||||||||||||||||
|缺陷发现率（单位：%）||缺陷修复率（单位：%）||缺陷探测率（单位：%） ||缺陷密度（单位：个）||缺陷严重率（单位：%） ||缺陷重开率（单位：%） ||缺陷逃逸率（单位：个）||用例执行效率（单位：个）||缺陷修复平均时长（单位：h）||
|本轮已发现的缺陷数量|本轮已执行的用例数量|本轮已修复的缺陷数量|本轮发现的总的缺陷数量|测试或探测者发现的缺陷数量|客户反馈的缺陷数量|缺陷数量|代码行数或功能点的数 代码行数或功能点的数量|本计划所发现的严重缺陷数量|本轮已发现的缺陷总数|本轮重新打开的缺陷数量|本轮已修复的缺陷数量|用户反馈或实际使用中发现的缺陷数量|测试发现的缺陷数|本轮迭代执行的测试用例数量|执行本轮迭代测试耗时|缺陷数量|统计周期内缺陷的总解决时长
|\${api.plan.defectCount[${p.planNumber}]}|\${api.plan.executedCount[${p.planNumber}]}个|\${api.plan.defectCloseStateCount[${p.planNumber}]}| \${api.plan.defectCount[${p.planNumber}]}|\${api.plan.createDefectCountByTester[${p.planNumber}]}|\${api.plan.createDefectCountByOutsider[${p.planNumber}]}|\${api.plan.defectCount[${p.planNumber}]}|\${api.plan.moduleCount[${p.planNumber}]}|\${api.plan.defectLevelUrgentCount[${p.planNumber}]}|\${api.plan.defectCount[${p.planNumber}]}|\${api.plan.defectRestartCount[${p.planNumber}]}|\${api.plan.defectHistoryPassCount[${p.planNumber}]}|\${api.plan.createDefectCountByOutsider[${p.planNumber}]}|\${api.plan.defectCount[${p.planNumber}]}|\${api.plan.defectCount[${p.planNumber}]}| |\${api.plan.defectCount[${p.planNumber}]}|\${api.plan.defectUseHourTime[${p.planNumber}]}h|

|\${api.plan.defectDiscoveryRate[${p.planNumber}]}||\${api.plan.defectRepairRate[${p.planNumber}]}||\${api.plan.defectDetectionRate[${p.planNumber}]}||\${api.plan.defectDensity[${p.planNumber}]}个||\${api.plan.defectSeverityRate[${p.planNumber}]}||\${api.plan.defectRestartRate[${p.planNumber}]}||\${api.plan.defectEscapeRate[${p.planNumber}]}|| ||\${api.plan.defectRepairAvgHour[${p.planNumber}]}h||`
            },{
              type: 'siding'
            },{
              icon: 'md-create-time',
              name: 'plan.item',
              children: [{
                icon: 'mk-total',
                name: 'plan.name',
                content: `${this.$i18n.t('plan.name')}: \${api.plan.planName[${p.planNumber}]}`
              },{
                icon: 'mk-total',
                name: 'plan.version',
                content: `${this.$i18n.t('plan.version')}: \${api.plan.planVersion[${p.planNumber}]}`
              },{
                icon: 'mk-total',
                name: 'plan.fail-count',
                content: `${this.$i18n.t('plan.fail-count')}: \${api.plan.failCount[${p.planNumber}]}`
              },{
                icon: 'mk-total',
                name: 'plan.pass-count',
                content: `${this.$i18n.t('plan.pass-count')}: \${api.plan.passCount[${p.planNumber}]}`
              },{
                icon: 'mk-total',
                name: 'plan.unexecuted-count',
                content: `${this.$i18n.t('plan.unexecuted-count')}: \${api.plan.unexecutedCount[${p.planNumber}]}`
              },{
                icon: 'mk-total',
                name: 'plan.executed-count',
                content: `${this.$i18n.t('plan.executed-count')}: \${api.plan.executedCount[${p.planNumber}]}`
              },{
                icon: 'mk-total',
                name: 'plan.item-total',
                content: `${this.$i18n.t('plan.item-total')}: \${api.plan.itemTotal[${p.planNumber}]}`
              }]
            },{
              icon: 'mk-bug',
              name: 'plan.defect',
              children: [{
                icon: 'mk-total',
                name: 'plan.defect-count',
                content: `${this.$i18n.t('plan.defect-count')}: \${api.plan.defectCount[${p.planNumber}]}`
              },{
                type: 'siding'
              },{
                icon: 'mk-member',
                name: 'member',
                children: [{
                  icon: 'mk-member',
                  name: 'plan.create-defect-count-by-tester',
                  content: `${this.$i18n.t('plan.create-defect-count-by-tester')}: \${api.plan.createDefectCountByTester[${p.planNumber}]}`
                },{
                  icon: 'mk-member',
                  name: 'plan.create-defect-count-by-outsider',
                  content: `${this.$i18n.t('plan.create-defect-count-by-outsider')}: \${api.plan.createDefectCountByOutsider[${p.planNumber}]}`
                }]
              },{
                type: 'siding'
              },{
                icon: 'mk-total',
                name: 'defect.state',
                children: [{
                  icon: 'mk-total',
                  name: 'plan.defect-processing-count',
                  content: `${this.$i18n.t('plan.defect-processing-count')}: \${api.plan.defectProcessingStateCount[${p.planNumber}]}`
                },{
                  icon: 'mk-total',
                  name: 'plan.defect-audit-count',
                  content: `${this.$i18n.t('plan.defect-audit-count')}: \${api.plan.defectAuditStateCount[${p.planNumber}]}`
                },{
                  icon: 'mk-total',
                  name: 'plan.defect-rejected-count',
                  content: `${this.$i18n.t('plan.defect-rejected-count')}: \${api.plan.defectRejectedStateCount[${p.planNumber}]}`
                },{
                  icon: 'mk-total',
                  name: 'plan.defect-close-count',
                  content: `${this.$i18n.t('plan.defect-close-count')}: \${api.plan.defectCloseStateCount[${p.planNumber}]}`
                },{
                  icon: 'mk-total',
                  name: 'plan.defect-history-pass-count',
                  content: `${this.$i18n.t('plan.defect-history-pass-count')}: \${api.plan.defectHistoryPassCount[${p.planNumber}]}`
                }]
              },{
                type: 'siding'
              },{
                icon: 'mk-level',
                name: 'plan.defect-level-count',
                children: [{
                  name: 'plan.defect-level-urgent-count',
                  content: `${this.$i18n.t('plan.defect-level-urgent-count')}: \${api.plan.defectLevelUrgentCount[${p.planNumber}]}`
                },{
                  name: 'plan.defect-level-height-count',
                  content: `${this.$i18n.t('plan.defect-level-height-count')}: \${api.plan.defectLevelHeightCount[${p.planNumber}]}`
                },{
                  name: 'plan.defect-level-middle-count',
                  content: `${this.$i18n.t('plan.defect-level-middle-count')}: \${api.plan.defectLevelMiddleCount[${p.planNumber}]}`
                },{
                  name: 'plan.defect-level-low-count',
                  content: `${this.$i18n.t('plan.defect-level-low-count')}: \${api.plan.defectLevelLowCount[${p.planNumber}]}`
                }]
              },{
                type: 'siding'
              },{
                icon: 'mk-total',
                name: 'plan.defect-restart-count',
                content: `${this.$i18n.t('plan.defect-re-open-count')}: \${api.plan.defectRestartCount[${p.planNumber}]}`
              },{
                icon: 'md-create-time',
                name: 'plan.defect-use-hour-time',
                content: `${this.$i18n.t('plan.defect-use-hour-time')}: \${api.plan.defectUseHourTime[${p.planNumber}]}`
              },{
                icon: 'mk-rate',
                name: 'plan.defect-discovery-rate',
                content: `${this.$i18n.t('plan.defect-discovery-rate')}: \${api.plan.defectDiscoveryRate[${p.planNumber}]}`
              },{
                icon: 'mk-rate',
                name: 'plan.defect-repair-rate',
                content: `${this.$i18n.t('plan.defect-repair-rate')}: \${api.plan.defectRepairRate[${p.planNumber}]}`
              },{
                icon: 'mk-value',
                name: 'plan.defect-density',
                content: `${this.$i18n.t('plan.defect-density')}: \${api.plan.defectDensity[${p.planNumber}]}`
              },{
                icon: 'mk-rate',
                name: 'plan.defect-detection-rate',
                content: `${this.$i18n.t('plan.defect-detection-rate')}: \${api.plan.defectDetectionRate[${p.planNumber}]}`
              },{
                icon: 'mk-rate',
                name: 'plan.defect-severity-rate',
                content: `${this.$i18n.t('plan.defect-severity-rate')}: \${api.plan.defectSeverityRate[${p.planNumber}]}`
              },{
                icon: 'mk-rate',
                name: 'plan.defect-restart-rate',
                content: `${this.$i18n.t('plan.defect-restart-rate')}: \${api.plan.defectRestartRate[${p.planNumber}]}`
              },{
                icon: 'mk-rate',
                name: 'plan.defect-escape-rate',
                content: `${this.$i18n.t('plan.defect-escape-rate')}: \${api.plan.defectEscapeRate[${p.planNumber}]}`
              },{
                icon: 'md-create-time',
                name: 'plan.defect-repair-avg-hour',
                content: `${this.$i18n.t('plan.defect-repair-avg-hour')}: \${api.plan.defectRepairAvgHour[${p.planNumber}]}`
              }]
            },{
              icon: 'cascader',
              name: 'plan.module-count',
              content: `${this.$i18n.t('plan.module-count')}: \${api.plan.moduleCount[${p.planNumber}]}`
            }]
          }
        });

        for(let i = 0; this.tools.length; i++){
          // 在用例菜单后面添加计划菜单
          if(this.tools[i].name=='case') {
            this.tools.splice(i+1,0, planMenu);
            break;
          }
        }
      }
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
      this.inputHandle();
    },
    /** 插入内容到编辑框 */
    async insertContent(tool) {
      const markdownEdit = this.$refs.markdownEdit;
      let content;
      if (tool.content instanceof Function || tool.content instanceof Promise) {
        content = await tool.content(this, tool);
      } else {
        content = this.$i18n.te(tool.content)?this.$i18n.t(tool.content):tool.content;
      }
      if (markdownEdit.selectionStart || markdownEdit.selectionStart === 0) {
        if (markdownEdit.selectionStart == markdownEdit.selectionEnd) {
          content = strFormat(content, tool.name);
        } else {
          let startPos = markdownEdit.selectionStart
          let endPos = markdownEdit.selectionEnd
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
