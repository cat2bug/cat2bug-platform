<template>
  <div class="member-avatar">
    <el-avatar
      :isStatistics="member.isStatistics?'true':'false'"
      :src="imgUrl"
      fit="cover" :size="size">
      {{member.avatar?'': member.nickName || member.userName || member.name}}
    </el-avatar>
    <span v-if="online && member.online" class="online"></span>
  </div>
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
    },
    online: {
      type: Boolean,
      default: false
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
  +.online {
    position: absolute;
    background-color: #67C23A;
    border-radius: 50%;
  }
}
.el-avatar--medium {
  height: 36px;
  width: 36px;
  line-height: 29px;
  +.online {
    bottom: 5px;
    left: 24px;
    line-height: 8px;
    width: 10px;
    height: 10px;
  }
}
.el-avatar--small {
  height: 29px;
  width: 29px;
  line-height: 22px;
  font-size: 12px;
  +.online {
    bottom: 5px;
    left: 22px;
    line-height: 6px;
    width: 6px;
    height: 6px;
  }
}
.el-avatar[isStatistics='true'] {
  background-color: #E4E7ED;
  color: #909399;
}
.member-avatar {
  position: relative;
  display: inline-flex;
  justify-content: center;
  align-items: center;
}
</style>
