<template>
  <div class="register" v-if="registerEnabled">
    <div ref="registerBody" class="register-body" tabindex="-1">
      <div class="register-refresh-page-host register-kbd-anchor-host">
        <span class="register-kbd-hint register-kbd-hint-refresh-page" aria-hidden="true" />
      </div>
      <el-form ref="registerForm" :model="registerForm" :rules="registerRules" class="register-form">
        <h3 class="title">{{$t('system-name')}}</h3>
        <div class="register-kbd-anchor-host">
          <el-form-item prop="username">
            <el-input
              ref="usernameInput"
              v-model="registerForm.username"
              type="text"
              auto-complete="off"
              :placeholder="$t('member.please-enter-account')"
              maxlength="30"
            >
              <svg-icon slot="prefix" icon-class="user" class="el-input__icon input-icon" />
            </el-input>
          </el-form-item>
          <span class="register-kbd-hint register-kbd-hint-username" aria-hidden="true" />
        </div>
        <div class="register-kbd-anchor-host">
          <el-form-item prop="nickName">
            <el-input
              ref="nickNameInput"
              v-model="registerForm.nickName"
              type="text"
              auto-complete="off"
              :placeholder="$t('member.please-enter-name')"
              maxlength="30"
            >
              <svg-icon slot="prefix" icon-class="name" class="el-input__icon input-icon" />
            </el-input>
          </el-form-item>
          <span class="register-kbd-hint register-kbd-hint-nickname" aria-hidden="true" />
        </div>
        <div class="register-kbd-anchor-host">
          <el-form-item prop="password">
            <el-input
              ref="passwordInput"
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
          <span class="register-kbd-hint register-kbd-hint-password" aria-hidden="true" />
        </div>
        <div class="register-kbd-anchor-host">
          <el-form-item prop="confirmPassword">
            <el-input
              ref="confirmPasswordInput"
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
          <span class="register-kbd-hint register-kbd-hint-confirm-password" aria-hidden="true" />
        </div>
        <el-form-item class="register-btn-item register-kbd-anchor-host" style="width:100%;">
          <span class="register-kbd-hint register-kbd-hint-submit" aria-hidden="true" />
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
          <div class="register-login-link-wrap">
            <router-link
              class="link-type register-kbd-anchor-host"
              :to="'/login'"
              @click.native.prevent="goLogin"
            >
              {{$t('login-existing-account')}}
              <span class="register-kbd-hint register-kbd-hint-go-login" aria-hidden="true" />
            </router-link>
          </div>
        </el-form-item>
      </el-form>
    </div>
    <div class="el-register-footer">
      <site-copyright />
    </div>
    <command-palette />
  </div>
</template>

<script>
import { register, getCodeImg } from "@/api/login";
import {strFormat} from "@/utils";
import pageActionHints from '@/mixins/page-action-hints'
import { hasBlockingUiLayer } from '@/plugins/shortcut/service'
import { shortcutStore } from '@/plugins/shortcut/shortcut-store'

const REGISTER_KBD_SCOPE = 'register'

export default {
  name: "Register",
  mixins: [pageActionHints],
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
        nickName: "",
        password: "",
        confirmPassword: ""
      },
      registerRules: {
        username: [
          {required: true, trigger: "blur", message: this.$i18n.t('please-enter-your-account')},
          {min: 2, max: 30, message: this.$i18n.t('account-size-exception'), trigger: 'blur'}
        ],
        nickName: [
          {required: true, trigger: "blur", message: this.$i18n.t('please-enter-your-name')},
          {min: 2, max: 30, message: this.$i18n.t('account-size-exception'), trigger: 'blur'}
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
    this.registerRegisterShortcuts()
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
  mounted() {
    this.registerRegisterShortcuts()
    this.$_attachRegisterEscListener()
  },
  beforeDestroy() {
    this.$_detachRegisterEscListener()
    if (this.$shortcut) this.$shortcut.unregisterPage(REGISTER_KBD_SCOPE)
  },
  methods: {
    registerRegisterShortcuts() {
      if (!this.$shortcut) return
      this.$shortcut.registerPage(REGISTER_KBD_SCOPE, [
        { key: 'username', defaultLetter: 'U', run: () => this.focusRegisterUsername() },
        { key: 'nickName', defaultLetter: 'H', run: () => this.focusRegisterNickName() },
        { key: 'password', defaultLetter: 'P', run: () => this.focusRegisterPassword() },
        { key: 'confirmPassword', defaultLetter: 'D', run: () => this.focusRegisterConfirmPassword() },
        { key: 'submit', defaultLetter: 'E', run: () => this.handleRegister() },
        { key: 'goLogin', defaultLetter: 'L', run: () => this.goLogin() },
        { key: 'refreshPage', defaultLetter: 'R', run: () => this.refreshRegisterPage() }
      ])
    },
    shouldAutoFocusPageActionHost() {
      return true
    },
    getPageActionHintContainer() {
      return this.$refs.registerBody || this.$el
    },
    getPageActionHints() {
      const L = (key, def) => shortcutStore.getLetter(`action.${REGISTER_KBD_SCOPE}.${key}`, def)
      const float = { placement: 'bottom-right-outset', outset: 2 }
      return [
        {
          key: 'username',
          letter: L('username', 'U'),
          badgeSelector: '.register-kbd-hint-username',
          floatOffset: float,
          skipViewportCheck: true,
          run: () => this.focusRegisterUsername()
        },
        {
          key: 'nickName',
          letter: L('nickName', 'H'),
          badgeSelector: '.register-kbd-hint-nickname',
          floatOffset: float,
          skipViewportCheck: true,
          run: () => this.focusRegisterNickName()
        },
        {
          key: 'password',
          letter: L('password', 'P'),
          badgeSelector: '.register-kbd-hint-password',
          floatOffset: float,
          skipViewportCheck: true,
          run: () => this.focusRegisterPassword()
        },
        {
          key: 'confirmPassword',
          letter: L('confirmPassword', 'D'),
          badgeSelector: '.register-kbd-hint-confirm-password',
          floatOffset: float,
          skipViewportCheck: true,
          run: () => this.focusRegisterConfirmPassword()
        },
        {
          key: 'submit',
          letter: L('submit', 'E'),
          badgeSelector: '.register-kbd-hint-submit',
          floatOffset: float,
          skipViewportCheck: true,
          run: () => this.handleRegister()
        },
        {
          key: 'goLogin',
          letter: L('goLogin', 'L'),
          badgeSelector: '.register-kbd-hint-go-login',
          floatOffset: float,
          skipViewportCheck: true,
          run: () => this.goLogin()
        },
        {
          key: 'refreshPage',
          letter: L('refreshPage', 'R'),
          badgeSelector: '.register-kbd-hint-refresh-page',
          floatOffset: { placement: 'bottom-right-outset', outset: 2 },
          skipViewportCheck: true,
          run: () => this.refreshRegisterPage()
        }
      ]
    },
    focusRegisterInput(refName) {
      const comp = this.$refs[refName]
      if (!comp) return
      const input = comp.$refs && comp.$refs.input
      if (input && typeof input.focus === 'function') {
        input.focus()
      }
    },
    focusRegisterUsername() {
      this.focusRegisterInput('usernameInput')
    },
    focusRegisterNickName() {
      this.focusRegisterInput('nickNameInput')
    },
    focusRegisterPassword() {
      this.focusRegisterInput('passwordInput')
    },
    focusRegisterConfirmPassword() {
      this.focusRegisterInput('confirmPasswordInput')
    },
    refreshRegisterPage() {
      window.location.reload()
    },
    hasRegisterFormData() {
      const f = this.registerForm
      return [f.username, f.nickName, f.password, f.confirmPassword]
        .some((v) => String(v || '').trim() !== '')
    },
    navigateToLogin() {
      this.$router.push('/login')
    },
    confirmLeaveToLogin() {
      if (!this.hasRegisterFormData()) {
        this.navigateToLogin()
        return
      }
      this.$confirm(this.$t('register.leave-confirm'), this.$t('prompted'), {
        confirmButtonText: this.$t('ok'),
        cancelButtonText: this.$t('cancel'),
        type: 'warning'
      }).then(() => {
        this.navigateToLogin()
      }).catch(() => {})
    },
    goLogin() {
      this.confirmLeaveToLogin()
    },
    $_attachRegisterEscListener() {
      if (this.$_registerEscBound) return
      this.$_registerEscBound = true
      this.$_onRegisterEscKeydown = (e) => {
        if (e.key !== 'Escape' && e.key !== 'Esc') return
        if (e.isComposing) return
        if (hasBlockingUiLayer()) return
        if (!this.registerEnabled) return
        e.preventDefault()
        e.stopPropagation()
        this.confirmLeaveToLogin()
      }
      document.addEventListener('keydown', this.$_onRegisterEscKeydown, true)
    },
    $_detachRegisterEscListener() {
      if (!this.$_registerEscBound) return
      this.$_registerEscBound = false
      document.removeEventListener('keydown', this.$_onRegisterEscKeydown, true)
      this.$_onRegisterEscKeydown = null
    },
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
  background-size: cover;
  background-color: var(--bg-color-base);
}

.register-body {
  position: relative;
  width: 100%;
  display: flex;
  justify-content: center;
}

.register-refresh-page-host {
  position: absolute;
  top: 0;
  right: calc(50% - 200px);
  width: 1px;
  height: 1px;
  pointer-events: none;
}

.register-kbd-anchor-host {
  position: relative;
}

.register-kbd-hint {
  position: absolute;
  right: -2px;
  bottom: -4px;
  width: 16px;
  height: 16px;
  pointer-events: none;
  z-index: 2;
}

.register-btn-item {
  position: relative;
}

.register-kbd-hint-submit {
  right: 4px;
  bottom: 44px;
}

.register-login-link-wrap {
  float: right;
  margin-top: 8px;
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
  font-size: 14px;
  letter-spacing: 1px;
}
</style>
