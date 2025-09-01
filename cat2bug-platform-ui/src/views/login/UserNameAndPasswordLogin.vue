<template>
  <div class="login-body">
    <el-image
      class="login-logo"
      :src="require('@/assets/images/cat2bug-logo.gif')"
    ></el-image>
    <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
      <div class="login-form-title">
        <h3 class="title">{{$t("system-name")}}</h3>
        <span v-if="version" class="version">{{ version }}</span>
      </div>
      <el-form-item prop="username">
        <el-input
          v-model="loginForm.username"
          type="text"
          auto-complete="off"
          :placeholder="$t('account')"
          maxlength="30"
        >
          <svg-icon slot="prefix" icon-class="user" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model="loginForm.password"
          type="password"
          auto-complete="off"
          :placeholder="$t('password')"
          maxlength="30"
          @keyup.enter.native="handleLogin"
        >
          <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item prop="code" v-if="captchaEnabled">
        <el-input
          v-model="loginForm.code"
          auto-complete="off"
          :placeholder="$t('verification-code')"
          style="width: 63%"
          @keyup.enter.native="handleLogin"
        >
          <svg-icon slot="prefix" icon-class="validCode" class="el-input__icon input-icon" />
        </el-input>
        <div class="login-code">
          <img :src="codeUrl" @click="getCode" class="login-code-img"/>
        </div>
      </el-form-item>
      <div class="between-row">
        <el-checkbox v-model="loginForm.rememberMe">{{$t("remember-password")}}</el-checkbox>
        <div class="lang-group">
          <svg-icon icon-class="lang_zh_CN" @mouseenter="changeLang('zh_CN')" />
          <svg-icon icon-class="lang_en_US" @mouseenter="changeLang('en_US')" />
        </div>
      </div>
      <div>
        <slot name="tools"></slot>
      </div>
      <el-form-item style="width:100%;">
        <el-button
          :loading="loading"
          size="medium"
          type="primary"
          style="width:100%;"
          @click.native.prevent="handleLogin"
        >
          <span v-if="!loading">{{$t("login")}}</span>
          <span v-else>{{$t("loading")}}...</span>
        </el-button>
        <div style="float: right;" v-if="register">
          <router-link class="link-type" :to="'/register'">{{$t("register-now")}}</router-link>
        </div>
      </el-form-item>
    </el-form>
    <span class="login-copyright">Copyright © 2023-2025 cat2bug.com All Rights Reserved.</span>
  </div>
</template>

<script>
import {getCodeImg} from "@/api/login";
import Cookies from "js-cookie";
import {decrypt, encrypt} from "@/utils/jsencrypt";

export default {
  name: "UserNameAndPasswordLogin",
  data() {
    return {
      codeUrl: "",
      loginForm: {
        username: "",
        password: "",
        rememberMe: false,
        code: "",
        uuid: ""
      },
      loginRules: {
        username: [
          { required: true, trigger: "blur", message: this.$i18n.t('please-enter-your-account') }
        ],
        password: [
          { required: true, trigger: "blur", message: this.$i18n.t('please-enter-your-account') }
        ],
        code: [{ required: true, trigger: "change", message: this.$i18n.t('please-enter-verification-code') }]
      },
      loading: false,
      // 验证码开关
      captchaEnabled: true,
      // 注册开关
      register: true,
      redirect: undefined
    }
  },
  props: {
    version: {
      type: String,
      default: null
    }
  },
  created() {
    this.getCode();
    this.getCookie();
  },
  methods: {
    getCode() {
      getCodeImg().then(res => {
        this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled;
        if (this.captchaEnabled) {
          this.codeUrl = "data:image/gif;base64," + res.img;
          this.loginForm.uuid = res.uuid;
        }
      });
    },
    getCookie() {
      const username = Cookies.get("username");
      const password = Cookies.get("password");
      const rememberMe = Cookies.get('rememberMe')
      this.loginForm = {
        username: username === undefined ? this.loginForm.username : username,
        password: password === undefined ? this.loginForm.password : decrypt(password),
        rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
      };
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true;
          if (this.loginForm.rememberMe) {
            Cookies.set("username", this.loginForm.username, { expires: 30 });
            Cookies.set("password", encrypt(this.loginForm.password), { expires: 30 });
            Cookies.set('rememberMe', this.loginForm.rememberMe, { expires: 30 });
          } else {
            Cookies.remove("username");
            Cookies.remove("password");
            Cookies.remove('rememberMe');
          }
          this.$store.dispatch("Login", this.loginForm).then(() => {
            this.$router.push({ path: this.redirect || "/" }).catch(()=>{});
          }).catch(() => {
            this.loading = false;
            if (this.captchaEnabled) {
              this.getCode();
            }
          });
        }
      });
    },
    changeLang(lang){
      this.$emit('lang', lang);
    }
  }
}
</script>

<style lang="scss" scoped>
.login-body {
  float: right;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.login-logo {
  width: 500px;
  height: 400px;
  z-index: 2;
  margin-top: -100px;
}
.login-form {
  margin-top: -77px;
  border-radius: 6px;
  background: #ffffff;
  width: 500px;
  margin-bottom: 10px;
  padding: 25px 25px 5px 25px;
  border:3px solid #5A5A59;
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
.login-tip {
  font-size: 13px;
  text-align: center;
  color: #bfbfbf;
}
.login-code {
  width: 33%;
  height: 38px;
  float: right;
img {
  cursor: pointer;
  vertical-align: middle;
}
}
.login-copyright {
  font-family: Arial;
  font-size: 14px;
  color: #606266;
}
.login-code-img {
  height: 38px;
}
.login-form-title {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: flex-end;
  padding: 20px;
  .title {
    margin: 0px 10px;
    text-align: center;
    color: #707070;
  }
  .version {
    font-size: 14px;
    color: #707070;
  }
}
</style>
