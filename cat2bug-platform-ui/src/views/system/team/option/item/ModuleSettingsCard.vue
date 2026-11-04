<template>
  <el-col v-if="cardVisible" :span="6">
    <el-card class="box-card">
      <template #header>
        <div class="clearfix">
          <svg-icon :icon-class="icon" />
          <div>
            <span>{{ $t(titleKey) }}</span>
            <span>{{ $t(describeKey) }}</span>
          </div>
        </div>
      </template>
      <router-link :to="href(pageRoute)">
        <el-link>{{ $t('defect.page-settings.manage') }}</el-link>
      </router-link>
    </el-card>
  </el-col>
</template>

<script>
import { hasAnyPermi, teamOptionHref } from '@/utils/project-option-card'

export default {
  name: 'TeamModuleSettingsCard',
  props: {
    icon: { type: String, required: true },
    titleKey: { type: String, required: true },
    describeKey: { type: String, required: true },
    pageRoute: { type: String, required: true }
  },
  computed: {
    cardVisible() {
      return hasAnyPermi(['system:team:query', 'system:team:edit'])
    }
  },
  methods: {
    href: teamOptionHref
  }
}
</script>
