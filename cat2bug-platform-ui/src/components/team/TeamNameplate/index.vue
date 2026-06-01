<template>
  <div class="team-nameplate">
    <cat2-bug-image
      style="width: 50px; height: 50px;flex-basis:auto;"
      :src="iconUrl"
      fit="cover"
    ></cat2-bug-image>
    <div class="team-nameplate-content">
      <p>{{ team.teamName }}</p>
      <span v-if="team.introduce">{{ team.introduce }}</span>
      <span v-else style="color: #DCDFE6;">{{ $t('team.introduction-not-filled') }}</span>
    </div>
  </div>
</template>

<script>
import Cat2BugImage from '@/components/Cat2BugImage'
import { resolveTeamIconUrl } from '@/utils/upload-asset'

export default {
  name: "TeamNameplate",
  components: { Cat2BugImage },
  props: {
    team: {
      type: Object,
      default: {}
    }
  },
  computed: {
    iconUrl() {
      return resolveTeamIconUrl(this.team && this.team.teamIcon)
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
