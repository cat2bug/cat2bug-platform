<template>
  <div class="member-nameplate">
    <slot name="icon" v-if="iconVisible">
      <cat2-bug-avatar :member="member" :size="size" />
    </slot>
    <div class="member-nameplate-content">
      <slot name="title">
        <p>{{ member.nickName }}</p>
      </slot>
      <slot name="body" v-if="bodyVisible">
        <span>{{ $t('phone-number') }}: {{ member.phoneNumber }}</span>
        <span>{{ $t('email') }}: {{ member.email }}</span>
      </slot>
    </div>
  </div>
</template>

<script>
import Cat2BugAvatar from '@/components/Cat2BugAvatar'

export default {
  name: 'MemberNameplate',
  components: { Cat2BugAvatar },
  props: {
    member: {
      type: Object,
      default: () => ({})
    },
    /** 与 Cat2BugAvatar / RowListMember 一致，列表默认 medium（EP default≈40px） */
    size: {
      type: [String, Number],
      default: 'medium'
    },
    iconVisible: {
      type: Boolean,
      default: true
    },
    bodyVisible: {
      type: Boolean,
      default: true
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
  > * {
    margin-right: 10px;
  }
  .member-nameplate-content {
    display: flex;
    align-items: flex-start;
    flex-direction: column;
    p {
      margin: 0;
    }
    span {
      font-size: 10px;
      color: #c0c4cc;
      line-height: 12px;
      height: 12px;
    }
  }
}
</style>
