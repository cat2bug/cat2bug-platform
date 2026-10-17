import { defineStore } from 'pinia'
import defaultSettings from '@/settings'
import { applyThemeMode, readStoredThemeMode, THEME_MODE_STORAGE_KEY } from '@/utils/theme-mode'

const { sideTheme, showSettings, topNav, tagsView, fixedHeader, sidebarLogo, dynamicTitle } = defaultSettings

const initThemeMode = () => {
  const mode = readStoredThemeMode()
  applyThemeMode(mode)
  return mode
}

const storageSetting = JSON.parse(localStorage.getItem('layout-setting') || '{}')
const themeMode = initThemeMode()
const storedSideTheme = storageSetting.sideTheme || sideTheme
const sideThemeValue =
  themeMode === 'light' && storedSideTheme === 'theme-dark'
    ? 'theme-light'
    : storedSideTheme

export const useSettingsStore = defineStore('settings', {
  state: () => ({
    title: '',
    theme: storageSetting.theme || '#1890ff',
    sideTheme: sideThemeValue,
    showSettings,
    topNav: storageSetting.topNav === undefined ? topNav : storageSetting.topNav,
    tagsView: storageSetting.tagsView === undefined ? tagsView : storageSetting.tagsView,
    fixedHeader: storageSetting.fixedHeader === undefined ? fixedHeader : storageSetting.fixedHeader,
    sidebarLogo: storageSetting.sidebarLogo === undefined ? sidebarLogo : storageSetting.sidebarLogo,
    dynamicTitle: storageSetting.dynamicTitle === undefined ? dynamicTitle : storageSetting.dynamicTitle,
    themeMode
  }),
  actions: {
    changeSetting(data) {
      const { key, value } = data
      if (Object.prototype.hasOwnProperty.call(this, key)) {
        this[key] = value
      }
    },
    setTitle(title) {
      this.title = title
    },
    changeThemeMode(themeMode) {
      this.themeMode = themeMode
      const syncedSideTheme = themeMode === 'dark' ? 'theme-dark' : 'theme-light'
      this.sideTheme = syncedSideTheme
      localStorage.setItem(THEME_MODE_STORAGE_KEY, themeMode)
      try {
        const layoutSetting = JSON.parse(localStorage.getItem('layout-setting') || '{}')
        layoutSetting.sideTheme = syncedSideTheme
        localStorage.setItem('layout-setting', JSON.stringify(layoutSetting))
      } catch (e) {
        /* ignore */
      }
      applyThemeMode(themeMode)
    }
  }
})
