<template>
  <div class="app-container">
    <el-row class="project-add-page-header">
      <el-page-header @back="goBack" :content="$t('feishu.robot')">
      </el-page-header>
    </el-row>
    <el-row class="project-add-page-container">
      <el-col :xs="24" :sm="24" :md="10" :lg="10" :xl="10">
        <el-form ref="form" :model="form" :rules="rules" label-width="150px">
          <el-form-item :label="$t('feishu.appId')" prop="appId">
            <el-input v-model="form.appId" maxlength="255" :placeholder="$t('feishu.enter-appId')"></el-input>
          </el-form-item>
          <el-form-item :label="$t('feishu.appSecret')" prop="appSecret">
            <el-input v-model="form.appSecret" maxlength="255" :show-password="true" :placeholder="$t('feishu.enter-appSecret')"></el-input>
          </el-form-item>
          <el-form-item class="page-form-actions">
            <div class="page-form-actions__buttons">
              <el-button @click="goBack">{{$t('cancel')}}</el-button>
              <el-button type="primary" @click="onSubmit">{{$t('save')}}</el-button>
            </div>
          </el-form-item>
        </el-form>
      </el-col>
      <el-col :xs="24" :sm="24" :md="14" :lg="14" :xl="14" class="doc">
        <h1 style="font-size: 2rem;">飞书企业应用配置说明</h1>
        <h2>飞书企业自建应用配置</h2>
        <p>当前配置目的是通过飞书企业应用发送通知消息给指定用户。企业应用支持发送个人消息，配置步骤如下。</p>
        <p>1. 登录飞书开放平台（https://open.feishu.cn/），进入【开发者后台】->【企业自建应用】。</p>
        <p>2. 点击【创建企业自建应用】，填写应用名称、描述等信息后创建应用。</p>
        <p>3. 进入应用详情页，在【凭证与基础信息】中找到【App ID】和【App Secret】。</p>
        <p>4. 将【App ID】复制到左侧【应用凭证】栏中，将【App Secret】复制到左侧【应用密钥】栏中。</p>
        <p>5. 在应用的【权限管理】中，添加【以应用身份发消息】权限（im:message）。</p>
        <p>6. 在【版本管理与发布】中创建版本并发布应用，使其在企业内可用。</p>
        <p>7. 点击左侧【保存】按钮完成配置。</p>
        <h2>通知说明</h2>
        <p>配置完成后，用户需要在个人通知设置中配置自己的飞书邮箱地址。</p>
        <p>系统将通过飞书开放平台消息发送 API（/im/v1/messages），使用 receive_id_type=email 直接向用户邮箱发送个人消息通知。</p>
        <p>用户也可以在个人配置中填写飞书群机器人的 Webhook 地址和关键词，无需配置企业应用即可接收群消息通知。</p>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import {getFeishuConfig, saveFeishuConfig} from "@/api/im/feishu";

export default {
  name: "FeishuConfig",
  data() {
    return {
      feishuConfig: {},
      form: {
        appId: null,
        appSecret: null,
      },
      rules: {
        appId: [
          { required: true, message: this.$i18n.t('feishu.please-enter-appId'), trigger: 'blur' }
        ],
        appSecret: [
          { required: true, message: this.$i18n.t('feishu.please-enter-appSecret'), trigger: 'blur' }
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
    /** 获取飞书配置 */
    getConfig() {
      getFeishuConfig(this.getProjectId()).then(res => {
        if (res.data) {
          this.feishuConfig = res.data;
          if (res.data.configParams) {
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
      this.feishuConfig = {};
      this.form = {
        appId: null,
        appSecret: null,
      };
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
          let query = JSON.parse(JSON.stringify(this.feishuConfig));
          query.projectId = this.getProjectId();
          query.configParams = JSON.stringify(this.form);
          saveFeishuConfig(query).then(() => {
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
    padding-top: 10px;
  }
  > * {
    border-left: 1px solid var(--border-color-light);
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
