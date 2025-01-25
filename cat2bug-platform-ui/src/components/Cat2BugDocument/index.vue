<template>
  <div class="c2d" :style="{width: width}">
    <input class="title" value="title">

    <draggable class="c2d-content" handle=".tools" v-model="componentList">
      <transition-group>
        <component
          v-for="(c,index) in componentList"
          :ref="'c2d'+c.code"
          :key="'c2d'+c.code"
          :is="c.type"
          @mouseover.native="handleMouseOver($event, c, index)"
          @mouseleave.native="handleMouseLeave(c, index)"
          @enter="handleEnter(c, index)"
          @delete="handleDelete(c, index)">
          <template #tools>
            <div v-show="toolsVisible" :ref="'tools'+c.code" class="tools">
              <el-button size="mini" icon="el-icon-plus" />
            </div>
          </template>
        </component>
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
      // 工具是否显示
      toolsVisible: false,
    }
  },
  mounted() {
    this.init();
  },
  methods: {
    init() {
      this.componentList.push(this.makeDefaultComponent())
    },
    makeDefaultComponent() {
      return {
        code: new Date().getTime(),
        type: DEFAULT_COMPONENT_NAME
      };
    },
    handleEnter(component, index) {
      this.componentList.splice(index+1, 0, this.makeDefaultComponent());
      this.setDragComponentFocus(index+1);

    },
    handleDelete(component, index) {
      // 如果是默认Input组件，并且只有一个input，则不删除
      if(component.type==DEFAULT_COMPONENT_NAME && this.componentList.filter(c=>c.type==DEFAULT_COMPONENT_NAME).length==1) {
        return;
      }
      this.componentList.splice(index, 1);
      const preIndex = Math.max(index-1, 0);

      this.setDragComponentFocus(preIndex);
    },
    handleMouseOver(event, c, index) {
      // const top = event.target.offsetTop;
      this.toolsVisible = true;
      // this.$nextTick(()=>{
      //   this.$refs.tools.style.top=top+'px';
      // })
    },
    handleMouseLeave(component, index) {
      this.toolsVisible = false;
    },
    setDragComponentFocus(index) {
      setTimeout(()=>{

      }, 1000)
      this.$nextTick(()=>{
        this.$refs['c2d'+this.componentList[index].code][0].focus();
      });
    }
  }
}
</script>

<style scoped>
.c2d {
  padding: 15px calc(100% / 2 - 800px / 2);
  position: relative;
}
.tools {
  position: absolute;
  left: calc(100% / 2 - 800px / 2 - 50px);
}
.title {
  font-size: 40px;
  border-width: 0px;
  outline: none;
  margin-bottom: 15px;
}
.c2d-content {
  width: 100%;
  display: inline-flex;
  flex-direction: column;
  gap: 10px;
}
</style>
