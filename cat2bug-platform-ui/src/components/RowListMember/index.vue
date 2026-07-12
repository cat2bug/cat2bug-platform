<template>
  <div class="project-member-icons">
    <el-tooltip
      class="item"
      effect="dark"
      v-for="(member, index) in displayMembers"
      :key="memberKey(member, index)"
      :disabled="!tooltip"
      :content="tooltipContent(member, index)"
      placement="top"
    >
      <span
        class="project-member-icons__avatar"
        :class="{ 'project-member-icons__avatar--overflow': showOverflowBadge && index === displayMembers.length - 1 }"
      >
        <cat2-bug-avatar :member="member" :size="size" />
        <span
          v-if="showOverflowBadge && index === displayMembers.length - 1"
          class="project-member-icons__count"
        >{{ overflowCount }}</span>
      </span>
    </el-tooltip>
  </div>
</template>

<script>
import Cat2BugAvatar from "@/components/Cat2BugAvatar";

export default {
  emits: ['update:members', 'change'],
  name: "RowListMember",
  components: { Cat2BugAvatar },
  props: {
    members: {
      type: Array,
      default: () => []
    },
    tooltip: {
      type: Boolean,
      default: true
    },
    size: {
      type: [String, Number],
      default: 'medium'
    },
    max: {
      type: Number,
      default: 3
    }
  },
  computed: {
    validMembers() {
      return (this.members || []).filter((member) => {
        if (!member || member.isStatistics) return false
        return member.userId != null || member.nickName || member.userName || member.name
      })
    },
    displayMembers() {
      const limit = Number.isFinite(this.max) && this.max > 0 ? this.max : 3
      return this.validMembers.slice(0, limit)
    },
    showOverflowBadge() {
      return this.validMembers.length > this.displayMembers.length
    },
    overflowCount() {
      return this.validMembers.length
    }
  },
  methods: {
    memberKey(member, index) {
      return member.userId != null ? member.userId : `member-${index}`
    },
    tooltipContent(member, index) {
      if (this.showOverflowBadge && index === this.displayMembers.length - 1) {
        return `共${this.overflowCount}人`
      }
      return member.nickName || member.userName || member.name || ''
    }
  }
}
</script>

<style lang="scss" scoped>
  .project-member-icons {
    display: inline-flex;
    flex-direction: row;
    flex-wrap: nowrap;
    justify-content: flex-start;
    align-items: center;
    vertical-align: middle;
    max-width: 100%;
    overflow: visible;
    position: relative;
    z-index: 0;

    > * {
      margin-right: -8px;
      display: flex;
      justify-content: center;
      align-items: center;
      position: relative;
      flex-shrink: 0;
    }

    @for $i from 1 through 12 {
      > *:nth-child(#{$i}) {
        z-index: #{$i};
      }
    }

    > *:last-child {
      margin-right: 0;
    }
  }

  .project-member-icons__avatar {
    position: relative;
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }

  .project-member-icons__avatar--overflow {
    z-index: 12 !important;
  }

  .project-member-icons__count {
    position: absolute;
    right: -2px;
    bottom: -2px;
    width: 20px;
    height: 20px;
    min-width: 20px;
    padding: 0;
    border-radius: 50%;
    font-size: 9px;
    line-height: 1;
    font-weight: 700;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    color: var(--member-overflow-badge-color, #909399);
    background-color: var(--member-overflow-badge-bg, #e4e7ed);
    border: 1.5px solid var(--member-overflow-badge-border, #fff);
    box-sizing: border-box;
    pointer-events: none;
    z-index: 2;
  }
</style>
