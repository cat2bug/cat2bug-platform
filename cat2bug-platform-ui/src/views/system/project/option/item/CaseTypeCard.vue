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
      <router-link v-if="canList" :to="href('case-types')">
        <el-link>{{ $t('case.type.manage') }}</el-link>
      </router-link>
      <router-link v-if="canList" :to="href('case-settings')">
        <el-link>{{ $t('case.review.template') }}</el-link>
      </router-link>
    </el-card>
  </el-col>
</template>

<script>
import { hasAnyPermi, projectOptionHref } from '@/utils/project-option-card'

export default {
  name: 'CaseTypeCard',
  props: {
    project: {
      type: Object,
      default: () => ({})
    }
  },
  computed: {
    canList() {
      return hasAnyPermi(['system:project:case-type:list'])
    },
    cardVisible() {
      return this.canList
    }
  },
  methods: {
    href: projectOptionHref
  }
}
</script>
