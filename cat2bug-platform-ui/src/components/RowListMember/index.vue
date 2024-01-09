<template>
  <div class="project-member-icons">
    <el-tooltip class="item" effect="dark" v-for="(member,index) in members" :key="index" :content="member.nickName" placement="top">
      <el-avatar
        :isStatistics="member.isStatistics?'true':'false'"
        :src="member.avatar?member.avatar:''"
        fit="cover" size="medium">
        {{member.avatar?'':member.userName}}
      </el-avatar>
    </el-tooltip>
  </div>
</template>

<script>
export default {
  name: "RowListMember",
  props: {
    members: {
      type: Array,
      default: ()=>[]
    }
  }
}
</script>

<style lang="scss" scoped>
  .project-member-icons {
    display: flex;
    display: -webkit-flex; /* Safari */
    flex-direction: row-reverse;
    flex-wrap: nowrap;
    justify-content: flex-end;
    align-items: center;
    .el-avatar {
      margin-right: -8px;
      border: #FFF 3px solid;
    }
    .el-avatar--medium {
      line-height: 29px;
    }
    .el-avatar[isStatistics='true'] {
      background-color: #E4E7ED;
      color: #909399;
    }
  }
</style>
