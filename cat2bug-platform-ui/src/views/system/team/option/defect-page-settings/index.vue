<template>
  <div class="app-container project-list-page-host defect-page-settings-page">
    <el-row class="project-add-page-header team-option-sub-hint-back">
      <el-page-header @back="goBack" :content="$t('defect.page-settings.title')" />
    </el-row>

    <el-form v-loading="loading" label-width="140px" class="defect-page-settings-form">
      <el-form-item :label="$t('team.module-page.show-module')">
        <el-switch v-model="moduleForm.enabled" :disabled="!canEdit" />
      </el-form-item>
      <el-form-item :label="$t('team.module-page.module-name')">
        <el-input
          v-model="moduleForm.label"
          maxlength="32"
          clearable
          :disabled="!canEdit"
          :placeholder="$t('defect')"
        />
      </el-form-item>
      <el-form-item>
        <p class="module-page-settings-hint">{{ $t('team.module-page.reopen-hint') }}</p>
      </el-form-item>
      <el-divider />

      <el-form-item :label="$t('defect.page-settings.views')">
        <el-checkbox
          v-for="item in viewOptions"
          :key="item.value"
          v-model="form.views[item.value]"
          :disabled="!canEdit"
        >
          {{ item.label }}
        </el-checkbox>
      </el-form-item>

      <el-divider content-position="left">{{ $t('defect.page-settings.life-colors') }}</el-divider>
      <el-form-item :label="$t('defect.page-settings.green-below-days')">
        <el-input-number v-model="form.lifeTimeColors.greenBelowDays" :min="0" :step="0.5" :disabled="!canEdit" />
      </el-form-item>
      <el-form-item :label="$t('defect.page-settings.orange-below-days')">
        <el-input-number v-model="form.lifeTimeColors.orangeBelowDays" :min="0" :step="0.5" :disabled="!canEdit" />
      </el-form-item>
      <el-form-item :label="$t('defect.page-settings.colors')">
        <el-color-picker v-model="form.lifeTimeColors.green" :disabled="!canEdit" />
        <el-color-picker v-model="form.lifeTimeColors.orange" :disabled="!canEdit" />
        <el-color-picker v-model="form.lifeTimeColors.red" :disabled="!canEdit" />
      </el-form-item>

      <el-divider content-position="left">{{ $t('defect.page-settings.reject-colors') }}</el-divider>
      <el-form-item :label="$t('defect.page-settings.green-below-count')">
        <el-input-number v-model="form.rejectCountColors.greenBelow" :min="0" :step="1" :disabled="!canEdit" />
      </el-form-item>
      <el-form-item :label="$t('defect.page-settings.orange-below-count')">
        <el-input-number v-model="form.rejectCountColors.orangeBelow" :min="0" :step="1" :disabled="!canEdit" />
      </el-form-item>
      <el-form-item :label="$t('defect.page-settings.colors')">
        <el-color-picker v-model="form.rejectCountColors.green" :disabled="!canEdit" />
        <el-color-picker v-model="form.rejectCountColors.orange" :disabled="!canEdit" />
        <el-color-picker v-model="form.rejectCountColors.red" :disabled="!canEdit" />
      </el-form-item>

      <el-form-item :label="$t('defect.page-settings.allow-share')">
        <el-switch v-model="form.allowShare" :disabled="!canEdit" />
      </el-form-item>

      <el-form-item v-if="canEdit">
        <el-button :disabled="saving" @click="reset">{{ $t('reset') }}</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ $t('save') }}</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import {
  getTeamDefectPageSettings,
  saveTeamDefectPageSettings
} from '@/api/system/defect-page-settings'
import { defaultDefectPageSettings, mergeDefectPageSettings } from '@/utils/defect-page-settings'
import {
  getTeamModulePageSettings,
  saveTeamModulePageSettings
} from '@/api/system/module-page-settings'
import {
  mergeModulePageSettings,
  toModulePageSettingsPayload
} from '@/utils/module-page-settings'
import { checkPermi } from '@/utils/permission'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/user'
import { useModulePageSettingsStore } from '@/stores/modulePageSettings'
import { usePermissionStore } from '@/stores/permission'
import { teamOptionHref } from '@/utils/project-option-card'

export default {
  name: 'TeamDefectPageSettings',
  setup() {
    const userStore = useUserStore()
    const { config } = storeToRefs(userStore)
    return { config }
  },
  data() {
    return {
      loading: false,
      saving: false,
      moduleSettings: mergeModulePageSettings(null),
      moduleForm: { enabled: true, label: '' },
      form: defaultDefectPageSettings(),
      viewOptions: [
        { value: 'DefectTable', labelKey: 'table' },
        { value: 'DefectExcel', labelKey: 'excel' },
        { value: 'DefectGantt', labelKey: 'gantt' },
        { value: 'DefectKanban', labelKey: 'kanban' }
      ]
    }
  },
  computed: {
    teamId() {
      return parseInt(this.config.currentTeamId, 10)
    },
    canEdit() {
      return checkPermi(['system:team:defect-field:edit'])
    }
  },
  created() {
    this.viewOptions = this.viewOptions.map((o) => ({
      ...o,
      label: this.$t(o.labelKey)
    }))
    this.load()
  },
  methods: {
    goBack() {
      this.$router.push(teamOptionHref('team-option'))
    },
    load() {
      if (!this.teamId) return
      this.loading = true
      Promise.all([
        getTeamDefectPageSettings(this.teamId),
        getTeamModulePageSettings(this.teamId).catch(() => ({ data: null }))
      ]).then(([pageRes, moduleRes]) => {
        this.form = mergeDefectPageSettings(pageRes.data)
        this.moduleSettings = mergeModulePageSettings(moduleRes && moduleRes.data)
        this.moduleForm = {
          enabled: this.moduleSettings.defect.enabled,
          label: this.moduleSettings.defect.label || ''
        }
      }).finally(() => {
        this.loading = false
      })
    },
    /** 确认后用内置默认配置写库并刷新表单 */
    reset() {
      if (!this.canEdit || this.saving || !this.teamId) return
      this.$modal
        .confirm(
          this.$t('defect.page-settings.reset-confirm').toString(),
          this.$t('prompted').toString(),
          {
            confirmButtonText: this.$t('ok').toString(),
            cancelButtonText: this.$t('cancel').toString(),
            type: 'warning'
          }
        )
        .then(() => {
          this.saving = true
          const defaults = defaultDefectPageSettings()
          return saveTeamDefectPageSettings(this.teamId, defaults).then(() => {
            this.form = defaults
            this.$modal.msgSuccess(this.$t('save-success') || this.$t('success'))
            this.load()
          })
        })
        .catch(() => {})
        .finally(() => {
          this.saving = false
        })
    },
    saveModuleSection() {
      const next = mergeModulePageSettings(this.moduleSettings)
      next.defect.enabled = !!this.moduleForm.enabled
      next.defect.label = this.moduleForm.label
      const payload = toModulePageSettingsPayload(next)
      return saveTeamModulePageSettings(this.teamId, payload).then(() => {
        this.moduleSettings = mergeModulePageSettings({
          ...payload,
          systemMaxUploadMb: next.systemMaxUploadMb
        })
        useModulePageSettingsStore().apply(this.moduleSettings, { teamId: this.teamId })
        const permissionStore = usePermissionStore()
        if (permissionStore.addRoutes && permissionStore.addRoutes.length) {
          return permissionStore.GenerateRoutes()
        }
        return null
      })
    },
    save() {
      this.saving = true
      this.saveModuleSection()
        .then(() => saveTeamDefectPageSettings(this.teamId, this.form))
        .then(() => {
          this.$modal.msgSuccess(this.$t('save-success') || this.$t('success'))
          this.load()
        })
        .finally(() => {
          this.saving = false
        })
    }
  }
}
</script>

<style scoped>
.defect-page-settings-form {
  max-width: 720px;
  margin-top: 12px;
}
.defect-page-settings-form :deep(.el-checkbox) {
  margin-right: 16px;
}
.defect-page-settings-form :deep(.el-color-picker) {
  margin-right: 8px;
  vertical-align: middle;
}
.module-page-settings-hint {
  margin: 0 0 8px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-color-secondary);
}
</style>
