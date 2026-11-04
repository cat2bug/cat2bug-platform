<template>
  <el-col v-if="cardVisible" :span="6">
    <el-card class="box-card">
      <template #header>
        <div class="clearfix">
          <svg-icon icon-class="bug" />
          <div>
            <span>{{ $t('defect.custom-field.settings') }}</span>
            <span>{{ $t('defect.custom-field.settings-describe') }}</span>
          </div>
        </div>
      </template>
      <router-link
        v-if="canListDefectFields"
        :to="href('team-defect-fields')"
      >
        <el-link>{{ $t('defect.custom-field.manage') }}</el-link>
      </router-link>
      <router-link
        v-if="canListDefectFields"
        class="defect-settings-card-page-link"
        :to="href('team-defect-page-settings')"
      >
        <el-link>{{ $t('defect.page-settings.manage') }}</el-link>
      </router-link>
    </el-card>
  </el-col>
</template>

<script>
import { hasAnyPermi, teamOptionHref } from '@/utils/project-option-card'

export default {
  name: 'TeamDefectSettingsCard',
  computed: {
    canListDefectFields() {
      return hasAnyPermi(['system:team:defect-field:list'])
    },
    cardVisible() {
      return this.canListDefectFields
    }
  },
  methods: {
    href: teamOptionHref
  }
}
</script>

<style scoped>
.defect-settings-card-page-link {
  display: block;
  margin-top: 8px;
}
</style>
