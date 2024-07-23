<template>
  <div class="defect-flag" :style="`${flagStyle}`">
    <cat2-bug-avatar size="small" :member="member" />
    <span>#{{defect.projectNum}}</span>
    <span>{{defect.defectName}}</span>
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
    flagStyle: function () {
      switch (this.defect.defectState) {
        case '':
          return '#ff4949';
      }
      return `background-color: #cccccc`;
    }
  }
}
</script>

<style scoped>
.defect-flag {
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  border-radius: 50px;
  background-color: red;
  padding: 2px 10px 2px 2px;
  gap: 5px;
  font-size: 12px;
}
</style>
