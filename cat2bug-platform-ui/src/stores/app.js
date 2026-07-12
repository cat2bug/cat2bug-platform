import { defineStore } from 'pinia'
import Cookies from 'js-cookie'

export const useAppStore = defineStore('app', {
  state: () => ({
    sidebar: {
      opened: Cookies.get('sidebarStatus') === '1',
      withoutAnimation: false,
      hide: false
    },
    device: 'desktop',
    size: Cookies.get('size') || 'medium'
  }),
  actions: {
    toggleSideBar() {
      if (this.sidebar.hide) return false
      this.sidebar.opened = !this.sidebar.opened
      this.sidebar.withoutAnimation = false
      Cookies.set('sidebarStatus', this.sidebar.opened ? 1 : 0)
    },
    closeSideBar({ withoutAnimation } = {}) {
      Cookies.set('sidebarStatus', 0)
      this.sidebar.opened = false
      this.sidebar.withoutAnimation = !!withoutAnimation
    },
    toggleDevice(device) {
      this.device = device
    },
    setSize(size) {
      this.size = size
      Cookies.set('size', size)
    },
    toggleSideBarHide(status) {
      this.sidebar.hide = status
    }
  }
})
