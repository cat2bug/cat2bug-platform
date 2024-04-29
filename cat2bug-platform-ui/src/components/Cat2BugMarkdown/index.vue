<template>
  <div class="markdown">
    <div class="markdown-tools">
      <div>
        <tools-menu :tools="MarkdownTools()" @select="toolsHandle" />
      </div>
      <div>333</div>
    </div>
    <multipane layout="vertical" ref="multiPane" class="markdown-body" @pane-resize-start="dragStopHandle">
      <textarea
        class="markdown-body-edit"
        ref="markdownEdit"
        v-resize="setDragComponentSize"
        v-model="markdownContent"
        @input="inputHandle"
      />
      <multipane-resizer class="markdown-body-resizer" :style="multipaneStyle"></multipane-resizer>
      <div
        class="markdown-body-view"
        v-resize="setDragComponentSize"
        ref="markdownViewParent">
        <markdown-it-vue
          ref="markdownView"
          :content="markdownContent"
          />
      </div>
    </multipane>
  </div>
</template>

<script>
import { Multipane, MultipaneResizer } from 'vue-multipane';
import MarkdownItVue from "markdown-it-vue"
import { TablePlugin } from "./plugins/TablePlugin"
import { CardPlugin } from "@/components/Cat2BugMarkdown/plugins/CardPlugin";
import {VariablePlugin} from "./plugins/VariablePlugin"
import { MarkdownTools } from "./MarkdownTools"
import 'markdown-it-vue/dist/markdown-it-vue.css'
import {listDefect} from "@/api/system/defect";
import {listCase} from "@/api/system/case";
import {getProject} from "@/api/system/project";
import ToolsMenu from "@/components/Cat2BugMarkdown/components/ToolsMenu";
import {ImagePlugin} from "@/components/Cat2BugMarkdown/plugins/ImagePlugin";
import html2canvas from 'html2canvas';

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
      markdownContent: this.content,
    }
  },
  props: {
    content: {
      type: String,
      default: ''
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
      console.log('---',v)
      if(this.markdownContent!=v) {
        console.log('=====2')
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
  },
  mounted() {
    this.initMarkdownPlug();
  },
  methods: {
    MarkdownTools,
    inputHandle() {
      this.$emit('input',this.markdownContent);
    },
    /** 初始化markdown插件 */
    initMarkdownPlug() {
      this.$refs.markdownView.use(require('markdown-it-multimd-table'), {
        multiline:  true,
        rowspan:    true,
        headerless: true,
        multibody:  true,
        aotolabel:  true,
      });
      listDefect().then(res=>{
        this.$refs.markdownView.use(TablePlugin,{
          name: 'api.defect.list',
          value: res.rows
        });
        this.$refs.markdownView.use(CardPlugin,{
          name: 'api.defect.list',
          value: res.rows
        });
      });
      listCase().then(res=>{
        this.$refs.markdownView.use(TablePlugin,{
          name: 'api.case.list',
          value: res.rows
        });
      });
      getProject(this.projectId).then(res=>{
        this.$refs.markdownView.use(ImagePlugin,{
          name: 'api.project',
          value: res.data
        });
        this.$refs.markdownView.use(VariablePlugin,{
          name: 'api.project',
          value: res.data
        });
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
    async toolsHandle(tool) {
      const markdownEdit = this.$refs.markdownEdit;
      if (markdownEdit.selectionStart || markdownEdit.selectionStart === 0) {
        let startPos = markdownEdit.selectionStart
        let endPos = markdownEdit.selectionEnd
        let content;
        if(tool.content instanceof Function || tool.content instanceof Promise) {
          content = await tool.content();
        } else {
          content = tool.content;
        }
        this.markdownContent = markdownEdit.value.substring(0, startPos) +'\n'+ content +'\n'+ markdownEdit.value.substring(endPos, markdownEdit.value.length)
        await this.$nextTick() // 这句是重点, 圈起来
        markdownEdit.focus()
        markdownEdit.setSelectionRange(endPos + tool.content.length, endPos + tool.content.length);
      } else {
        this.markdownContent += '\n'+tool.content+'\n';
      }
    },
    async getMarkdownImage() {
      let canvas = await html2canvas(this.$refs.markdownViewParent);
      const base64Img = canvas.toDataURL("image/png");
      let blob = this.base64ToBlob(base64Img.replace("data:image/png;base64,", ""), "image/png", 512);
      return blob;
    },
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
  padding: 5px;
  border-bottom: 1px solid #f2f6fc;
  ::v-deep .el-submenu__icon-arrow {
    display: none !important;
  }

  //> div:first-child {
  //  flex: 1;
  //  display: inline-flex;
  //  flex-direction: row;
  //  justify-content: flex-start;
  //  align-items: center;
  //}
  //.el-button {
  //  border-width: 0px;
  //  padding: 8px;
  //}
  //.el-button:not(:first-child) {
  //  margin-left: 2px;
  //}
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
