<template>
  <div class="project-member-icons">
    <el-tooltip class="item" effect="dark" v-for="(member,index) in members" :key="member.userId" :disabled="!tooltip" :content="member.nickName" placement="top">
      <cat2-bug-avatar :member="member" :size="size" />
    </el-tooltip>
  </div>
</template>

<script>
import Cat2BugAvatar from "@/components/Cat2BugAvatar";

export default {
  name: "RowListMember",
  model: {
    prop:'members',
    event:'change'
  },
  components: {Cat2BugAvatar},
  props: {
    members: {
      type: Array,
      default: ()=>[]
    },
    tooltip: {
      type: Boolean,
      default: true
    },
    size: {
      type: [String, Number],
      default: 'medium'
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
    overflow: hidden;
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
    /* 叠放顺序仅在组内生效，勿用过大 z-index 以免盖住右侧固定操作列 */
    @for $i from 1 through 12 {
      > *:nth-child(#{$i}) {
        z-index: #{$i};
      }
    }
    > *:last-child {
      margin-right: 0;
    }
  }
</style>
