<template>
  <div class="app-container">
    <el-page-header @back="goBack" :content="$t('ding')">
    </el-page-header>
      <el-row class="project-add-page-container">
        <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
          <el-form ref="form" :model="form" :rules="rules" label-width="120px">
            <!--        基础信息-->
            <el-row :gutter="100">
              <el-col :xs="24" :sm="24" :md="16" :lg="16" :xl="12">
                <el-form-item :label="$t('ding.app-id')" prop="appKey">
                  <el-input v-model="form.appKey" maxlength="64" :placeholder="$t('ding.enter-app-id')"></el-input>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="24" :md="16" :lg="16" :xl="12">
                <el-form-item :label="$t('ding.app-secret')" prop="appSecret">
                  <el-input v-model="form.appSecret" maxlength="64" :show-password="true" :placeholder="$t('ding.enter-app-secret')"></el-input>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="24" :md="16" :lg="16" :xl="12">
                <el-form-item :label="$t('ding.robot-code')" prop="robotCode">
                  <el-input v-model="form.robotCode" maxlength="64" :placeholder="$t('ding.enter-robot-code')"></el-input>
                </el-form-item>
              </el-col>
              <!--            保存取消按钮-->
              <el-col :xs="24" :sm="24" :md="16" :lg="16" :xl="12">
                <el-form-item>
                  <el-button type="primary" @click="onSubmit">{{$t('save')}}</el-button>
                  <el-button @click="goBack">{{$t('cancel')}}</el-button>
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
      </el-col>
      <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
<!--        <markdown-it-vue :content="helpContent"></markdown-it-vue>-->
      </el-col>
    </el-row>
  </div>
</template>

<script>
import MarkdownItVue from "markdown-it-vue"
import {getDingConfig, saveDingConfig} from "@/api/im/ding";
import HelpContent from './help.md';
export default {
  name: "EnterpriseWeChat",
  components:{ MarkdownItVue },
  data() {
    return {
      // 钉钉配置
      dingConfig: {},
      // markdown说明内容
      helpContent: "",
      form:{
        appKey: null,             // 应用id
        appSecret: null,          // 应用密钥
        robotCode: null,          // 机器人ID
      },                          // 提交的表单
      // 表单校验
      rules: {
        appKey: [
          { required: true, message: this.$i18n.t('ding.app-id-cannot-empty'), trigger: "change" }
        ],
        appSecret: [
          { required: true, message: this.$i18n.t('ding.app-secret-cannot-empty'), trigger: 'change'}
        ],
        robotCode: [
          { required: true, message: this.$i18n.t('ding.robot-code-cannot-empty'), trigger: 'change'}
        ]
      }
    }
  },
  created() {
    this.getConfig();
    // this.loadMarkdown();
  },
  methods: {
    loadMarkdown() {
      // 假设您有一个本地markdown文件路径
      const markdownPath = require('./help.md');
      //通过fetch请求将.md文件转化为markdown-it-vue可以解析的字符串
      fetch(markdownPath)
        .then(response => response.text())
        .then(markdown => {
          this.helpContent = markdown
        })
        .catch(error => {
          console.error('Error loading markdown:', error);
        });
    },
    /** 获取项目ID */
    getProjectId() {
      return parseInt(this.$store.state.user.config.currentProjectId);
    },
    /** 获取微信企业配置 */
    getConfig() {
      getDingConfig(this.getProjectId()).then(res=>{
        if(res.data) {
          this.dingConfig = res.data;
          if(res.data.configParams) {
            this.form = JSON.parse(res.data.configParams);
          } else {
            this.form = {};
          }
        } else {
          this.reset();
        }
      });
    },
    /** 重置表单 */
    reset() {
      this.wechatConfig = {};
      this.form = {
        corpId: null,             // 企业id
        agentid: null,            // 应用id
        corpSecret: null,         // 应用密钥
      }
      this.resetForm("form");
    },
    /** 返回 */
    goBack() {
      this.$router.back();
    },
    /** 提交按钮 */
    onSubmit() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          let query = JSON.parse(JSON.stringify(this.wechatConfig));
          query.projectId = this.getProjectId();
          query.configParams = JSON.stringify(this.form);
          saveDingConfig(query).then(response => {
            this.$modal.msgSuccess(this.$i18n.t('save.success'));
            this.goBack();
          });
        }
      });
    },
  }
}
</script>

<style lang="scss" scoped>
.el-alert {
  padding: 0px 16px;
}
.project-add-page-container {
  .step2 {
    display: flex;
    display: -webkit-flex; /* Safari */
    flex-direction: column;
    column-gap: 10px;
    row-gap: 10px;
    border-left: #EBEEF5 1px solid;
    :first-child {
      margin-bottom: 10px;
      border-left: 0px;
    }
    :last-child {
      width: 150px;
    }
    > * {
      border-left: 1px solid #5a5e66;
    }
  }
}
</style>
