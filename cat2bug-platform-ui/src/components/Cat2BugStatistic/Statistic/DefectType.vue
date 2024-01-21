<template>
  <div>
    <cat2-but-title v-model="title" />
    <div>
      <cat2-but-label icon="all" icon-color="#409EFF" label="全部" content="123" />
      <cat2-but-label icon="bug" icon-color="#F56C6C" label="BUG" content="3" />
      <cat2-but-label icon="task" icon-color="#67C23A" label="任务" content="2" />
      <cat2-but-label icon="demand" icon-color="#FBB13F" label="需求" content="12" />
    </div>
  </div>
</template>

<script>
import Cat2ButTitle from "../Components/Title"
import Cat2ButLabel from "../Components/Label"
export default {
  name: "DefectType",
  components: {Cat2ButTitle,Cat2ButLabel},
  data() {
    return {
      title: "类型统计"
    }
  },
  props: {
    params: {
      type: Object,
      default: ()=>[]
    }
  }
}
</script>

<style scoped>

</style>
