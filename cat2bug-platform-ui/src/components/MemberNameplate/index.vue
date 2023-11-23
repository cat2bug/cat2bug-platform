<template>
  <div class="member-nameplate">
    <el-avatar v-if="member.avatar" src="member.avatar" fit="cover" size="small"></el-avatar>
    <el-avatar v-else size="small">{{member.userName}}</el-avatar>
    <div class="member-nameplate-content">
      <p>{{member.nickName}}</p>
      <span>{{$t('phone-number')}}: {{member.phoneNumber}}</span>
      <span>{{$t('email')}}: {{member.email}}</span>
    </div>
  </div>
</template>

<script>
export default {
  name: "MemberNameplate",
  props: {
    member: {
      type: Object,
      default: {}
    }
  }
}
</script>

<style lang="scss" scoped>
  .member-nameplate {
    display: flex;
    align-items: center;
    flex-direction: row;
    flex-wrap: nowrap;
    justify-content: start;
    padding: 5px;
    .el-avatar {
      font-size: 12px;
      flex-shrink: 0;
    }
    > * {
      margin-right: 10px;
    }
    .member-nameplate-content {
      display: flex;
      align-items: start;
      flex-direction: column;
      p {
        margin: 0px;
      }
      span {
        font-size: 10px;
        color: #C0C4CC;
        line-height: 12px;
        height: 12px;
      }
    }
  }
</style>
