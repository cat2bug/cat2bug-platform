<template>
  <div>
    <browser-input v-show="!iframeVisible" v-model="testUrl" @input="urlInputHandle" />
    <drag class="browser-tools" v-show="toolsVisible">
      <template v-slot="content">
        <el-row>
          <el-col :span="24">
            <el-tooltip class="item" effect="dark" content="新建测试窗口" placement="left">
              <router-link to="/tools/browser" target="_blank">
                <el-button icon="el-icon-plus" type="danger" plain></el-button>
              </router-link>
            </el-tooltip>
          </el-col>
          <el-col :span="24">
            <el-tooltip class="item" effect="dark" content="跳转到网址输入框" placement="left">
              <el-button type="danger" plain @click="iframeVisible=false"><svg-icon icon-class="input" /></el-button>
            </el-tooltip>
          </el-col>
          <el-divider></el-divider>
          <el-col :span="24">
            <el-tooltip class="item" effect="dark" content="截屏" placement="left">
              <el-button icon="el-icon-scissors" @click="screenShotHandle" type="danger" plain></el-button>
            </el-tooltip>
          </el-col>
          <el-col :span="24">
            <el-tooltip class="item" effect="dark" content="Bug日志" placement="left">
              <el-button type="danger" plain><svg-icon icon-class="bug-list" /></el-button>
            </el-tooltip>
          </el-col>
          <el-divider></el-divider>
          <el-col :span="24">
            <el-tooltip class="item" effect="dark" content="设置" placement="left">
              <el-button icon="el-icon-setting" @click="optionHandle" type="danger" plain></el-button>
            </el-tooltip>
          </el-col>
        </el-row>

  <!--      <el-image-->
  <!--        style="width: 100px; height: 100px"-->
  <!--        v-for="(img,index) in screenShotList"-->
  <!--        :key="index"-->
  <!--        :src="screenShotUrl(img)"-->
  <!--        fit="cover"></el-image>-->
      </template>
    </drag>
    <iframe id="main-iframe" v-show="iframeVisible" :src="testUrl" class="main-iframe" security="restricted"> </iframe>
    <add-defect ref="addDefectForm" :project-id="getProjectId()" />
  </div>
</template>

<script>
import ScreenShot from "js-web-screen-shot";
import AddDefect from "@/components/Defect/AddDefect"
import BrowserInput from "@/views/tool/project/browser/BrowserInput";
import Drag from "@/components/Drag";

import {uploadScreenShot} from "@/api/common/upload";

export default {
  name: "index",
  components: { AddDefect,BrowserInput,Drag },
  data() {
    return {
      // 是否显示工具栏
      toolsVisible: false,
      // 是否显示测试的iframe
      iframeVisible: false,
      // 测试地址
      testUrl: null,
      // 截图上传路径
      uploadUrl: process.env.VUE_APP_BASE_API + "/common/upload/screen-shot",
      screenShotList:[],
    }
  },
  computed:{
    /** 截取的图片网址 */
    screenShotUrl: function (){
      return function (image){
        return process.env.VUE_APP_BASE_API + image.fileUrl
      }
    }
  },
  mounted() {

  },
  methods: {
    /** 测试网址改变的处理 */
    urlInputHandle() {
      this.toolsVisible=true;
      this.iframeVisible=true;
    },
    /** 配置处理 */
    optionHandle() {

    },
    addWindowsHandle() {

    },
    screenShotHandle(){
      this.toolsVisible = false;
      new ScreenShot({
        enableWebRtc:true,
        loadCrossImg:true,
        level:999999,
        clickCutFullScreen: true,
        showScreenData: true,
        completeCallback: this.screenShotSuccessHandle,
        cancelCallback: this.screenShotCancelHandle,
        closeCallback: this.screenShotCancelHandle,
      })
    },
    /** 获取项目id */
    getProjectId() {
      return parseInt(this.$store.state.user.currentProjectId);
    },
    /** 截屏成功的处理 */
    screenShotSuccessHandle(base64Data) {
      this.toolsVisible = false;
      uploadScreenShot({
        srcUrl: window.location.href,
        fileBody: base64Data.base64
      }).then(res=>{
        this.toolsVisible = true;
        this.screenShotList.push(res.data);
        this.$refs.addDefectForm.open({
          imgUrls: res.data.fileUrl
        });
      }).catch(e=>{
        this.toolsVisible = true;
      })
    },
    /** 截屏取消的处理 */
    screenShotCancelHandle(){
      this.toolsVisible = true;
    }
  }
}
</script>

<style lang="scss" scoped>
  .browser-tools {
    position: absolute;
    z-index: 999;
    background-color: rgba(255, 255, 255, 0.5);
    border-radius: 5px;
    padding: 5px;
    border: #DCDFE6 1px solid;
    animation: glow 800ms ease-out infinite alternate;
    .el-divider {
      margin: 5px 0px;
    }
    .el-col {
      float:none;
      margin-bottom: 5px;
    }
    .el-button {
      border-width: 0px;
    }
  }
  .main-iframe {
    z-index: 998;
    width: 100%;
    height: 100%;
    position: absolute;
    bottom: 0px;
    right:0px;
    border-width: 0px;
  }
  @keyframes glow {
    0% {
      border-color: red;
      box-shadow: 0 0 5px red, inset 0 0 5px red, 0 1px 0 red;
    }
    100% {
      border-color: red;
      box-shadow: 0 0 20px red, inset 0 0 10px red, 0 1px 0 red;
    }
  }
</style>
