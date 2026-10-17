import { createApp, defineAsyncComponent } from 'vue'
import Cookies from 'js-cookie'
import { createHead } from '@unhead/vue'

import '@fontsource/dseg7-classic/latin-400.css'
import '@/assets/styles/element-plus-variables.scss'
import 'element-plus/theme-chalk/base.css'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/loading/style/css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import '@/assets/styles/index.scss'
import '@/assets/styles/ruoyi.scss'
import '@/assets/styles/cat2bug.scss'
import '@/assets/styles/column-picker-popover.scss'
import '@/assets/styles/el-table-scrollbar.scss'
import '@/assets/styles/el-table-fixed-column.scss'
import '@/assets/styles/project-list-page.scss'
import '@/assets/styles/page-kbd-hints.scss'

import App from './App.vue'
import { pinia } from './store/pinia.js'
import router from './router'
import directive from './directive'
import plugins from './plugins'
import { download } from '@/utils/request'

import './assets/icons'
import './permission'
import { getDicts } from '@/api/system/dict/data'
import { getConfigKey } from '@/api/system/config'
import {
  parseTime, resetForm, addDateRange, selectDictLabel, selectDictLabels, handleTree
} from '@/utils/ruoyi'

import Pagination from '@/components/Pagination'
import RightToolbar from '@/components/RightToolbar'
import FileUpload from '@/components/FileUpload'
import ImageUpload from '@/components/ImageUpload'
import ImagePreview from '@/components/ImagePreview'
import Cat2BugImage from '@/components/Cat2BugImage'
import { Cat2BugImageErrorSlot } from '@/utils/upload-asset'
import DictTag from '@/components/DictTag'
import SiteCopyright from '@/components/SiteCopyright'
import DictData from '@/components/DictData'
import SvgIcon from '@/components/SvgIcon'
import LegacyIcon from '@/components/LegacyIcon'
import CommandPalette from '@/components/Shortcut/CommandPalette.vue'

import i18n, { loadLocaleMessages } from './utils/i18n/i18n.js'

import ShortcutPlugin from '@/plugins/shortcut'
import TabDirectionPlugin from '@/plugins/tab-direction'
import SwitchKeyboardPlugin from '@/plugins/switch-keyboard'
import UploadFocusTabPlugin from '@/plugins/upload-focus-tab'
import ComboFocusTabPlugin from '@/plugins/combo-focus-tab'
import DropdownBlurClosePlugin from '@/plugins/dropdown-blur-close'
import DropdownKbdPlugin from '@/plugins/dropdown-kbd'
import DatePickerEscapePlugin from '@/plugins/date-picker-escape'
import DatePickerKbdPlugin from '@/plugins/date-picker-kbd'
import DefectDrawerShortcutsPlugin from '@/plugins/defect-drawer-shortcuts'
import WebSocketPlugin from '@/plugins/websocket'
import VueExcelEditor from '@/vendor/vue-excel-editor'
import { ElMessageBox } from 'element-plus'
import { appNotify, showAppMessage, appMessageError, appMessageSuccess, appMessageWarning, appMessageInfo } from '@/utils/app-message'
import { resolveLegacyIcon } from '@/utils/element-icon'
import { bootstrapAuthenticatedRoutes } from '@/router/bootstrap'
import { installKeyboardMixinBridges } from '@/composables/keyboard/installKeyboardMixinBridges'

const head = createHead()
const app = createApp(App)

// Vue3 会忽略 mixin 的 setup()，此处注册全局桥接运行时，
// 使各页面通过 @/mixins/* 声明的键盘增强 composable 重新生效。
installKeyboardMixinBridges(app)

// 内嵌 excel-editor 等仍使用 this.$set；须与 Vue.set 一致并返回 value
app.config.globalProperties.$set = (target, key, value) => {
  if (target == null) return value
  target[key] = value
  return value
}

app.use(pinia)

function finishPageLoadBar() {
  const bar = document.getElementById('page-load-bar')
  if (!bar) return
  bar.classList.add('is-done')
  window.setTimeout(() => bar.remove(), 400)
}

async function mountApp() {
  try {
    await bootstrapAuthenticatedRoutes()
  } catch (_err) {
    // 登录失效等由 permission 守卫处理
  }

  try {
    await loadLocaleMessages(localStorage.getItem('i18n-locale'))
  } catch (_err) {
    // 语言包加载失败不阻塞启动
  }

  // app.use(router) 会立即同步当前 hash，须在动态路由注入之后执行
  app.use(router)
  app.use(i18n)
  app.use(head)
  app.use(directive)
  app.use(plugins)
  app.use(ShortcutPlugin)
  app.use(TabDirectionPlugin)
  app.use(SwitchKeyboardPlugin)
  app.use(UploadFocusTabPlugin)
  app.use(ComboFocusTabPlugin)
  app.use(DropdownBlurClosePlugin)
  app.use(DropdownKbdPlugin)
  app.use(DatePickerEscapePlugin)
  app.use(DatePickerKbdPlugin)
  app.use(DefectDrawerShortcutsPlugin)
  app.use(WebSocketPlugin, {
    reconnection: true,
    reconnectionAttempts: 5,
    reconnectionDelay: 3000,
    connectManually: true
  })
  app.use(VueExcelEditor)

  DictData.install(app)

  app.component('SvgIcon', SvgIcon)
  app.component('LegacyIcon', LegacyIcon)
  app.component('DictTag', DictTag)
  app.component('SiteCopyright', SiteCopyright)
  app.component('Pagination', Pagination)
  app.component('RightToolbar', RightToolbar)
  // Quill 体积大；仅在页面真正用到 <Editor> 时再加载
  app.component('Editor', defineAsyncComponent(() => import('@/components/Editor/index.vue')))
  app.component('FileUpload', FileUpload)
  app.component('ImageUpload', ImageUpload)
  app.component('ImagePreview', ImagePreview)
  app.component('Cat2BugImage', Cat2BugImage)
  app.component('Cat2BugImageErrorSlot', Cat2BugImageErrorSlot)
  app.component('CommandPalette', CommandPalette)

  app.config.globalProperties.getDicts = getDicts
  app.config.globalProperties.getConfigKey = getConfigKey
  app.config.globalProperties.parseTime = parseTime
  app.config.globalProperties.resetForm = resetForm
  app.config.globalProperties.addDateRange = addDateRange
  app.config.globalProperties.selectDictLabel = selectDictLabel
  app.config.globalProperties.selectDictLabels = selectDictLabels
  app.config.globalProperties.download = download
  app.config.globalProperties.handleTree = handleTree
  app.config.globalProperties.$confirm = ElMessageBox.confirm
  app.config.globalProperties.$notify = appNotify
  app.config.globalProperties.$message = Object.assign(
    (content, options) => showAppMessage(content, options),
    {
      success: appMessageSuccess,
      warning: appMessageWarning,
      info: appMessageInfo,
      error: appMessageError
    }
  )
  app.config.globalProperties.$legacyIcon = resolveLegacyIcon
  app.provide('elSize', Cookies.get('size') || 'medium')

  app.mount('#app')
  finishPageLoadBar()
}

mountApp()
