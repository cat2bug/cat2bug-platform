<template>
  <el-card class="box-card">
    <template #header>
      <div class="clearfix">
        <legacy-icon icon="el-icon-s-flag"></legacy-icon>
        <div>
          <span>{{$t('browser')}}</span>
          <span>{{$t('browser.describe')}}</span>
        </div>
      </div>
    </template>
    <router-link to="team-base-info"><el-link>{{$t('member.base-info')}}</el-link></router-link>
  </el-card>
</template>

<script>
export default {
  name: "BrowserToolOption"
}
</script>
