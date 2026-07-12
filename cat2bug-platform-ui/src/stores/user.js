import { defineStore } from 'pinia'
import { login, logout, getInfo } from '@/api/login'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { updateConfig } from '@/api/system/user-config'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    id: '',
    name: '',
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
    SwitchCurrentProject(projectId) {
      return import('@/stores/permission').then(({ usePermissionStore }) => {
        const permissionStore = usePermissionStore()
        permissionStore.SetRoutesRefreshing(true)
        return updateConfig({ currentProjectId: projectId })
          .then(() => this.GetInfo())
          .then(() => permissionStore.GenerateRoutes())
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
