<template>
  <div class="member-nameplate">
    <slot name="icon" v-if="iconVisible">
      <el-avatar v-if="member.avatar" :src="imgUrl" fit="cover" size="small"></el-avatar>
      <el-avatar v-else size="small">{{member.userName}}</el-avatar>
    </slot>
    <div class="member-nameplate-content">
      <slot name="title">
        <p>{{member.nickName}}</p>
      </slot>
      <slot name="body" v-if="bodyVisible">
        <span>{{$t('phone-number')}}: {{member.phoneNumber}}</span>
        <span>{{$t('email')}}: {{member.email}}</span>
      </slot>
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
    },
    iconVisible: {
      type: Boolean,
      default: true
    },
    bodyVisible: {
      type: Boolean,
      default: true
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
  .member-nameplate {
    display: flex;
    align-items: center;
    flex-direction: row;
    flex-wrap: nowrap;
    justify-content: flex-start;
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
      align-items: flex-start;
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
