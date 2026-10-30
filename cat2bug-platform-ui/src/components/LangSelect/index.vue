<template>
  <el-dropdown
    class="lang avatar-container right-menu-item hover-effect layout-nav-hint-anchor"
    data-layout-nav="lang"
    popper-class="navbar-lang-dropdown"
    @command="handleLanguageCommand"
    @visible-change="onLangDropdownVisible"
  >
        <span class="dropdown-title" tabindex="-1">
          <svg-icon :icon-class="langIcon" />{{ langName }}
          <legacy-icon icon="el-icon-caret-bottom"></legacy-icon>
        </span>
    <template #dropdown>
      <el-dropdown-menu class="dropdown-menu">
        <el-dropdown-item command="zh_CN"><svg-icon icon-class="lang_zh_CN" />简体中文</el-dropdown-item>
        <el-dropdown-item command="zh_TW"><svg-icon icon-class="lang_zh_CN" />繁体中文</el-dropdown-item>
        <el-dropdown-item command="en_US"><svg-icon icon-class="lang_en_US" />English</el-dropdown-item>
        <el-dropdown-item command="ru"><svg-icon icon-class="lang_ru" />Русский</el-dropdown-item>
        <el-dropdown-item command="ja_JP"><svg-icon icon-class="lang_ja_JP" />日本語</el-dropdown-item>
        <el-dropdown-item command="ko_KR"><svg-icon icon-class="lang_ko_KR" />한국어</el-dropdown-item>
        <el-dropdown-item command="ar"><svg-icon icon-class="lang_ar" />العربية</el-dropdown-item>
      </el-dropdown-menu>
    </template>
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
    blurLangTrigger() {
      this.$nextTick(() => {
        const ae = document.activeElement
        if (ae && this.$el && this.$el.contains(ae) && typeof ae.blur === 'function') {
          ae.blur()
        }
      })
    },
    onLangDropdownVisible(visible) {
      if (!visible) this.blurLangTrigger()
    },
    handleLanguageCommand(lang) {
      this.$i18n.locale = lang;
      this.$cache.local.set(I18N_LOCALE_KEY,lang);
      this.langIcon = 'lang_'+lang;
      switch (lang){
        case 'zh_CN':
          this.langName = '简体中文';
          break;
        case 'zh_TW':
          this.langName = '繁体中文';
          break;
        case 'ja_JP':
          this.langName = '日本語';
          break;
        case 'ko_KR':
          this.langName = '한국어';
          break;
        case 'ar':
          this.langName = 'العربية';
          break;
        case 'en_US':
          this.langName = 'English';
          break;
        case 'ru':
          this.langName = 'Русский';
          break;
      }
      this.blurLangTrigger()
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
  outline: none;
  box-shadow: none;
  border: none;
  .svg-icon {
    font-size: 22px;
  }
  i {
    font-size: 12px;
  }
}
.lang:focus,
.lang:focus-visible,
.lang :deep(.el-tooltip__trigger:focus),
.lang :deep(.el-tooltip__trigger:focus-visible),
.dropdown-title:focus,
.dropdown-title:focus-visible {
  outline: none !important;
  box-shadow: none !important;
}
.dropdown-menu {
  display: flex;
  flex-direction: column;
  :deep(.el-dropdown-menu__item) {
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
