<template>
  <el-card class="box-card">
    <div slot="header" class="clearfix">
      <i class="el-icon-s-flag"></i>
      <div>
        <span>{{$t('browser')}}</span>
        <span>{{$t('browser.describe')}}</span>
      </div>
    </div>
    <router-link to="team-base-info"><el-link>{{$t('team.base-info')}}</el-link></router-link>
  </el-card>
</template>

<script>
export default {
  name: "BrowserToolOption"
}
</script>
