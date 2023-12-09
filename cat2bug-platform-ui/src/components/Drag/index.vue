<template>
  <div class="cat2bug-draggable-el" :style="{ top: top + 'px', left: left + 'px' }">
    <div class="allow-drag" @mousedown="onMouseDown">
      <svg-icon icon-class="drag2" />
    </div>
    <div class="content">
      <slot></slot>
    </div>
  </div>
</template>

<script>
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
    dialog() {
      return document.querySelector('.cat2bug-draggable-el')
    }
  },
  methods: {
    onMouseDown(event) {
      this.dragging = true
      this.startX = event.clientX - this.dialog.offsetLeft;
      this.startY = event.clientY - this.dialog.offsetTop;
      this.dialog.style.zIndex="99999";
      this.mask = this.createMask();
    },
    onMouseMove(event) {
      if (!this.dragging) return
      this.currentX = event.clientX - this.startX
      this.currentY = event.clientY - this.startY
      this.currentX=Math.max(this.currentX,0);
      this.currentY=Math.max(this.currentY,0);
      this.currentX=Math.min(this.currentX,window.innerWidth-this.dialog.clientWidth);
      this.currentY=Math.min(this.currentY,window.innerHeight-this.dialog.clientHeight);
      this.dialog.style.left = `${this.currentX}px`
      this.dialog.style.top = `${this.currentY}px`
    },
    onMouseUp(event) {
      this.dragging = false;
      this.currentX = 0
      this.currentY = 0
      if(this.mask){
        document.body.removeChild(this.mask);
      }
    },
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
    }
  },
  mounted() {
    document.addEventListener('mousemove', this.onMouseMove)
    document.addEventListener('mouseup', this.onMouseUp)
  },
  beforeDestroy() {
    document.removeEventListener('mousemove', this.onMouseMove)
    document.removeEventListener('mouseup', this.onMouseUp)
  }
}
</script>

<style scoped>
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
}
</style>
