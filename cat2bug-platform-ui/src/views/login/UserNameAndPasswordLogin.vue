<template>
  <div class="login-body">
    <cat-illustration class="login-illustration" />
    <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
      <div class="custom-form-item">
        <label class="item-label">
          <svg-icon icon-class="user" /> {{$t('account')}}
        </label>
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            type="text"
            auto-complete="off"
            :placeholder="$t('account')"
            :maxlength="LOGIN_USERNAME_MAX_LENGTH"
          />
        </el-form-item>
      </div>

      <div class="custom-form-item">
        <label class="item-label">
          <svg-icon icon-class="password" /> {{$t('password')}}
        </label>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            auto-complete="off"
            :placeholder="$t('password')"
            :maxlength="LOGIN_PASSWORD_MAX_LENGTH"
            @keyup.enter.native="handleLogin"
            show-password
          />
        </el-form-item>
      </div>

      <div class="custom-form-item" v-if="captchaEnabled">
        <label class="item-label">{{$t('verification-code')}}</label>
        <el-form-item prop="code">
          <el-input
            v-model="loginForm.code"
            auto-complete="off"
            :placeholder="$t('verification-code')"
            style="width: 63%"
            @keyup.enter.native="handleLogin"
          />
          <div class="login-code">
            <pixel-captcha-canvas
              v-if="captchaExpr"
              :expression="captchaExpr"
              @refresh="getCode"
            />
          </div>
        </el-form-item>
      </div>

      <div class="form-options">
        <el-checkbox v-model="loginForm.rememberMe">{{$t("remember-password")}}</el-checkbox>
        <router-link v-if="register" class="register-link" :to="'/register'">{{$t('register-now')}}</router-link>
      </div>

      <el-form-item class="login-btn-item">
        <el-button
          :loading="loading"
          size="medium"
          class="login-btn"
          @click.native.prevent="handleLogin"
        >
          <span v-if="!loading">{{$t("login")}}<i class="el-icon-right login-btn-arrow"></i></span>
          <span v-else>{{$t("logging-in")}}...</span>
        </el-button>
      </el-form-item>

      <div class="custom-form-item lang-section">
        <label class="item-label">
          <svg-icon icon-class="international" /> Language
        </label>
        <div class="lang-group">
          <svg-icon icon-class="lang_zh_CN" @click="changeLang('zh_CN')" title="简体中文" />
          <svg-icon icon-class="lang_zh_TW" @click="changeLang('zh_TW')" title="繁體中文" />
          <svg-icon icon-class="lang_en_US" @click="changeLang('en_US')" title="English" />
          <svg-icon icon-class="lang_ru" @click="changeLang('ru')" title="Русский" />
          <svg-icon icon-class="lang_ja_JP" @click="changeLang('ja_JP')" title="日本語" />
          <svg-icon icon-class="lang_ko_KR" @click="changeLang('ko_KR')" title="한국어" />
          <svg-icon icon-class="lang_ar" @click="changeLang('ar')" title="العربية" />
        </div>
      </div>

      <div>
        <slot name="tools"></slot>
      </div>

      <div class="login-copyright">Copyright © 2024–2026 版权所有</div>
    </el-form>
  </div>
</template>

<script>
import {getCodeImg} from "@/api/login";
import PixelCaptchaCanvas from "@/components/Captcha/PixelCaptchaCanvas.vue";
import CatIllustration from "./components/CatIllustration.vue";
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
  components: { PixelCaptchaCanvas, CatIllustration },
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
      captchaEnabled: true,
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
      this.loginForm.username = username !== undefined ? username : this.loginForm.username;
      this.loginForm.password = password !== undefined ? decrypt(password) : this.loginForm.password;
      this.loginForm.rememberMe = rememberMe !== undefined ? Boolean(rememberMe) : false;
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
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  position: relative;
  z-index: 2;
}

.login-illustration {
  z-index: 2;
}

.login-form {
  width: 100%;
  max-width: 550px;
  border-radius: 16px;
  background: var(--login-card-bg, var(--card-bg, #ffffff));
  padding: 32px 40px 28px;
  border: 1px solid var(--login-card-border, var(--border-color-base));
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.08);
  position: relative;
  z-index: 3;

  ::v-deep .el-form-item {
    margin-bottom: 0;
  }

  ::v-deep .el-input input {
    height: 35px;
    line-height: 35px;
    padding: 0 14px;
    font-size: 14px;
    background-color: var(--login-input-bg, var(--bg-color-page, #f5f7fa));
    border: 1px solid var(--login-input-border, var(--border-color-base));
    border-radius: 8px;
    color: var(--login-text-primary, var(--text-color-primary));
    transition: border-color 0.2s, box-shadow 0.2s;
  }

  ::v-deep .el-input input:focus {
    border-color: var(--login-accent, #ffc107);
    box-shadow: 0 0 0 2px rgba(255, 193, 7, 0.18);
  }
}

html.dark .login-form {
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.45);
}

.custom-form-item {
  margin-bottom: 16px;

  .item-label {
    display: flex;
    align-items: center;
    gap: 7px;
    font-size: 13px;
    color: var(--login-text-secondary, var(--text-color-secondary));
    margin-bottom: 8px;

    .svg-icon {
      font-size: 15px;
    }
  }
}

.register-link {
  font-size: 13px;
  color: var(--login-accent, #ffc107);
  text-decoration: none;
  &:hover {
    opacity: 0.85;
  }
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 4px 0 16px;
  ::v-deep .el-checkbox__label {
    font-size: 13px;
    color: var(--login-text-regular, var(--text-color-regular));
  }
}

.login-form ::v-deep .login-btn {
  width: 100%;
  height: 39px;
  font-size: 15px;
  font-weight: 700;
  border-radius: 8px;
  border: none !important;
  background: var(--login-accent, #ffc107) !important;
  color: #1a1a1a !important;
  transition: filter 0.2s, transform 0.05s;

  span {
    display: inline-flex;
    align-items: center;
    gap: 8px;
  }

  .login-btn-arrow {
    font-size: 18px;
    font-weight: 700;
  }

  &:hover,
  &:focus {
    filter: brightness(1.05);
    background: var(--login-accent, #ffc107) !important;
    border-color: transparent !important;
    color: #1a1a1a !important;
  }

  &:active {
    transform: translateY(1px);
    background: var(--login-accent, #ffc107) !important;
    color: #1a1a1a !important;
  }
}

.form-options ::v-deep .el-checkbox__input.is-checked .el-checkbox__inner,
.form-options ::v-deep .el-checkbox__input.is-indeterminate .el-checkbox__inner {
  background-color: var(--login-accent, #ffc107);
  border-color: var(--login-accent, #ffc107);
}

.lang-section {
  margin-top: 20px;
  margin-bottom: 4px;
}

.lang-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;

  .svg-icon {
    width: 38px;
    height: 32px;
    padding: 3px 6px;
    box-sizing: border-box;
    border: 1px solid var(--login-input-border, var(--border-color-base));
    border-radius: 6px;
    cursor: pointer;
    opacity: 0.8;
    transition: transform 0.15s, opacity 0.15s, border-color 0.15s;

    &:hover {
      opacity: 1;
      transform: scale(1.1);
      border-color: var(--login-accent, #ffc107);
    }
  }
}

.login-code {
  width: 33%;
  height: 44px;
  float: right;
  box-sizing: border-box;
  border: 1px solid var(--login-card-border, var(--border-color-base));
  border-radius: 8px;
  overflow: hidden;
  background: #FFC145;
  cursor: pointer;
}

.login-copyright {
  margin-top: 16px;
  font-size: 12px;
  color: var(--login-text-muted, var(--text-color-secondary));
  text-align: center;
}
</style>
