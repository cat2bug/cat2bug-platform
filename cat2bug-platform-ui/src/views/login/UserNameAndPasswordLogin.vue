<template>
  <div ref="loginBody" class="login-body" tabindex="-1">
    <cat-illustration class="login-illustration" />
    <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
      <div class="custom-form-item login-kbd-anchor-host">
        <label class="item-label">
          <svg-icon icon-class="user" /> {{$t('account')}}
        </label>
        <el-form-item prop="username">
          <el-input
            ref="usernameInput"
            v-model="loginForm.username"
            type="text"
            auto-complete="off"
            :placeholder="$t('account')"
            :maxlength="LOGIN_USERNAME_MAX_LENGTH"
          />
        </el-form-item>
        <span class="login-kbd-hint login-kbd-hint-username" aria-hidden="true" />
      </div>

      <div class="custom-form-item login-kbd-anchor-host">
        <label class="item-label">
          <svg-icon icon-class="password" /> {{$t('password')}}
        </label>
        <el-form-item prop="password">
          <el-input
            ref="passwordInput"
            v-model="loginForm.password"
            type="password"
            auto-complete="off"
            :placeholder="$t('password')"
            :maxlength="LOGIN_PASSWORD_MAX_LENGTH"
            @keyup.enter.native="handleLogin"
            show-password
          />
        </el-form-item>
        <span class="login-kbd-hint login-kbd-hint-password" aria-hidden="true" />
      </div>

      <div v-if="captchaEnabled" class="custom-form-item login-kbd-anchor-host">
        <label class="item-label">{{$t('verification-code')}}</label>
        <el-form-item prop="code">
          <el-input
            ref="captchaInput"
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
        <span class="login-kbd-hint login-kbd-hint-captcha" aria-hidden="true" />
        <span class="login-kbd-hint login-kbd-hint-captcha-refresh" aria-hidden="true" />
      </div>

      <div class="form-options">
        <div class="login-remember-host login-kbd-anchor-host">
          <el-checkbox v-model="loginForm.rememberMe">{{$t("remember-password")}}</el-checkbox>
          <span class="login-kbd-hint login-kbd-hint-remember" aria-hidden="true" />
        </div>
        <router-link v-if="register" class="register-link login-kbd-anchor-host" :to="'/register'">
          {{$t('register-now')}}
          <span class="login-kbd-hint login-kbd-hint-register" aria-hidden="true" />
        </router-link>
      </div>

      <div class="login-divider">
        <span class="login-divider-line"></span>
        <span class="login-divider-text">{{$t('login.or')}}</span>
        <span class="login-divider-line"></span>
      </div>

      <div class="custom-form-item lang-section">
        <label class="item-label">
          <svg-icon icon-class="international" /> {{$t('login.language')}}
        </label>
        <div class="lang-group">
          <div
            v-for="lang in loginLangOptions"
            :key="lang.locale"
            class="lang-icon-host login-kbd-anchor-host"
            :class="{ 'lang-icon-host--active': currentLocale === lang.locale }"
            :data-login-lang="lang.locale"
            :title="lang.title"
            @click="changeLang(lang.locale)"
          >
            <svg-icon :icon-class="lang.icon" />
          </div>
        </div>
      </div>

      <el-form-item class="login-btn-item login-kbd-anchor-host">
        <span class="login-kbd-hint login-kbd-hint-submit" aria-hidden="true" />
        <el-button
          :loading="loading"
          size="medium"
          class="login-btn"
          @click.native.prevent="handleLogin"
        >
          <span v-if="!loading">{{$t("login")}}<svg-icon icon-class="login_button" class-name="login-btn-arrow" /></span>
          <span v-else>{{$t("logging-in")}}...</span>
        </el-button>
      </el-form-item>

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
import pageActionHints from '@/mixins/page-action-hints'
import { shortcutStore } from '@/plugins/shortcut/shortcut-store'
import { LOGIN_LANG_ACTION_DEFAULTS } from '@/plugins/shortcut/keymap'

const LOGIN_KBD_SCOPE = 'login'

export default {
  name: "UserNameAndPasswordLogin",
  components: { PixelCaptchaCanvas, CatIllustration },
  mixins: [pageActionHints],
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
  computed: {
    currentLocale() {
      return this.$i18n.locale
    },
    loginLangOptions() {
      return LOGIN_LANG_ACTION_DEFAULTS.map((item) => ({
        ...item,
        title: this.$t(item.titleKey)
      }))
    }
  },
  created() {
    this.syncLoginRules()
    this.getCode();
    this.getCookie();
    this.registerLoginShortcuts()
  },
  mounted() {
    this.registerLoginShortcuts()
  },
  beforeDestroy() {
    if (this.$shortcut) this.$shortcut.unregisterPage(LOGIN_KBD_SCOPE)
  },
  methods: {
    registerLoginShortcuts() {
      if (!this.$shortcut) return
      const langActions = LOGIN_LANG_ACTION_DEFAULTS.map((lang) => ({
        key: lang.key,
        defaultLetter: lang.defaultLetter,
        run: () => this.changeLang(lang.locale)
      }))
      this.$shortcut.registerPage(LOGIN_KBD_SCOPE, [
        { key: 'username', defaultLetter: 'U', run: () => this.focusLoginUsername() },
        { key: 'password', defaultLetter: 'P', run: () => this.focusLoginPassword() },
        { key: 'submit', defaultLetter: 'L', run: () => this.handleLogin() },
        { key: 'remember', defaultLetter: 'O', run: () => this.toggleLoginRemember() },
        { key: 'register', defaultLetter: 'E', run: () => this.goRegister() },
        { key: 'captcha', defaultLetter: 'K', run: () => this.focusLoginCaptcha() },
        { key: 'refreshCaptcha', defaultLetter: 'F', run: () => this.getCode() },
        ...langActions
      ])
    },
    shouldAutoFocusPageActionHost() {
      return true
    },
    getPageActionHintContainer() {
      return this.$refs.loginBody || this.$el
    },
    getPageActionHints() {
      const L = (key, def) => shortcutStore.getLetter(`action.${LOGIN_KBD_SCOPE}.${key}`, def)
      const float = { placement: 'bottom-right-outset', outset: 2 }
      return [
        {
          key: 'username',
          letter: L('username', 'U'),
          badgeSelector: '.login-kbd-hint-username',
          floatOffset: float,
          skipViewportCheck: true,
          run: () => this.focusLoginUsername()
        },
        {
          key: 'password',
          letter: L('password', 'P'),
          badgeSelector: '.login-kbd-hint-password',
          floatOffset: float,
          skipViewportCheck: true,
          run: () => this.focusLoginPassword()
        },
        {
          key: 'submit',
          letter: L('submit', 'L'),
          badgeSelector: '.login-kbd-hint-submit',
          floatOffset: float,
          skipViewportCheck: true,
          run: () => this.handleLogin()
        },
        {
          key: 'remember',
          letter: L('remember', 'O'),
          badgeSelector: '.login-kbd-hint-remember',
          floatOffset: float,
          skipViewportCheck: true,
          run: () => this.toggleLoginRemember()
        },
        {
          key: 'register',
          letter: L('register', 'E'),
          badgeSelector: '.login-kbd-hint-register',
          floatOffset: float,
          skipViewportCheck: true,
          visible: () => this.register,
          run: () => this.goRegister()
        },
        {
          key: 'captcha',
          letter: L('captcha', 'K'),
          badgeSelector: '.login-kbd-hint-captcha',
          floatOffset: float,
          skipViewportCheck: true,
          visible: () => this.captchaEnabled,
          run: () => this.focusLoginCaptcha()
        },
        {
          key: 'refreshCaptcha',
          letter: L('refreshCaptcha', 'F'),
          badgeSelector: '.login-kbd-hint-captcha-refresh',
          floatOffset: { placement: 'bottom-right-outset', outset: 2 },
          skipViewportCheck: true,
          visible: () => this.captchaEnabled,
          run: () => this.getCode()
        }
      ]
    },
    getPageDynamicActionHints({ usedLetters }) {
      const container = this.getPageActionHintContainer()
      if (!container) return []
      const float = { placement: 'bottom-right-outset', outset: 2 }
      return LOGIN_LANG_ACTION_DEFAULTS.map((lang) => {
        const anchor = container.querySelector(`[data-login-lang="${lang.locale}"]`)
        if (!anchor) return null
        const letter = shortcutStore.getLetter(`action.${LOGIN_KBD_SCOPE}.${lang.key}`, lang.defaultLetter)
        const norm = String(letter || lang.defaultLetter || '')
        if (!norm || usedLetters.has(norm)) return null
        return {
          key: lang.key,
          letter: norm,
          anchor,
          skipViewportCheck: true,
          floatOffset: float,
          run: () => this.changeLang(lang.locale)
        }
      }).filter(Boolean)
    },
    focusLoginInput(refName) {
      const comp = this.$refs[refName]
      if (!comp) return
      const input = comp.$refs && comp.$refs.input
      if (input && typeof input.focus === 'function') {
        input.focus()
      }
    },
    focusLoginUsername() {
      this.focusLoginInput('usernameInput')
    },
    focusLoginPassword() {
      this.focusLoginInput('passwordInput')
    },
    focusLoginCaptcha() {
      this.focusLoginInput('captchaInput')
    },
    toggleLoginRemember() {
      this.loginForm.rememberMe = !this.loginForm.rememberMe
    },
    goRegister() {
      if (!this.register) return
      this.$router.push('/register')
    },
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
    changeLang(lang) {
      this.$emit('lang', lang)
      this.syncLoginRules()
      this.$nextTick(() => {
        if (this.$_pageActionModifierHeld) {
          this.$_revealPageActionBadges()
        }
      })
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
    box-shadow: 0 0 0 2px var(--login-accent-glow, rgba(255, 193, 7, 0.18));
  }
}

html.dark .login-form {
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.45);
}

.login-kbd-anchor-host {
  position: relative;
}

.login-kbd-hint {
  position: absolute;
  right: -2px;
  bottom: -4px;
  width: 16px;
  height: 16px;
  pointer-events: none;
  z-index: 2;
}

.login-kbd-hint-captcha-refresh {
  right: 0;
  bottom: 8px;
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

.login-remember-host {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.login-kbd-hint-remember {
  right: -8px;
  bottom: -6px;
}

.register-link {
  position: relative;
  display: inline-block;
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
  color: var(--login-on-accent, #1a1a1a) !important;
  transition: filter 0.2s, transform 0.05s;

  span {
    display: inline-flex;
    align-items: center;
    gap: 14px;
  }

  .login-btn-arrow {
    width: 18px;
    height: 18px;
    flex-shrink: 0;
  }

  &:hover,
  &:focus {
    filter: brightness(1.05);
    background: var(--login-accent, #ffc107) !important;
    border-color: transparent !important;
    color: var(--login-on-accent, #1a1a1a) !important;
  }

  &:active {
    transform: translateY(1px);
    background: var(--login-accent, #ffc107) !important;
    color: var(--login-on-accent, #1a1a1a) !important;
  }
}

.form-options ::v-deep .el-checkbox__input.is-checked .el-checkbox__inner,
.form-options ::v-deep .el-checkbox__input.is-indeterminate .el-checkbox__inner {
  background-color: var(--login-accent, #ffc107);
  border-color: var(--login-accent, #ffc107);
}

.login-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 18px 0 16px;

  .login-divider-line {
    flex: 1;
    height: 1px;
    background-color: var(--login-card-border, var(--border-color-base));
  }

  .login-divider-text {
    flex: 0 0 auto;
    font-size: 13px;
    color: var(--login-text-muted, var(--text-color-secondary));
  }
}

.lang-section {
  margin-bottom: 16px;
}

.lang-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.lang-icon-host {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 32px;
  padding: 3px 6px;
  box-sizing: border-box;
  border: 1px solid var(--login-input-border, var(--border-color-base));
  border-radius: 6px;
  cursor: pointer;
  opacity: 0.8;
  transition: transform 0.15s, opacity 0.15s, border-color 0.15s;

  .svg-icon {
    width: 100%;
    height: 100%;
  }

  &:hover {
    opacity: 1;
    transform: scale(1.1);
    border-color: var(--login-accent, #ffc107);
  }

  &.lang-icon-host--active {
    opacity: 1;
    border-color: var(--login-accent, #ffc107);
    box-shadow: 0 0 0 1px var(--login-accent-glow, rgba(255, 193, 7, 0.18));
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
  background: var(--login-accent, #FFC145);
  cursor: pointer;
}

.login-copyright {
  margin-top: 16px;
  font-size: 12px;
  color: var(--login-text-muted, var(--text-color-secondary));
  text-align: center;
}
</style>
