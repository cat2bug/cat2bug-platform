<template>
  <div class="defect-flag" :state="defect.defectStateName">
    <cat2-bug-avatar size="small" :member="member" />
    <span>#{{defect.projectNum}}</span>
    <span class="defect-name">{{defect.defectName}}</span>
  </div>
</template>

<script>
import Cat2BugAvatar from "@/components/Cat2BugAvatar";
export default {
  name: "DefectFlag",
  components: {Cat2BugAvatar},
  props: {
    defect: {
      type: Object,
      default: ()=>{}
    }
  },
  computed: {
    member: function () {
      return this.defect && this.defect.handleByList && this.defect.handleByList.length>0?this.defect.handleByList[0]:{};
    },
  }
}
</script>

<style lang="scss" scoped>
.defect-flag {
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  border-radius: 50px;
  background-color: white;
  padding: 2px 10px 2px 2px;
  gap: 5px;
  font-size: 12px;
  .defect-name {
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 1;
    line-clamp: 1;
    -webkit-box-orient: vertical;
  }
}
.defect-flag[state="PROCESSING"] {
  background-color: #ff4949;
  border-color: #ff4949;
  color: #FFFFFF;
}
.defect-flag[state="AUDIT"] {
  background-color: #1890ff;
  border-color: #1890ff;
  color: #FFFFFF;
}
.defect-flag[state="RESOLVED"] {
  background-color: #67c23a;
  border-color: #67c23a;
  color: #FFFFFF;
}
.defect-flag[state="REJECTED"] {
  background-color: #ffba00;
  border-color: #ffba00;
  color: #FFFFFF;
}
.defect-flag[state="CLOSED"] {
  background-color: white;
  border: 1px solid #d3d4d6;
  color: #C0C4CC;
}
</style>
