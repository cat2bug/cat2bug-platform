<template>
  <el-form ref="form" :model="form" :rules="rules" label-width="120px">
    <el-form-item :label="$t('enterprise-wechat')" prop="switch">
      <el-switch v-model="form.switch" @change="handleSwitchChange"></el-switch>
    </el-form-item>
    <el-form-item :label="$t('enterprise-wechat.account')" prop="userId">
      <el-input v-model="form.userId" @input="handleChange" maxlength="128"></el-input>
    </el-form-item>
  </el-form>
</template>

<script>
import {validEmail, validURL} from "@/utils/validate";

export default {
  name: "DingDingNoticePlatform",
  model: {
    prop: 'wechat',
    event: 'change'
  },
  data() {
    return {
      form: this.wechat,
      defaultRules: {
        userId: [
          { required: true, message: this.$i18n.t('enterprise-wechat.account-cannot-empty'), trigger: 'change' },
        ],
      }
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
        this.form = n;
      }
    },
  },
  computed: {
    rules: function (){
      return this.form.switch?this.defaultRules: {}
    }
  },
  methods: {
    /** 操作改变 */
    handleChange() {
      this.$emit('change', this.form);
    },
    /** 处理钉钉开关改变的操作 */
    handleSwitchChange() {
      this.$refs['form'].clearValidate();
      this.handleChange();
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

</style>
