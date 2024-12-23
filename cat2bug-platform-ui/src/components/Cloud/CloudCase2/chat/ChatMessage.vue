<template>
  <div>
    <div>
      <cat2-bug-avatar :member="message.member" />
    </div>
    <component
       :is="message.component"
       :ref="message.ref"
       :data="c.data"
       @change="handleChange"
       @submit="handleSubmit" />
  </div>
</template>

<script>
import CaseDemand from "@/components/Cloud/CloudCase2/chat/CaseDemand";
import CaseMind from "@/components/Cloud/CloudCase2/chat/CaseMind";
import CaseList from "@/components/Cloud/CloudCase2/chat/CaseList";
import Cat2BugAvatar from "@/components/Cat2BugAvatar";
export default {
  name: "ChatMessage",
  components: { Cat2BugAvatar, CaseDemand, CaseMind, CaseList },
  props: {
    message: {
      type: Object,
      default: ()=>{}
    }
  },
  methods: {
    handleChange() {
      this.$emit('change')
    },
    handleSubmit() {
      this.$emit('submit')
    }
  }
}
</script>

<style scoped>

</style>
