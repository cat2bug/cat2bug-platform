<template>
  <div class="cat2bug-browser" :style="`width:${widthPx};height:${heightPx};`">
    <browser-input v-show="!bodyVisible" v-model="url" @input="urlInputHandle" />
    <div class="cat2bug-browser-body" v-show="bodyVisible">
      <div class="cat2bug-browser-header" v-show="screenHeaderToolsVisible">
        <div></div>
  <!--      屏幕尺寸选择-->
        <el-dropdown trigger="click" @command="screenTypeChangedHandle" class="pointer">
          <span>{{activeScreenType.name}}<i class="el-icon-arrow-down el-icon--right"></i></span>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item>满屏</el-dropdown-item>
            <el-dropdown-item v-for="(screen,index) in screenTypeList" :key="index" :divided="index==0" :command="screen.screenSizeId">{{screen.name}}</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
  <!--      屏幕宽高显示-->
        <div class="cat2bug-browser-header-size">
          <el-input v-model="screenWidth" size="mini" @keydown.enter.native="widthChangedHandle()" oninput="value=value.replace(/[^0-9.]/g,'')"></el-input>
          <i class="el-icon-close"></i>
          <el-input v-model="screenHeight" size="mini" @keydown.enter.native="heightChangedHandle()" oninput="value=value.replace(/[^0-9.]/g,'')"></el-input>
        </div>
  <!--      屏幕缩放比选择-->
        <el-dropdown trigger="click" @command="zoomChangedHandle" class="pointer">
          <span>
            {{parseInt(zoom * 100)}}%<i class="el-icon-arrow-down el-icon--right"></i>
          </span>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item v-for="(z,index) in zoomList" :key="index" :command="z">{{z}}</el-dropdown-item>
            <el-dropdown-item divided>自动适应</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
        <!--      屏幕旋转-->
        <svg-icon icon-class="phone-rotate" class="pointer" />
        <!--切换输入网址-->
        <svg-icon icon-class="input" class="pointer" @click="goInputHandle" />
        <div>
          <!--关闭窗口-->
          <svg-icon icon-class="close" class="pointer" @click="closeHandle" />
          <!--最小化工具窗口-->
<!--          <svg-icon icon-class="mini" class="pointer" @click="goInputHandle" />-->
        </div>
      </div>
      <iframe
        ref="mainIframe"
        id="main-iframe" type="text/html"
        allow="*"
        referrerpolicy="no-referrer"
        sandbox="allow-forms allow-modals allow-orientation-lock allow-pointer-lock allow-popups allow-presentation allow-same-origin allow-scripts allow-top-navigation allow-storage-access-by-user-activation allow-top-navigation-by-user-activation"
        :src="url"
        class="cat2bug-browser-iframe"
        :style="`width:${screenWidthPx};height:${screenHeightPx};transform:scale(${zoom});margin-top:${screenHeaderToolsVisible?30:0}px;`" />
<!--      <iframe id="main-iframe" type="text/html"-->
<!--              :src="`/tools/browser/proxy?proxy=${url}`"-->
<!--              class="cat2bug-browser-iframe"-->
<!--              :style="`width:${screenWidthPx};height:${screenHeightPx};transform:scale(${zoom});margin-top:${screenHeaderToolsVisible?30:0}px;`" />-->
    </div>
  </div>
</template>

<script>
import {listScreenSize} from "@/api/system/ScreenSize";
import BrowserInput from "@/views/tool/project/browser/BrowserInput";

export default {
  name: "Cat2bugBrowser",
  components: { BrowserInput },
  props: {
    screenToolsVisible: {
      type: Boolean,
      default: true
    },
    width: {
      type: [Number,String],
      default: window.innerWidth
    },
    height: {
      type: [Number,String],
      default: window.innerHeight
    }
  },
  data() {
    return {
      // 是否显示内容
      bodyVisible: false,
      // 是否显示屏幕工具栏
      screenHeaderToolsVisible: this.screenToolsVisible,
      // 屏幕类型选择列表
      screenTypeList: [],
      // 激活的屏幕类型对象
      activeScreenType: {
        name: '满屏'
      },
      // 访问的网址
      url: null,
      // 缩放比
      zoom: 1,
      // 屏幕宽
      screenWidth: this.width,
      // 屏幕高
      screenHeight: this.height,
      // 缩放比列表
      zoomList: [
        '50%',
        '75%',
        '100%',
        '125%',
        '150%',
        '200%'
      ]
    }
  },
  computed:{
    widthPx: function (){
      return parseInt(this.width).toString().length==(this.width+'').toString().length ? this.width + 'px' : this.width;
    },
    heightPx: function (){
      return parseInt(this.height).toString().length==(this.height+'').toString().length  ? this.height + 'px' : this.height;
    },
    screenWidthPx: function (){
      return parseInt(this.screenWidth).toString().length==(this.screenWidth+'').toString().length ? this.screenWidth + 'px' : this.screenWidth;
    },
    screenHeightPx: function (){
      return parseInt(this.screenHeight).toString().length==(this.screenHeight+'').toString().length ? this.screenHeight + 'px' : this.screenHeight;
    },
  },
  created() {
    this.getScreenTypeList();
  },
  methods: {
    closeHandle(e) {
      this.$emit('close',e);
    },
    urlInputHandle() {
      this.bodyVisible=true;
    },
    zoomChangedHandle(zoom){
      if(zoom) {
        let scale = parseInt(zoom)/100;
        this.setZoom(scale);
      }
    },
    setZoom(zoom) {
      if(zoom){
        this.zoom = zoom
      } else {
        let toolHeight = this.screenHeaderToolsVisible?30:0;
        this.zoom = Math.min(
          this.width/Math.min(this.screenWidth,this.width),
          (this.height-toolHeight)/Math.min(this.screenHeight,this.height-toolHeight));
      }
    },
    widthChangedHandle(){
      this.setSize(this.screenWidthPx,this.screenHeightPx);
    },
    heightChangedHandle(){
      this.setSize(this.screenWidthPx,this.screenHeightPx);
    },
    /** 测试屏幕尺寸改变的处理 */
    setSize(width,height){
      this.screenWidth = parseInt(width);
      this.screenHeight = parseInt(height);
    },
    open(type){
      this.resetScreen(type);
    },
    resetScreen(type) {
      if(!type) {
        type = {
          name: '满屏',
          width: this.width+'px',
          height: this.height+'px'
        };
        this.screenHeaderToolsVisible = false;
      } else {
        this.screenHeaderToolsVisible = true;
      }
      this.activeScreenType = type;
      this.setZoom();
      this.setSize(type.width, type.height);
    },
    /** 获取屏幕尺寸 */
    getScreenTypeList() {
      listScreenSize({
        pageSize: 999,
        pageNum:1
      }).then(res=>{
        this.screenTypeList = res.rows;
      })
    },
    /** 测试屏幕尺寸改变的处理 */
    screenTypeChangedHandle(id){
      if(id){
        let _this = this;
        this.screenTypeList.filter(s=>s.screenSizeId==id).forEach(s=>{
          _this.resetScreen(s);
        });
      } else {
        this.resetScreen();
      }
    },
    goInputHandle() {
      this.bodyVisible = false;
    }
  }
}
</script>

<style lang="scss" scoped>
.cat2bug-browser {
  position: relative;
  display: inline-block;
  margin: 0;
}
.cat2bug-browser-body {
  position: absolute;
  width: 100%;
  height: 100%;
  z-index: 998;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: #606266;
  overflow: auto;
}
.cat2bug-browser-header {
  z-index: 999;
  width: 100%;
  background-color: #303133;
  padding: 5px;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: row;
  border-top: #bbbbbb 1px solid;
  border-bottom: #606266 1px solid;
  position: absolute;
  top: 0px;
  div:first-child, div:last-child {
    flex: 1;
    display: inline-block;
    margin-right: 0px;
  }
  div:first-child {
    > * {
      float: left;
      margin-right: 15px;
    }
  }
  div:last-child {
    > * {
      float: right;
      margin-left: 15px;
    }
  }
  > * {
    margin-right: 15px;
    flex-shrink: 0;
  }
  i {
    color: #FFFFFF;
  }
  span {
    color: #FFFFFF;
    font-size: 12px;
  }
  .el-input {
    width: 50px;
    ::v-deep .el-input__inner {
      padding: 0 5px;
      border-width: 0px;
    }
    ::v-deep input {
      height: 20px;
      line-height: 20px;
      background-color: #303133;
      color: #FFFFFF;
      text-align: center;
      font-size: 11px;
    }
  }
  .svg-icon {
    fill: #FFFFFF;
  }
  .cat2bug-browser-header-size {
    display: flex;
    justify-content: center;
    align-items: center;
    flex-direction: row;
    i {
      font-size: 11px;
    }
  }
}
.cat2bug-browser-iframe {
  z-index: 998;
  border-width: 0px;
}
.pointer {
  cursor: pointer;
}
</style>
