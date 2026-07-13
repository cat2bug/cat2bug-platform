/**
 * Resolve toolbar menu label. Flat keys like `case.total` fail vue-i18n `te()`
 * when `case` also exists as a string message. Plan menus append `.{planNumber}`
 * for unique el-menu indices (e.g. `plan.statistics.1`).
 * @param {import('vue-i18n').Composer | { locale: string, te: Function, t: Function, getLocaleMessage?: Function, messages?: object, global?: object }} i18n
 * @param {string} name
 */
export function resolveToolbarMenuLabel(i18n, name) {
  if (!name) {
    return ''
  }

  const locale = resolveActiveLocale(i18n)
  const messages = resolveLocaleMessages(i18n, locale)

  const candidates = [name]

  // plan.statistics.1 → plan.statistics; member.2 → member
  const withoutNumericSuffix = name.replace(/\.\d+$/, '')
  if (withoutNumericSuffix !== name) {
    candidates.push(withoutNumericSuffix)
  }

  for (const key of candidates) {
    const label = lookupMessageString(i18n, messages, key)
    if (label) {
      return label
    }
  }

  return name
}

function resolveActiveLocale(i18n) {
  return i18n.locale?.value
    ?? i18n.locale
    ?? i18n.global?.locale?.value
    ?? i18n.global?.locale
    ?? 'zh_CN'
}

function resolveLocaleMessages(i18n, locale) {
  if (typeof i18n.getLocaleMessage === 'function') {
    return i18n.getLocaleMessage(locale)
  }
  if (i18n.messages?.[locale]) {
    return i18n.messages[locale]
  }
  const global = i18n.global
  if (global && typeof global.getLocaleMessage === 'function') {
    return global.getLocaleMessage(locale)
  }
  if (global?.messages?.[locale]) {
    return global.messages[locale]
  }
  return null
}

function lookupMessageString(i18n, messages, key) {
  if (messages && Object.prototype.hasOwnProperty.call(messages, key) && typeof messages[key] === 'string') {
    return messages[key]
  }
  // te() returns false for flat keys when a parent segment exists (e.g. case.total vs case).
  // t() still resolves the full key correctly.
  if (typeof i18n.t === 'function') {
    const translated = i18n.t(key)
    if (translated != null && translated !== '' && translated !== key) {
      return translated
    }
  }
  return null
}
