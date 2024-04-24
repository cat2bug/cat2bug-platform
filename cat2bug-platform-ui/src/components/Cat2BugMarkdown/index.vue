<template>
  <div class="markdown">
    <div class="markdown-tools">
      <div>
        <template v-for="(tool,index) in MarkdownTools()">
          <el-popover
            v-if="tool.children"
            placement="bottom"
            width="200"
            trigger="hover"
            popper-class="cat2bug-markdown-column-menu"
            :key="index"
            >
            <el-button v-for="(children,cIndex) in tool.children" :key="cIndex" @click="toolsHandle(children)"><svg-icon :icon-class="children.icon"></svg-icon>{{children.name}}</el-button>
            <el-button slot="reference"><svg-icon :icon-class="tool.icon"></svg-icon></el-button>
          </el-popover>
          <div class="siding" v-else-if="tool.type=='siding'" :key="index" />
          <el-button v-else @click="toolsHandle(tool)"><svg-icon :icon-class="tool.icon"></svg-icon></el-button>
        </template>
      </div>
      <div>333</div>
    </div>
    <multipane layout="vertical" ref="multiPane" class="markdown-body" @pane-resize-start="dragStopHandle">
      <textarea
        class="markdown-body-edit"
        ref="markdownEdit"
        v-resize="setDragComponentSize"
        v-model="markdownContent"
      />
      <multipane-resizer class="markdown-body-resizer" :style="multipaneStyle"></multipane-resizer>
      <markdown-it-vue
        class="markdown-body-view"
        ref="markdownView"
        v-resize="setDragComponentSize"
        :content="markdownContent"
        />
    </multipane>
  </div>
</template>

<script>
import { Multipane, MultipaneResizer } from 'vue-multipane';
import MarkdownItVue from "markdown-it-vue"
import { TablePlugin } from "./plugins/TablePlugin"
import {VariablePlugin} from "./plugins/VariablePlugin"
import { MarkdownTools } from "./MarkdownTools"
import 'markdown-it-vue/dist/markdown-it-vue.css'
import {listDefect} from "@/api/system/defect";
import {listCase} from "@/api/system/case";
import {getProject} from "@/api/system/project";

export default {
  name: "Cat2BugMarkdown",
  components: { Multipane, MultipaneResizer, MarkdownItVue },
  data() {
    return {
      multipaneStyle: {'--marginTop':'0px'},
      form: {
        content: ''
      },
      markdownContent: ''
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
    'form.content'(v) {
      console.log('form.content',v)
    },
    markdownContent(v) {
      console.log('markdownContent',v)
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
    /** 初始化markdown插件 */
    initMarkdownPlug() {
      listDefect().then(res=>{
        this.$refs.markdownView.use(TablePlugin,{
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
        this.markdownContent = markdownEdit.value.substring(0, startPos) +'\n'+ tool.content +'\n'+ markdownEdit.value.substring(endPos, markdownEdit.value.length)
        await this.$nextTick() // 这句是重点, 圈起来
        markdownEdit.focus()
        markdownEdit.setSelectionRange(endPos + tool.content.length, endPos + tool.content.length);
      } else {
        this.markdownContent += '\n'+tool.content+'\n';
      }
    },
  }
}
</script>
<style>
.cat2bug-markdown-column-menu {
  display: flex;
  flex-direction: column;
}
.cat2bug-markdown-column-menu > .el-button {
  margin-left: 0px;
  border: none;
}
</style>
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
  > div:first-child {
    flex: 1;
    display: inline-flex;
    flex-direction: row;
    justify-content: flex-start;
    align-items: center;
  }
  .el-button {
    border-width: 0px;
    padding: 8px;
  }
  .el-button:not(:first-child) {
    margin-left: 2px;
  }
}
.siding {
  width: 1px;
  height: 15px;
  background-color: #dcdcdc;
  margin: 0px 5px;
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
