<template>
  <div class="app-container">
    <el-page-header @back="goBack" :content="$t('enterprise-wechat')">
    </el-page-header>
    <el-row class="project-add-page-container">
      <el-form ref="form" :model="form" :rules="rules" label-width="150px">
        <!--        基础信息-->
        <el-row :gutter="100">
          <el-col :xs="24" :sm="24" :md="16" :lg="16" :xl="12">
            <el-form-item :label="$t('enterprise-wechat.id')" prop="corpId">
              <el-input v-model="form.corpId" maxlength="64" :placeholder="$t('enterprise-wechat.enter-id')"></el-input>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="16" :lg="16" :xl="12">
            <el-form-item :label="$t('enterprise-wechat.app-id')" prop="agentid">
              <el-input v-model="form.agentid" maxlength="64" :placeholder="$t('enterprise-wechat.enter-app-id')"></el-input>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="16" :lg="16" :xl="12">
            <el-form-item :label="$t('enterprise-wechat.app-secret')" prop="corpSecret">
              <el-input v-model="form.corpSecret" maxlength="64" :placeholder="$t('enterprise-wechat.enter-app-secret')"></el-input>
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="24" :md="16" :lg="16" :xl="12">
            <el-form-item>
              <el-alert
                :title="$t('enterprise-wechat.project-config-remind')"
                type="warning">
              </el-alert>

            </el-form-item>

          </el-col>
          <!--            保存取消按钮-->
          <el-col :xs="24" :sm="24" :md="16" :lg="16" :xl="12">
            <el-form-item>
              <el-button type="primary" @click="onSubmit">{{$t('update')}}</el-button>
              <el-button @click="goBack">{{$t('cancel')}}</el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
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
  computed: {
    /** 计算当前项目图标地址 */
    activeProjectIconUrl: function () {
      return function (index){
        return require('@/assets/images/project/project_icon'+index+'.svg')
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
            this.$modal.msgSuccess(this.$i18n.t('update.success'));
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
    }
    :last-child {
      width: 150px;
    }
  }
}
</style>
