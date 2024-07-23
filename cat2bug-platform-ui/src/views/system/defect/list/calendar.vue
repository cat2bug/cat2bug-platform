<template>
  <el-calendar v-model="value">
    <template
      slot="dateCell"
      slot-scope="{date, data}">
      <defect-flag v-for="d in defectList" :defect="d" />
    </template>
  </el-calendar>
</template>

<script>
import DefectFlag from "@/components/Defect/DefectFlag";
import {listDefect} from "@/api/system/defect";
export default {
  name: "DefectCalendar",
  components: {DefectFlag},
  data() {
    return {
      value: new Date(),
      defectList: [],
      defectGroup: new Map(),
    }
  },
  computed: {
    // defectList: function () {
    //   return function (date) {
    //     return this.defectGroup.get(date);
    //   }
    // },
  },
  methods: {
    find(query) {
      listDefect(query).then(response => {
        this.loading = false;
        this.defectList = response.rows;
        response.rows.forEach(d=>{

        });
      });
    }
  }
}
</script>

<style lang="scss" scoped>
::v-deep .el-calendar-table .el-calendar-day {
  height: auto;

  display: inline-flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 5px;
  .el-button + .el-button {
    margin-left: 0px;
    width: 15px;
    height: 15px;
  }
}
</style>
