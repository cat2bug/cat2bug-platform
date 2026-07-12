import zhCn from 'element-plus/es/locale/lang/zh-cn'
import zhTw from 'element-plus/es/locale/lang/zh-tw'
import en from 'element-plus/es/locale/lang/en'
import ar from 'element-plus/es/locale/lang/ar'
import ja from 'element-plus/es/locale/lang/ja'
import ko from 'element-plus/es/locale/lang/ko'
import ru from 'element-plus/es/locale/lang/ru'

const ELEMENT_PLUS_LOCALE_MAP = {
  zh_CN: zhCn,
  zh_TW: zhTw,
  en_US: en,
  ar,
  ja_JP: ja,
  ko_KR: ko,
  ru
}

export function resolveElementPlusLocale(i18nLocale) {
  return ELEMENT_PLUS_LOCALE_MAP[i18nLocale] || zhCn
}
