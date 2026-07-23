/**
 * Element Plus service shim for legacy `element-ui` imports.
 * Message 统一走顶部居中提示，避免与 Notification 右上角混用。
 */
import {
  ElMessage,
  ElMessageBox as MessageBox,
  ElLoading as Loading
} from 'element-plus'
import {
  showAppMessage,
  appMessageInfo,
  appMessageSuccess,
  appMessageWarning,
  appMessageError,
  appNotify
} from '@/utils/app-message'

const Message = Object.assign(
  (content, options) => showAppMessage(content, options),
  {
    success: appMessageSuccess,
    warning: appMessageWarning,
    info: appMessageInfo,
    error: appMessageError,
    closeAll: () => {
      if (typeof ElMessage.closeAll === 'function') ElMessage.closeAll()
    }
  }
)

const Notification = appNotify

export { Message, MessageBox, Notification, Loading }

export default function installElementPlus() {
  // Legacy Vue.use(Element) no-op; components registered via unplugin-vue-components.
}
