<template>
  <el-avatar
    :isStatistics="member.isStatistics?'true':'false'"
    :src="imgUrl"
    fit="cover" :size="size">
    {{member.avatar?'': member.nickName || member.userName || member.name}}
  </el-avatar>
</template>

<script>
export default {
  name: "Cat2BugAvatar",
  model: {
    prop: 'member',
    event: 'change'
  },
  props: {
    member: {
      type: Object,
      default: () => {}
    },
    size: {
      type: String,
      default: 'medium'
    }
  },
  computed: {
    imgUrl: function () {
      return this.member.avatar?process.env.VUE_APP_BASE_API + this.member.avatar:''
    }
  }
}
</script>

<style lang="scss" scoped>
.el-avatar {
  border: #FFF 3px solid;
}
.el-avatar--medium {
  line-height: 29px;
}
.el-avatar[isStatistics='true'] {
  background-color: #E4E7ED;
  color: #909399;
}
</style>
