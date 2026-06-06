/**
 * 快捷键插件入口：注册 $shortcut 与页面注册 API，并启动全局监听。
 */
import { shortcutService } from './service'
import { shortcutStore } from './shortcut-store'
import { pageRegistry } from './registry'

const ShortcutPlugin = {
  install(Vue) {
    const api = {
      service: shortcutService,
      store: shortcutStore,
      open: (mode) => shortcutService.open(mode),
      close: () => shortcutService.close(),
      registerPage: (scopeKey, actions) => pageRegistry.registerPage(scopeKey, actions),
      unregisterPage: (scopeKey) => pageRegistry.unregisterPage(scopeKey)
    }
    Vue.prototype.$shortcut = api
    shortcutService.start()
  }
}

export default ShortcutPlugin
export { shortcutService, shortcutStore, pageRegistry }
