<template>
  <div :class="message.direction">
    <div class="chat-message-member">
      <cat2-bug-avatar :member="message.member"></cat2-bug-avatar>
      <span>{{message.member.name}}</span>
    </div>
    <slot name="content"></slot>
  </div>
</template>

<script>
import Cat2BugAvatar from "@/components/Cat2BugAvatar";
export default {
  name: "ChatMessage",
  components: { Cat2BugAvatar },
  props: {
    message: {
      type: Object,
      default: ()=>{}
    }
  },
}
</script>

<style lang="scss" scoped>
.chat-message-member {
  margin-top: 0px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  flex-shrink: 0;
  > span {
    font-size: 0.8rem;
    color: #606266;
  }
}
.left {
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: flex-start;
  gap: 50px;
}
.right {
  display: flex;
  flex-direction: row-reverse;
  justify-content: flex-start;
  align-content: flex-end;
  gap: 50px;
}
</style>
