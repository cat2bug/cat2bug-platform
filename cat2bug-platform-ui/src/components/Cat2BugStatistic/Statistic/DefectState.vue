<template>
  <div>
    <cat2-but-title v-model="title" />
    <div>
      <cat2-but-label icon="all" icon-color="#409EFF" label="全部" content="12">
        <template slot="content">
          <span>15</span>
          <span class="width50">今日</span>
          <span class="width50">本周</span>
        </template>
      </cat2-but-label>
      <cat2-but-label icon="pending-processing" icon-color="#F56C6C" label="待处理" content="123">
      <template slot="content">
        <span>15</span>
        <span class="width50" flag="+">+1</span>
        <span class="width50" flag="-">-2</span>
      </template>
      </cat2-but-label>
      <cat2-but-label icon="processed" icon-color="#FBB13F" label="已处理" content="3">
      <template slot="content">
        <span>15</span>
        <span class="width50">0</span>
        <span class="width50">+11</span>
      </template>
      </cat2-but-label>
      <cat2-but-label icon="pass" icon-color="#67C23A" label="已通过" content="2">
      <template slot="content">
        <span>15</span>
        <span class="width50">+21</span>
        <span class="width50">-1</span>
      </template>
      </cat2-but-label>
    </div>
  </div>
</template>

<script>
import Cat2ButTitle from "../Components/Title"
import Cat2ButLabel from "../Components/Label"

export default {
  name: "DefectState",
  components: {Cat2ButTitle,Cat2ButLabel},
  data() {
    return {
      title: "状态统计"
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

<style lang="scss" scoped>
  .width50 {
    width: 50px;
    min-width: 50px;
  }
  .cat2bug-statistic-label {
    span[flag="+"] {
      color: orangered;
    }
    span[flag="-"] {
      color: #67C23A;
    }
    span[flag=""] {
      color: dodgerblue;
    }
  }
</style>
