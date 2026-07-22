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
        :to="href('defect-fields')"
      >
        <el-link>{{ $t('defect.custom-field.manage') }}</el-link>
      </router-link>
    </el-card>
  </el-col>
</template>

<script>
import { hasAnyPermi, projectOptionHref } from '@/utils/project-option-card'

export default {
  name: "DefectSettingsCard",
  props: {
    project: {
      type: Object,
      default: () => ({})
    }
  },
  computed: {
    canListDefectFields() {
      return hasAnyPermi(['system:project:defect-field:list'])
    },
    cardVisible() {
      return this.canListDefectFields
    }
  },
  methods: {
    href: projectOptionHref
  }
}
</script>
