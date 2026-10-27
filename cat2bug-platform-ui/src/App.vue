<template>
  <el-config-provider
    :locale="elementPlusLocale"
    :size="elementPlusSize"
    :message="elementPlusMessage"
  >
    <div class="app-root">
      <router-view />
      <theme-picker ghost />
    </div>
  </el-config-provider>
</template>

<script>
import { ElConfigProvider } from 'element-plus'
import Cookies from 'js-cookie'
import { resolveElementPlusLocale } from '@/utils/element-plus-locale'
import ThemePicker from '@/components/ThemePicker'
import { pinia } from '@/store/pinia'
import { useSettingsStore } from '@/stores/settings'
import { useUserStore } from '@/stores/user'
import { APP_MESSAGE_DEFAULTS } from '@/utils/app-message'

export default {
  name: 'App',
  components: { ElConfigProvider, ThemePicker },
  data() {
    return {
      wsUrl: null,
      taskId: null,
      elementPlusMessage: {
        placement: APP_MESSAGE_DEFAULTS.placement,
        showClose: APP_MESSAGE_DEFAULTS.showClose,
        duration: APP_MESSAGE_DEFAULTS.duration,
        offset: APP_MESSAGE_DEFAULTS.offset,
        grouping: APP_MESSAGE_DEFAULTS.grouping
      }
    }
  },
  computed: {
    elementPlusLocale() {
      return resolveElementPlusLocale(this.$i18n.locale)
    },
    settingsTitle() {
      return useSettingsStore(pinia).title
    },
    settingsDynamicTitle() {
      return useSettingsStore(pinia).dynamicTitle
    },
    /** Vue2 全局 size 多为 small/medium；统一映射为 EP small（输入框等已覆写为 32px）。开关须显式 default 或依赖 cat2bug.scss 表单内修正。 */
    elementPlusSize() {
      const raw = Cookies.get('size') || 'small'
      if (raw === 'large') return 'large'
      return 'small'
    }
  },
  watch: {
    settingsTitle() {
      this.syncDocumentTitle()
    },
    settingsDynamicTitle() {
      this.syncDocumentTitle()
    },
    '$i18n.locale'() {
      this.syncDocumentTitle()
    }
  },
  created() {
    this.welcome()
    this.syncDocumentTitle()
    this.initWebSocket()
  },
  beforeUnmount() {
    if (this.taskId) {
      clearInterval(this.taskId)
      this.taskId = null
    }
  },
  methods: {
    welcome() {
      console.log(this.$i18n.t('welcome'))
    },
    syncDocumentTitle() {
      const settings = useSettingsStore(pinia)
      const dynamic = settings.dynamicTitle
      const pageTitle = settings.title
      const systemName = this.$i18n.t('system-name')
      document.title = dynamic && pageTitle ? `${pageTitle} - ${systemName}` : systemName
    },
    initWebSocket() {
      this.taskId = setInterval(() => {
        const userId = useUserStore(pinia).id
        if (userId) {
          const wsBase = process.env.VUE_APP_BASE_WEBSOCKET || ''
          this.wsUrl = (window.location.protocol === 'https:'
            ? `wss://${location.host}${wsBase}/websocket/${userId}/message`
            : `ws://${location.host}${wsBase}/websocket/${userId}/message`)
          this.$connect(this.wsUrl, { format: 'json' })
          this.$socket.onopen = this.webSocketOnOpen
          this.$socket.onerror = this.webSocketOnError
          this.$socket.onmessage = this.webSocketOnMessage
          this.$socket.onclose = this.webSocketClose
          clearInterval(this.taskId)
          this.taskId = null
        }
      }, 1000)
    },
    webSocketOnOpen() {},
    webSocketOnMessage(e) {
      if (!e.data) return
      const res = JSON.parse(e.data)
      if (res.code == 200) {
        this.$topic.publish(res.action, res)
      }
    },
    webSocketOnError(e) {
      console.error(e)
    },
    webSocketClose(e) {
      console.log(e)
      this.initWebSocket()
    }
  }
}
</script>
<style>
.app-root {
  height: 100%;
  overflow: hidden;
}
/* 根级 ThemePicker 仅负责启动时换肤，不可见；设置抽屉内单独实例不带 ghost */
.app-root > .theme-picker.theme-picker--ghost {
  position: absolute !important;
  width: 0 !important;
  height: 0 !important;
  margin: 0 !important;
  padding: 0 !important;
  overflow: hidden !important;
  opacity: 0 !important;
  pointer-events: none !important;
}
</style>
