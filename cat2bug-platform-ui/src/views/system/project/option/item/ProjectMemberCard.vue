<template>
  <el-col v-if="cardVisible" :span="6">
    <el-card class="box-card">
      <template #header>
        <div class="clearfix">
          <legacy-icon icon="el-icon-user-solid"></legacy-icon>
          <div>
            <span>{{$t('project.project-and-member')}}</span>
            <span>{{$t('project.project-and-member-describe')}}</span>
          </div>
        </div>
      </template>
      <router-link v-if="canManageMember" :to="href('project-member')"><el-link>{{$t('project.member')}}</el-link></router-link>
    </el-card>
  </el-col>
</template>

<script>
import { hasAnyPermi, projectOptionHref } from '@/utils/project-option-card'

export default {
  name: "ProjectMemberCard",
  props: {
    project: {
      type: Object,
      default: {}
    }
  },
  computed: {
    canManageMember() {
      return hasAnyPermi(['system:project:member'])
    },
    cardVisible() {
      return this.canManageMember
    }
  },
  methods: {
    href: projectOptionHref
  },
}
</script>

<style scoped>

</style>
