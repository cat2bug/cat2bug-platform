import router from '@/router'
import { useRouter } from 'vue-router'
import { pinia } from '@/store/pinia.js'
import { useTagsViewStore } from '@/stores/tagsView'

const tagsViewStore = useTagsViewStore(pinia)

function getRouter() {
  try {
    return useRouter()
  } catch (_e) {
    return router
  }
}

export default {
  // 刷新当前tab页签
  refreshPage(obj) {
    const appRouter = getRouter()
    const currentRoute = appRouter.currentRoute.value || appRouter.currentRoute
    const { path, query, matched } = currentRoute
    if (obj === undefined) {
      matched.forEach((m) => {
        if (m.components && m.components.default && m.components.default.name) {
          if (!['Layout', 'ParentView'].includes(m.components.default.name)) {
            obj = { name: m.components.default.name, path, query }
          }
        }
      })
    }
    return tagsViewStore.delCachedView(obj).then(() => {
      const { path, query } = obj
      return appRouter.replace({
        path: '/redirect' + path,
        query: query
      })
    })
  },
  // 关闭当前tab页签，打开新页签
  closeOpenPage(obj) {
    const appRouter = getRouter()
    const currentRoute = appRouter.currentRoute.value || appRouter.currentRoute
    tagsViewStore.delView(currentRoute)
    if (obj !== undefined) {
      return appRouter.push(obj)
    }
  },
  // 关闭指定tab页签
  closePage(obj) {
    const appRouter = getRouter()
    const currentRoute = appRouter.currentRoute.value || appRouter.currentRoute
    if (obj === undefined) {
      return tagsViewStore.delView(currentRoute).then(({ visitedViews }) => {
        const latestView = visitedViews.slice(-1)[0]
        if (latestView) {
          return appRouter.push(latestView.fullPath)
        }
        return appRouter.push('/')
      })
    }
    return tagsViewStore.delView(obj)
  },
  // 关闭所有tab页签
  closeAllPage() {
    return tagsViewStore.delAllViews()
  },
  // 关闭左侧tab页签
  closeLeftPage(obj) {
    const appRouter = getRouter()
    const currentRoute = appRouter.currentRoute.value || appRouter.currentRoute
    return tagsViewStore.delLeftTags(obj || currentRoute)
  },
  // 关闭右侧tab页签
  closeRightPage(obj) {
    const appRouter = getRouter()
    const currentRoute = appRouter.currentRoute.value || appRouter.currentRoute
    return tagsViewStore.delRightTags(obj || currentRoute)
  },
  // 关闭其他tab页签
  closeOtherPage(obj) {
    const appRouter = getRouter()
    const currentRoute = appRouter.currentRoute.value || appRouter.currentRoute
    return tagsViewStore.delOthersViews(obj || currentRoute)
  },
  // 添加tab页签
  openPage(title, url, params) {
    const appRouter = getRouter()
    var obj = { path: url, meta: { title: title } }
    tagsViewStore.addView(obj)
    return appRouter.push({ path: url, query: params })
  },
  // 修改tab页签
  updatePage(obj) {
    return Promise.resolve(tagsViewStore.updateVisitedView(obj))
  }
}
