<template>
  <el-form ref="form" :model="form" :rules="rules" label-width="150px">
    <el-form-item :label="$t('enterprise-wechat')" prop="switch">
      <el-switch v-model="form.switch" @change="handleSwitchChange"></el-switch>
    </el-form-item>
    <el-form-item :label="$t('enterprise-wechat.account')" prop="userId">
      <template slot="label">
        <div class="form-label">
          <el-tooltip class="item" effect="dark" :content="$t('enterprise-wechat.account-illustrate')" placement="bottom">
            <i class="el-icon-question"></i>
          </el-tooltip>
          <label>{{$t('enterprise-wechat.account')}}</label>
        </div>
      </template>
      <el-input v-model="form.userId" @input="handleChange" :placeholder="$t('enterprise-wechat.enter-account-id')" maxlength="128"></el-input>
    </el-form-item>
  </el-form>
</template>

<script>
import {validEmail, validURL} from "@/utils/validate";
import Label from "@/components/Cat2BugStatistic/Components/Label";

export default {
  name: "DingDingNoticePlatform",
  components: {Label},
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
.form-label {
  float: right;
  display: inline-flex;
  flex-direction: row;
  gap: 5px;
  justify-content: center;
  align-items: center;
}
</style>
