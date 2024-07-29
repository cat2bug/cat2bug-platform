<template>
  <el-form ref="form" :rules="rules" :model="form" label-width="150px">
    <el-form-item :label="$t('email')">
      <el-switch v-model="form.switch" @change="handleSwitchChange"></el-switch>
    </el-form-item>
    <el-form-item :label="$t('receiver-email')" prop="receiver">
      <el-input v-model="form.receiver" @input="handleChange" :placeholder="$t('email.please-enter-receiver')" maxlength="256"></el-input>
    </el-form-item>
  </el-form>
</template>

<script>
import {validEmail} from "@/utils/validate";

export default {
  name: "EMailNoticePlatform",
  model: {
    prop: 'mail',
    event: 'change'
  },
  data() {
    let validateEMail = (rule, value, callback) => {
      if(validEmail(value)){
        callback();
      } else {
        callback(new Error(this.$i18n.t('email.format-error').toString()));
      }
    };
    return {
      form: this.mail,
      defaultRules: {
        receiver: [
          { required: true, message: this.$i18n.t('email.receiver-not-empty'), trigger: 'change' },
          { validator: validateEMail, trigger: 'change' }
        ],
      }
    }
  },
  props: {
    mail: {
      type: Object,
      default: ()=>{
        return {}
      }
    }
  },
  watch: {
    mail: function (n,o) {
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
    /** 处理邮件开关改变的操作 */
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
