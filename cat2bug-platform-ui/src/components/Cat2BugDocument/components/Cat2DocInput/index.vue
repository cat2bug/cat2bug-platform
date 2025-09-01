<template>
  <div class="c2d-input">
    <slot name="tools"></slot>
    <textarea ref="c2d-input"
              :rows="1"
      class="c2d-input-textarea"
              v-model="inputContent.value"
      @input="handleInput"
      @keypress="handleKeyDown"
      @keydown.up="handleUp"
      @keydown.left="handleUp"
      @keydown.down="handleDown"
      @keydown.right="handleDown"
      @keydown.delete="handleBack"
    ></textarea>
  </div>
</template>

<script>
const KEY_ENTER = 13;

export default {
  name: "Cat2DocInput",
  model: {
    prop: 'content',
    event: 'input'
  },
  props: {
    content: {
      type: Object,
      default: ()=>{
        return {}
      }
    }
  },
  data() {
    return {
      inputContent: this.content,
    }
  },
  mounted() {
    this.focus();
  },
  computed: {
    textareaContent: function () {
      return this.inputContent.value?this.inputContent.value.replace('\n',''):null;
    },
    c2dInput() {
      return this.$refs['c2d-input'];
    }
  },
  methods: {
    /** 设置焦点 */
    focus() {
      this.$nextTick(()=>{
        this.c2dInput.focus();
      });
    },
    /** 获取Json格式内容 */
    toJson() {

    },
    /** 在尾部添加内容 */
    appendValue(value) {
      this.inputContent.value += value;
      this.$emit('input', this.inputContent);
    },
    /** 设置光标选择范围 */
    selectableRange(start, end) {
      this.$nextTick(()=>{
        this.c2dInput.setSelectionRange(start,end);
      });
    },
    /** 处理input */
    handleInput(event) {
      this.c2dInput.style.height = 'auto';
      this.c2dInput.style.height = (this.c2dInput.scrollHeight) + 'px';
      this.$emit('input', this.inputContent);
    },
    /** 处理按键按下 */
    handleKeyDown(event) {
      console.log(event.keyCode)
      switch (event.keyCode) {
        case KEY_ENTER:
          return this.handleEnter(event);
        default:
          return this.handleString(event);
      }
    },
    /** 处理任意字符 */
    handleString(event) {

    },
    /** 处理回车 */
    handleEnter(event) {
      const start = this.c2dInput.selectionStart;
      const end = this.c2dInput.selectionEnd;
      let frontVal = this.inputContent.value.substr(0,start);
      let afterVal = this.inputContent.value.substr(end);
      this.inputContent.value = frontVal;
      this.handleInput();
      this.$emit('enter', afterVal);
      event.preventDefault();
    },
    /** 处理回车 */
    handleUp(event) {
      const start = this.c2dInput.selectionStart;
      if(start==0) {
        this.$emit('up');
        event.preventDefault();
      }
    },
    /** 处理回车 */
    handleDown(event) {
      const end = this.c2dInput.selectionEnd;
      const len = this.inputContent.value?this.inputContent.value.length:0;
      if(end==len) {
        this.$emit('down');
        event.preventDefault();
      }
    },
    /** 处理退格键 */
    handleBack(event) {
      const selectionEnd = this.c2dInput.selectionEnd;
      if(selectionEnd===0) {
        this.$emit('delete', this.inputContent.value);
        event.preventDefault();
      }
    }
  }
}
</script>

<style scoped>
.c2d-input {
  width: 100%;
  display: inline-flex;
  flex-direction: column;
  justify-content: center;
}
.c2d-input-textarea {
  width: 100%;
  font-size: 20px;
  border-width: 0px;
  border-radius: 5px;
  outline: none;
  resize:none;
}
</style>
