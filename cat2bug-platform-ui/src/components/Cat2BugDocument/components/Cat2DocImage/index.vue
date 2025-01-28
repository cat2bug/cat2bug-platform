<template>
  <div style="position: relative">
    <slot name="tools"></slot>
    <div class="image-content" :style="{width: imageContent.value.width+'px',height: imageContent.value.height+'px'}">
      <el-image class="c2d-image" ref="c2d-image" :src="require('@/assets/images/demand.png')" />
      <div class="c2d-block" v-for="i in 3" :key="i" :style="{left: dragBlock(i).x+'px', top: dragBlock(i).y+'px'}"></div>
    </div>
<!--    <textarea ref="c2d-input"-->
<!--              :rows="1"-->
<!--      class="c2d-input"-->
<!--              v-model="inputContent.value"-->
<!--      @input="handleInput"-->
<!--      @keypress="handleKeyDown"-->
<!--      @keydown.up="handleUp"-->
<!--      @keydown.left="handleUp"-->
<!--      @keydown.down="handleDown"-->
<!--      @keydown.right="handleDown"-->
<!--      @keydown.delete="handleBack"-->
<!--    ></textarea>-->
  </div>
</template>

<script>
import ImageUpload from "@/components/ImageUpload";
const KEY_ENTER = 13;

export default {
  name: "Cat2DocImage",
  components: { ImageUpload },
  model: {
    prop: 'content',
    event: 'input'
  },
  props: {
    content: {
      type: Object,
      default: ()=>{
        return {
          value: {
            x: 0,
            y: 0,
            width: 100,
            height: 100
          }
        }
      }
    }
  },
  data() {
    return {
      imageContent: this.content,
    }
  },
  mounted() {
    this.$nextTick(()=>{
      console.log(this.c2dImage)
    })

  },
  computed: {
    dragBlock: function () {
      console.log(this.c2dImage)
      return function (index) {
        switch (index) {
          case 0:
            return { x: 0, y:0 }
          case 1:
            return { x: this.c2dImage?this.c2dImage.style.width:0, y:0 }
          case 2:
            return { x: 0, y:0 }
          case 3:
            return { x: 0, y:0 }
        }
      }
    },
    c2dImage() {
      return this.$refs['c2d-image'];
    }
  },
  methods: {
    onDragging(left, top, width, height) {
      console.log(left, top, width, height)
    },
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

<style lang="scss" scoped>
.image-content {
  position: relative;
  > * {
    position: absolute;
  }
  > .c2d-image {
    left: 10px;
    top: 10px;
    width: calc(100% - 10px);
    height: calc(100% - 10px);
    z-index: 1;
  }
  > .c2d-block {
    background-color: red;
    border-radius: 50%;
    width: 20px;
    height: 20px;
    z-index: 2;
  }
}
</style>
