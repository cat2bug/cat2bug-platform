import { watch } from 'vue'
import { createI18n } from 'vue-i18n'
import en from 'element-plus/es/locale/lang/en'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import zhTw from 'element-plus/es/locale/lang/zh-tw'
import ar from 'element-plus/es/locale/lang/ar'
import ja from 'element-plus/es/locale/lang/ja'
import ko from 'element-plus/es/locale/lang/ko'
import ru from 'element-plus/es/locale/lang/ru'

import myI18nZh from './i18n-zh-CN.json'
import myI18nEn from './i18n-en-US.json'

const lang = 'zh_CN'

const ELEMENT_LOCALES = {
  zh_TW: zhTw,
  en_US: en,
  ar,
  ja_JP: ja,
  ko_KR: ko,
  ru
}

const MESSAGE_LOADERS = {
  zh_TW: () => import('./i18n-zh-TW.json'),
  en_US: () => import('./i18n-en-US.json'),
  ar: () => import('./i18n-ar.json'),
  ja_JP: () => import('./i18n-ja-JP.json'),
  ko_KR: () => import('./i18n-ko-KR.json'),
  ru: () => import('./i18n-ru.json')
}

const i18n = createI18n({
  legacy: true,
  globalInjection: true,
  locale: lang,
  fallbackLocale: 'en_US',
  messages: {
    zh_CN: { ...zhCn, ...myI18nZh },
    // 预置英文，供其它语言缺 key 时回退
    en_US: { ...en, ...myI18nEn }
  }
})

const loadedLocales = new Map([
  [lang, Promise.resolve()],
  ['en_US', Promise.resolve()]
])

/** 非默认语言按需加载；切换 locale 时自动触发，启动时需在 mount 前 await 已保存的语言以免闪现 key */
export function loadLocaleMessages(locale) {
  if (loadedLocales.has(locale)) return loadedLocales.get(locale)
  const loader = MESSAGE_LOADERS[locale]
  if (!loader) return Promise.resolve()
  const pending = loader()
    .then((mod) => {
      i18n.global.setLocaleMessage(locale, { ...ELEMENT_LOCALES[locale], ...(mod.default || mod) })
    })
    .catch((err) => {
      loadedLocales.delete(locale)
      throw err
    })
  loadedLocales.set(locale, pending)
  return pending
}

watch(() => i18n.global.locale, (locale) => {
  loadLocaleMessages(locale).catch(() => {})
})

// Vue 2 style API for plain JS modules (vue-i18n 9 only exposes t/te on global)
if (typeof i18n.t !== 'function') {
  i18n.t = (...args) => i18n.global.t(...args)
}
if (typeof i18n.te !== 'function') {
  i18n.te = (...args) => i18n.global.te(...args)
}

export default i18n
