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
          :maxlength="LOGIN_USERNAME_MAX_LENGTH"
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
          :maxlength="LOGIN_PASSWORD_MAX_LENGTH"
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
          <pixel-captcha-canvas
            v-if="captchaExpr"
            :expression="captchaExpr"
            @refresh="getCode"
          />
        </div>
      </el-form-item>
      <div class="between-row">
        <el-checkbox v-model="loginForm.rememberMe">{{$t("remember-password")}}</el-checkbox>
        <div class="lang-group">
          <svg-icon icon-class="lang_zh_CN" @click="changeLang('zh_CN')" />
          <svg-icon icon-class="lang_zh_CN" @click="changeLang('zh_TW')" />
          <svg-icon icon-class="lang_en_US" @click="changeLang('en_US')" />
          <svg-icon icon-class="lang_ru" @click="changeLang('ru')" />
          <svg-icon icon-class="lang_ja_JP" @click="changeLang('ja_JP')" />
          <svg-icon icon-class="lang_ko_KR" @click="changeLang('ko_KR')" />
          <svg-icon icon-class="lang_ar" @click="changeLang('ar')" />
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
          <span v-else>{{$t("logging-in")}}...</span>
        </el-button>
        <div style="float: right;" v-if="register">
          <router-link class="link-type" :to="'/register'">{{$t("register-now")}}</router-link>
        </div>
      </el-form-item>
    </el-form>
    <site-copyright class="login-copyright" />
  </div>
</template>

<script>
import {getCodeImg} from "@/api/login";
import PixelCaptchaCanvas from "@/components/Captcha/PixelCaptchaCanvas.vue";
import Cookies from "js-cookie";
import {decrypt, encrypt} from "@/utils/jsencrypt";
import { resetSetupStatusCache } from '@/utils/setup-status'
import {
  buildLoginCredentialRules,
  LOGIN_PASSWORD_MAX_LENGTH,
  LOGIN_USERNAME_MAX_LENGTH
} from '@/utils/login-credential-rules'

export default {
  name: "UserNameAndPasswordLogin",
  components: { PixelCaptchaCanvas },
  data() {
    return {
      LOGIN_USERNAME_MAX_LENGTH,
      LOGIN_PASSWORD_MAX_LENGTH,
      captchaExpr: "",
      loginForm: {
        username: "",
        password: "",
        rememberMe: false,
        code: "",
        uuid: ""
      },
      loginRules: {},
      loading: false,
      // 验证码开关
      captchaEnabled: true,
      // 注册开关（由 /captchaImage 返回的 registerEnabled 决定）
      register: false,
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
    this.syncLoginRules()
    this.getCode();
    this.getCookie();
  },
  methods: {
    syncLoginRules() {
      const credentialRules = buildLoginCredentialRules(key => this.$i18n.t(key))
      this.loginRules = {
        ...credentialRules,
        code: [{ required: true, trigger: 'change', message: this.$i18n.t('please-enter-verification-code') }]
      }
    },
    getCode() {
      getCodeImg().then(res => {
        this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled;
        this.register = res.registerEnabled === undefined ? true : res.registerEnabled;
        if (this.captchaEnabled) {
          this.captchaExpr = res.captchaExpr || "";
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
            resetSetupStatusCache()
            /* 从登录页进入主框架时默认折叠侧栏（避免历史 sidebarStatus=1 等仍保持展开） */
            this.$store.dispatch("app/closeSideBar", { withoutAnimation: true });
            const redirect = this.$route.query.redirect || this.redirect || '/'
            return this.$router.push({ path: redirect })
          }).catch(() => {
            if (this.captchaEnabled) {
              this.getCode();
            }
          }).finally(() => {
            this.loading = false
          });
        }
      });
    },
    changeLang(lang){
      this.$emit('lang', lang);
      this.syncLoginRules();
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
  background: var(--card-bg);
  width: 500px;
  margin-bottom: 10px;
  padding: 25px 25px 5px 25px;
  border: 3px solid var(--border-color-base);
  .el-input {
    height: 38px;
    ::v-deep input {
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
  box-sizing: border-box;
  border: 1px solid var(--border-color-base);
  border-radius: 4px;
  overflow: hidden;
  background: #FFC145;
  cursor: pointer;
}
.login-copyright {
  font-family: Arial;
  font-size: 14px;
  color: var(--text-color-secondary);
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
    color: var(--text-color-primary);
  }
  .version {
    font-size: 14px;
    color: var(--text-color-secondary);
  }
}
</style>
