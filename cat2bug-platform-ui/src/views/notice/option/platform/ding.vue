<template>
  <el-form ref="form" :model="form" :rules="rules" label-width="190px">
    <!-- 单发配置区域 -->
    <el-form-item prop="singleSwitch">
      <template slot="label">
        <span>{{$t('ding.single-send-config')}}</span>
      </template>
      <el-switch v-model="form.singleSwitch" @change="handleChange"></el-switch>
    </el-form-item>
    <el-form-item prop="mobile">
      <template slot="label">
        <div class="form-label">
          <label>{{$t('ding.enterprise-mobile')}}</label>
          <el-tooltip class="item" effect="dark" :content="$t('ding.mobile-illustrate')" placement="bottom">
            <i class="el-icon-question"></i>
          </el-tooltip>
        </div>
      </template>
      <el-input v-model="form.mobile" @input="handleMobileInput" :placeholder="$t('ding.enter-mobile')" maxlength="32" :disabled="!form.singleSwitch">
        <el-button
          slot="append"
          :loading="mobileTestLoading"
          :disabled="!form.singleSwitch || !hasMobile"
          @click="handleMobileTest">{{$t('ding.single-test')}}</el-button>
      </el-input>
    </el-form-item>
    <el-alert
      :title="$t('ding.single-send-notice')"
      type="warning"
      :closable="false"
      show-icon
      style="margin-top: 12px; margin-bottom: 20px;">
    </el-alert>

    <!-- 群发配置区域 -->
    <el-divider></el-divider>
    <el-form-item prop="groupSwitch">
      <template slot="label">
        <span>{{$t('ding.group-send-config')}}</span>
      </template>
      <el-switch v-model="form.groupSwitch" @change="handleChange"></el-switch>
    </el-form-item>
    <el-form-item prop="key">
      <template slot="label">
        <div class="form-label">
          <label>{{$t('ding.custom-keyword')}}</label>
          <el-tooltip class="item" effect="dark" :content="$t('ding.keyword-illustrate')" placement="bottom">
            <i class="el-icon-question"></i>
          </el-tooltip>
        </div>
      </template>
      <el-input v-model="form.key" @input="handleChange" :placeholder="$t('ding.enter-hook-keyword')" maxlength="128" :disabled="!form.groupSwitch"></el-input>
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
      <el-input v-model="form.secret" @input="handleChange" :placeholder="$t('ding.enter-secret')" maxlength="128" :disabled="!form.groupSwitch"></el-input>
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
      <el-input v-model="form.hook" @input="handleHookInput" :placeholder="$t('ding.enter-hook-url')" maxlength="255" :disabled="!form.groupSwitch">
        <el-button
          slot="append"
          :loading="groupTestLoading"
          :disabled="!form.groupSwitch || !hasHook"
          @click="handleGroupTest">{{$t('ding.group-test')}}</el-button>
      </el-input>
    </el-form-item>
  </el-form>
</template>

<script>
import {groupTestDingNotice, singleTestDingNotice} from "@/api/im/ding";

export default {
  name: "DingDingNoticePlatform",
  model: {
    prop: 'ding',
    event: 'change'
  },
  data() {
    return {
      form: this.ding,
      mobileTestLoading: false,
      groupTestLoading: false,
      autoFilledMobile: false,
      defaultRules: {
        mobile: [
          { required: true, message: this.$i18n.t('ding.mobile-required'), trigger: 'blur' }
        ],
        hook: [
          { required: true, message: this.$i18n.t('ding.hook-required'), trigger: 'blur' }
        ]
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
        this.form = {
          ...n,
          singleSwitch: n.singleSwitch !== undefined ? n.singleSwitch : !!(n.switch && n.mobile),
          groupSwitch: n.groupSwitch !== undefined ? n.groupSwitch : !!(n.switch && n.hook)
        };
        if (!this.form.mobile && !this.autoFilledMobile && this.$store.state.user.phoneNumber) {
          this.form.mobile = this.$store.state.user.phoneNumber;
          this.autoFilledMobile = true;
          this.handleChange();
        }
      }
    },
  },
  computed: {
    rules: function (){
      const rules = {};
      if (this.form.singleSwitch) {
        rules.mobile = this.defaultRules.mobile;
      }
      if (this.form.groupSwitch) {
        rules.hook = this.defaultRules.hook;
      }
      return rules;
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
    if (!this.form.mobile && !this.autoFilledMobile && this.$store.state.user.phoneNumber) {
      this.form.mobile = this.$store.state.user.phoneNumber;
      this.autoFilledMobile = true;
      this.handleChange();
    }
  },
  methods: {
    handleChange() {
      this.$nextTick(() => {
        if (this.$refs['form']) {
          this.$refs['form'].clearValidate();
        }
      });
      this.$emit('change', this.form);
    },
    handleMobileInput() {
      if (!this.form.mobile) {
        this.autoFilledMobile = true;
      }
      this.$forceUpdate();
      this.handleChange();
    },
    handleHookInput() {
      this.$forceUpdate();
      this.handleChange();
    },
    handleMobileTest() {
      if (!this.form.singleSwitch) {
        this.$message.warning(this.$i18n.t('ding.please-enable-single-switch'));
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
          singleSwitch: this.form.singleSwitch,
          groupSwitch: this.form.groupSwitch,
          mobile: this.form.mobile,
          key: '',
          hook: ''
        }
      };
      singleTestDingNotice(testData).then(() => {
        this.mobileTestLoading = false;
        this.$message.success(this.$i18n.t('ding.test-success'));
      }).catch(err => {
        this.mobileTestLoading = false;
        this.$message.error(this.$i18n.t('ding.test-failed') + ': ' + (err.msg || err.message || '未知错误'));
      });
    },
    handleGroupTest() {
      if (!this.form.groupSwitch) {
        this.$message.warning(this.$i18n.t('ding.please-enable-group-switch'));
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
          singleSwitch: this.form.singleSwitch,
          groupSwitch: this.form.groupSwitch,
          mobile: '',
          key: this.form.key,
          secret: this.form.secret,
          hook: this.form.hook
        }
      };
      groupTestDingNotice(testData).then(() => {
        this.groupTestLoading = false;
        this.$message.success(this.$i18n.t('ding.test-success'));
      }).catch(err => {
        this.groupTestLoading = false;
        this.$message.error(this.$i18n.t('ding.test-failed') + ': ' + (err.msg || err.message || '未知错误'));
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
