<template>
  <div class="cat2bug-browser" :style="`width:${widthPx};height:${heightPx};`">
    <browser-input v-show="!bodyVisible" v-model="url" @input="urlInputHandle" />
    <div class="cat2bug-browser-body" v-show="bodyVisible">
      <div class="cat2bug-browser-header" v-show="screenHeaderToolsVisible">
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
      </div>
      <iframe id="main-iframe" :src="url" class="cat2bug-browser-iframe" security="restricted" :style="`width:${screenWidthPx};height:${screenHeightPx};transform:scale(${zoom});margin-top:${screenHeaderToolsVisible?25:0}px;`"> </iframe>
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
      default: false
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
      return this.width instanceof String ? this.width : this.width + 'px';
    },
    heightPx: function (){
      return this.height instanceof String ? this.height : this.height + 'px';
    },
    screenWidthPx: function (){
      return this.screenWidth instanceof String ? this.screenWidth : this.screenWidth + 'px';
    },
    screenHeightPx: function (){
      return this.screenHeight instanceof String ? this.screenHeight : this.screenHeight + 'px';
    },
  },
  created() {
    this.getScreenTypeList();
  },
  mounted() {
  },
  methods: {
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
        let toolHeight = this.screenHeaderToolsVisible?25:0;
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
  > * {
    margin-right: 15px;
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
