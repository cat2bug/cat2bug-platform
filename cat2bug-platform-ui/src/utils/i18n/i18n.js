// I18n
import VueI18n from 'vue-i18n'
import Vue from 'vue'
import locale from 'element-ui/lib/locale'

// 引入 elementui 的多语言
import enLocale from 'element-ui/lib/locale/lang/en'
import zhCnLocale from 'element-ui/lib/locale/lang/zh-CN'
import zhTwLocale from 'element-ui/lib/locale/lang/zh-TW'
import arLocale from 'element-ui/lib/locale/lang/ar'
import jaJpLocale from 'element-ui/lib/locale/lang/ja'
import koKrLocale from 'element-ui/lib/locale/lang/ko'
import ruLocale from 'element-ui/lib/locale/lang/ru-RU'
// 如果还有新的语言在下面继续添加

// 引入自己定义的 I18n 文件
import myI18nEn from './i18n-en-US.json'
import myI18nZh from './i18n-zh-CN.json'
import myI18nZhTW from './i18n-zh-TW.json'
import myI18nAr from './i18n-ar.json'
import myI18nRu from './i18n-ru.json'
import myI18nJaJp from './i18n-ja-JP.json'
import myI18nKoKr from './i18n-ko-KR.json'
// 如果还有新的语言在下面继续添加

// 注册 vue-i18n
Vue.use(VueI18n)

// 默认中文
const lang = 'zh_CN'
const i18n = new VueI18n({
  locale: lang,
  messages: {
    // 会把myI18nZh的所有内容拷贝到zhCnLocale文件中
    'zh_CN': Object.assign(zhCnLocale, myI18nZh),
    'zh_TW': Object.assign(zhTwLocale, myI18nZhTW),
    'en_US': Object.assign(enLocale, myI18nEn),
    'ar': Object.assign(arLocale, myI18nAr),
    'ja_JP': Object.assign(jaJpLocale, myI18nJaJp),
    'ko_KR': Object.assign(koKrLocale, myI18nKoKr),
    'ru': Object.assign(ruLocale, myI18nRu),
  }
})

locale.i18n((key, value) => i18n.t(key, value))
export default i18n
