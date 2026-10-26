import { defineStore } from 'pinia'
import { login, logout, getInfo } from '@/api/login'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { updateConfig } from '@/api/system/user-config'
import { getProject } from '@/api/system/project'
import { buildPathForProjectSwitch } from '@/utils/project-module-path'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    id: '',
    name: '',
    nickName: '',
    email: '',
    avatar: '',
    roles: [],
    permissions: [],
    config: {},
    teamMember: true,
    contextTeam: null,
    phoneNumber: ''
  }),
  actions: {
    UpdateCurrentProjectId(projectId) {
      return updateConfig({ currentProjectId: projectId }).then(() => {})
    },
    /**
     * Switch current project, refresh auth menus, optionally rewrite URL prefix (3.2).
     * @param {number|string} projectId
     * @param {{ rewriteUrl?: boolean, currentPath?: string }} [options]
     *   rewriteUrl — when true, navigate to same module under new /{teamKey}/{projectKey}
     *   currentPath — override location path used for module extraction
     */
    SwitchCurrentProject(projectId, options = {}) {
      const rewriteUrl = !!(options && options.rewriteUrl)
      const currentPath = options && options.currentPath
      return import('@/stores/permission').then(({ usePermissionStore }) => {
        const permissionStore = usePermissionStore()
        permissionStore.SetRoutesRefreshing(true)
        // 先解析项目所属团队，避免只写 currentProjectId 导致侧栏 teamId 与项目不一致、菜单 path 重写失败
        return getProject(projectId).then((res) => {
          const project = res && res.data
          const payload = { currentProjectId: projectId }
          const teamId = project && Number(project.teamId)
          if (Number.isFinite(teamId) && teamId > 0) {
            payload.currentTeamId = teamId
          }
          return updateConfig(payload)
        }).catch(() => updateConfig({ currentProjectId: projectId }))
          .then(() => this.GetInfo())
          .then(() => permissionStore.GenerateRoutes())
          .then(() => {
            if (!rewriteUrl) return null
            return navigateAfterProjectSwitch(projectId, currentPath)
          })
          .finally(() => {
            permissionStore.SetRoutesRefreshing(false)
          })
      })
    },
    Login(userInfo) {
      const username = userInfo.username.trim()
      const { password, code, uuid } = userInfo
      return login(username, password, code, uuid).then(res => {
        setToken(res.token)
        this.token = res.token
      })
    },
    GetInfo() {
      return getInfo().then(res => {
        const user = res.user
        const avatar = (user.avatar === '' || user.avatar == null) ? null : user.avatar
        if (res.roles && res.roles.length > 0) {
          this.roles = res.roles
          this.permissions = res.permissions
        } else {
          this.roles = ['ROLE_DEFAULT']
        }
        if (res.config) {
          this.config = res.config
        }
        this.teamMember = res.teamMember !== false
        this.contextTeam = res.contextTeam || null
        this.id = user.userId
        this.name = user.userName
        this.nickName = user.nickName || ''
        this.email = user.email || ''
        this.avatar = avatar
        this.phoneNumber = user.phoneNumber || ''
        return res
      })
    },
    LogOut() {
      return logout(this.token).then(() => {
        this.token = ''
        this.roles = []
        this.permissions = []
        this.config = {}
        this.teamMember = true
        this.contextTeam = null
        removeToken()
      })
    },
    FedLogOut() {
      this.token = ''
      this.roles = []
      this.permissions = []
      this.config = {}
      this.teamMember = true
      this.contextTeam = null
      removeToken()
    }
  }
})

/**
 * After a successful project switch, rewrite the History URL to the same module
 * under the destination project's path keys when applicable.
 *
 * @param {number|string} projectId
 * @param {string} [currentPath]
 * @returns {Promise<string|null>} navigated path or null
 */
function navigateAfterProjectSwitch(projectId, currentPath) {
  return getProject(projectId).then(res => {
    const project = res && res.data
    if (!project || !project.teamKey || !project.projectKey) return null

    const fromPath = currentPath
      || (typeof window !== 'undefined' && window.location
        ? window.location.pathname
        : '')
    const nextPath = buildPathForProjectSwitch(fromPath, project.teamKey, project.projectKey)
    if (!nextPath || nextPath === fromPath) return null

    return import('@/router').then(routerMod => {
      const router = routerMod.default
      return router.replace(nextPath).then(() => nextPath)
    })
  }).catch(() => null)
}
