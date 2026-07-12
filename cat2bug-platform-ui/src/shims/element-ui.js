/**
 * Element Plus service shim for legacy `element-ui` imports.
 */
import {
  ElMessage as Message,
  ElMessageBox as MessageBox,
  ElNotification as Notification,
  ElLoading as Loading
} from 'element-plus'

export { Message, MessageBox, Notification, Loading }

export default function installElementPlus() {
  // Legacy Vue.use(Element) no-op; components registered via unplugin-vue-components.
}
