<template>
  <el-col v-if="cardVisible" :span="6">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <i class="el-icon-s-order"></i>
        <div>
          <span>{{ $t('defect.custom-field.settings') }}</span>
          <span>{{ $t('defect.custom-field.settings-describe') }}</span>
        </div>
      </div>
      <router-link
        v-if="canListDefectFields"
        to="defect-fields"
      >
        <el-link>{{ $t('defect.custom-field.manage') }}</el-link>
      </router-link>
    </el-card>
  </el-col>
</template>

<script>
import { hasAnyPermi } from '@/utils/project-option-card'

export default {
  name: "DefectSettingsCard",
  model: {
    prop: 'project'
  },
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
  }
}
</script>
