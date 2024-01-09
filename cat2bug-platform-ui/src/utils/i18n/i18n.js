// I18n
import VueI18n from 'vue-i18n'
import Vue from 'vue'
import locale from 'element-ui/lib/locale'

// 引入 elementui 的多语言
import enLocale from 'element-ui/lib/locale/lang/en'
import zhCnLocale from 'element-ui/lib/locale/lang/zh-CN'
import zhTwLocale from 'element-ui/lib/locale/lang/zh-TW'
// 如果还有新的语言在下面继续添加

// 引入自己定义的 I18n 文件
import myI18nEn from './i18n-en-US.json'
import myI18nZh from './i18n-zh-CN.json'
// 如果还有新的语言在下面继续添加

// 注册 vue-i18n
Vue.use(VueI18n)

// 默认中文
const lang = 'en-US'
const i18n = new VueI18n({
  locale: lang,
  messages: {
    // 会把myI18nZh的所有内容拷贝到zhCnLocale文件中
    'zh-CN': Object.assign(zhCnLocale, myI18nZh),
    'en-US': Object.assign(enLocale, myI18nEn),
  }
})

locale.i18n((key, value) => i18n.t(key, value))
export default i18n
