<template>
  <el-col v-if="cardVisible" :span="6">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <i class="el-icon-user-solid"></i>
        <div>
          <span>{{$t('project.project-and-member')}}</span>
          <span>{{$t('project.project-and-member-describe')}}</span>
        </div>
      </div>
      <router-link v-if="canManageMember" to="project-member"><el-link>{{$t('member.manage')}}</el-link></router-link>
    </el-card>
  </el-col>
</template>

<script>
import { hasAnyPermi } from '@/utils/project-option-card'

export default {
  name: "ProjectMemberCard",
  model: {
    prop: 'project'
  },
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
}
</script>

<style scoped>

</style>
