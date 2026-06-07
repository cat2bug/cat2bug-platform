<template>
  <div class="logo-page">
    <div class="login">
      <div class="login-introduce">
        <p class="brand-welcome">{{ $t('login.hero.welcome') }}</p>
        <div class="brand-row">
          <div class="brand-text-row">
            <span class="brand-name">
              <span class="brand-name-cat2">Cat2</span><span class="brand-name-bug">Bug</span><span class="brand-name-platform">-Platform</span>
            </span>
            <span v-if="version" class="brand-version">{{ version }}</span>
          </div>
        </div>
        <div class="brand-accent-line" aria-hidden="true"></div>
        <h1 class="login-hero-title">{{ $t('login.hero.title') }}</h1>
        <div class="login-hero-desc">
          <p
            v-for="(paragraph, index) in heroDescParagraphs"
            :key="index"
            class="login-hero-desc-para"
          >{{ paragraph }}</p>
        </div>
        <ul class="login-features">
          <li class="login-feature-item">
            <svg-icon icon-class="mk-bug" class="login-feature-icon" />
            <div class="login-feature-text">
              <strong>{{ $t('login.features.tracking.title') }}</strong>
              <p>{{ $t('login.features.tracking.desc') }}</p>
            </div>
          </li>
          <li class="login-feature-item">
            <svg-icon icon-class="peoples" class="login-feature-icon" />
            <div class="login-feature-text">
              <strong>{{ $t('login.features.collaboration.title') }}</strong>
              <p>{{ $t('login.features.collaboration.desc') }}</p>
            </div>
          </li>
          <li class="login-feature-item">
            <svg-icon icon-class="chart" class="login-feature-icon" />
            <div class="login-feature-text">
              <strong>{{ $t('login.features.reports.title') }}</strong>
              <p>{{ $t('login.features.reports.desc') }}</p>
            </div>
          </li>
        </ul>
      </div>
      <div class="login-right-panel">
        <component
          :version="version"
          :is="login.name"
          @lang="changeLang"
        >
          <template v-if="loginTypeList.length>0" slot="tools">
            <div class="login-type">
              <div class="login-type-title">
                <el-divider></el-divider>
                <span>{{ $t('other-login') }}</span>
                <el-divider></el-divider>
              </div>
              <div class="login-type-button">
                <div v-for="icon in loginTypeList" :key="icon.name" @click="handleLoginType(icon.name)">
                  <div>
                    <svg-icon :icon-class="`login-type-${icon.name}`"/>
                    <p>{{ $t('other-login.'+icon.name) }}</p>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </component>
      </div>
    </div>
    <mouse-dpad-controls />
    <mouse-runner />
    <command-palette />
  </div>
</template>

<script>
import 'element-ui/lib/theme-chalk/display.css';
import {getVersion} from "@/api/version";
import MouseRunner from "./login/components/MouseRunner.vue";
import MouseDpadControls from "./login/components/MouseDpadControls.vue";

const I18N_LOCALE_KEY='i18n-locale'
const path = require('path');
const files = require.context('@/views/login/', false, /\.vue$/);
const modules = {};
let defaultLogin = null;
const loginList = [];
files.keys().forEach(key=>{
  const name = path.basename(key,'.vue');
  loginList.push({ name: name });
  modules[name] = files(key).default||files(key);
  if(!defaultLogin) {
    defaultLogin = modules[name]
  }
});

export default {
  name: "Login",
  components: { ...modules, MouseRunner, MouseDpadControls },
  data() {
    return {
      login: defaultLogin,
      loginComponentNameList: loginList,
      lang: null,
      systemVersion: null,
    };
  },
  watch: {
    $route: {
      handler: function(route) {
        this.redirect = route.query && route.query.redirect;
      },
      immediate: true
    }
  },
  computed: {
    version: function () {
      return this.systemVersion?'V'+this.systemVersion:'';
    },
    loginTypeList: function () {
      return this.loginComponentNameList.filter(l=>this.login && l.name!=this.login.name);
    },
    heroDescParagraphs() {
      const text = this.$t('login.hero.desc');
      if (!text) return [];
      return String(text).split('\n').map(s => s.trim()).filter(Boolean);
    }
  },
  created() {
    this.getSystemVersion();
    this.lang = this.$cache.local.get(I18N_LOCALE_KEY);
    this.lang = this.lang ||'zh_CN';
    this.$i18n.locale = this.lang;
  },
  methods: {
    getSystemVersion() {
      getVersion().then(res=>{
        this.systemVersion = res;
      })
    },
    changeLang(lang){
      this.$i18n.locale = lang;
      this.$cache.local.set(I18N_LOCALE_KEY,lang);
    },
    handleLoginType(loginComponentName) {
      this.loginComponentNameList.forEach(c=>{
        if(loginComponentName == c.name) {
          this.login = c;
        }
      })
    }
  }
};
</script>

<style rel="stylesheet/scss" lang="scss">
body {
  overflow: hidden;
}

.logo-page {
  --login-bg: #fdfdfd;
  --login-card-bg: #ffffff;
  --login-accent: #409EFF;
  --login-on-accent: #ffffff;
  --login-accent-glow: rgba(64, 158, 255, 0.18);
  --login-text-primary: #303133;
  --login-text-secondary: #606266;
  --login-text-muted: #909399;
  --login-input-bg: #f5f7fa;
  --login-input-border: #dcdfe6;
  --login-card-border: #e4e7ed;
  --login-zzz-color: #606266;
  height: 100%;
  overflow: hidden;
  background-color: var(--login-bg);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
}

html.dark .logo-page {
  --login-bg: #17181a;
  --login-card-bg: #1f2023;
  --login-accent: #ffc107;
  --login-on-accent: #1a1a1a;
  --login-accent-glow: rgba(255, 193, 7, 0.18);
  --login-text-primary: #ffffff;
  --login-text-secondary: #9ca3af;
  --login-text-muted: #6b7280;
  --login-input-bg: #141416;
  --login-input-border: #3f3f41;
  --login-card-border: #2a2a2c;
  --login-zzz-color: #e5e7eb;
}

.login {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 40px 48px;
  box-sizing: border-box;
  gap: 64px;
}

.login-introduce {
  flex: 1.1;
  max-width: 520px;
  position: relative;
  z-index: 5;
  padding-top: 28px;
  font-size: 16px;
  line-height: 1.6;
  color: var(--login-text-primary);
  text-align: left;

  .brand-welcome {
    margin: 0 0 4px;
    font-size: 30px;
    font-weight: 500;
    line-height: 1.25;
    color: var(--login-text-secondary);
  }

  .brand-row {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 8px;
  }

  .brand-text-row {
    display: flex;
    align-items: baseline;
    gap: 8px;
    flex-wrap: nowrap;
    white-space: nowrap;
  }

  .brand-name {
    font-size: 56px;
    font-weight: 700;
    line-height: 1.2;
    letter-spacing: -0.5px;
    white-space: nowrap;
  }

  .brand-version {
    font-size: 14px;
    color: var(--login-text-muted, var(--text-color-secondary));
  }

  .brand-name-cat2 {
    color: var(--login-text-primary);
  }

  .brand-name-bug {
    color: var(--login-accent);
  }

  .brand-name-platform {
    color: var(--login-text-muted);
  }

  .brand-accent-line {
    width: 72px;
    height: 5px;
    margin: 0 0 16px;
    border-radius: 3px;
    background-color: var(--login-accent);
  }

  .login-hero-title {
    font-size: 20px;
    font-weight: 700;
    line-height: 1.2;
    margin: 0 0 10px;
    color: var(--login-text-primary) !important;
  }

  .login-hero-desc {
    margin: 0 0 18px;
  }

  .login-hero-desc-para {
    margin: 0 0 10px;
    color: var(--login-text-secondary) !important;
    font-size: 14px;
    line-height: 1.45;

    &:last-child {
      margin-bottom: 0;
    }
  }

  .login-features {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .login-feature-item {
    display: flex;
    align-items: flex-start;
    gap: 16px;
  }

  .login-feature-icon {
    flex-shrink: 0;
    width: 44px;
    height: 44px;
    margin-top: 0;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    box-sizing: border-box;
    padding: 5px;
    border: 1px solid var(--login-input-border, #3f3f41);
    border-radius: 10px;
    background-color: var(--login-bg);
    color: var(--login-accent);
    font-size: 22px;
  }

  .login-feature-text {
    strong {
      display: block;
      font-size: 16px;
      font-weight: 600;
      color: var(--login-text-primary);
      margin-bottom: 4px;
    }

    p {
      margin: 0;
      font-size: 14px;
      line-height: 1.5;
      color: var(--login-text-secondary) !important;
    }
  }
}

.login-right-panel {
  flex: 0 0 575px;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  z-index: 2;
}

.login-type {
  margin: 10px 0 0;
  .login-type-title {
    width: 100%;
    display: inline-flex;
    flex-direction: row;
    align-items: center;
    justify-content: center;
    margin-bottom: 16px;
    > * {
      flex: 1;
      text-align: center;
      color: var(--login-text-secondary);
      font-size: 0.85rem;
      margin: 0;
    }
  }
  .login-type-button {
    width: 100%;
    display: inline-flex;
    flex-direction: row;
    > div {
      flex: 1;
      display: inline-flex;
      flex-direction: row;
      align-items: center;
      justify-content: center;
      > div {
        display: inline-flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        cursor: pointer;
      }
      p {
        font-size: 0.85rem;
        text-align: center;
        margin: 8px 0 0;
        color: var(--login-text-secondary);
      }
      .svg-icon {
        color: var(--login-text-secondary);
        font-size: 1.6rem;
      }
    }
  }
}

@media screen and (max-width: 980px) {
  .login {
    flex-direction: column;
    padding: 24px 20px;
    gap: 24px;
  }
  .login-introduce {
    display: none;
  }
  .login-right-panel {
    flex: 1;
    width: 100%;
  }
}
</style>
