import { Message, MessageBox, Notification, Loading } from '@/shims/element-ui'
import i18n from '@/utils/i18n/i18n'

const t = (key) => i18n.global.t(key)

let loadingInstance
let loadingCount = 0

export default {
  msg(content) {
    Message.info(content)
  },
  msgError(content) {
    Message.error(content)
  },
  msgSuccess(content) {
    Message.success(content)
  },
  msgWarning(content) {
    Message.warning(content)
  },
  alert(content) {
    MessageBox.alert(content, t('prompted').toString())
  },
  alertError(content) {
    MessageBox.alert(content, t('prompted').toString(), { type: 'error' })
  },
  alertSuccess(content) {
    MessageBox.alert(content, t('prompted').toString(), { type: 'success' })
  },
  alertWarning(content) {
    MessageBox.alert(content, t('prompted').toString(), { type: 'warning' })
  },
  notify(content) {
    Notification.info(content)
  },
  notifyError(content) {
    Notification.error(content)
  },
  notifySuccess(content) {
    Notification.success(content)
  },
  notifyWarning(content) {
    Notification.warning(content)
  },
  confirm(content, title, options) {
    return MessageBox.confirm(content, title || t('prompted').toString(), options || {
      confirmButtonText: t('ok').toString(),
      cancelButtonText: t('cancel').toString(),
      type: 'warning'
    })
  },
  prompt(content, title, options) {
    return MessageBox.prompt(content, title || t('prompted').toString(), options || {
      confirmButtonText: t('ok').toString(),
      cancelButtonText: t('cancel').toString(),
      type: 'warning'
    })
  },
  loading(content) {
    if (loadingCount === 0) {
      loadingInstance = Loading.service({
        lock: true,
        text: content,
        background: 'rgba(0, 0, 0, 0.7)'
      })
    }
    loadingCount++
  },
  closeLoading() {
    if (loadingCount <= 0) {
      loadingCount = 0
      return
    }
    loadingCount--
    if (loadingCount <= 0) {
      loadingCount = 0
      if (loadingInstance) {
        loadingInstance.close()
        loadingInstance = null
      }
    }
  },
  closeAllLoading() {
    loadingCount = 0
    if (loadingInstance) {
      loadingInstance.close()
      loadingInstance = null
    }
  }
}
