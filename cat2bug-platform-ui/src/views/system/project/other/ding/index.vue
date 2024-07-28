<template>
  <div class="app-container">
    <el-page-header @back="goBack" :content="$t('ding')">
    </el-page-header>
      <el-row class="project-add-page-container">
        <el-col :xs="24" :sm="24" :md="10" :lg="10" :xl="10">
          <el-form ref="form" :model="form" :rules="rules" label-width="120px">
            <el-form-item :label="$t('ding.app-id')" prop="appKey">
              <el-input v-model="form.appKey" maxlength="64" :placeholder="$t('ding.enter-app-id')"></el-input>
            </el-form-item>
            <el-form-item :label="$t('ding.app-secret')" prop="appSecret">
              <el-input v-model="form.appSecret" maxlength="64" :show-password="true" :placeholder="$t('ding.enter-app-secret')"></el-input>
            </el-form-item>
            <el-form-item :label="$t('ding.robot-code')" prop="robotCode">
              <el-input v-model="form.robotCode" maxlength="64" :placeholder="$t('ding.enter-robot-code')"></el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="onSubmit">{{$t('save')}}</el-button>
              <el-button @click="goBack">{{$t('cancel')}}</el-button>
            </el-form-item>
          </el-form>
      </el-col>
      <el-col :xs="24" :sm="24" :md="14" :lg="14" :xl="14" class="doc">
        <h1>钉钉配置说明</h1>
        <h2>钉钉开发者平台配置</h2>
        <p>发送通知到钉钉个人功能是通过钉钉【人与机器人会话接口】实现的，目前标准版接口累计可调用次数为1万次/月，如需更多次数调用，可升级钉钉专业版；调用此接口还需要在钉钉开发者平台创建应用机器人，步骤如下。</p>
        <p>1. 用企业管理员的账号登陆钉钉开发者平台 <a href="https://open-dev.dingtalk.com">https://open-dev.dingtalk.com</a></p>
        <p>2. 根据下图步骤创建应用。</p>
        <el-image :src="require('@/assets/images/help/ding/ding_create_app.png')" />
        <p>3. 输入应用配置后点击【保存】按钮。</p>
        <el-image :src="require('@/assets/images/help/ding/ding_create_app_page1.png')" />
        <p>4. 应用保存后，点击【添加应用能力】功能，为应用添加机器人服务。</p>
        <el-image :src="require('@/assets/images/help/ding/ding_create_app_page2.png')" />
        <p>5. 根据自己需求配置机器人选项。</p>
        <el-image :src="require('@/assets/images/help/ding/ding_create_app_page3.png')" />
        <p>6. 根据下图发布版本，使应用转为"已上线"状态。</p>
        <el-image :src="require('@/assets/images/help/ding/ding_create_version.png')" />
        <p>7. 将下图的应用Client ID和Client Secret填入左侧配置项目。</p>
        <el-image :src="require('@/assets/images/help/ding/ding_view_app.png')" />
        <p>8. 将下图的机器人RobotCode填入左侧配置项目。</p>
        <el-image :src="require('@/assets/images/help/ding/ding_view_robot.png')" />
        <h2>配置用户信息</h2>
        <p>如果需要将通知信息单独发送给指定成员，需要获取成员在钉钉企业内的UserId，并配置到Cat2Bug-Platform个人通知配置中，操作步骤如下。</p>
        <p>1. 登陆钉钉OA平台 <a href="https://oa.dingtalk.com">https://oa.dingtalk.com</a></p>
        <p>2. 进入指定企业组织后，选择【通讯录】->【成员管理】菜单，在右侧成员列表中点击需要查看的成员信息，在右侧弹出界面上侧，查看拷贝UserId。</p>
        <el-image :src="require('@/assets/images/help/ding/ding_oa_member.png')" />
        <p>3. 进入Cat2Bug-Platform系统，点击右上角的通知图标，进入后选择右侧配置按钮，将钉钉中获取的UserId设置在【接收平台】->【钉钉机器人】->【单发User ID】中。</p>
        <el-image :src="require('@/assets/images/help/ding/ding_member_config.png')" />
        <p>至此，所有配置完成，当缺陷或报告发生变化后，即可通过钉钉收到消息。</p>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import MarkdownItVue from "markdown-it-vue"
import {getDingConfig, saveDingConfig} from "@/api/im/ding";
// import marked from 'marked';
// import { ref } from 'vue';
// import { readFileSync } from 'fs';

export default {
  name: "EnterpriseWeChat",
  components:{ MarkdownItVue },
  data() {
    return {
      // 钉钉配置
      dingConfig: {},
      // markdown说明内容
      helpContent: '',
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
  },
  methods: {
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
  > *:first-child {
    border-left: 0px;
    padding-left: 0px;
    padding-top: 30px;
  }
  > *:last-child {
  }
  > * {
    border-left: 1px solid #EBEEF5;
    padding-left: 50px;
    padding-right: 50px;
  }
}
.doc {
  display: inline-flex;
  flex-direction: column;
  overflow-y: auto;
}
</style>
