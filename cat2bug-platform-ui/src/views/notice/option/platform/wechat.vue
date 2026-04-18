<template>
  <el-form ref="form" :model="form" :rules="rules" label-width="190px">
    <!-- 单发配置区域 -->
    <el-form-item prop="singleSwitch">
      <template slot="label">
        <span>{{$t('enterprise-wechat.single-send-config')}}</span>
      </template>
      <el-switch v-model="form.singleSwitch" @change="handleChange"></el-switch>
    </el-form-item>
    <el-form-item prop="mobile">
      <template slot="label">
        <div class="form-label">
          <label>{{$t('enterprise-wechat.mobile')}}</label>
          <el-tooltip class="item" effect="dark" :content="$t('enterprise-wechat.mobile-illustrate')" placement="bottom">
            <i class="el-icon-question"></i>
          </el-tooltip>
        </div>
      </template>
      <el-input v-model="form.mobile" @input="handleMobileInput" :placeholder="$t('enterprise-wechat.enter-mobile')" maxlength="32" :disabled="!form.singleSwitch">
        <el-button
          slot="append"
          :loading="mobileTestLoading"
          :disabled="!form.singleSwitch || !hasMobile"
          @click="handleMobileTest">{{$t('enterprise-wechat.single-test')}}</el-button>
      </el-input>
    </el-form-item>
    <el-alert
      :title="$t('enterprise-wechat.single-send-notice')"
      type="warning"
      :closable="false"
      show-icon
      style="margin-top: -10px; margin-bottom: 20px;">
    </el-alert>

    <!-- 群发配置区域 -->
    <el-divider></el-divider>
    <el-form-item prop="groupSwitch">
      <template slot="label">
        <span>{{$t('enterprise-wechat.group-send-config')}}</span>
      </template>
      <el-switch v-model="form.groupSwitch" @change="handleChange"></el-switch>
    </el-form-item>
    <el-form-item prop="hook">
      <template slot="label">
        <div class="form-label">
          <label>{{$t('enterprise-wechat.hook')}}</label>
          <el-tooltip class="item" effect="dark" :content="$t('enterprise-wechat.hook-illustrate')" placement="bottom">
            <i class="el-icon-question"></i>
          </el-tooltip>
        </div>
      </template>
      <el-input v-model="form.hook" @input="handleHookInput" :placeholder="$t('enterprise-wechat.enter-hook-url')" maxlength="255" :disabled="!form.groupSwitch">
        <el-button
          slot="append"
          :loading="groupTestLoading"
          :disabled="!form.groupSwitch || !hasHook"
          @click="handleGroupTest">{{$t('enterprise-wechat.group-test')}}</el-button>
      </el-input>
    </el-form-item>
  </el-form>
</template>

<script>
import {singleTestWeChatNotice, groupTestWeChatNotice} from "@/api/im/wechat";

export default {
  name: "EnterpriseWeChatNoticePlatform",
  model: {
    prop: 'wechat',
    event: 'change'
  },
  data() {
    return {
      form: this.wechat,
      mobileTestLoading: false,
      groupTestLoading: false,
      defaultRules: {}
    }
  },
  props: {
    wechat: {
      type: Object,
      default: ()=>{
        return {}
      }
    }
  },
  watch: {
    wechat: function (n,o) {
      if(n && n!=o) {
        this.form = {
          ...n,
          singleSwitch: n.singleSwitch !== undefined ? n.singleSwitch : !!(n.switch && n.mobile),
          groupSwitch: n.groupSwitch !== undefined ? n.groupSwitch : !!(n.switch && n.hook)
        };
        if (!this.form.mobile && this.$store.state.user.phoneNumber) {
          this.form.mobile = this.$store.state.user.phoneNumber;
          this.handleChange();
        }
      }
    },
  },
  computed: {
    rules: function (){
      return (this.form.singleSwitch || this.form.groupSwitch) ? this.defaultRules : {}
    },
    hasMobile() {
      return this.form.mobile && this.form.mobile.trim().length > 0;
    },
    hasHook() {
      return this.form.hook && this.form.hook.trim().length > 0;
    }
  },
  mounted() {
    this.form = {
      ...this.form,
      singleSwitch: this.form.singleSwitch !== undefined ? this.form.singleSwitch : !!(this.form.switch && this.form.mobile),
      groupSwitch: this.form.groupSwitch !== undefined ? this.form.groupSwitch : !!(this.form.switch && this.form.hook)
    };
    if (!this.form.mobile && this.$store.state.user.phoneNumber) {
      this.form.mobile = this.$store.state.user.phoneNumber;
      this.handleChange();
    }
  },
  methods: {
    handleChange() {
      this.$emit('change', this.form);
    },
    handleMobileInput() {
      this.$forceUpdate();
      this.handleChange();
    },
    handleHookInput() {
      this.$forceUpdate();
      this.handleChange();
    },
    handleMobileTest() {
      if (!this.form.singleSwitch) {
        this.$message.warning(this.$i18n.t('enterprise-wechat.please-enable-single-switch'));
        return;
      }
      const hasMobile = this.form.mobile && this.form.mobile.trim();
      if (!hasMobile) {
        this.$message.warning(this.$i18n.t('enterprise-wechat.please-enter-mobile'));
        return;
      }
      this.mobileTestLoading = true;
      const testData = {
        projectId: this.$store.state.user.config.currentProjectId,
        memberId: this.$store.state.user.id,
        config: {
          singleSwitch: this.form.singleSwitch,
          groupSwitch: this.form.groupSwitch,
          mobile: this.form.mobile,
          hook: ''
        }
      };
      singleTestWeChatNotice(testData).then(() => {
        this.mobileTestLoading = false;
        this.$message.success(this.$i18n.t('enterprise-wechat.test-success'));
      }).catch(err => {
        this.mobileTestLoading = false;
        this.$message.error(this.$i18n.t('enterprise-wechat.test-failed') + ': ' + (err.msg || err.message || '未知错误'));
      });
    },
    handleGroupTest() {
      if (!this.form.groupSwitch) {
        this.$message.warning(this.$i18n.t('enterprise-wechat.please-enable-group-switch'));
        return;
      }
      const hasHook = this.form.hook && this.form.hook.trim();
      if (!hasHook) {
        this.$message.warning(this.$i18n.t('enterprise-wechat.please-enter-hook'));
        return;
      }
      this.groupTestLoading = true;
      const testData = {
        projectId: this.$store.state.user.config.currentProjectId,
        memberId: this.$store.state.user.id,
        config: {
          singleSwitch: this.form.singleSwitch,
          groupSwitch: this.form.groupSwitch,
          mobile: this.form.mobile || '',
          hook: this.form.hook
        }
      };
      groupTestWeChatNotice(testData).then(() => {
        this.groupTestLoading = false;
        this.$message.success(this.$i18n.t('enterprise-wechat.test-success'));
      }).catch(err => {
        this.groupTestLoading = false;
        this.$message.error(this.$i18n.t('enterprise-wechat.test-failed') + ': ' + (err.msg || err.message || '未知错误'));
      });
    },
    resetFields() {
      this.$refs['form'].resetFields();
    },
    validate() {
      const validatePromises = new Promise((resolve, reject) => {
        this.$refs['form'].validate((valid) => {
          if (valid) {
            resolve()
          } else {
            reject()
          }
        })
      });
      if (validatePromises) {
        return Promise.all([validatePromises])
          .then(() => {
            return true;
          }).catch(() => {
            return false;
          });
      } else {
        return false;
      }
    }
  }
}
</script>

<style scoped>
.form-label {
  float: right;
  display: inline-flex;
  flex-direction: row;
  gap: 5px;
  justify-content: center;
  align-items: center;
}
</style>
