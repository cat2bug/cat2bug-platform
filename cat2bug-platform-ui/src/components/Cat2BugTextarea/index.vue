<template>
  <div class="cat2bug-textarea">
    <textarea
      ref="cat2bugTextarea"
      v-model="textContent"
      :placeholder="placeholder"
      :maxlength="maxlength"
      :rows="showRows"
      @keyup.stop="changeHandle"
    />
    <div v-show="showTools" class="tools">
      <slot name="tools"></slot>
      <el-tooltip v-if="showDefaultTools" v-for="(button,index) in buttons" :key="index" class="item" effect="dark" :content="$t(button.name)" placement="top">
        <el-button link @click="button.method"><svg-icon :icon-class="button.icon"></svg-icon></el-button>
      </el-tooltip>
      <span v-show="showWordLimit" class="text-length">{{textLength}}</span>
    </div>
    <el-dialog
      :title="title"
      width="95%"
      v-model="dialogVisible"
      class="cat2bug-textarea-dialog"
      :before-close="handleCloseDialog"
      append-to-body>
      <cat2-bug-markdown
        ref="cat2bugMarkdown"
        :exclude-tools="['siding-9899', 'project', 'case', 'defect', 'member', 'save', 'plan']"
        :placeholder="placeholder"
        v-model:content="textContent"
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
  emits: ['update:modelValue', 'change'],
  data() {
    return {
      textContent: null,
      defectButtons: [{
        name: 'maximize',
        icon: 'windows-max',
        method: this.maxWindows
      }],
      dialogVisible: false,
      prevRows: null
    }
  },
  watch: {
    modelValue(n, o) {
      if (n !== o) {
        this.textContent = n
      }
    },
    content(n, o) {
      if (n !== o) {
        this.textContent = n
      }
    },
  },
  props: {
    modelValue: {
      type: String,
      default: null
    },
    /** @deprecated Vue2 兼容 */
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
      default: null
    },
    minRows: {
      type: [Number,String],
      default: 3
    },
    maxRows: {
      type: [Number,String],
      default: 9
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
    this.textContent = this.modelValue != null ? this.modelValue : this.content
    this.$refs.cat2bugTextarea.addEventListener('paste',this.getClipboardImage);
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
    },
    showRows() {
      const lines = this.textContent ? this.textContent.split('\n') : [];
      const minRows = this.minRows || 5;
      const maxRows = this.maxRows || 9;
      return this.rows || Math.min(maxRows, Math.max(minRows, lines.length));
    }
  },
  methods: {
    maxWindows() {
      this.dialogVisible = true;
    },
    changeHandle(event) {
      this.$emit('update:modelValue', this.textContent)
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
              let baseUrl = location.protocol + '//' + location.hostname + (location.port ? ':' + location.port : '');
              self.insertTextAtCursor(`![](${baseUrl + process.env.VUE_APP_BASE_API + res.fileName})`);
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
  display: block;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  textarea {
    display: block;
    //line-height: 1.5;
    width: 100%;
    box-sizing: border-box;
    padding: 10px 15px;
    padding-bottom: 36px;
    font-size: inherit;
    color: var(--input-color, #606266);
    background-color: var(--el-input-bg-color, var(--input-bg, #ffffff));
    background-image: none;
    border: 1px solid var(--input-border, #DCDFE6);
    border-radius: var(--cat2bug-border-radius, 4px);
  }
  textarea:focus {
    border-color: var(--cat2bug-field-focus-color, #1890ff);
    outline: none;
    border-width: 1px;
  }
  textarea::placeholder {
    color: var(--text-color-placeholder, #c0c4cc);
  }
  .tools {
    position: absolute;
    right: 10px;
    bottom: 4px;
    display: flex;
    flex-flow: row nowrap;
    justify-content: flex-end;
    align-items: center;
    gap: 0;
    max-width: calc(100% - 20px);
    /* 与 ProjectAiModelSelect 组合条 hover：同色、同圆角、同高度感 */
    $tools-icon-hover-bg: var(--cat2bug-textarea-tools-hover-bg, #f0f7ff);
    $tools-icon-radius: 3px;
    $tools-icon-gap: 4px;
    :deep(.el-select .el-select__wrapper){
      min-height: 0 !important;
      height: 100% !important;
      max-height: 24px !important;
      box-shadow: none !important;
      background: transparent !important;
    }
    :deep(.el-tooltip__trigger){
      display: inline-flex;
      align-items: center;
      align-self: center;
      flex: 0 0 auto;
    }
    /* 机器人组合条与最大化按钮之间留白（对齐 Vue2 视觉） */
    :deep(.c2b-model-select-wrap--combo) {
      margin-right: $tools-icon-gap;
    }
    .text-length, .el-button {
      height: 24px;
      background: var(--el-input-bg-color, var(--input-bg, #ffffff));
      flex: 0 0 auto;
    }
    .text-length {
      display: inline-flex;
      align-items: center;
      padding-left: 4px;
      padding-right: 4px;
      color: #909399;
      font-size: 12px;
      border-radius: $tools-icon-radius;
      text-align: center;
      line-height: 1;
    }
    /* 水平留白左右对称（略窄于原先左侧 8px） */
    $tools-icon-pad-h: 4px;
    .el-button {
      padding: 0 $tools-icon-pad-h;
      margin: 0;
      line-height: 12px;
      border: none !important;
      box-shadow: none !important;
      outline: none;
      border-radius: $tools-icon-radius;
      transition: background-color 0.2s ease;
    }
    .el-button:hover,
    .el-button:focus,
    .el-button:focus-visible {
      background-color: $tools-icon-hover-bg !important;
      border: none !important;
      box-shadow: none !important;
      outline: none !important;
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
  /* 弹框内不再套一层 markdown 卡片边框 */
  .cat2bug-textarea-dialog .markdown {
    border: none !important;
    box-shadow: none !important;
    border-radius: 0 !important;
  }
</style>
