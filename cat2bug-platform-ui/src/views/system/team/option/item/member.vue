<template>
  <el-card class="box-card">
    <div slot="header" class="clearfix">
      <i class="el-icon-user-solid"></i>
      <div>
        <span>{{$t('team.team-and-member')}}</span>
        <span>{{$t('team.team-and-member-describe')}}</span>
      </div>
    </div>
    <router-link to="team-member"><el-link>{{$t('member.manage')}}</el-link></router-link>
  </el-card>
</template>

<script>
export default {
  name: "TeamMemberCard"
}
</script>

<style scoped>

</style>
