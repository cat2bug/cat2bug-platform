<template>
  <el-card class="box-card">
    <div slot="header" class="clearfix">
      <legacy-icon icon="el-icon-user-solid"></legacy-icon>
      <div>
        <span>{{$t('team.team-and-member')}}</span>
        <span>{{$t('team.team-and-member-describe')}}</span>
      </div>
    </div>
    <router-link to="team-member" v-hasPermi="['system:team:member']"><el-link>{{$t('member.manage')}}</el-link></router-link>
  </el-card>
</template>

<script>
export default {
  name: "TeamMemberCard"
}
</script>

<style scoped>

</style>
