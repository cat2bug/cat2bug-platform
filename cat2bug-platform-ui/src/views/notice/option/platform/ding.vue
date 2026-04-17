<template>
  <el-form ref="form" :model="form" :rules="rules" label-width="190px">
    <div style="margin-bottom: 18px;">
      <span style="font-weight: bold; margin-right: 10px;">{{$t('ding.robot')}}</span>
      <el-switch v-model="form.switch" @change="handleSwitchChange"></el-switch>
    </div>

    <!-- 单发配置区域 -->
    <div style="font-weight: bold; margin-bottom: 18px;">{{$t('ding.single-send-config')}}</div>
    <el-form-item prop="mobile">
      <template slot="label">
        <div class="form-label">
          <label>{{$t('ding.enterprise-mobile')}}</label>
          <el-tooltip class="item" effect="dark" :content="$t('ding.mobile-illustrate')" placement="bottom">
            <i class="el-icon-question"></i>
          </el-tooltip>
        </div>
      </template>
      <el-input v-model="form.mobile" @input="handleMobileInput" :placeholder="$t('ding.enter-mobile')" maxlength="32" :disabled="!form.switch">
        <el-button
          slot="append"
          :loading="mobileTestLoading"
          :disabled="!form.switch || !hasMobile"
          @click="handleMobileTest">{{$t('ding.single-test')}}</el-button>
      </el-input>
    </el-form-item>
    <el-alert
      :title="$t('ding.single-send-notice')"
      type="warning"
      :closable="false"
      show-icon
      style="margin-top: -10px; margin-bottom: 20px;">
    </el-alert>

    <!-- 群发配置区域 -->
    <el-divider></el-divider>
    <div style="font-weight: bold; margin-bottom: 18px;">{{$t('ding.group-send-config')}}</div>
    <el-form-item prop="key">
      <template slot="label">
        <div class="form-label">
          <label>{{$t('ding.custom-keyword')}}</label>
          <el-tooltip class="item" effect="dark" :content="$t('ding.keyword-illustrate')" placement="bottom">
            <i class="el-icon-question"></i>
          </el-tooltip>
        </div>
      </template>
      <el-input v-model="form.key" @input="handleChange" :placeholder="$t('ding.enter-hook-keyword')" maxlength="128" :disabled="!form.switch"></el-input>
    </el-form-item>
    <el-form-item prop="secret">
      <template slot="label">
        <div class="form-label">
          <label>{{$t('ding.signature-verification')}}</label>
          <el-tooltip class="item" effect="dark" :content="$t('ding.secret-illustrate')" placement="bottom">
            <i class="el-icon-question"></i>
          </el-tooltip>
        </div>
      </template>
      <el-input v-model="form.secret" @input="handleChange" :placeholder="$t('ding.enter-secret')" maxlength="128" :disabled="!form.switch"></el-input>
    </el-form-item>
    <el-form-item prop="hook">
      <template slot="label">
        <div class="form-label">
          <label>{{$t('ding.hook')}}</label>
          <el-tooltip class="item" effect="dark" :content="$t('ding.hook-illustrate')" placement="bottom">
            <i class="el-icon-question"></i>
          </el-tooltip>
        </div>
      </template>
      <el-input v-model="form.hook" @input="handleHookInput" :placeholder="$t('ding.enter-hook-url')" maxlength="255" :disabled="!form.switch">
        <el-button
          slot="append"
          :loading="groupTestLoading"
          :disabled="!form.switch || !hasHook"
          @click="handleGroupTest">{{$t('ding.group-test')}}</el-button>
      </el-input>
    </el-form-item>
  </el-form>
</template>

<script>
import {validEmail, validURL} from "@/utils/validate";
import {groupTestDingNotice, singleTestDingNotice} from "@/api/im/ding";

export default {
  name: "DingDingNoticePlatform",
  model: {
    prop: 'ding',
    event: 'change'
  },
  data() {
    let validateUrl = (rule, value, callback) => {
      if(validURL(value)){
        callback();
      } else {
        callback(new Error(this.$i18n.t('ding.format-error').toString()));
      }
    };
    return {
      form: this.ding,
      mobileTestLoading: false,
      groupTestLoading: false,
      defaultRules: {
        // hook: [
        //   { required: true, message: this.$i18n.t('ding.please-enter-hook'), trigger: 'blur' },
        //   { validator: validateUrl, trigger: 'change' }
        // ],
      }
    }
  },
  props: {
    ding: {
      type: Object,
      default: ()=>{
        return {}
      }
    }
  },
  watch: {
    ding: function (n,o) {
      if(n && n!=o) {
        this.form = n;
        // 如果企业手机号未设置，默认显示当前用户的手机号
        if (!this.form.mobile && this.$store.state.user.phoneNumber) {
          this.form.mobile = this.$store.state.user.phoneNumber;
          this.handleChange();
        }
      }
    },
  },
  computed: {
    rules: function (){
      return this.form.switch?this.defaultRules: {}
    },
    hasMobile() {
      return this.form.mobile && this.form.mobile.trim().length > 0;
    },
    hasHook() {
      return this.form.hook && this.form.hook.trim().length > 0;
    }
  },
  mounted() {
    // 如果企业手机号未设置，默认显示当前用户的手机号
    if (!this.form.mobile && this.$store.state.user.phoneNumber) {
      this.form.mobile = this.$store.state.user.phoneNumber;
      this.handleChange();
    }
  },
  methods: {
    /** 操作改变 */
    handleChange() {
      this.$emit('change', this.form);
    },
    /** 处理手机号输入 */
    handleMobileInput() {
      this.$forceUpdate();
      this.handleChange();
    },
    /** 处理 Hook 输入 */
    handleHookInput() {
      this.$forceUpdate();
      this.handleChange();
    },
    /** 处理钉钉开关改变的操作 */
    handleSwitchChange() {
      this.$refs['form'].clearValidate();
      this.handleChange();
    },
    /** 单发测试钉钉通知 */
    handleMobileTest() {
      if (!this.form.switch) {
        this.$message.warning(this.$i18n.t('ding.please-enable-switch'));
        return;
      }

      const hasMobile = this.form.mobile && this.form.mobile.trim();
      if (!hasMobile) {
        this.$message.warning(this.$i18n.t('ding.please-enter-mobile'));
        return;
      }

      this.mobileTestLoading = true;
      const testData = {
        projectId: this.$store.state.user.config.currentProjectId,
        memberId: this.$store.state.user.id,
        config: {
          mobile: this.form.mobile,
          key: '',
          hook: ''
        }
      };

      singleTestDingNotice(testData).then(res => {
        this.mobileTestLoading = false;
        this.$message.success(this.$i18n.t('ding.test-success'));
      }).catch(err => {
        this.mobileTestLoading = false;
        this.$message.error(this.$i18n.t('ding.test-failed') + ': ' + (err.msg || err.message || '未知错误'));
      });
    },
    /** 群发测试钉钉通知 */
    handleGroupTest() {
      if (!this.form.switch) {
        this.$message.warning(this.$i18n.t('ding.please-enable-switch'));
        return;
      }

      const hasHook = this.form.hook && this.form.hook.trim();
      if (!hasHook) {
        this.$message.warning(this.$i18n.t('ding.please-enter-hook'));
        return;
      }

      this.groupTestLoading = true;
      const testData = {
        projectId: this.$store.state.user.config.currentProjectId,
        memberId: this.$store.state.user.id,
        config: {
          mobile: '',
          key: this.form.key,
          hook: this.form.hook
        }
      };

      groupTestDingNotice(testData).then(res => {
        this.groupTestLoading = false;
        this.$message.success(this.$i18n.t('ding.test-success'));
      }).catch(err => {
        this.groupTestLoading = false;
        this.$message.error(this.$i18n.t('ding.test-failed') + ': ' + (err.msg || err.message || '未知错误'));
      });
    },
    /** 重制表单 */
    resetFields() {
      this.$refs['form'].resetFields();
    },
    /** 验证表单 */
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
          .then(res => {
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
