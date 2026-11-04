import { defineStore } from 'pinia'
import {
  getProjectModulePageSettings,
  getTeamModulePageSettings
} from '@/api/system/module-page-settings'
import {
  defaultModulePageSettings,
  effectiveDocumentUploadMb,
  isTeamModuleEnabled,
  mergeModulePageSettings
} from '@/utils/module-page-settings'
import { applyModulePageI18n } from '@/utils/module-page-i18n'

export const useModulePageSettingsStore = defineStore('modulePageSettings', {
  state: () => ({
    projectId: null,
    teamId: null,
    loaded: false,
    settings: defaultModulePageSettings()
  }),
  getters: {
    effectiveUploadMb(state) {
      return effectiveDocumentUploadMb(state.settings)
    }
  },
  actions: {
    apply(raw, ids = {}) {
      this.settings = mergeModulePageSettings(raw)
      if (ids.projectId != null && ids.projectId !== '') {
        this.projectId = Number(ids.projectId)
      }
      if (ids.teamId != null && ids.teamId !== '') {
        this.teamId = Number(ids.teamId)
      }
      this.loaded = true
      applyModulePageI18n(this.settings)
    },
    isEnabled(key) {
      return isTeamModuleEnabled(this.settings, key)
    },
    loadProject(projectId) {
      const id = Number(projectId)
      if (!Number.isFinite(id) || id <= 0) return Promise.resolve(this.settings)
      return getProjectModulePageSettings(id).then((res) => {
        this.apply(res && res.data, { projectId: id })
        return this.settings
      }).catch(() => {
        if (this.loaded && Number(this.projectId) === id) return this.settings
        this.apply(defaultModulePageSettings(), { projectId: id })
        return this.settings
      })
    },
    loadTeam(teamId) {
      const id = Number(teamId)
      if (!Number.isFinite(id) || id <= 0) return Promise.resolve(this.settings)
      return getTeamModulePageSettings(id).then((res) => {
        this.apply(res && res.data, { teamId: id })
        return this.settings
      }).catch(() => {
        if (this.loaded && Number(this.teamId) === id) return this.settings
        this.apply(defaultModulePageSettings(), { teamId: id })
        return this.settings
      })
    }
  }
})
