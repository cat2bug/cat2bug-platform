<template>
  <el-card class="box-card">
    <div slot="header" class="clearfix">
      <i class="el-icon-user-solid"></i>
      <div>
        <span>{{$t('project.project-and-member')}}</span>
        <span>{{$t('project.project-and-member-describe')}}</span>
      </div>
    </div>
    <router-link to="project-member"><el-link>{{$t('member.manage')}}</el-link></router-link>
  </el-card>
</template>

<script>
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
}
</script>

<style scoped>

</style>
