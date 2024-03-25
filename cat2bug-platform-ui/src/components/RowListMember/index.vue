<template>
  <div class="project-member-icons">
    <el-tooltip class="item" effect="dark" v-for="(member,index) in members" :key="member.userId" :content="member.nickName" placement="top">
      <cat2-bug-avatar :member="member" />
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
    > * {
      margin-right: -8px;
      display: flex;
      justify-content: center;
      align-items: center;
    }
  }
</style>
