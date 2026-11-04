import i18n from '@/utils/i18n/i18n'
import {
  buildLabelOverrideMessages,
  mergeModulePageSettings
} from '@/utils/module-page-settings'

const baselines = new Map()
let activeSettings = null

function currentLocale() {
  const locale = i18n.global.locale
  if (locale && typeof locale === 'object' && 'value' in locale) return locale.value
  return locale || 'zh_CN'
}

function captureBaseline(locale) {
  if (baselines.has(locale)) return baselines.get(locale)
  const snapshot = JSON.parse(JSON.stringify(i18n.global.getLocaleMessage(locale) || {}))
  baselines.set(locale, snapshot)
  return snapshot
}

/** 语言包热更新或重新加载后丢弃旧快照，避免缺 key 时一直回退英文。 */
export function invalidateLocaleBaseline(locale) {
  if (locale) baselines.delete(locale)
  else baselines.clear()
}

export function applyModulePageI18n(settings) {
  activeSettings = mergeModulePageSettings(settings)
  const locales = new Set([currentLocale(), 'zh_CN', 'en_US'])
  locales.forEach((locale) => {
    const base = captureBaseline(locale)
    if (!base || !Object.keys(base).length) return
    const next = buildLabelOverrideMessages(base, activeSettings, locale)
    i18n.global.setLocaleMessage(locale, next)
  })
}

/** 语言包异步加载后，用当前团队别名再盖一次。 */
export function reapplyModulePageI18n() {
  if (!activeSettings) return
  const locale = currentLocale()
  baselines.delete(locale)
  const base = captureBaseline(locale)
  if (!base || !Object.keys(base).length) return
  const next = buildLabelOverrideMessages(base, activeSettings, locale)
  i18n.global.setLocaleMessage(locale, next)
}
