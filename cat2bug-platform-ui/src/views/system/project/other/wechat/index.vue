<template>
  <div class="app-container">
    <el-page-header @back="goBack" :content="$t('enterprise-wechat')">
    </el-page-header>
    <el-row class="project-add-page-container">
      <el-col :xs="24" :sm="24" :md="10" :lg="10" :xl="10">
      <el-form ref="form" :model="form" :rules="rules" label-width="150px">
        <el-form-item :label="$t('enterprise-wechat.id')" prop="corpId">
          <el-input v-model="form.corpId" maxlength="64" :placeholder="$t('enterprise-wechat.enter-id')"></el-input>
        </el-form-item>
        <el-form-item :label="$t('enterprise-wechat.app-id')" prop="agentid">
          <el-input v-model="form.agentid" maxlength="64" :placeholder="$t('enterprise-wechat.enter-app-id')"></el-input>
        </el-form-item>
        <el-form-item :label="$t('enterprise-wechat.app-secret')" prop="corpSecret">
          <el-input v-model="form.corpSecret" maxlength="64" :show-password="true" :placeholder="$t('enterprise-wechat.enter-app-secret')"></el-input>
        </el-form-item>
      <!--            保存取消按钮-->
        <el-form-item>
          <el-button type="primary" @click="onSubmit">{{$t('save')}}</el-button>
          <el-button @click="goBack">{{$t('cancel')}}</el-button>
        </el-form-item>
      </el-form>
      </el-col>
      <el-col :xs="24" :sm="24" :md="14" :lg="14" :xl="14" class="doc">
        <h1 style="font-size: 2rem;">企业微信配置说明</h1>
        <h2>企业微信平台配置</h2>
        <p>当前配置目的是发送个人通知到企业微信平台，此功能是通过微信【企业内部开发】->【服务器API】->【消息推送】实现的；调用此接口需要先在企业微信平台创建应用，并将配置完成的账号ID配置到Cat2Bug-Platform平台，步骤如下。</p>
        <p>1. 用企业管理员的账号登陆企业微信平台 <a href="https://work.weixin.qq.com">https://work.weixin.qq.com</a></p>
        <p>2. 选择【应用管理】菜单，点击【创建应用】功能创建发送消息的应用。</p>
        <el-image :src="require('@/assets/images/help/wechat/wechat-create-app.png')" />
        <p>3. 上传图标，填写应用名称，可见范围选择您想发送消息的部门，之后点击【创建应用】按钮。</p>
        <el-image :src="require('@/assets/images/help/wechat/wechat-create-app-page1.png')" />
        <p>4. 进入应用界面后配置企业可信IP。</p>
        <el-image :src="require('@/assets/images/help/wechat/wechat-create-app-page2.png')" />
        <p>注意：可信IP为Cat2Bug-Platform平台外网IP地址，不配置此项，企业微信将无法正常接收通知消息。</p>
        <p>5. 将应用详情上方的【AgentId】复制到左侧【应用ID】一栏，将【Secret】复制到左侧【应用凭证】栏中。</p>
        <el-image :src="require('@/assets/images/help/wechat/wechat-app-id.png')" />
        <p>6. 选择导航栏中的【我的企业】菜单，将最下放的【企业ID】复制到左侧栏中，点击【保存】按钮保存配置。</p>
        <el-image :src="require('@/assets/images/help/wechat/wechat-enterprise-id.png')" />
        <h2>配置用户信息</h2>
        <p>如果需要将通知信息单独发送给指定成员，需要获取成员在企业微信中的账号，并配置到Cat2Bug-Platform个人通知配置中，操作步骤如下。</p>
        <p>1. 用企业管理员的账号登陆企业微信平台 <a href="https://work.weixin.qq.com">https://work.weixin.qq.com</a></p>
        <p>2. 在导航菜单中选择【通讯录】，在右侧成员列表中点击需要查看的成员信息，查看并复制成员账号。</p>
        <el-image :src="require('@/assets/images/help/wechat/wechat-user-id.png')" />
        <p>3. 进入Cat2Bug-Platform系统，点击右上角的通知图标，进入后选择右侧配置按钮，将复制的账号粘贴在【接收平台】->【企业微信】->【成员账号】中。</p>
        <el-image :src="require('@/assets/images/help/wechat/wechat-user-config.png')" />
        <p>注意：当用户没有配置账号时，默认采用【个人中心】中配置的默认企业微信账号。</p>
        <p>至此，所有配置完成，当缺陷或报告发生变化后，即可通过企业微信收到消息。</p>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import {addProject, getProject, listProjectRole, updateProject} from "@/api/system/project";
import { listMember } from "@/api/system/team";
import MemberNameplate from "@/components/MemberNameplate"
import {getWeChatConfig, saveWeChatConfig} from "@/api/im/wechat";

export default {
  name: "EnterpriseWeChat",
  components:{ MemberNameplate },
  data() {
    return {
      wechatConfig: {},
      form:{
        corpId: null,             // 企业id
        agentid: null,            // 应用id
        corpSecret: null,         // 应用密钥
      },                          // 提交的表单
      // 表单校验
      rules: {
        corpId: [
          { required: true, message: this.$i18n.t('enterprise-wechat.id-cannot-empty'), trigger: "change" }
        ],
        agentid: [
          { required: true, message: this.$i18n.t('enterprise-wechat.app-id-cannot-empty'), trigger: 'change'}
        ],
        corpSecret: [
          { required: true, message: this.$i18n.t('enterprise-wechat.app-secret-cannot-empty'), trigger: 'change'}
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
      getWeChatConfig(this.getProjectId()).then(res=>{
        if(res.data) {
          this.wechatConfig = res.data;
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
          saveWeChatConfig(query).then(response => {
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
  height: calc(100vh - 160px);
  .el-image {
    overflow: initial !important;
  }
}
</style>
