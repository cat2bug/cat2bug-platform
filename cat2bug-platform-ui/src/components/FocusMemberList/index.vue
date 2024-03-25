<template>
  <row-list-member v-model="memberList" />
</template>

<script>
import RowListMember from "@/components/RowListMember";
export default {
  name: "FocusMemberList",
  components: {RowListMember},
  model: {
    prop: 'members',
    event: 'change'
  },
  data(){
    return {
      topicId: null,
      memberList: [],
    }
  },
  props: {
    moduleName: {
      type: String,
      default: null
    },
    dataId: {
      type: Number,
      default: null
    },
    members: {
      type: Array,
      default: ()=>[]
    }
  },
  watch: {
    members: function (n,o) {
      if (n && n != o) {
        this.memberList = n;
      }
    }
  },
  mounted() {
    this.memberList = this.members;
    // 订阅用户焦点消息
    this.topicId = this.$topic.subscribe(this.$topic.MEMBER_FOCUS_TOPIC, (name, data) => {
      this.memberList = this.memberList.filter(m=>m.userId!=data.data.user.userId);
      if(data.data.moduleName==this.moduleName && this.dataId==data.data.dataId) {
        this.memberList.push(data.data.user);
      }
      this.$emit('change',this.memberList);
    });
  },
  beforeDestroy() {
    // 取消用户活动焦点的订阅
    this.$topic.unsubscribe(this.topicId);
    this.topicId = null;
  },
}
</script>

<style scoped>

</style>
