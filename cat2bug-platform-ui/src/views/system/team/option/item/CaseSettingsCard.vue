<template>
  <el-col v-if="cardVisible" :span="6">
    <el-card class="box-card">
      <template #header>
        <div class="clearfix">
          <svg-icon icon-class="case" />
          <div>
            <span>{{ $t('case.type.settings') }}</span>
            <span>{{ $t('case.type.settings-describe') }}</span>
          </div>
        </div>
      </template>
      <router-link v-if="canList" :to="href('team-case-types')">
        <el-link>{{ $t('case.type.manage') }}</el-link>
      </router-link>
      <router-link v-if="canList" :to="href('team-case-settings')">
        <el-link>{{ $t('case.review.template') }}</el-link>
      </router-link>
    </el-card>
  </el-col>
</template>

<script>
import { hasAnyPermi, teamOptionHref } from '@/utils/project-option-card'

export default {
  name: 'TeamCaseSettingsCard',
  computed: {
    canList() {
      return hasAnyPermi(['system:team:case-type:list'])
    },
    cardVisible() {
      return this.canList
    }
  },
  methods: {
    href: teamOptionHref
  }
}
</script>
