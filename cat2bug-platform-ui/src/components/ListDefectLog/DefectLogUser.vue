<template>
  <span v-if="memberList.length" class="defect-log-user-group">
    <el-tooltip
      v-for="(m, i) in memberList"
      :key="memberKey(m, i)"
      :content="displayName(m)"
      placement="top"
      effect="dark"
    >
      <span class="defect-log-user" :class="{ 'defect-log-user--muted': muted }">
        <cat2-bug-avatar :member="m" :size="size" />
      </span>
    </el-tooltip>
  </span>
</template>

<script>
import Cat2BugAvatar from '@/components/Cat2BugAvatar'
import { resolveMemberDisplayName } from '@/utils/member-avatar'

export default {
  name: 'DefectLogUser',
  components: { Cat2BugAvatar },
  props: {
    member: {
      type: Object,
      default: null
    },
    members: {
      type: Array,
      default: null
    },
    name: {
      type: String,
      default: ''
    },
    avatar: {
      type: String,
      default: ''
    },
    muted: {
      type: Boolean,
      default: false
    },
    /** 日志标题行用 medium，详情行用 small */
    size: {
      type: [String, Number],
      default: 'small'
    }
  },
  computed: {
    memberList() {
      if (this.members && this.members.length) {
        return this.members
      }
      if (this.member) {
        return [this.member]
      }
      if (this.name) {
        return [{ nickName: this.name, avatar: this.avatar, userName: this.name }]
      }
      return []
    }
  },
  methods: {
    displayName(member) {
      return resolveMemberDisplayName(member) || ''
    },
    memberKey(member, index) {
      if (member && member.userId != null) {
        return String(member.userId)
      }
      return (this.displayName(member) || 'u') + '-' + index
    }
  }
}
</script>

<style lang="scss" scoped>
.defect-log-user-group {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  vertical-align: middle;
}
.defect-log-user {
  display: inline-flex;
  align-items: center;
  vertical-align: middle;
  line-height: 0;

  ::v-deep .el-avatar {
    border-width: 1px;
  }
}
.defect-log-user--muted {
  opacity: 0.75;
}
</style>
