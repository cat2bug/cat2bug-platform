<template>
  <div class="c2d" :style="{width: width}">
    <input class="title" value="title">

    <draggable class="c2d-content" handle=".move" v-model="componentList">
      <transition-group>
        <div
          class="c2d-block"
          v-for="(c,index) in componentList"
          :key="'c2d'+c.code"
          @mouseover="handleMouseOver($event, c, index)"
          @mouseleave="handleMouseLeave(c, index)"
        >
          <div class="drop-tools">
            <svg-icon v-show="c.toolsVisible" class="move" icon-class="drag2" />
          </div>
          <div class="content">
            <component
              :ref="'c2d'+c.code"
              :is="c.type"
              :content="c"
              @up="handleUp(c, index, $event)"
              @down="handleDown(c, index, $event)"
              @enter="handleEnter(c, index, $event)"
              @delete="handleDelete(c, index, $event)">
<!--              <template #tools>-->
<!--                <div v-show="c.toolsVisible" class="tools">-->
<!--                  <el-button size="mini" icon="el-icon-plus" />-->
<!--                </div>-->
<!--              </template>-->
            </component>
          </div>
        </div>
      </transition-group>
    </draggable>
  </div>
</template>

<script>
import Cat2DocInput from "@/components/Cat2BugDocument/components/Cat2DocInput";
import draggable from 'vuedraggable'
/** 默认组件名 */
const DEFAULT_COMPONENT_NAME = 'Cat2DocInput';
export default {
  name: "Cat2Doc",
  components: { draggable, Cat2DocInput },
  props: {
    width: {
      type: String,
      default: '100%'
    }
  },
  data() {
    return {
      // 文档标题
      title: '标题',
      // 组件列表
      componentList: [],
    }
  },
  computed: {
    ele: function () {
      return function (index) {
        return this.$refs['c2d'+this.componentList[index].code][0];
      }
    }
  },
  mounted() {
    this.init();
  },
  methods: {
    /** 初始化 */
    init() {
      this.componentList.push(this.makeDefaultComponent());
    },
    /** 创建默认input组件 */
    makeDefaultComponent(value) {
      return {
        code: new Date().getTime(),
        type: DEFAULT_COMPONENT_NAME,
        toolsVisible: false,
        value: value
      };
    },
    /** 处理向上按钮事件 */
    handleUp(component, index, event) {
      if(index>0) {
        this.setDragComponentFocus(index-1);
      }
    },
    /** 处理向下按钮事件 */
    handleDown(component, index, event) {
      if(index<this.componentList.length - 1) {
        this.setDragComponentFocus(index+1);
      }
    },
    /** 处理回车事件 */
    handleEnter(component, index, event) {
      switch (component.type) {
        case DEFAULT_COMPONENT_NAME:
          this.handleInputEnter(component, index, event);
          break;
      }
      this.$nextTick(()=>{
        this.setDragComponentFocus(index+1);
      })
    },
    /** 处理退格删除事件 */
    handleDelete(component, index, event) {
      switch (component.type) {
        case DEFAULT_COMPONENT_NAME:
          this.handleInputDelete(component, index, event);
          break;
      }
      // 设置焦点
      const preIndex = Math.max(index-1, 0);
      this.setDragComponentFocus(preIndex);
    },
    /** 处理Input组件的回车事件 */
    handleInputEnter(component, index, event) {
      setTimeout(()=>{

      },300)
      this.componentList.splice(index+1, 0, this.makeDefaultComponent(event));
      this.$nextTick(()=>{
        this.ele(index+1).selectableRange(0, 0);
      })
    },
    /** 处理Input组件的退格事件 */
    handleInputDelete(component, index, value) {
      // 如果还有文字，将当前组件文字拼接到上一个input组件
      const preIndex = Math.max(index-1, 0);
      if(value && this.componentList[preIndex].type === DEFAULT_COMPONENT_NAME) {
        const start = this.componentList[preIndex].value?this.componentList[preIndex].value.length:0;
        this.ele(preIndex).appendValue(value);
        this.ele(preIndex).selectableRange(start,start);
      }
      // 如果input大于1个，就删除当前的
      if(this.componentList.filter(c=>c.type==DEFAULT_COMPONENT_NAME).length>1) {
        this.componentList.splice(index, 1);
      }
    },
    /** 处理鼠标移入事件 */
    handleMouseOver(event, c, index) {
      c.toolsVisible = true;
      // this.$nextTick(()=>{
      //   this.$refs.tools.style.top=top+'px';
      // })
    },
    /** 处理鼠标移出事件 */
    handleMouseLeave(component, index) {
      component.toolsVisible = false;
    },
    /** 设置组件获取焦点 */
    setDragComponentFocus(index) {
      this.$nextTick(()=>{
        this.ele(index).focus();
      });
    }
  }
}
</script>

<style lang="scss" scoped>
.c2d {
  padding: 15px calc(100% / 2 - 800px / 2);
  /*position: relative;*/
}
.tools {
  position: absolute;
  padding-right: 10px;
  right: calc(100% / 2 + 800px / 2);
  display: inline-flex;
  flex-direction: row;
  gap: 5px;
  align-items: center;
}
.title {
  font-size: 40px;
  border-width: 0px;
  outline: none;
  padding-left: 30px;
  margin-bottom: 15px;
}
.c2d-content {
  width: 100%;
  display: inline-flex;
  flex-direction: column;
  gap: 10px;
}
.c2d-block {
  width: 100%;
  display: inline-flex;
  flex-direction: row;
  align-items: flex-start;
  gap: 0px;
  min-height: 35px;
}
.drop-tools {
  padding: 3px 5px;
  border-radius: 5px;
  width: 28px;
  height: 28px;
  background-color: white;
  overflow: hidden;
}
.c2d-block:hover {
  .drop-tools {
    border: 1px solid #EBEEF5;
    background-color: #F2F6FC;
  }
}
.content {
  width: 100%;
  padding: 0 10px;
}
.move {
  cursor: move;
  flex-shrink: 0;
}
</style>
