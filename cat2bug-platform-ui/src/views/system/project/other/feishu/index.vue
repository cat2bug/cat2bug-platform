<template>
  <div class="app-container">
    <el-page-header @back="goBack" :content="$t('feishu.robot')">
    </el-page-header>
    <el-row class="project-add-page-container">
      <el-col :xs="24" :sm="24" :md="10" :lg="10" :xl="10">
        <el-form ref="form" :model="form" :rules="rules" label-width="150px">
          <el-form-item :label="$t('feishu.hook')" prop="hook">
            <el-input v-model="form.hook" maxlength="255" :placeholder="$t('feishu.enter-hook-url')"></el-input>
          </el-form-item>
          <el-form-item :label="$t('feishu.secret')" prop="secret">
            <el-input v-model="form.secret" maxlength="64" :show-password="true" :placeholder="$t('feishu.enter-secret')"></el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="onSubmit">{{$t('save')}}</el-button>
            <el-button @click="goBack">{{$t('cancel')}}</el-button>
          </el-form-item>
        </el-form>
      </el-col>
      <el-col :xs="24" :sm="24" :md="14" :lg="14" :xl="14" class="doc">
        <h1 style="font-size: 2rem;">飞书群机器人配置说明</h1>
        <h2>飞书自定义群机器人配置</h2>
        <p>当前配置目的是通过飞书群机器人发送通知消息。飞书自定义机器人无需应用审核，配置简单，步骤如下。</p>
        <p>1. 打开需要接收通知的飞书群，点击群右上角【设置】图标，选择【群机器人】->【添加机器人】。</p>
        <p>2. 在机器人列表中选择【自定义机器人】，填写机器人名称后点击【添加】。</p>
        <p>3. 复制生成的 Webhook 地址，粘贴到左侧【Webhook 地址】栏中。</p>
        <p>4. （可选）如需提升安全性，可开启【签名校验】，将生成的密钥复制到左侧【签名密钥】栏中。</p>
        <p>5. 点击左侧【保存】按钮完成配置。</p>
        <h2>通知说明</h2>
        <p>配置完成后，当项目中的缺陷或报告发生变化时，系统将自动通过飞书群机器人发送通知消息到对应飞书群，无需成员单独配置。</p>
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
        hook: null,
        secret: null,
      },
      rules: {
        hook: [
          { required: true, message: this.$i18n.t('feishu.please-enter-hook'), trigger: 'blur' }
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
        hook: null,
        secret: null,
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
    padding-top: 30px;
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
