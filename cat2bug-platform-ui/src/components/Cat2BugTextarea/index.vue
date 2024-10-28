<template>
  <div class="cat2bug-textarea">
    <textarea
      ref="cat2bugTextarea"
      v-model="textContent"
      :placeholder="placeholder"
      :maxlength="maxlength"
      :rows="rows"
      @keyup.stop="changeHandle"
    />
    <div v-show="showTools" class="tools">
      <slot name="tools"></slot>
      <el-tooltip v-if="showDefaultTools" v-for="(button,index) in buttons" :key="index" class="item" effect="dark" :content="$t(button.name)" placement="top">
        <el-button type="text" @click="button.method"><svg-icon :icon-class="button.icon"></svg-icon></el-button>
      </el-tooltip>
      <span v-show="showWordLimit" class="text-length">{{textLength}}</span>
    </div>
    <el-dialog
      :title="title"
      width="95%"
      :visible.sync="dialogVisible"
      custom-class="cat2bug-textarea-dialog"
      :before-close="handleCloseDialog"
      append-to-body>
      <cat2-bug-markdown
        ref="cat2bugMarkdown"
        :exclude-tools="['siding-9899', 'project', 'case', 'defect', 'member', 'save', 'plan']"
        :placeholder="placeholder"
        v-model="textContent"
        @input="handleMarkdownContent"/>
    </el-dialog>
  </div>
</template>

<script>
import Cat2BugMarkdown from "@/components/Cat2BugMarkdown";
import {upload} from "@/api/common/upload";
export default {
  name: "index",
  components: {Cat2BugMarkdown},
  model: {
    prop:'content',
    event:'change'
  },
  data() {
    return {
      textContent: null,
      defectButtons: [{
        name: 'maximize',
        icon: 'windows-max',
        method: this.maxWindows
      }],
      dialogVisible: false
    }
  },
  watch: {
    content: function (n,o) {
      if(n!=o) {
        this.textContent = n;
      }
    },
  },
  props: {
    content: {
      type: String,
      default: null
    },
    name: {
      type: String,
      default: null
    },
    placeholder: {
      type: String,
      default: null
    },
    maxlength: {
      type: [Number,String],
      default: 65536
    },
    rows: {
      type: [Number,String],
      default: 3
    },
    showWordLimit: {
      type: Boolean,
      default: true
    },
    showTools: {
      type: Boolean,
      default: true
    },
    showDefaultTools: {
      type: Boolean,
      default: true
    },
    tools: {
      type: Array,
      default: ()=>[]
    }
  },
  mounted() {
    this.$refs.cat2bugTextarea.addEventListener('paste',this.getClipboardImage);
  },
  destroyed() {
    this.$refs.cat2bugTextarea.removeEventListener('paste',this.getClipboardImage);
  },
  computed: {
    title: function () {
      return `[${this.name?this.name:""}] ${this.$i18n.t('edit-mode')}`
    },
    textLength: function (){
      return `${this.textContent?this.textContent.length:0}/${this.maxlength}`
    },
    buttons: function (){
      return [...this.tools, ...this.defectButtons]
    }
  },
  methods: {
    maxWindows() {
      this.dialogVisible = true;
    },
    changeHandle(event) {
      // if (event.clipboardData || event.originalEvent) {
      //   let clipboardData = (event.clipboardData || event.originalEvent.clipboardData);
      //   if(clipboardData.items){
      //     let  blob;
      //     for (let i = 0; i < clipboardData.items.length; i++) {
      //       if (clipboardData.items[i].type.indexOf("image") !== -1) {
      //         blob = clipboardData.items[i].getAsFile();
      //       }
      //     }
      //     let render = new FileReader();
      //     render.onload = function (evt) {
      //       //输出base64编码
      //       let base64 = evt.target.result;
      //       let imgElem = document.createElement('img')
      //       imgElem.src = base64
      //       document.getElementsByClassName('img-textarea')[0].appendChild(imgElem)
      //     }
      //     if(blob){
      //       render.readAsDataURL(blob);
      //     }
      //   }
      // }

      this.$emit('change', this.textContent)
    },
    insertTextAtCursor(text) {
      let textarea = this.$refs.cat2bugTextarea;
      let startPos = textarea.selectionStart;
      let endPos = textarea.selectionEnd;
      let value = textarea.value;

      let beforeText = value.substring(0, startPos);
      let afterText = value.substring(endPos, value.length);

      let newValue = beforeText + text + afterText;
      this.textContent = newValue;

      // textarea.value = newValue;
      textarea.selectionStart = textarea.selectionEnd = startPos + text.length;
    },
    async getClipboardImage() {
      try {
        let self = this;
        const clipboardItems = await navigator.clipboard.read();
        for (const clipboardItem of clipboardItems) {
          for (const type of clipboardItem.types) {
            const blob = await clipboardItem.getType(type);
            if(blob.type=='image/png') {
              const formData = new FormData();
              formData.append('file', blob);
              let res = await upload(formData);
              self.insertTextAtCursor(`![](${process.env.VUE_APP_BASE_API + res.fileName})`);
            }
          }
        }
      } catch (err) {
        console.error(err.name, err.message);
      }
    },
    handleCloseDialog(done) {
      done();
    },
    handleMarkdownContent(content) {
      this.textContent = content;
      this.changeHandle();
    },
    focus() {
      this.$refs.cat2bugTextarea.focus();
    }
  }
}
</script>

<style lang="scss" scoped>
.cat2bug-textarea {
  position: relative;
  textarea {
    display: block;
    //line-height: 1.5;
    width: 100%;
    box-sizing: border-box;
    padding: 10px 15px;
    font-size: inherit;
    color: #606266;
    background-color: #FFFFFF;
    background-image: none;
    border: 1px solid #DCDFE6;
    border-radius: 4px;
  }
  textarea:focus {
    border-color: #1890ff;
    outline: none;
    border-width: 1px;
  }
  textarea::placeholder {
    color: #c0c4cc;
  }
  .tools {
    position: absolute;
    right: 10px;
    bottom: 4px;
    display: flex;
    flex-direction: row;
    justify-content: flex-end;
    align-items: center;
    gap:2px;
    .text-length, .el-button {
      height: 24px;
      background: #FFFFFF;
    }
    .text-length {
      padding-left: 4px;
      padding-right: 4px;
      color: #909399;
      font-size: 12px;
      border-radius: 3px;
      text-align: center;
      line-height: 24px;
    }
    .el-button {
      padding: 0px 4px;
      margin: 0px;
      line-height: 12px;
    }
    .el-button:hover {
      background-color: #EBEEF5;
    }
  }
}
</style>
<style>
  .cat2bug-textarea-dialog {
    height: 90%;
  }
  .cat2bug-textarea-dialog .el-dialog__body {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    top: 60px;
  }
</style>
