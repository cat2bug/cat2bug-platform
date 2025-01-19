<template>
  <div class="app-container">
    <project-label />
    <el-card class="box-card">
      <defect-state-chart />
    </el-card>
  </div>
</template>

<script>
import ProjectLabel from "@/components/Project/ProjectLabel";
import DefectStateChart from "@/views/system/dashboard/components/DefectStateChart";

export default {
  name: "index",
  components: { ProjectLabel, DefectStateChart },
  data() {
    return {

    }
  }
}
</script>

<style scoped>

</style>
