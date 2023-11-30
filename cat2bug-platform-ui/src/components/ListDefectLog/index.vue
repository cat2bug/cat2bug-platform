<template>
  <el-row>
    <el-col :span="24" v-for="log in logList" :key="log.defectLogId">
      <component :is="log.defectLogType" :log="log"></component>
    </el-col>
  </el-row>
</template>

<script>
import {listLog} from "@/api/system/log";
import CREATE from "@/components/ListDefectLog/create";
import ASSIGN from "@/components/ListDefectLog/assign"
export default {
  name: "ListDefectLog",
  components:{ CREATE,ASSIGN },
  data() {
    return {
      logList: []
    }
  },
  methods: {
    open(defectId) {
      this.getLog(defectId);
    },
    getLog(defectId) {
      listLog({
        defectId: defectId
      }).then(res=>{
        this.logList = res.rows;
      })
    }
  }
}
</script>

<style scoped>

</style>
