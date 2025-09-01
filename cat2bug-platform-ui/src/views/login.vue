<template>
  <div class="logo-page">
    <div class="login">
      <div class="login-introduce">
        <h1>{{ $t('welcome') }}</h1>
        <p>{{ $t('login.introduce1') }}</p>
        <p>{{ $t('login.introduce2') }}</p>
        <p>{{ $t('login.introduce3') }}</p>
      </div>
      <div>
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
    <el-image
      class="login-mouse"
      style="width: 150px;"
      :src="require('@/assets/images/cat2bug-mouse.gif')"
    ></el-image>
  </div>
</template>

<script>
import 'element-ui/lib/theme-chalk/display.css';
import {getVersion} from "@/api/version";

const I18N_LOCALE_KEY='i18n-locale'
const path = require('path');
const files = require.context('@/views/login/', true, /\.vue$/);
const modules = {};
let defaultLogin = null;
const loginList = [];
// 动态加载组件
files.keys().forEach(key=>{
  const name = path.basename(key,'.vue');
  loginList.push({
    name: name,
  });
  modules[name] = files(key).default||files(key);
  if(!defaultLogin) {
    defaultLogin = modules[name]
  }
});

export default {
  name: "Login",
  components: modules,
  data() {
    return {
      login: defaultLogin,
      loginComponentNameList: loginList,
      lang: null,
      systemVersion: null,
      randDelay: Math.random()*10,
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
          return;
        }
      })
    }
  }
};
</script>

<style rel="stylesheet/scss" lang="scss">

@media screen and (max-width: 980px) {
  .login {
    justify-content: center;
  }
  .login-introduce {
    display: none;
  }
  .login-body {
    padding-left: 0px;
  }
}

@media screen and (min-width: 980px) {
  .login {
    justify-content: right;
    margin-left: 90px;
    margin-right: 90px;
  }
  .login-introduce {
    display: inline;
  }
  .login-body {
    padding-left: 90px;
  }
}

body {
  overflow: hidden;
}
.logo-page {
  height: 100%;
  overflow: hidden;
}
.login {
  display: flex;
  //justify-content: right;
  align-items: center;
  height: 100%;
  //background-image: url("../assets/img/login-background.jpg");
  background-size: cover;
  background-color: white;
}
.login-type {
  margin: 10px 0px 25px 0px;
  .login-type-title {
    width: 100%;
    display: inline-flex;
    flex-direction: row;
    align-items: center;
    justify-content: center;
    margin-bottom: 20px;
    > * {
      flex: 1;
      text-align: center;
      color: #C0C4CC;
      font-size: 0.9rem;
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
        font-size: 0.9rem;
        text-align: center;
        margin: 10px 0px;
      }
      .svg-icon {
        color: #C0C4CC;
        font-size: 2rem;
      }
    }

  }
}
.login-introduce {
  float: right;
  max-width: 45%;
  font-size: 22px;
  padding-right: 80px;
  border-right: 1px solid #DCDFE6;
}
.login-mouse {
  position: absolute;
  bottom: 0px;
  position: absolute;
  animation: move 16s linear;
  animation-iteration-count: infinite;
  left: 100%;
  @-webkit-keyframes move {
    0%   {
      left: 100%;
      transform: rotateY(0deg);
    }
    15%  {
      left: calc(0% - 200px);
      transform: rotateY(0deg);
    }
    41% {
      left: calc(0% - 200px);
      transform: rotateY(-180deg);
    }
    55%   {
      left: 100%;
      transform: rotateY(-180deg);
    }
    56%  {
      left: 100%;
      transform: rotateY(0deg);
    }
    100%  {
      left: 100%;
      transform: rotateY(0deg);
    }
  }
}
.between-row {
  width: 100%;
  display: inline-flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  .lang-group {
    display: inline-flex;
    flex-direction: row;
    justify-content: flex-end;
    > * {
      font-size: 22px;
      margin-left: 3px;
    }
  }
}
</style>
