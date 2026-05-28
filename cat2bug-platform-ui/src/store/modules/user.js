import { login, logout, getInfo } from '@/api/login'
import { getToken, setToken, removeToken } from '@/utils/auth'
import {updateConfig} from "@/api/system/user-config";

const user = {
  state: {
    token: getToken(),
    id: '',
    name: '',
    avatar: '',
    roles: [],
    permissions: [],
    config: {},
    phoneNumber: ''
  },

  mutations: {
    SET_TOKEN: (state, token) => {
      state.token = token
    },
    SET_ID: (state, id) => {
      state.id = id
    },
    SET_NAME: (state, name) => {
      state.name = name
    },
    SET_AVATAR: (state, avatar) => {
      state.avatar = avatar
    },
    SET_ROLES: (state, roles) => {
      state.roles = roles
    },
    SET_PERMISSIONS: (state, permissions) => {
      state.permissions = permissions
    },
    SET_CONFIG: (state, config) => {
      state.config = config
    },
    SET_PROJECT_ID: (state, projectId) => {
      state.config.currentProjectId = projectId
    },
    SET_PHONE_NUMBER: (state, phoneNumber) => {
      state.phoneNumber = phoneNumber
    },
  },

  actions: {
    // 更新当前项目ID
    UpdateCurrentProjectId(_, projectId) {
      return new Promise((resolve, reject) => {
        updateConfig({
          currentProjectId: projectId
        }).then(res => {
          // currentProjectId 以 GetInfo / SET_CONFIG 为准，避免与侧栏路由切换节拍不一致
          resolve()
        }).catch(error => {
          reject(error)
        });
      })
    },
    /** 切换当前项目：持久化配置 → 刷新用户信息 → 重生成动态路由（侧栏只在这一段结束后展示一次） */
    SwitchCurrentProject({ dispatch }, projectId) {
      dispatch('SetRoutesRefreshing', true)
      return updateConfig({
        currentProjectId: projectId
      }).then(() => dispatch('GetInfo'))
        .then(() => dispatch('GenerateRoutes'))
        .finally(() => {
          dispatch('SetRoutesRefreshing', false)
        })
    },
    // 登录
    Login({ commit }, userInfo) {
      const username = userInfo.username.trim()
      const password = userInfo.password
      const code = userInfo.code
      const uuid = userInfo.uuid
      return new Promise((resolve, reject) => {
        login(username, password, code, uuid).then(res => {
          setToken(res.token)
          commit('SET_TOKEN', res.token)
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 获取用户信息
    GetInfo({ commit, state }) {
      return new Promise((resolve, reject) => {
        getInfo().then(res => {
          const user = res.user
          // const avatar = (user.avatar == "" || user.avatar == null) ? require("@/assets/images/profile.jpg") : process.env.VUE_APP_BASE_API + user.avatar;
          const avatar = (user.avatar == "" || user.avatar == null) ? null : user.avatar;
          if (res.roles && res.roles.length > 0) { // 验证返回的roles是否是一个非空数组
            commit('SET_ROLES', res.roles)
            commit('SET_PERMISSIONS', res.permissions)
          } else {
            commit('SET_ROLES', ['ROLE_DEFAULT'])
          }
          if(res.config) {
            commit('SET_CONFIG', res.config)
          }
          commit('SET_ID', user.userId)
          commit('SET_NAME', user.userName)
          commit('SET_AVATAR', avatar)
          commit('SET_PHONE_NUMBER', user.phoneNumber || '')
          resolve(res)
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 退出系统
    LogOut({ commit, state }) {
      return new Promise((resolve, reject) => {
        logout(state.token).then(() => {
          commit('SET_TOKEN', '')
          commit('SET_ROLES', [])
          commit('SET_PERMISSIONS', [])
          commit('SET_CONFIG', {})
          removeToken()
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 前端 登出
    FedLogOut({ commit }) {
      return new Promise(resolve => {
        commit('SET_TOKEN', '')
        commit('SET_ROLES', [])
        commit('SET_PERMISSIONS', [])
        commit('SET_CONFIG', {})
        removeToken()
        resolve()
      })
    }
  }
}

export default user
