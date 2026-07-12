import { defineStore } from 'pinia'
import i18n from '@/utils/i18n/i18n'

function titleName(menu) {
  if (menu.meta.titleI18nKey) {
    return i18n.global.t(menu.meta.titleI18nKey)
  }
  return menu.meta.title
}

export const useTagsViewStore = defineStore('tagsView', {
  state: () => ({
    visitedViews: [],
    cachedViews: [],
    iframeViews: []
  }),
  actions: {
    addView(view) {
      this.addVisitedView(view)
      this.addCachedView(view)
    },
    addIframeView(view) {
      if (this.iframeViews.some(v => v.path === view.path)) return
      this.iframeViews.push(Object.assign({}, view, { title: titleName(view) || 'no-name' }))
    },
    addVisitedView(view) {
      if (this.visitedViews.some(v => v.path === view.path)) return
      this.visitedViews.push(Object.assign({}, view, { title: titleName(view) || 'no-name' }))
    },
    addCachedView(view) {
      if (this.cachedViews.includes(view.name)) return
      if (view.meta && !view.meta.noCache) {
        this.cachedViews.push(view.name)
      }
    },
    delView(view) {
      this.delVisitedView(view)
      this.delCachedView(view)
      return Promise.resolve({
        visitedViews: [...this.visitedViews],
        cachedViews: [...this.cachedViews]
      })
    },
    delVisitedView(view) {
      for (const [i, v] of this.visitedViews.entries()) {
        if (v.path === view.path) {
          this.visitedViews.splice(i, 1)
          break
        }
      }
      this.iframeViews = this.iframeViews.filter(item => item.path !== view.path)
      return Promise.resolve([...this.visitedViews])
    },
    delIframeView(view) {
      this.iframeViews = this.iframeViews.filter(item => item.path !== view.path)
      return Promise.resolve([...this.iframeViews])
    },
    delCachedView(view) {
      const index = this.cachedViews.indexOf(view.name)
      if (index > -1) this.cachedViews.splice(index, 1)
      return Promise.resolve([...this.cachedViews])
    },
    delOthersViews(view) {
      this.delOthersVisitedViews(view)
      this.delOthersCachedViews(view)
      return Promise.resolve({
        visitedViews: [...this.visitedViews],
        cachedViews: [...this.cachedViews]
      })
    },
    delOthersVisitedViews(view) {
      this.visitedViews = this.visitedViews.filter(v => v.meta.affix || v.path === view.path)
      this.iframeViews = this.iframeViews.filter(item => item.path === view.path)
      return Promise.resolve([...this.visitedViews])
    },
    delOthersCachedViews(view) {
      const index = this.cachedViews.indexOf(view.name)
      if (index > -1) {
        this.cachedViews = this.cachedViews.slice(index, index + 1)
      } else {
        this.cachedViews = []
      }
      return Promise.resolve([...this.cachedViews])
    },
    delAllViews() {
      this.delAllVisitedViews()
      this.delAllCachedViews()
      return Promise.resolve({
        visitedViews: [...this.visitedViews],
        cachedViews: [...this.cachedViews]
      })
    },
    delAllVisitedViews() {
      const affixTags = this.visitedViews.filter(tag => tag.meta.affix)
      this.visitedViews = affixTags
      this.iframeViews = []
      return Promise.resolve([...this.visitedViews])
    },
    delAllCachedViews() {
      this.cachedViews = []
      return Promise.resolve([...this.cachedViews])
    },
    updateVisitedView(view) {
      for (let v of this.visitedViews) {
        if (v.path === view.path) {
          Object.assign(v, view)
          break
        }
      }
    },
    delRightTags(view) {
      const index = this.visitedViews.findIndex(v => v.path === view.path)
      if (index === -1) return Promise.resolve([...this.visitedViews])
      this.visitedViews = this.visitedViews.filter((item, idx) => {
        if (idx <= index || (item.meta && item.meta.affix)) return true
        const i = this.cachedViews.indexOf(item.name)
        if (i > -1) this.cachedViews.splice(i, 1)
        if (item.meta.link) {
          const fi = this.iframeViews.findIndex(v => v.path === item.path)
          this.iframeViews.splice(fi, 1)
        }
        return false
      })
      return Promise.resolve([...this.visitedViews])
    },
    delLeftTags(view) {
      const index = this.visitedViews.findIndex(v => v.path === view.path)
      if (index === -1) return Promise.resolve([...this.visitedViews])
      this.visitedViews = this.visitedViews.filter((item, idx) => {
        if (idx >= index || (item.meta && item.meta.affix)) return true
        const i = this.cachedViews.indexOf(item.name)
        if (i > -1) this.cachedViews.splice(i, 1)
        if (item.meta.link) {
          const fi = this.iframeViews.findIndex(v => v.path === item.path)
          this.iframeViews.splice(fi, 1)
        }
        return false
      })
      return Promise.resolve([...this.visitedViews])
    }
  }
})
