import defaultSettings from '@/settings'
import { applyThemeMode, readStoredThemeMode, THEME_MODE_STORAGE_KEY } from '@/utils/theme-mode'

const { sideTheme, showSettings, topNav, tagsView, fixedHeader, sidebarLogo, dynamicTitle } = defaultSettings

const initThemeMode = () => {
  const mode = readStoredThemeMode()
  applyThemeMode(mode)
  return mode
}

const storageSetting = JSON.parse(localStorage.getItem('layout-setting')) || ''
const state = {
  title: '',
  theme: storageSetting.theme || '#409EFF',
  sideTheme: storageSetting.sideTheme || sideTheme,
  showSettings: showSettings,
  topNav: storageSetting.topNav === undefined ? topNav : storageSetting.topNav,
  tagsView: storageSetting.tagsView === undefined ? tagsView : storageSetting.tagsView,
  fixedHeader: storageSetting.fixedHeader === undefined ? fixedHeader : storageSetting.fixedHeader,
  sidebarLogo: storageSetting.sidebarLogo === undefined ? sidebarLogo : storageSetting.sidebarLogo,
  dynamicTitle: storageSetting.dynamicTitle === undefined ? dynamicTitle : storageSetting.dynamicTitle,
  themeMode: initThemeMode()
}
const mutations = {
  CHANGE_SETTING: (state, { key, value }) => {
    if (state.hasOwnProperty(key)) {
      state[key] = value
    }
  },
  SET_THEME_MODE: (state, themeMode) => {
    state.themeMode = themeMode
    localStorage.setItem(THEME_MODE_STORAGE_KEY, themeMode)
    applyThemeMode(themeMode)
  }
}

const actions = {
  // 修改布局设置
  changeSetting({ commit }, data) {
    commit('CHANGE_SETTING', data)
  },
  // 设置网页标题
  setTitle({ commit }, title) {
    state.title = title
  },
  // 改变主题模式
  changeThemeMode({ commit }, themeMode) {
    commit('SET_THEME_MODE', themeMode)
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}

