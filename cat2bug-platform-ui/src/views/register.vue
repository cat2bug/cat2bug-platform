<template>
  <div class="register" v-if="registerEnabled">
    <el-form ref="registerForm" :model="registerForm" :rules="registerRules" class="register-form">
      <h3 class="title">{{$t('system-name')}}</h3>
      <el-form-item prop="username">
        <el-input v-model="registerForm.username" type="text" auto-complete="off" :placeholder="$t('member.please-enter-account')" maxlength="30">
          <svg-icon slot="prefix" icon-class="user" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item prop="nickName">
        <el-input v-model="registerForm.nickName" type="text" auto-complete="off" :placeholder="$t('member.please-enter-name')" maxlength="30">
          <svg-icon slot="prefix" icon-class="name" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item prop="phoneNumber">
        <el-input
          v-model="registerForm.phoneNumber"
          type="text"
          auto-complete="off"
          :placeholder="$t('member.please-enter-phone-number')"
          maxlength="16"
        >
          <svg-icon slot="prefix" icon-class="shoujihao" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model="registerForm.password"
          type="password"
          auto-complete="off"
          :placeholder="$t('member.please-enter-password')"
          @keyup.enter.native="handleRegister"
          maxlength="30"
        >
          <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item prop="confirmPassword">
        <el-input
          v-model="registerForm.confirmPassword"
          type="password"
          auto-complete="off"
          :placeholder="$t('confirm-password')"
          @keyup.enter.native="handleRegister"
          maxlength="30"
        >
          <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item style="width:100%;">
        <el-button
          :loading="loading"
          size="medium"
          type="primary"
          style="width:100%;"
          @click.native.prevent="handleRegister"
        >
          <span v-if="!loading">{{$t('register')}}</span>
          <span v-else>{{$t('registering')}}...</span>
        </el-button>
        <div style="float: right;">
          <router-link class="link-type" :to="'/login'">{{$t('login-existing-account')}}</router-link>
        </div>
      </el-form-item>
    </el-form>
    <!--  底部  -->
    <div class="el-register-footer">
      <span>Copyright © 2023-2024 cat2but.com All Rights Reserved.</span>
    </div>
  </div>
</template>

<script>
import { register, getCodeImg } from "@/api/login";
import {strFormat} from "@/utils";

export default {
  name: "Register",
  data() {
    const equalToPassword = (rule, value, callback) => {
      if (this.registerForm.password !== value) {
        callback(new Error(this.$i18n.t('entered-passwords-differ').toString()));
      } else {
        callback();
      }
    };
    return {
      registerForm: {
        username: "",
        password: "",
        phoneNumber: "",
        confirmPassword: ""
      },
      registerRules: {
        registerUserName: [
          {required: true, trigger: "blur", message: this.$i18n.t('please-enter-your-account')},
          {min: 2, max: 30, message: this.$i18n.t('account-size-exception'), trigger: 'blur'}
        ],
        registerNickName: [
          {required: true, trigger: "blur", message: this.$i18n.t('please-enter-your-name')},
          {min: 2, max: 30, message: this.$i18n.t('account-size-exception'), trigger: 'blur'}
        ],
        phoneNumber: [
          {required: true, trigger: "blur", message: this.$i18n.t('please-enter-phone')},
          {min: 11, max: 16, message: this.$i18n.t('phone-size-exception'), trigger: 'blur'}
        ],
        password: [
          {required: true, trigger: "blur", message: this.$i18n.t('please-enter-your-password')},
          {min: 5, max: 30, message: this.$i18n.t('password-size-exception'), trigger: 'blur'}
        ],
        confirmPassword: [
          {required: true, trigger: "blur", message: this.$i18n.t('please-enter-your-password-again')},
          {required: true, validator: equalToPassword, trigger: "blur"}
        ]
      },
      loading: false,
      registerEnabled: true
    };
  },
  created() {
    getCodeImg().then(res => {
      this.registerEnabled = res.registerEnabled === undefined ? true : res.registerEnabled;
      if (!this.registerEnabled) {
        this.$message.warning(this.$t('registration-closed'));
        this.$router.replace('/login');
      }
    }).catch(() => {
      this.$router.replace('/login');
    });
  },
  methods: {
    handleRegister() {
      if (!this.registerEnabled) {
        this.$message.warning(this.$t('registration-closed'));
        return;
      }
      this.$refs.registerForm.validate(valid => {
        if (valid) {
          this.loading = true;
          register(this.registerForm).then(res => {
            const username = this.registerForm.username;
            this.$alert("<font color='red'>"+strFormat(this.$i18n.t('account-successfully-registered'),username)+"</font>", this.$i18n.t('prompted').toString(), {
              dangerouslyUseHTMLString: true,
              type: 'success'
            }).then(() => {
              this.$router.push("/login");
            }).catch(() => {});
          }).catch(() => {
            this.loading = false;
          })
        }
      });
    }
  }
};
</script>

<style rel="stylesheet/scss" lang="scss">
.register {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  //background-image: url("../assets/img/login-background.jpg");
  background-size: cover;
  background-color: var(--bg-color-base);
}
.title {
  margin: 0px auto 30px auto;
  text-align: center;
  color: var(--text-color-primary);
}

.register-form {
  border-radius: 6px;
  background: var(--card-bg);
  width: 400px;
  padding: 25px 25px 5px 25px;
  border: 2px solid var(--border-color-base);
  .el-input {
    height: 38px;
    input {
      height: 38px;
    }
  }
  .input-icon {
    height: 39px;
    width: 14px;
    margin-left: 2px;
  }
}
.register-tip {
  font-size: 13px;
  text-align: center;
  color: #bfbfbf;
}
.el-register-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: var(--text-color-secondary);
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 1px;
}
</style>
