<template>
  <div>
    <div class="defect-tools">
      <slot name="left-tools"></slot>
      <div class="table-tools row">
        <div>
          <slot name="right-tools"></slot>
        </div>
      </div>
    </div>
    <el-calendar v-model="value">
      <template
        slot="dateCell"
        slot-scope="{date, data}">
        <div class="col calendar-item">
          <p :class="data.isSelected ? 'is-selected' : ''">
            {{ data.day.split('-').slice(1).join('-') }} {{ data.isSelected ? '✔️' : ''}}
          </p>
          <defect-flag v-for="d in defectList" :defect="d" @click.native="handleDefectClick($event,d)" />
        </div>
      </template>
    </el-calendar>
  </div>
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
  methods: {
    /** 初始化 */
    init() {

    },
    /** 查找缺陷列表 */
    search(query) {
      listDefect(query).then(response => {
        this.loading = false;
        this.defectList = response.rows;
        response.rows.forEach(d=>{

        });
      });
    },
    /** 处理点击了表格中的某一行 */
    handleDefectClick(event, defect) {
      this.$emit('defect-click',defect);
      event.stopPropagation();
    }
  },
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
.defect-tools {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  > * {
    display: inline-block;
    justify-content: flex-start;
    margin-bottom: 0px;
    ::v-deep .el-form-item {
      margin-bottom: 0px;
    }
  }
}
.row {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
  > * {
    margin: 0px;
  }
}
.col {
  display: flex;
  flex-direction: column;
}
.calendar-item {
  gap: 5px;
  p {
    margin: 0px;
  }
}
.defect-field-divider {
  margin: 8px 0px;
}
</style>
