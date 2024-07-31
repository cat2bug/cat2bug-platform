<template>
  <div class="comment-input" :type="type">
    <div>
      <div class="comment-input-content"
           ref="commentInputContent"
           :style="{ '--wordLimitContent': showWordLimit ? `'${wordLimit}'`: null }"
           :contenteditable="true"
           @click="handleSelection"
           @input="handleSelection"
           @paste="handlePaste"
           @keydown.enter.prevent="keyDownHandle"
      ></div>
    </div>
    <div class="comment-input-tools">
      <el-popover
        popper-class="comment-input-emoji-popover"
        v-model="commentInputEmojiPopoverVisible"
        placement="top"
        width="240"
        trigger="click">
        <div class="comment-input-emoji-select">
          <el-image
            v-for="i in 18"
            :key="i"
            :src="imgUrl(i)"
            @click="insertImage(imgName(i), imgUrl(i))"
            fit="cover"></el-image>
        </div>
        <el-image
          class="emoji-button pointer"
          :src="imgUrl(9)"
          slot="reference"
          fit="cover"></el-image>
      </el-popover>
      <el-button :disabled="currentTextLength==0" type="primary" size="mini" @click="submitHandle">{{ $t('comment.speak') }}</el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: "CommentInput",
  model: {
    prop: 'value',
    event: 'change'
  },
  data() {
    return {
      content: this.value,
      selection: null,
      range: null,
      textNode: null,
      rangeStartOffset: null,
      currentTextLength: 0,
      commentInputEmojiPopoverVisible: false
    }
  },
  props: {
    value: {
      type: String,
      default: null
    },
    /**
     * 是否显示输入字数统计，只在 type = "text" 或 type = "textarea" 时有效
     */
    showWordLimit: {
      type: Boolean,
      default: false
    },
    /**
     * 显示类型(text,textarea)
     */
    type: {
      type: String,
      default: 'textarea'
    },
    /**
     * 原生属性，最大输入长度
     */
    maxLength: {
      type: Number,
      default: 255
    }
  },
  watch: {
    '$refs.commentInputContent': 'currentTextLength'
  },
  computed: {
    imgUrl: function () {
      return function (index) {
        return require(`@/assets/emoji/default/${this.imgName(index)}.png`)
      }
    },
    imgName: function () {
      return function (index) {
        return `emoji${index}`;
      }
    },
    wordLimit: function () {
      return `${this.currentTextLength}/${this.maxLength}`
    }
  },
  mounted() {
    this.updateCurrentTextLength(); // 初始化文本长度
    const observer = new MutationObserver(this.updateCurrentTextLength);
    observer.observe(this.$refs.commentInputContent, { subtree: true, characterData: true, childList: true });
  },
  methods: {
    handlePaste(event) {
      // 阻止默认粘贴行为
      event.preventDefault();
    },
    /** 重置数据 */
    reset () {
      const contentDiv = this.$refs.commentInputContent;
      contentDiv.innerHTML = '';
      this.content = null;
      this.selection = null;
      this.range = null;
      this.textNode = null;
      this.rangeStartOffset = null;
      this.currentTextLength = 0;
      this.commentInputEmojiPopoverVisible = false;
    },
    /** 更新当前内容及长度 */
    updateCurrentTextLength: function () {
      const contentDiv = this.$refs.commentInputContent;
      if(!contentDiv || !contentDiv.childNodes){
        return 0;
      }

      let visibleText = '';
      Array.from(contentDiv.childNodes).forEach(node => {
        if (node.nodeType === Node.TEXT_NODE) {
          visibleText += node.textContent;
        } else {
          visibleText += node.outerHTML;
        }
      });

      if (visibleText.length > this.maxLength) {
        visibleText = visibleText.slice(0, this.maxLength);
      }

      if(contentDiv.innerHTML != visibleText) {
        contentDiv.innerHTML = visibleText; // 只设置可见内容
      }
      this.content = visibleText;
      this.currentTextLength = visibleText.length;
    },
    /** 将html转换成编码 */
    codeParse(code) {
      const divRegex = /<img.*?>/gi;
      const altRegex = /<img[^>]+alt="([^"]*)"/gi;
      const divs = Array.from(new Set(code.match(divRegex)));
      divs.forEach(img=>{
        let match;
        while ((match = altRegex.exec(img)) !== null) {
          code=code.replaceAll(img,`[img:${match[1]}]`);
        }
      })
      return code.replaceAll('<br>','\n');
    },
    /**
     * 提交数据
     */
    submitHandle() {
      this.$emit('submit', this.codeParse(this.content));
      this.reset();
    },
    /** 插入h5代码到编辑页面 */
    insertDom(code) {
      let objE = document.createElement("div");
      objE.innerHTML = code;
      return objE.childNodes[0];
    },
    /** 点击回车的处理 */
    keyDownHandle(event) {
      if (event.key === 'Enter') {
        this.selection = window.getSelection();
        this.range = this.selection.getRangeAt(0);
        const br = document.createElement('br');

        this.range.deleteContents();
        this.range.insertNode(br);

        this.range.setStartAfter(br);
        this.range.collapse(true);
        this.selection.removeAllRanges();
        this.selection.addRange(this.range);
      }
      event.preventDefault();
    },
    /**
     * 记录光标位置等 （debounce防抖提升性能）
     */
    handleSelection() {
      this.selection = getSelection();
      // 光标对象
      this.range = this.selection.getRangeAt(0)
      // 获取光标对象的范围界定对象
      this.textNode = this.range.startContainer
      // 获取光标位置
      this.rangeStartOffset = this.range.startOffset;
    },
    /** 插入图片到编辑页面 */
    insertImage(name, url) {
      const parseDom = this.insertDom(`<img alt="${name}" style="width:30px;" @click="handleTag" src="${url}" />`);
      // 在光标处插入dom
      this.range.insertNode(parseDom);
      // 光标开始和光标结束重叠
      this.range.collapse(true);
      this.commentInputEmojiPopoverVisible = false;
    }
  }
}
</script>
<style>
.comment-input-emoji-popover {
  max-width: 240px;
}
.comment-input-emoji-popover > .comment-input-emoji-select {
  display: inline-flex;
  flex-wrap: wrap;
  flex-direction: row;
}
.comment-input-emoji-popover > .comment-input-emoji-select > .el-image {
  width: calc(100% / 6);
  max-width: 40px;
  cursor: pointer;
}
</style>
<style lang="scss" scoped>
.pointer {
  cursor: pointer;
}
.comment-input {
  display: inline-flex;
  width: 100%;
}
.comment-input[type="text"] {
  flex-direction: row;
  justify-content: center;
  align-items: center;
  .comment-input-content {
    flex: 1;
    height:40px;
    overflow-x: auto;
    overflow-y: hidden;
    text-align: left;
    white-space: nowrap;
  }
}
.comment-input[type="textarea"] {
  display: inline-flex;
  flex-direction: column;
  .comment-input-content {
    width: 100%;
    height:100px;
    overflow-x: hidden;
    overflow-y: auto;
    text-align: left;
    white-space: normal;
  }
  .comment-input-content:after {
    content: var(--wordLimitContent);
    position: absolute;
    top: 105px;
    right: 10px;
    background-color: #F2F6FC88;
    color: #909399;
  }
}
.comment-input-content {
  background-color: #F2F6FC;
  border-radius: 5px;
  padding: 10px 20px;
  br {
    display: block;
    content: "";
    margin: 0;
    padding: 0;
  }
}
.comment-input-tools {
  display: inline-flex;
  flex-direction: row;
  justify-content: flex-end;
  align-items: center;
  .emoji-button {
    padding-top: 8px;
    width: 30px;
  }
}
</style>
