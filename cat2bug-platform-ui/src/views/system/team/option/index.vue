<template>
  <div class="app-container">
    <el-row :gutter="20">
      <el-col :span="6">
        <team-base-info-card />
      </el-col>
      <el-col :span="6">
        <team-member-card />
      </el-col>
    </el-row>
  </div>
</template>

<script>
import TeamBaseInfoCard from './item/base'
import TeamMemberCard from './item/member'

export default {
  name: "Team",
  components: { TeamBaseInfoCard, TeamMemberCard },
  data() {
    return {

    };
  },
};
</script>
<style lang="scss" scoped>
  ::v-deep .el-card__header {
    font-weight: 600;
    div:first-child {
      display: flex;
      flex-direction: row;
      width: calc(100% - 30px);
      div {
        display: flex;
        flex-direction: column;
        width: 100%;
        span {
          margin-bottom: 5px;
        }
        span:first-child, span:last-child {
          text-overflow: ellipsis;
          overflow: hidden;
          white-space: nowrap;
        }
        span:last-child {
          line-height: 12px;
          font-size: 12px;
          color: #C0C4CC;
        }
      }
    }
    i {
      color: #409EFF;
      margin-right: 10px;
      margin-top: 3px;
    }
  }
  ::v-deep .el-card__body {
    min-height: 150px;
    display: flex;
    align-items: flex-start;
    flex-direction: column;
    > * {
      margin-left: 20px;
      margin-bottom: 15px;
    }
  }
</style>
