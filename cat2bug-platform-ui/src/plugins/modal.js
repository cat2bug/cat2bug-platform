import { Message, MessageBox, Notification, Loading } from 'element-ui'
import i18n from "@/utils/i18n/i18n";

let loadingInstance;
let loadingCount = 0;

export default {
  // 消息提示
  msg(content) {
    Message.info(content)
  },
  // 错误消息
  msgError(content) {
    Message.error(content)
  },
  // 成功消息
  msgSuccess(content) {
    Message.success(content)
  },
  // 警告消息
  msgWarning(content) {
    Message.warning(content)
  },
  // 弹出提示
  alert(content) {
    MessageBox.alert(content, i18n.t('prompted').toString())
  },
  // 错误提示
  alertError(content) {
    MessageBox.alert(content, i18n.t('prompted').toString(), { type: 'error' })
  },
  // 成功提示
  alertSuccess(content) {
    MessageBox.alert(content, i18n.t('prompted').toString(), { type: 'success' })
  },
  // 警告提示
  alertWarning(content) {
    MessageBox.alert(content, i18n.t('prompted').toString(), { type: 'warning' })
  },
  // 通知提示
  notify(content) {
    Notification.info(content)
  },
  // 错误通知
  notifyError(content) {
    Notification.error(content);
  },
  // 成功通知
  notifySuccess(content) {
    Notification.success(content)
  },
  // 警告通知
  notifyWarning(content) {
    Notification.warning(content)
  },
  // 确认窗体
  confirm(content, title, options) {
    return MessageBox.confirm(content, title || i18n.t('prompted').toString(), options || {
      confirmButtonText: i18n.t('ok').toString(),
      cancelButtonText: i18n.t('cancel').toString(),
      type: "warning",
    })
  },
  // 提交内容
  prompt(content,title,options) {
    return MessageBox.prompt(content, title || i18n.t('prompted').toString(), options || {
      confirmButtonText: i18n.t('ok').toString(),
      cancelButtonText: i18n.t('cancel').toString(),
      type: "warning",
    })
  },
  // 打开遮罩层（引用计数，避免多组件同时上传时只关掉最后一次）
  loading(content) {
    if (loadingCount === 0) {
      loadingInstance = Loading.service({
        lock: true,
        text: content,
        spinner: "el-icon-loading",
        background: "rgba(0, 0, 0, 0.7)",
      })
    }
    loadingCount++
  },
  // 关闭遮罩层
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
  /** 上传结束等场景：一次性关闭所有未配对的 loading */
  closeAllLoading() {
    loadingCount = 0
    if (loadingInstance) {
      loadingInstance.close()
      loadingInstance = null
    }
  }
}
