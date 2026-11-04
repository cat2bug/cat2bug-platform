<template>
  <div class="app-container project-list-page-host module-page-settings-page">
    <el-row class="project-add-page-header team-option-sub-hint-back">
      <el-page-header @back="goBack" :content="pageTitle" />
    </el-row>

    <el-form v-loading="loading" label-width="140px" class="module-page-settings-form">
      <el-form-item :label="$t('team.module-page.show-module')">
        <el-switch v-model="form.enabled" :disabled="!canEdit" />
      </el-form-item>

      <template v-if="isCasePlan">
        <el-form-item :label="$t('team.module-page.case-name')">
          <el-input
            v-model="form.labelCase"
            maxlength="32"
            clearable
            :disabled="!canEdit"
            :placeholder="$t('case')"
          />
        </el-form-item>
        <el-form-item :label="$t('team.module-page.plan-name')">
          <el-input
            v-model="form.labelPlan"
            maxlength="32"
            clearable
            :disabled="!canEdit"
            :placeholder="$t('plan')"
          />
        </el-form-item>
      </template>
      <el-form-item v-else :label="$t('team.module-page.module-name')">
        <el-input
          v-model="form.label"
          maxlength="32"
          clearable
          :disabled="!canEdit"
          :placeholder="defaultLabel"
        />
      </el-form-item>

      <template v-if="isDocument">
        <el-form-item :label="$t('team.module-page.max-upload')">
          <div class="module-page-settings-upload">
            <el-checkbox v-model="form.useCustomMax" :disabled="!canEdit">
              {{ $t('team.module-page.custom-max') }}
            </el-checkbox>
            <el-input-number
              v-if="form.useCustomMax"
              v-model="form.maxUploadMb"
              :min="1"
              :max="systemMax"
              :step="1"
              :precision="0"
              :disabled="!canEdit"
              controls-position="right"
            />
            <div class="module-page-settings-hint">
              {{ $t('team.module-page.max-upload-hint', { max: systemMax }) }}
            </div>
          </div>
        </el-form-item>
      </template>

      <el-form-item>
        <p class="module-page-settings-hint">{{ $t('team.module-page.reopen-hint') }}</p>
      </el-form-item>

      <el-form-item v-if="canEdit">
        <el-button type="primary" :loading="saving" @click="save">{{ $t('save') }}</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { getTeamModulePageSettings, saveTeamModulePageSettings } from '@/api/system/module-page-settings'
import {
  mergeModulePageSettings,
  toModulePageSettingsPayload
} from '@/utils/module-page-settings'
import { checkPermi } from '@/utils/permission'
import { teamOptionHref } from '@/utils/project-option-card'
import { useUserStore } from '@/stores/user'
import { useModulePageSettingsStore } from '@/stores/modulePageSettings'
import { usePermissionStore } from '@/stores/permission'

const LABEL_FALLBACK_KEY = {
  code: 'git.repo.menu',
  deliverable: 'module',
  document: 'doc',
  defect: 'defect'
}

export default {
  name: 'TeamModulePageSettings',
  data() {
    return {
      loading: false,
      saving: false,
      settings: mergeModulePageSettings(null),
      form: {
        enabled: true,
        label: '',
        labelCase: '',
        labelPlan: '',
        useCustomMax: false,
        maxUploadMb: null
      }
    }
  },
  computed: {
    moduleKey() {
      return (this.$route.meta && this.$route.meta.modulePageKey) || 'code'
    },
    isCasePlan() {
      return this.moduleKey === 'case_plan'
    },
    isDocument() {
      return this.moduleKey === 'document'
    },
    teamId() {
      return parseInt(useUserStore().config.currentTeamId, 10)
    },
    systemMax() {
      return this.settings.systemMaxUploadMb
    },
    pageTitle() {
      return this.$t('team.module-page.' + this.moduleKey + '-page')
    },
    defaultLabel() {
      const key = LABEL_FALLBACK_KEY[this.moduleKey]
      return key ? this.$t(key) : ''
    },
    canEdit() {
      if (this.isCasePlan) {
        return checkPermi(['system:team:case-type:edit']) || checkPermi(['system:team:edit'])
      }
      return checkPermi(['system:team:edit'])
    }
  },
  created() {
    this.load()
  },
  methods: {
    goBack() {
      this.$router.push(teamOptionHref('team-option'))
    },
    fillForm(settings) {
      if (this.isCasePlan) {
        this.form.enabled = settings.case_plan.enabled
        this.form.labelCase = settings.case_plan.labelCase || ''
        this.form.labelPlan = settings.case_plan.labelPlan || ''
        return
      }
      const block = settings[this.moduleKey] || {}
      this.form.enabled = block.enabled !== false
      this.form.label = block.label || ''
      if (this.isDocument) {
        this.form.useCustomMax = block.maxUploadMb != null
        this.form.maxUploadMb = block.maxUploadMb || settings.systemMaxUploadMb
      }
    },
    load() {
      if (!this.teamId) return
      this.loading = true
      getTeamModulePageSettings(this.teamId).then((res) => {
        this.settings = mergeModulePageSettings(res && res.data)
        this.fillForm(this.settings)
      }).finally(() => {
        this.loading = false
      })
    },
    applyForm(settings) {
      const next = mergeModulePageSettings(settings)
      if (this.isCasePlan) {
        next.case_plan.enabled = !!this.form.enabled
        next.case_plan.labelCase = this.form.labelCase
        next.case_plan.labelPlan = this.form.labelPlan
        return next
      }
      next[this.moduleKey].enabled = !!this.form.enabled
      next[this.moduleKey].label = this.form.label
      if (this.isDocument) {
        next.document.maxUploadMb = this.form.useCustomMax ? this.form.maxUploadMb : null
      }
      return next
    },
    save() {
      if (!this.canEdit || !this.teamId) return
      const next = this.applyForm(this.settings)
      if (this.isDocument && next.document.maxUploadMb != null) {
        const max = next.systemMaxUploadMb
        if (next.document.maxUploadMb > max) {
          this.$modal.msgError(this.$t('team.module-page.over-system-max', { max }))
          return
        }
      }
      this.saving = true
      const payload = toModulePageSettingsPayload(next)
      saveTeamModulePageSettings(this.teamId, payload).then(() => {
        this.settings = mergeModulePageSettings({ ...payload, systemMaxUploadMb: next.systemMaxUploadMb })
        useModulePageSettingsStore().apply(this.settings, { teamId: this.teamId })
        this.$modal.msgSuccess(this.$t('save-success'))
        const permissionStore = usePermissionStore()
        if (permissionStore.addRoutes && permissionStore.addRoutes.length) {
          permissionStore.GenerateRoutes()
        }
      }).finally(() => {
        this.saving = false
      })
    }
  }
}
</script>

<style scoped>
.module-page-settings-form {
  max-width: 720px;
  margin-top: 12px;
}
.module-page-settings-upload {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}
.module-page-settings-hint {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-color-secondary);
}
</style>
