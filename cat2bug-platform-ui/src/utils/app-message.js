/**
 * 全站轻提示统一入口：顶部水平居中（placement: top），样式一致。
 * 业务代码优先走 $modal.msg* / 本模块；勿再直接 ElNotification 做普通提示。
 */
import { ElMessage } from 'element-plus'

export const APP_MESSAGE_DEFAULTS = Object.freeze({
  placement: 'top',
  showClose: true,
  duration: 3000,
  offset: 20,
  grouping: true
})

function normalizePayload(content, options = {}) {
  if (content != null && typeof content === 'object' && !Array.isArray(content) && (
    Object.prototype.hasOwnProperty.call(content, 'message')
    || Object.prototype.hasOwnProperty.call(content, 'title')
    || Object.prototype.hasOwnProperty.call(content, 'type')
  )) {
    const {
      title,
      message,
      type,
      dangerouslyUseHTMLString,
      duration,
      showClose,
      offset,
      grouping,
      placement,
      onClose
    } = content
    let text = message
    if ((text == null || text === '') && title != null) {
      text = title
    } else if (title && typeof message === 'string' && !dangerouslyUseHTMLString) {
      text = `${title}：${message}`
    }
    return {
      ...APP_MESSAGE_DEFAULTS,
      type: type || 'info',
      message: text,
      dangerouslyUseHTMLString: !!dangerouslyUseHTMLString,
      ...(duration != null ? { duration } : null),
      ...(showClose != null ? { showClose } : null),
      ...(offset != null ? { offset } : null),
      ...(grouping != null ? { grouping } : null),
      ...(placement != null ? { placement } : null),
      ...(onClose ? { onClose } : null),
      ...options
    }
  }
  return {
    ...APP_MESSAGE_DEFAULTS,
    message: content,
    ...options
  }
}

export function showAppMessage(content, options = {}) {
  return ElMessage(normalizePayload(content, options))
}

export function appMessageInfo(content, options) {
  return showAppMessage(content, { ...options, type: 'info' })
}

export function appMessageSuccess(content, options) {
  return showAppMessage(content, { ...options, type: 'success' })
}

export function appMessageWarning(content, options) {
  return showAppMessage(content, { ...options, type: 'warning' })
}

export function appMessageError(content, options) {
  return showAppMessage(content, { ...options, type: 'error' })
}

/** 兼容旧 $notify / Notification 调用形态 */
export function appNotify(options) {
  if (typeof options === 'string') {
    return appMessageInfo(options)
  }
  const type = (options && options.type) || 'info'
  return showAppMessage(options, { type })
}

appNotify.success = (options) => {
  if (typeof options === 'string') return appMessageSuccess(options)
  return showAppMessage(options, { type: 'success' })
}
appNotify.warning = (options) => {
  if (typeof options === 'string') return appMessageWarning(options)
  return showAppMessage(options, { type: 'warning' })
}
appNotify.error = (options) => {
  if (typeof options === 'string') return appMessageError(options)
  return showAppMessage(options, { type: 'error' })
}
appNotify.info = (options) => {
  if (typeof options === 'string') return appMessageInfo(options)
  return showAppMessage(options, { type: 'info' })
}

export default {
  show: showAppMessage,
  info: appMessageInfo,
  success: appMessageSuccess,
  warning: appMessageWarning,
  error: appMessageError,
  notify: appNotify
}
