import Vue from 'vue'

import Cookies from 'js-cookie'

import Element from 'element-ui'
import '@fontsource/dseg7-classic/latin-400.css'
import './assets/styles/element-variables.scss'
import '@/assets/styles/index.scss' // global css
import '@/assets/styles/ruoyi.scss' // ruoyi css
// 不使用 default-passive-events：其全局劫持 addEventListener 可能影响依赖 passive 检测的组件。
import '@/assets/styles/cat2bug.scss'
import '@/assets/styles/el-table-scrollbar.scss'
import '@/assets/styles/project-list-page.scss'
import App from './App'
import store from './store'
import router from './router'
import directive from './directive' // directive
import plugins from './plugins' // plugins
import { download } from '@/utils/request'

import './assets/icons' // icon
import './permission' // permission control
import { getDicts } from '@/api/system/dict/data'
import { getConfigKey } from '@/api/system/config'
import { parseTime, resetForm, addDateRange, selectDictLabel, selectDictLabels, handleTree } from '@/utils/ruoyi'
// 分页组件
import Pagination from '@/components/Pagination'
// 自定义表格工具组件
import RightToolbar from '@/components/RightToolbar'
// 富文本组件
import Editor from '@/components/Editor'
// 文件上传组件
import FileUpload from '@/components/FileUpload'
// 图片上传组件
import ImageUpload from '@/components/ImageUpload'
// 图片预览组件
import ImagePreview from '@/components/ImagePreview'
// 带默认占位图的图片组件
import Cat2BugImage from '@/components/Cat2BugImage'
import { Cat2BugImageErrorSlot } from '@/utils/upload-asset'
// 字典标签组件
import DictTag from '@/components/DictTag'
// 站点版权
import SiteCopyright from '@/components/SiteCopyright'
// 头部标签组件
import VueMeta from 'vue-meta'

// 字典数据组件
import DictData from '@/components/DictData'

import i18n from './utils/i18n/i18n.js'

import websocket from 'vue-native-websocket'

import VueHotkey from 'v-hotkey'
import VueExcelEditor from 'vue-excel-editor'

// 键盘快捷键引擎
import ShortcutPlugin from '@/plugins/shortcut'
import TabDirectionPlugin from '@/plugins/tab-direction'
import SwitchKeyboardPlugin from '@/plugins/switch-keyboard'
import UploadFocusTabPlugin from '@/plugins/upload-focus-tab'
import ComboFocusTabPlugin from '@/plugins/combo-focus-tab'
import DropdownBlurClosePlugin from '@/plugins/dropdown-blur-close'
import DatePickerEscapePlugin from '@/plugins/date-picker-escape'
import DefectDrawerShortcutsPlugin from '@/plugins/defect-drawer-shortcuts'
import CommandPalette from '@/components/Shortcut/CommandPalette.vue'

// 全局方法挂载
Vue.prototype.getDicts = getDicts
Vue.prototype.getConfigKey = getConfigKey
Vue.prototype.parseTime = parseTime
Vue.prototype.resetForm = resetForm
Vue.prototype.addDateRange = addDateRange
Vue.prototype.selectDictLabel = selectDictLabel
Vue.prototype.selectDictLabels = selectDictLabels
Vue.prototype.download = download
Vue.prototype.handleTree = handleTree

// 全局组件挂载
Vue.component('DictTag', DictTag)
Vue.component('SiteCopyright', SiteCopyright)
Vue.component('Pagination', Pagination)
Vue.component('RightToolbar', RightToolbar)
Vue.component('Editor', Editor)
Vue.component('FileUpload', FileUpload)
Vue.component('ImageUpload', ImageUpload)
Vue.component('ImagePreview', ImagePreview)
Vue.component('Cat2BugImage', Cat2BugImage)
Vue.component('Cat2BugImageErrorSlot', Cat2BugImageErrorSlot)

Vue.use(directive)
Vue.use(plugins)
Vue.use(VueMeta)
Vue.use(VueHotkey)
Vue.use(VueExcelEditor)
Vue.use(ShortcutPlugin)
Vue.use(TabDirectionPlugin)
Vue.use(SwitchKeyboardPlugin)
Vue.use(UploadFocusTabPlugin)
Vue.use(ComboFocusTabPlugin)
Vue.use(DropdownBlurClosePlugin)
Vue.component('CommandPalette', CommandPalette)
DictData.install()

/**
 * If you don't want to use mock-server
 * you want to use MockJs for mock api
 * you can execute: mockXHR()
 *
 * Currently MockJs will be used in the production environment,
 * please remove it before going online! ! !
 */

Vue.use(Element, {
  size: Cookies.get('size') || 'medium' // set element-ui default size
})
Vue.use(DatePickerEscapePlugin)
Vue.use(DefectDrawerShortcutsPlugin)

const userId = store.state.user.id
const wsUrl = (window.location.protocol === 'https:' ? `wss://${location.host}${process.env.VUE_APP_BASE_WEBSOCKET}` : `ws://${location.host}${process.env.VUE_APP_BASE_WEBSOCKET}`)
Vue.use(websocket, wsUrl, {
  reconnection: true,
  reconnectionAttempts: 5,
  reconnectionDelay: 3000,
  connectManually: true
})

Vue.config.productionTip = false

function finishPageLoadBar() {
  const bar = document.getElementById('page-load-bar')
  if (!bar) return
  bar.classList.add('is-done')
  window.setTimeout(() => bar.remove(), 400)
}

new Vue({
  el: '#app',
  i18n,
  router,
  store,
  mounted() {
    finishPageLoadBar()
  },
  render: h => h(App)
})
