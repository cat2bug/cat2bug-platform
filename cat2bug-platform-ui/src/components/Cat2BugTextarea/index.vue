<template>
  <div class="cat2bug-textarea">
    <textarea
      v-model="textContent"
      :placeholder="placeholder"
      :maxlength="maxlength"
      :rows="rows"
      @change="changeHandle"
    />
    <div v-show="showTools" class="tools">
      <slot name="tools"></slot>
      <el-tooltip v-for="(button,index) in buttons" :key="index" class="item" effect="dark" :content="$t(button.name)" placement="top">
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
      <cat2-bug-markdown ref="cat2bugMarkdown" v-model="textContent" @input="handleMarkdownContent"></cat2-bug-markdown>
    </el-dialog>
  </div>
</template>

<script>
import Cat2BugMarkdown from "@/components/Cat2BugMarkdown";
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
    tools: {
      type: Array,
      default: ()=>[]
    }
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
    changeHandle(e) {
      this.$emit('change', this.textContent)
    },
    handleCloseDialog(done) {
      done();
    },
    handleMarkdownContent(content) {
      this.textContent = content;
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
    bottom: 1px;
    display: flex;
    flex-direction: row;
    justify-content: flex-end;
    align-items: center;
    gap:5px;
    .text-length {
      color: #909399;
      background: #FFFFFF;
      font-size: 12px;
    }
    .el-button {
      padding: 0px 4px;
      margin: 0px;
      height: 24px;
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
