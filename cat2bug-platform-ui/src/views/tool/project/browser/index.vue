<template>
  <div>
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
          <el-divider></el-divider>
          <el-col :span="24">
            <el-tooltip class="item" effect="dark" content="截屏" placement="left">
              <el-button icon="el-icon-scissors" @click="screenShotHandle" type="danger" plain></el-button>
            </el-tooltip>
          </el-col>
          <el-col :span="24">
            <el-tooltip class="item" effect="dark" content="拆分窗口" placement="left">
              <el-dropdown trigger="click" @command="splitChangedHandle">
                <el-button type="danger" plain><svg-icon icon-class="split" /></el-button>
                <el-dropdown-menu slot="dropdown">
                  <el-dropdown-item command="">单屏</el-dropdown-item>
                  <el-dropdown-item command="horizontal">水平拆分</el-dropdown-item>
                  <el-dropdown-item command="vertical">垂直拆分</el-dropdown-item>
                </el-dropdown-menu>
              </el-dropdown>
            </el-tooltip>
          </el-col>
          <el-col :span="24">
            <el-tooltip class="item" effect="dark" content="Bug日志" placement="left">
              <el-button type="danger" plain @click="defectListClickedHandle"><svg-icon icon-class="bug-list" /></el-button>
            </el-tooltip>
          </el-col>
          <el-divider></el-divider>
          <el-col :span="24">
            <el-tooltip class="item" effect="dark" content="设置" placement="left">
              <el-button icon="el-icon-setting" @click="optionHandle" type="danger" plain></el-button>
            </el-tooltip>
          </el-col>
        </el-row>
      </template>
    </drag>
    <div>
      <cat2bug-split-panel ref="cat2bugSplitPanel" v-model="split" :width="screenWidth" :height="screenHeight">
        <template slot="left">
          <cat2bug-browser width="100%" height="100%" @close="closeLeftBrowserHandle"/>
        </template>
        <template slot="right">
          <cat2bug-browser width="100%" height="100%" @close="closeRightBrowserHandle"/>
        </template>
      </cat2bug-split-panel>
    </div>
    <add-defect ref="addDefectForm" :project-id="getProjectId()" />
    <list-defect ref="listDefect" />
  </div>
</template>

<script>
import ScreenShot from "js-web-screen-shot";
import AddDefect from "@/components/Defect/AddDefect"
import Drag from "@/components/Drag";
import ListDefect from "@/components/Defect/ListDefect"
import Cat2bugBrowser from "@/components/Cat2bugBrowser";
import Cat2bugSplitPanel from "@/components/Cat2bugSplitPanel";
import {uploadScreenShot} from "@/api/common/upload";
import {listScreenSize} from "@/api/system/ScreenSize";

export default {
  name: "index",
  components: { AddDefect,Drag,ListDefect,Cat2bugBrowser,Cat2bugSplitPanel },
  data() {
    return {
      split: '',
      // 是否显示工具栏
      toolsVisible: true,
      // 截图上传路径
      uploadUrl: process.env.VUE_APP_BASE_API + "/common/upload/screen-shot",
      screenShotList:[],
      screenWidth: document.body.clientWidth,
      screenHeight: document.body.clientHeight
    }
  },
  computed:{
    /** 截取的图片网址 */
    screenShotUrl: function (){
      return function (image){
        return process.env.VUE_APP_BASE_API + image.fileUrl
      }
    },
  },
  mounted() {
    window.onresize = () => {
      return (() => {
        this.screenWidth = document.body.clientWidth;
        this.screenHeight = document.body.clientHeight;
      })();
    };
  },
  methods: {
    closeLeftBrowserHandle() {
      this.$refs.cat2bugSplitPanel.hideLeft();
    },
    closeRightBrowserHandle() {
      this.$refs.cat2bugSplitPanel.hideRight();
    },
    splitChangedHandle(split){
      this.split = split;
    },
    /** 配置处理 */
    optionHandle() {

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
      return this.$route.params.projectId?parseInt(this.$route.params.projectId):parseInt(this.$store.state.user.config.currentProjectId);
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
    },
    /** 缺陷列表的点击处理 */
    defectListClickedHandle() {
      this.$refs.listDefect.open();
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
