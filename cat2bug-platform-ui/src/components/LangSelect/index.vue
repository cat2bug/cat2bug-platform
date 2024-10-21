<template>
  <el-dropdown class="lang avatar-container right-menu-item hover-effect" @command="handleLanguageCommand">
        <span class="dropdown-title">
          <svg-icon :icon-class="langIcon" />{{ langName }}
          <i class="el-icon-caret-bottom"></i>
        </span>
    <el-dropdown-menu class="dropdown-menu" slot="dropdown">
      <el-dropdown-item command="zh_CN"><svg-icon icon-class="lang_zh_CN" />简体中文</el-dropdown-item>
      <el-dropdown-item command="en_US"><svg-icon icon-class="lang_en_US" />English</el-dropdown-item>
    </el-dropdown-menu>
  </el-dropdown>
</template>
<script>

const I18N_LOCALE_KEY='i18n-locale'

export default {
  name: "LangSelect",
  data() {
    return {
      langIcon: 'lang-zh-CN',
      langName: '简体中文'
    }
  },
  created() {
    const lang = this.$cache.local.get(I18N_LOCALE_KEY)
    this.handleLanguageCommand(lang||'zh_CN');
  },
  methods: {
    handleLanguageCommand(lang) {
      this.$i18n.locale = lang;
      this.$cache.local.set(I18N_LOCALE_KEY,lang);
      this.langIcon = 'lang_'+lang;
      switch (lang){
        case 'zh_CN':
          this.langName = '简体中文';
          break;
        case 'en_US':
          this.langName = 'English';
          break;
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.lang {
  margin-right: 0px !important;
}
.lang:hover {
  cursor: pointer;
}
.dropdown-title{
  height: 100%;
  font-size: 16px;
  display: inline-flex;
  flex-direction: row;
  gap: 5px;
  align-items: center;
  .svg-icon {
    font-size: 22px;
  }
  i {
    font-size: 12px;
  }
}
.dropdown-menu {
  display: flex;
  flex-direction: column;
  ::v-deep .el-dropdown-menu__item {
    //display: inline-flex;
    //flex-direction: row;
    //align-items: center;
    //gap: 5px;
    .svg-icon {
      font-size: 20px;
      margin-right: 5px;
    }
  }
}
</style>
