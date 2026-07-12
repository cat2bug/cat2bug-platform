<template>
  <div class="member-avatar">
    <el-avatar
      v-if="isVisible"
      :key="avatarRenderKey"
      :isStatistics="member && member.isStatistics ? 'true' : 'false'"
      :src="displayImgUrl"
      :class="{ 'cat2bug-avatar--text-initial': isTextInitial }"
      :style="textAvatarStyle"
      fit="cover"
      :size="resolvedSize"
      @error="onAvatarImgError">
      {{ textInitial }}
    </el-avatar>
    <el-avatar v-else :size="resolvedSize">?</el-avatar>
    <span v-if="online && member.online" class="online"></span>
    <slot name="extend"></slot>
  </div>
</template>

<script>
import {
  getAvatarInitial,
  getAvatarColorKey,
  getAvatarPaletteStyle,
  resolveMemberDisplayName,
  resolveMemberAvatarUrl
} from '@/utils/member-avatar'

export default {
  emits: ['update:member', 'change'],
  name: "Cat2BugAvatar",
  props: {
    member: {
      type: Object,
      default: () => {}
    },
    size: {
      type: [String, Number],
      default: 'medium'
    },
    online: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      photoLoadFailed: false,
      lastAvatarUrl: ''
    }
  },
  watch: {
    member: {
      handler(member) {
        const url = resolveMemberAvatarUrl(member, process.env.VUE_APP_BASE_API)
        if (url !== this.lastAvatarUrl) {
          this.lastAvatarUrl = url
          this.photoLoadFailed = false
        }
      },
      deep: true,
      immediate: true
    }
  },
  computed: {
    resolvedSize() {
      const s = this.size
      if (s === 'medium' || s === 'default') return 'default'
      if (s === 'mini') return 'small'
      if (s === 'small' || s === 'large' || s === '') return s
      return typeof s === 'number' ? s : 'default'
    },
    isVisible() {
      if (!this.member || typeof this.member !== 'object') return false
      return this.member.avatar || this.member.avatarUrl || this.member.nickName || this.member.userName || this.member.name
    },
    imgUrl() {
      if (!this.member) return ''
      return resolveMemberAvatarUrl(this.member, process.env.VUE_APP_BASE_API)
    },
    displayImgUrl() {
      if (this.photoLoadFailed || !this.imgUrl) return undefined
      return this.imgUrl
    },
    displayName() {
      if (!this.member) return ''
      return resolveMemberDisplayName(this.member)
    },
    isTextInitial() {
      if (!this.member || this.member.isStatistics) return false
      return !this.displayImgUrl
    },
    avatarRenderKey() {
      return this.imgUrl || this.displayName || 'avatar'
    },
    textInitial() {
      if (!this.member || this.member.isStatistics) return ''
      return getAvatarInitial(this.displayName)
    },
    textAvatarStyle() {
      if (!this.isTextInitial) return {}
      return getAvatarPaletteStyle(getAvatarColorKey(this.displayName))
    }
  },
  methods: {
    onAvatarImgError() {
      this.photoLoadFailed = true
    }
  }
}
</script>

<style lang="scss" scoped>
.el-avatar {
  border: #FFF 2px solid;
  flex-shrink: 0;
  +.online {
    position: absolute;
    background-color: #67C23A;
    border-radius: 50%;
  }
}
.el-avatar--medium {
  height: 36px;
  width: 36px;
  line-height: 30px;
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
  line-height: 24px;
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
.el-avatar.cat2bug-avatar--text-initial {
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  line-height: 1;
  text-align: center;
  box-sizing: border-box;
}
.el-avatar--medium.cat2bug-avatar--text-initial {
  line-height: 1;
}
.el-avatar--small.cat2bug-avatar--text-initial {
  line-height: 1;
}
.el-avatar--large.cat2bug-avatar--text-initial {
  line-height: 1;
}
.member-avatar {
  position: relative;
  display: inline-flex;
  justify-content: center;
  align-items: center;
}
</style>
