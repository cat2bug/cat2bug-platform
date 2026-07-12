import { createI18n } from 'vue-i18n'
import en from 'element-plus/es/locale/lang/en'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import zhTw from 'element-plus/es/locale/lang/zh-tw'
import ar from 'element-plus/es/locale/lang/ar'
import ja from 'element-plus/es/locale/lang/ja'
import ko from 'element-plus/es/locale/lang/ko'
import ru from 'element-plus/es/locale/lang/ru'

import myI18nEn from './i18n-en-US.json'
import myI18nZh from './i18n-zh-CN.json'
import myI18nZhTW from './i18n-zh-TW.json'
import myI18nAr from './i18n-ar.json'
import myI18nRu from './i18n-ru.json'
import myI18nJaJp from './i18n-ja-JP.json'
import myI18nKoKr from './i18n-ko-KR.json'

const lang = 'zh_CN'

const i18n = createI18n({
  legacy: true,
  globalInjection: true,
  locale: lang,
  messages: {
    zh_CN: { ...zhCn, ...myI18nZh },
    zh_TW: { ...zhTw, ...myI18nZhTW },
    en_US: { ...en, ...myI18nEn },
    ar: { ...ar, ...myI18nAr },
    ja_JP: { ...ja, ...myI18nJaJp },
    ko_KR: { ...ko, ...myI18nKoKr },
    ru: { ...ru, ...myI18nRu }
  }
})

// Vue 2 style API for plain JS modules (vue-i18n 9 only exposes t on global)
if (typeof i18n.t !== 'function') {
  i18n.t = (...args) => i18n.global.t(...args)
}

export default i18n
