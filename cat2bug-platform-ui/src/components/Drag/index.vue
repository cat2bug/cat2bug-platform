<template>
  <div class="cat2bug-draggable-el" :style="{ top: top + 'px', left: left + 'px' }">
    <div class="allow-drag" @mousedown="onMouseDown">
      <svg-icon icon-class="drag2" class="cat2bug-draggable-el-drag-button" />
      <svg-icon icon-class="mini" class="cat2bug-draggable-el-mini-button" @click="miniHandle"/>
    </div>
    <div v-show="contentVisible" class="content">
      <slot></slot>
    </div>
  </div>
</template>

<script>
const POSITION_KEY = 'cat2bug-drag-position'
const MINI_KEY = 'cat2bug-drag-mini'

export default {
  name: "index",
  props: {
    defaultTop: {
      type: Number,
      default: 0
    },
    defaultLeft: {
      type: Number,
      default: 0
    }
  },
  data() {
    return {
      contentVisible: true,
      mask:null,
      left:this.defaultLeft,
      top:this.defaultTop,
      dragging: false,
      startX: 0,
      startY: 0,
      currentX: 0,
      currentY: 0
    }
  },
  computed: {
    dragElement() {
      return document.querySelector('.cat2bug-draggable-el')
    }
  },
  methods: {
    /** 鼠标点下事件 */
    onMouseDown(event) {
      this.dragging = true
      this.startX = event.clientX - this.dragElement.offsetLeft;
      this.startY = event.clientY - this.dragElement.offsetTop;
      this.dragElement.style.zIndex="999";
      this.mask = this.createMask();
    },
    /** 鼠标移动事件 */
    onMouseMove(event) {
      if (!this.dragging) return
      this.currentX = event.clientX - this.startX
      this.currentY = event.clientY - this.startY
      this.currentX=Math.max(this.currentX,0);
      this.currentY=Math.max(this.currentY,0);
      this.currentX=Math.min(this.currentX,window.innerWidth-this.dragElement.clientWidth);
      this.currentY=Math.min(this.currentY,window.innerHeight-this.dragElement.clientHeight);
      this.dragElement.style.left = `${this.currentX}px`
      this.dragElement.style.top = `${this.currentY}px`
    },
    /** 鼠标点上事件 */
    onMouseUp(event) {
      if(this.dragging) {
        this.dragging = false;
        // 保存拖拽位置
        this.$cache.session.setJSON(POSITION_KEY, {
          left: this.currentX,
          top: this.currentY
        });
        this.currentX = 0
        this.currentY = 0
        if (this.mask) {
          document.body.removeChild(this.mask);
        }
      }
    },
    /** 创建背景div */
    createMask() {
      let newMask = document.createElement("div");
      newMask.id = 'cat2bug-drag-mask';
      newMask.style.position = "absolute";
      newMask.style.zIndex = "99998";
      newMask.style.width = "100%";
      newMask.style.height = "100%";
      newMask.style.left = "0px";
      newMask.style.top = "0px";
      newMask.style.background = "#00000000";
      document.body.appendChild(newMask);
      return newMask;
    },
    /** 最小化按钮操作 */
    miniHandle() {
      this.contentVisible = !this.contentVisible;
      this.$cache.session.set(MINI_KEY,this.contentVisible);
    }
  },
  /** 页面加载完成 */
  mounted() {
    // 添加事件
    document.addEventListener('mousemove', this.onMouseMove);
    document.addEventListener('mouseup', this.onMouseUp);
    // 获取是否显示内容
    this.contentVisible=!this.$cache.session.get(MINI_KEY) || this.$cache.session.get(MINI_KEY)=='true';

    // 初始化拖拽位置
    let pos = this.$cache.session.getJSON(POSITION_KEY);
    if(!pos){
      pos={
        left: window.innerWidth-this.dragElement.clientWidth-100,
        top: 100
      }
    }
    this.dragElement.style.left = `${pos.left}px`
    this.dragElement.style.top = `${pos.top}px`
  },
  /** 页面释放 */
  beforeDestroy() {
    document.removeEventListener('mousemove', this.onMouseMove);
    document.removeEventListener('mouseup', this.onMouseUp);
  }
}
</script>

<style lang="scss" scoped>
.cat2bug-draggable-el {
  position: absolute;
  background-color: white;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.2);
}
.cat2bug-draggable-el .content {
  padding-top: 10px;
}
.allow-drag {
  width: 100%;
  cursor: move;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  .cat2bug-draggable-el-mini-button {
    cursor: pointer;
    border-radius: 3px;
  }
  .cat2bug-draggable-el-mini-button:hover {
    background-color: #ff4949;
    color: #ffffff;
  }
  .cat2bug-draggable-el-drag-button {
    color: #606266;
  }
}
</style>
