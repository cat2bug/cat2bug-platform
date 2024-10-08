<template>
  <div v-show="visible" ref="floatMenu" :style="{ top: top + 'px', left: left + 'px' }" class="cat2bug-float-menu">
    <!-- 拖动元素-->
    <div ref="floatMenuDrag" class="cat2bug-float-menu-draggable-el" v-show="menus.length>1">
      <svg-icon icon-class="drag2" />
    </div>
    <!-- 工具菜单 -->
    <div v-for="(m,mi) in menus" :key="mi" v-show="m.visible" v-if="hasPermi(m.permissions)" class="tools">
      <div
        class="button"
        :plain="m.plain"
        :type="m.type"
        @click="m.click">
        <svg-icon :icon-class="m.icon"></svg-icon>
      </div>
      <p class="tooltipText"><span>{{ prompt(m) }}</span></p>
    </div>
  </div>
</template>

<script>
import Vue from 'vue'
import i18n from "@/utils/i18n/i18n";
import { checkPermi } from "@/utils/permission";

const POSITION_KEY = 'cat2bug-float-menu-drag-position'
const VISIBLE_KEY = 'cat2bug-float-menu-drag-visible'
/**
 * 悬浮菜单
 */
export default {
  name: "Cat2BugFloatMenu",
  components: {Vue},
  data() {
    return {
      visible: true,
      // 拖拽元素
      left:this.defaultLeft,
      top:this.defaultTop,
      dragging: false,
      startX: 0,
      startY: 0,
      currentX: 0,
      currentY: 0,
      currentElement: window.document.body,
      menus: [{
        id: 'top',
        name: '',
        visible: false,
        plain: false,
        type: 'primary',
        icon: 'top',
        permission: null,
        prompt: '置顶',
        click : this.handleTopWindow
      }],
    }
  },
  computed: {
    rootElement: function() {
      return this.$refs.floatMenu;
    },
    /** 拖拽元素 */
    dragElement: function() {
      return this.$refs.floatMenuDrag;
    },
    prompt:function () {
      return function (m) {
        return i18n.te(m.prompt)?i18n.t(m.prompt):m.prompt
      }
    },
    hasPermi: function() {
      return function (permissions) {
        return permissions&&permissions.length>0?this.checkPermi(permissions):true;
      }
    },
    topButton: function () {
      return this.menus.find(m=>m.id=='top');
    }
  },
  mounted() {
    // 初始化拖拽位置
    let pos = this.$cache.local.getJSON(POSITION_KEY);
    if(!pos){
      pos={
        left: window.innerWidth-this.dragElement.clientWidth-100,
        top: 100
      }
    }
    this.rootElement.style.left = `${pos.left}px`
    this.rootElement.style.top = `${pos.top}px`
    this.dragElement.addEventListener('mousedown',this.onMouseDown);
    document.addEventListener('mouseup',this.onMouseUp);
    document.addEventListener('mousemove',this.onMouseMove);
  },
  /** 页面释放 */
  beforeDestroy() {
    this.dragElement.removeEventListener('mousedown',this.onMouseDown);
    document.removeEventListener('mousemove', this.onMouseMove);
    document.removeEventListener('mouseup', this.onMouseUp);
  },
  methods: {
    checkPermi,
    /** 置顶需要操作的窗口初始化 */
    windowsInit(el) {
      this.currentElement = el;
      el.addEventListener('scroll', this.scrollListener) // 监听滚动条
    },
    windowsDestory() {
      if(this.currentElement) {
        this.currentElement.removeEventListener('scroll', this.scrollListener);
      }
    },
    /** 处理置顶操作 */
    handleTopWindow() {
      this.backTop()
    },
    /** 重置菜单 */
    resetMenus() {
      this.menus = [{
        id: 'top',
        name: '',
        visible: false,
        icon: 'top',
        plain: false,
        type: 'primary',
        permission: null,
        prompt: '置顶',
        click : this.handleTopWindow
      }]
    },
    /** 添加菜单 */
    addMenus (menus) {
      this.menus = [...menus||[],...this.menus]
    },
    /** 根据菜单ID移除菜单 */
    removeMenu(id) {
      this.menus = this.menus.filter(m=>m.id!=id);
    },
    setVisible(val) {
      this.visible = val;
      this.$cache.local.set(VISIBLE_KEY, val);
    },
    getVisible() {
      this.visible = (this.$cache.local.get(VISIBLE_KEY)=='true');
      return this.visible;
    },
    /** 返回顶端 */
    backTop() {
      const that = this
      const timer = setInterval(() => {
        const isPeed = Math.floor(-that.scrollTop / 5)
        this.currentElement.scrollTop = document.body.scrollTop = that.scrollTop + isPeed
        if (that.scrollTop === 0) {
          clearInterval(timer)
        }
      }, 16)
    },
    /** 滚动条滚动处理 */
    scrollListener() {
      const that = this
      const scrollTop = this.currentElement.scrollTop
      that.scrollTop = scrollTop
      if (that.scrollTop > 100) {
        that.topButton.visible = true
      } else {
        that.topButton.visible = false
      }
    },
    /** 鼠标点下事件 */
    onMouseDown(event) {
      this.dragging = true
      this.startX = event.clientX - this.rootElement.offsetLeft;
      this.startY = event.clientY - this.rootElement.offsetTop;
      this.dragElement.style.zIndex="2999";
    },
    /** 鼠标移动事件 */
    onMouseMove(event) {
      if (!this.dragging) return
      this.currentX = event.clientX - this.startX
      this.currentY = event.clientY - this.startY
      this.currentX=Math.max(this.currentX,0);
      this.currentY=Math.max(this.currentY,0);
      this.currentX=Math.min(this.currentX,window.innerWidth-this.rootElement.clientWidth);
      this.currentY=Math.min(this.currentY,window.innerHeight-this.rootElement.clientHeight);
      this.rootElement.style.left = `${this.currentX}px`
      this.rootElement.style.top = `${this.currentY}px`
    },
    /** 鼠标点上事件 */
    onMouseUp(event) {
      if(this.dragging) {
        this.dragging = false;
        // 保存拖拽位置
        this.$cache.local.setJSON(POSITION_KEY, {
          left: this.currentX,
          top: this.currentY
        });
        this.currentX = 0
        this.currentY = 0
      }
    },
  }
}
</script>

<style lang="scss" scoped>
.cat2bug-float-menu {
  position: absolute;
  display: inline-flex;
  flex-direction: column;
  gap: 10px;
  z-index: 2198;
  .cat2bug-float-menu-draggable-el {
    display: inline-flex;
    flex-direction: row;
    align-items: center;
    width: 40px;
    font-size: 0.8rem;
    border-radius: 3px;
    background-color: #65686c22;
    cursor: move;
  }
  .cat2bug-float-menu-draggable-el:hover {
    background-color: #65686c44;
  }
  .tools {
    position: relative;
  }
  .button {
    display: inline-flex;
    flex-direction: row;
    justify-content: center;
    align-items: center;
    border-radius: 50%;
    width: 40px;
    height: 40px;
    background: #fff;
    border: 1px solid #dcdfe6;
    color: #606266;
    cursor: pointer;
  }
  .tooltipText {
    visibility: hidden;
    right: 60px;
    background-color: black;
    color: #fff;
    text-align: center;
    padding: 5px 10px;
    border-radius: 3px;
    position: absolute;
    white-space: nowrap;
    top: 5px;
    margin: 0;
    z-index: 2199;
    span{
      display: block;
      position: relative;
      font-size: 12px;
      &::before {
        content: "";
        position: absolute;
        top: -3px;
        right: -14px;
        bottom: 0;
        border-style: solid;
        border-color: transparent;
        border-width: 10px 0 10px 10px;
        border-left-color: black;

      }
    }

  }
  .button:hover+.tooltipText {
    visibility: visible;
  }
  .button:hover {
    color: #409eff;
    border-color: #c6e2ff;
    background-color: #ecf5ff;
    scale: 110%;
  }
  .button[plain='true'] {

  }
  .button[type='danger'], .button[plain='true'][type='danger']:hover {
    color: #fff;
    background-color: #f56c6c;
    border-color: #f56c6c;
  }
  .button[type='danger']:hover, .button[plain='true'][type='danger'] {
    color: #f56c6c;
    background: #fef0f0;
    border-color: #fbc4c4;
  }
  .button[type='warning'], .button[plain='true'][type='warning']:hover {
    color: #fff;
    background-color: rgb(251, 177, 63);
    border-color: rgb(251, 177, 63);
  }
  .button[type='warning']:hover, .button[plain='true'][type='warning'] {
    color: rgb(251, 177, 63);
    background: #fdf6ec;
    border-color: #f5dab1;
  }
  .button[type='success'], .button[plain='true'][type='success']:hover {
    color: #fff;
    background-color: #13ce66;
    border-color: #13ce66;
  }
  .button[type='success']:hover, .button[plain='true'][type='success'] {
    color: #13ce66;
    background: #f0f9eb;
    border-color: #c2e7b0;
  }
  .button[type='info'], .button[plain='true'][type='info']:hover {
    color: #fff;
    background-color: #909399;
    border-color: #909399;
  }
  .button[type='info']:hover, .button[plain='true'][type='info'] {
    color: #909399;
    background: #f4f4f5;
    border-color: #d3d4d6;
  }
  .button[type='primary'], .button[plain='true'][type='primary']:hover {
    background-color: #1890ff;
    border-color: #1890ff;
    color: white;
  }
  .button[type='primary']:hover, .button[plain='true'][type='primary'] {
    color: #1890ff;
    background-color: #e8f4ff;
    border-color: #a3d3ff;
  }
}
</style>
