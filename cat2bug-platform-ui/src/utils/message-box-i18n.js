import { ElMessageBox } from 'element-plus'
import i18n from '@/utils/i18n/i18n'

const t = (key) => i18n.global.t(key).toString()

/**
 * 合并 MessageBox 按钮文案：未显式传入时用 vue-i18n（ok/cancel），
 * 避免仅依赖 Element Plus locale 时出现 Cancel/OK。
 * @param {Record<string, any>} [options]
 */
export function withMessageBoxI18nDefaults(options = {}) {
  const opts = options && typeof options === 'object' ? options : {}
  return {
    ...opts,
    confirmButtonText: opts.confirmButtonText != null ? opts.confirmButtonText : t('ok'),
    cancelButtonText: opts.cancelButtonText != null ? opts.cancelButtonText : t('cancel')
  }
}

/**
 * 与 ElMessageBox.confirm 同签名，自动补齐按钮 i18n。
 * @param {string} message
 * @param {string|Record<string, any>} [titleOrOptions]
 * @param {Record<string, any>} [options]
 */
export function confirmWithI18n(message, titleOrOptions, options) {
  if (titleOrOptions != null && typeof titleOrOptions === 'object') {
    return ElMessageBox.confirm(message, withMessageBoxI18nDefaults(titleOrOptions))
  }
  return ElMessageBox.confirm(
    message,
    titleOrOptions != null && titleOrOptions !== '' ? titleOrOptions : t('prompted'),
    withMessageBoxI18nDefaults(options || {})
  )
}

/**
 * @param {string} message
 * @param {string|Record<string, any>} [titleOrOptions]
 * @param {Record<string, any>} [options]
 */
export function alertWithI18n(message, titleOrOptions, options) {
  if (titleOrOptions != null && typeof titleOrOptions === 'object') {
    return ElMessageBox.alert(message, withMessageBoxI18nDefaults(titleOrOptions))
  }
  return ElMessageBox.alert(
    message,
    titleOrOptions != null && titleOrOptions !== '' ? titleOrOptions : t('prompted'),
    withMessageBoxI18nDefaults(options || {})
  )
}

/**
 * @param {string} message
 * @param {string|Record<string, any>} [titleOrOptions]
 * @param {Record<string, any>} [options]
 */
export function promptWithI18n(message, titleOrOptions, options) {
  if (titleOrOptions != null && typeof titleOrOptions === 'object') {
    return ElMessageBox.prompt(message, withMessageBoxI18nDefaults(titleOrOptions))
  }
  return ElMessageBox.prompt(
    message,
    titleOrOptions != null && titleOrOptions !== '' ? titleOrOptions : t('prompted'),
    withMessageBoxI18nDefaults(options || {})
  )
}
