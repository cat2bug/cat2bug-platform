<template>
  <div class="team-nameplate">
    <el-image
      style="width: 50px; height: 50px;flex-basis:auto;"
      :src="iconUrl(team)"
      fit="cover"
    ></el-image>
    <div class="team-nameplate-content">
      <p>{{ team.teamName }}</p>
      <span v-if="team.introduce">{{ team.introduce }}</span>
      <span v-else style="color: #DCDFE6;">{{ $t('team.introduction-not-filled') }}</span>
    </div>
  </div>
</template>

<script>
export default {
  name: "TeamNameplate",
  props: {
    team: {
      type: Object,
      default: {}
    }
  },
  computed: {
    iconUrl: function (){
      return function (team){
        return process.env.VUE_APP_BASE_API + team.teamIcon
      }
    },
  }
}
</script>

<style lang="scss" scoped>
  .team-nameplate {
    display: flex;
    align-items: center;
    flex-wrap: nowrap;
    padding: 5px;
    overflow: hidden;
    .el-image {
      flex-shrink: 0;
    }
    > * {
      margin-right: 10px;
    }
    .team-nameplate-content {
      display: flex;
      align-items: flex-start;
      flex-direction: column;
      width: 100%;
      overflow: hidden;
      p {
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
        margin: 0px;
        font-size: 16px;
        width: 100%;
      }
      span {
        font-size: 12px;
        color: #C0C4CC;
        line-height: 12px;
        height: 12px;
        margin-top: 3px;
        width: 100%;
      }
    }
  }
</style>
