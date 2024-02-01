<template>
  <el-row>
    <el-col class="list-defect-log-row" :span="24" v-for="log in logList" :key="log.defectLogId">
      <component :is="log.defectLogType" :log="log"></component>
      <span class="time">{{ parseTime(log.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
    </el-col>
  </el-row>
</template>

<script>
import {listLog} from "@/api/system/log";
import CREATE from "@/components/ListDefectLog/create";
import ASSIGN from "@/components/ListDefectLog/assign"
import REJECTED from "@/components/ListDefectLog/reject";
import REPAIR from "@/components/ListDefectLog/repair";
import PASS from "@/components/ListDefectLog/pass";
import CLOSED from "@/components/ListDefectLog/close";

export default {
  name: "ListDefectLog",
  components:{ CREATE,ASSIGN,REJECTED,REPAIR,PASS,CLOSED },
  data() {
    return {
      logList: []
    }
  },
  props: {
    pageNum: {
      type: Number,
      default: 1
    },
    pageSize: {
      type: Number,
      default: 10
    }
  },
  methods: {
    open(defectId) {
      this.getLog(defectId);
    },
    getLog(defectId) {
      listLog({
        pageNum: this.pageNum,
        pageSize: this.pageSize,
        defectId: defectId
      }).then(res=>{
        this.logList = res.rows;
      })
    },
    addLog(log) {
      this.logList.unshift(log);
      this.$set(this,'logList',this.logList);
    }
  }
}
</script>

<style lang="scss" scoped>
.list-defect-log-row {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 5px;
  .time {
    margin-left: 10px;
    flex-shrink: 0;
    font-size: 12px;
    color: #909399;
  }
}
</style>
