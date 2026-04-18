<template>
  <el-form ref="form" :model="form" :rules="rules" label-width="190px">
    <!-- 单发配置区域 -->
    <el-form-item prop="singleSwitch">
      <template slot="label">
        <span>{{$t('feishu.single-send-config')}}</span>
      </template>
      <el-switch v-model="form.singleSwitch" @change="handleChange"></el-switch>
    </el-form-item>
    <el-form-item prop="mobile">
      <template slot="label">
        <div class="form-label">
          <label>{{$t('feishu.enterprise-mobile')}}</label>
          <el-tooltip class="item" effect="dark" :content="$t('feishu.mobile-illustrate')" placement="bottom">
            <i class="el-icon-question"></i>
          </el-tooltip>
        </div>
      </template>
      <el-input v-model="form.mobile" @input="handleMobileInput" :placeholder="$t('feishu.enter-mobile')" maxlength="32" :disabled="!form.singleSwitch">
        <el-button
          slot="append"
          :loading="mobileTestLoading"
          :disabled="!form.singleSwitch || !hasMobile"
          @click="handleMobileTest">{{$t('feishu.single-test')}}</el-button>
      </el-input>
    </el-form-item>
    <el-alert
      :title="$t('feishu.single-send-notice')"
      type="warning"
      :closable="false"
      show-icon
      style="margin-top: 12px; margin-bottom: 20px;">
    </el-alert>

    <!-- 群发配置区域 -->
    <el-divider></el-divider>
    <el-form-item prop="groupSwitch">
      <template slot="label">
        <span>{{$t('feishu.group-send-config')}}</span>
      </template>
      <el-switch v-model="form.groupSwitch" @change="handleChange"></el-switch>
    </el-form-item>
    <el-form-item prop="key">
      <template slot="label">
        <div class="form-label">
          <label>{{$t('feishu.custom-keyword')}}</label>
          <el-tooltip class="item" effect="dark" :content="$t('feishu.keyword-illustrate')" placement="bottom">
            <i class="el-icon-question"></i>
          </el-tooltip>
        </div>
      </template>
      <el-input v-model="form.key" @input="handleChange" :placeholder="$t('feishu.enter-hook-keyword')" maxlength="128" :disabled="!form.groupSwitch"></el-input>
    </el-form-item>
    <el-form-item prop="secret">
      <template slot="label">
        <div class="form-label">
          <label>{{$t('feishu.signature-verification')}}</label>
          <el-tooltip class="item" effect="dark" :content="$t('feishu.secret-illustrate')" placement="bottom">
            <i class="el-icon-question"></i>
          </el-tooltip>
        </div>
      </template>
      <el-input v-model="form.secret" @input="handleChange" :placeholder="$t('feishu.enter-secret')" maxlength="128" :disabled="!form.groupSwitch"></el-input>
    </el-form-item>
    <el-form-item prop="hook">
      <template slot="label">
        <div class="form-label">
          <label>{{$t('feishu.hook')}}</label>
          <el-tooltip class="item" effect="dark" :content="$t('feishu.hook-illustrate')" placement="bottom">
            <i class="el-icon-question"></i>
          </el-tooltip>
        </div>
      </template>
      <el-input v-model="form.hook" @input="handleHookInput" :placeholder="$t('feishu.enter-hook-url')" maxlength="255" :disabled="!form.groupSwitch">
        <el-button
          slot="append"
          :loading="groupTestLoading"
          :disabled="!form.groupSwitch || !hasHook"
          @click="handleGroupTest">{{$t('feishu.group-test')}}</el-button>
      </el-input>
    </el-form-item>
  </el-form>
</template>

<script>
import {groupTestFeishuNotice, singleTestFeishuNotice} from "@/api/im/feishu";

export default {
  name: "FeishuNoticePlatform",
  model: {
    prop: 'feishu',
    event: 'change'
  },
  data() {
    return {
      form: {},
      mobileTestLoading: false,
      groupTestLoading: false,
      defaultRules: {
        mobile: [
          { required: true, message: this.$i18n.t('feishu.mobile-required'), trigger: 'blur' }
        ],
        hook: [
          { required: true, message: this.$i18n.t('feishu.hook-required'), trigger: 'blur' }
        ]
      }
    }
  },
  props: {
    feishu: {
      type: Object,
      default: ()=>{
        return {}
      }
    }
  },
  watch: {
    feishu: {
      handler: function (n) {
        const next = n || {};
        this.$set(this, 'form', {
          ...next,
          singleSwitch: next.singleSwitch !== undefined ? next.singleSwitch : !!(next.switch && next.mobile),
          groupSwitch: next.groupSwitch !== undefined ? next.groupSwitch : !!(next.switch && next.hook),
          mobile: next.mobile || '',
          key: next.key || '',
          secret: next.secret || '',
          hook: next.hook || ''
        });
      },
      immediate: true,
      deep: true
    }
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
  methods: {
    handleChange() {
      this.$nextTick(() => {
        if (this.$refs['form']) {
          this.$refs['form'].clearValidate();
        }
      });
      this.$emit('change', {
        ...this.feishu,
        ...this.form
      });
    },
    handleMobileInput() {
      this.$forceUpdate();
      this.handleChange();
    },
    handleHookInput() {
      this.$forceUpdate();
      this.handleChange();
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
    },
    handleMobileTest() {
      if (!this.form.singleSwitch) {
        this.$message.warning(this.$i18n.t('feishu.please-enable-single-switch'));
        return;
      }
      const hasMobile = this.form.mobile && this.form.mobile.trim();
      if (!hasMobile) {
        this.$message.warning(this.$i18n.t('feishu.please-enter-mobile'));
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
      singleTestFeishuNotice(testData).then(() => {
        this.mobileTestLoading = false;
        this.$message.success(this.$i18n.t('feishu.test-success'));
      }).catch(err => {
        this.mobileTestLoading = false;
        this.$message.error(this.$i18n.t('feishu.test-failed') + ': ' + (err.msg || err.message || '未知错误'));
      });
    },
    handleGroupTest() {
      if (!this.form.groupSwitch) {
        this.$message.warning(this.$i18n.t('feishu.please-enable-group-switch'));
        return;
      }
      const hasHook = this.form.hook && this.form.hook.trim();
      if (!hasHook) {
        this.$message.warning(this.$i18n.t('feishu.please-enter-hook'));
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
      groupTestFeishuNotice(testData).then(() => {
        this.groupTestLoading = false;
        this.$message.success(this.$i18n.t('feishu.test-success'));
      }).catch(err => {
        this.groupTestLoading = false;
        this.$message.error(this.$i18n.t('feishu.test-failed') + ': ' + (err.msg || err.message || '未知错误'));
      });
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
