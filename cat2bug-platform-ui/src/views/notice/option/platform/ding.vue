<template>
  <el-form ref="form" :model="form" :rules="rules" label-width="150px">
    <el-form-item :label="$t('ding.robot')" prop="switch">
      <el-switch v-model="form.switch" @change="handleSwitchChange"></el-switch>
    </el-form-item>
    <el-form-item :label="$t('ding.keyword')" prop="key">
      <el-input v-model="form.key" @input="handleChange" maxlength="128"></el-input>
    </el-form-item>
    <el-form-item :label="$t('ding.hook')" prop="hook">
      <el-input v-model="form.hook" @input="handleChange" maxlength="255"></el-input>
    </el-form-item>
  </el-form>
</template>

<script>
import {validEmail, validURL} from "@/utils/validate";

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
      defaultRules: {
        hook: [
          { required: true, message: this.$i18n.t('ding.please-enter-hook'), trigger: 'blur' },
          { validator: validateUrl, trigger: 'change' }
        ],
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
