<template>
  <div ref="cat2bugSplitPanel" class="cat2bug-split-panel" :style="`width:${widthPx};height:${heightPx};flex-direction: ${split=='vertical'?'column':'row'};`">
    <div v-show="leftVisible" class="cat2bug-split-panel-left" :style="`width:${leftWidthPx};height:${leftHeightPx};`">
      <slot name="left"></slot>
    </div>
    <div ref="cat2bugSplitPanelSlider"
         v-show="(split=='vertical' || split=='horizontal') && leftVisible && rightVisible"
         class="cat2bug-split-panel-slider" :style="`width:${sliderWidthPx};height:${sliderHeightPx};cursor: ${split=='vertical'?'row-resize':'col-resize'};`"
         @mousedown="onMouseDown"
    ></div>
    <div class="cat2bug-split-panel-right" v-show="rightVisible" :style="`width:${rightWidthPx};height:${rightHeightPx};`">
      <slot name="right"></slot>
    </div>
  </div>
</template>

<script>
const LEFT_SIZE='cat2bug-split-panel-left-size';

export default {
  name: "Cat2bugSplitPanel",
  model: {
    prop: 'split',
    event: 'change'
  },
  props: {
    width:{
      type:[String,Number],
      default: '100%'
    },
    height:{
      type:[String,Number],
      default: '100%'
    },
    split: {
      type: String,
      default: 'vertical'
    },
    splitSize: {
      type: Number,
      default: 8
    }
  },
  data() {
    return {
      mask:null,
      leftSize: 50,
      rightSize: 50,
      dragging: false,
      leftVisible:true,
      rightVisible:false
    }
  },
  watch: {
    split(val) {
      if(val=='vertical' || val=='horizontal'){
        this.leftVisible=true;
        this.rightVisible=true;
      } else if(this.leftVisible && this.rightVisible) {
        this.rightVisible = false;
      }
    }
  },
  computed:{
    cat2bugSplitPanel: function () {
      return this.$refs.cat2bugSplitPanel;
    },
    cat2bugSplitPanelSlider: function () {
      return this.$refs.cat2bugSplitPanelSlider;
    },
    leftWidthPx: function (){
      switch (this.split){
        case 'vertical':
          return '100%';
        case 'horizontal':
          return this.leftSize + '%';
        default:
          return '100%';
      }
    },
    leftHeightPx: function (){
      switch (this.split){
        case 'vertical':
          return this.leftSize + '%';
        case 'horizontal':
          return '100%';
        default:
          return '100%';
      }
    },
    rightWidthPx: function (){
      switch (this.split){
        case 'vertical':
          return '100%';
        case 'horizontal':
          return this.rightSize + '%';
        default:
          return '100%';
      }
    },
    rightHeightPx: function (){
      switch (this.split){
        case 'vertical':
          return this.rightSize + '%';
        case 'horizontal':
          return '100%';
        default:
          return '100%';
      }
    },
    sliderWidthPx: function() {
      if(this.split=='vertical'){
        return '100%';
      } else {
        return this.splitSize+'px'
      }
    },
    sliderHeightPx: function (){
      if(this.split=='horizontal') {
        return '100%';
      } else {
        return this.splitSize+'px'
      }
    },
    widthPx: function (){
      return parseInt(this.width).toString().length==(this.width+'').toString().length ? this.width + 'px' : this.width;
    },
    heightPx: function (){
      return parseInt(this.height).toString().length==(this.height+'').toString().length  ? this.height + 'px' : this.height;
    },
  },
  created() {
    this.leftSize = parseInt(this.$cache.session.get(LEFT_SIZE) || 50);
    this.rightSize = 100 - this.leftSize;
  },
  /** 页面加载完成 */
  mounted() {
    // 添加事件
    document.addEventListener('mousemove', this.onMouseMove);
    document.addEventListener('mouseup', this.onMouseUp);
  },
  /** 页面释放 */
  beforeDestroy() {
    document.removeEventListener('mousemove', this.onMouseMove);
    document.removeEventListener('mouseup', this.onMouseUp);
  },
  methods: {
    hideLeft() {
      if(this.rightVisible){
        this.leftVisible = false;
        this.$emit('change','');
      } else {
        this.$message.warning(this.$i18n.t('browser.window-not-close').toString());
      }
    },
    hideRight() {
      if(this.leftVisible) {
        this.rightVisible = false;
        this.$emit('change','');
      } else {
        this.$message.warning(this.$i18n.t('browser.window-not-close').toString());
      }
    },
    /** 鼠标点下事件 */
    onMouseDown(event) {
      this.dragging = true;
      this.cat2bugSplitPanelSlider.style.zIndex="899";
      this.mask = this.createMask();
    },
    /** 鼠标移动事件 */
    onMouseMove(event) {
      if (!this.dragging) return
      switch(this.split) {
        case 'vertical':
          let diffY = event.clientY - this.cat2bugSplitPanel.offsetTop;
          let rootHeight = parseInt(this.cat2bugSplitPanel.style.height);
          this.leftSize = parseInt(diffY/rootHeight*100);
          break;
        case 'horizontal':
          let diffX = event.clientX - this.cat2bugSplitPanel.offsetLeft;
          let rootWidth = parseInt(this.cat2bugSplitPanel.style.width);
          this.leftSize = parseInt(diffX/rootWidth*100);
          break;
      }
      this.rightSize = 100-this.leftSize;
    },
    /** 鼠标点上事件 */
    onMouseUp(event) {
      if(this.dragging) {
        this.dragging = false;
        if (this.mask) {
          document.body.removeChild(this.mask);
        }
        this.$cache.session.set(LEFT_SIZE,this.leftSize)
      }
    },
    /** 创建背景div */
    createMask() {
      let newMask = document.createElement("div");
      newMask.id = 'cat2bug-drag-mask';
      newMask.style.position = "absolute";
      newMask.style.zIndex = "898";
      newMask.style.width = "100%";
      newMask.style.height = "100%";
      newMask.style.left = "0px";
      newMask.style.top = "0px";
      newMask.style.background = "#00000000";
      document.body.appendChild(newMask);
      return newMask;
    },
  }
}
</script>

<style lang="scss" scoped>
.cat2bug-split-panel {
  display: flex;
  overflow: hidden;
  .cat2bug-split-panel-left, .cat2bug-split-panel-right {
    z-index: 1;
    overflow: hidden;
    iframe {
      z-index: 1;
    }
  }
}
.cat2bug-split-panel-slider {
  width: 5px;
  background-color: #f4516c;
  z-index: 899;
}
</style>
