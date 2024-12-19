<template>
  <div>
    12324123
    <div v-for="(d,index) in demandList" :key="index">
      <label>{{ d.number }}. {{ d.name }}</label>
      <span>{{ d.describe }}</span>
    </div>
  </div>
</template>

<script>
import {makeCaseDemand} from "@/api/ai/AiCase2";
import Label from "@/components/Cat2BugStatistic/Components/Label";

export default {
  name: "CaseDemand",
  components: {Label},
  data() {
    return {
      loading: false,
      demandList: [],
    }
  },
  mounted() {
    this.getDemandList();
  },
  methods: {
    getDemandList() {
      makeCaseDemand(this.query).then(res=>{
        this.demandList = res.rows;
      })
    }
  }
}
</script>

<style scoped>

</style>
