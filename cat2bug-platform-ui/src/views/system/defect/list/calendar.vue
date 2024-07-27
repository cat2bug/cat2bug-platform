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
    <div>
      <el-button type="text" @click="handlePrevMonthChange"><i class="el-icon-arrow-left"/> </el-button>
      <el-date-picker
        class="defect-calendar-picker"
        v-model="currentDate"
        type="month"
        :editable="false"
        :clearable="false"
        format="yyyy 年 MM 月"
        @change="handleCurrentDateChange"
        placeholder="选择月">
      </el-date-picker>
      <el-button type="text" @click="handleNextMonthChange"><i class="el-icon-arrow-right"/> </el-button>
    </div>
    <div class="calendar">
      <div class="row">
        <div v-for="week in 7" :key="'week'+week">{{$t('week'+(week-1))}}</div>
      </div>
      <div v-for="(row,rowIndex) in defectList" :key="'row'+rowIndex">
        <div v-for="(day,dayIndex) in row" :key="dayIndex" :week="dayIndex" class="calendar-day">
          <div class="calendar-day-title">
            <div class="calendar-day-title-header">
              <i class="el-icon-date"/>
              <span :now="day.day.getMonth()==currentMonth">{{parseTime(day.day,'{y}-{m}-{d}')}}</span>
            </div>
            <div>
              <!-- 当前的任务总数-->
              <div v-if="day.list.length>0" class="row">
                <div class="row">
                  <div class="block red"></div>
                  <el-link>{{day.processingCount}}</el-link>
                </div>
                <div class="row">
                  <div class="block blue"></div>
                  <el-link>{{day.auditCount}}</el-link>
                </div>
                <div class="row">
                  <div class="block orange"></div>
                  <el-link>{{day.rejectedCount}}</el-link>
                </div>
                <div class="row">
                  <div class="block white"></div>
                  <el-link>{{day.closedCond}}</el-link>
                </div>
                <el-divider direction="vertical"></el-divider>
                总:<el-link>{{day.list.length}}</el-link>
              </div>
            </div>
          </div>
          <el-tooltip effect="dark" :content="d.defectName" placement="right-end" v-for="(d,dayIndex) in pageDefectList(day)" :key="dayIndex">
            <defect-flag class="defect-flag" :defect="d" @click.native="handleDefectClick($event,d)" />
          </el-tooltip>
          <div class="right">
            <el-pagination
              :hide-on-single-page="true"
              :current-page.sync="day.pageNum"
              :page-size.sync="day.pageSize"
              :total="day.list.length"
              layout="prev, next">
            </el-pagination>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import DefectFlag from "@/components/Defect/DefectFlag";
import {listDefect} from "@/api/system/defect";
import {parseTime} from "@/utils/ruoyi";
export default {
  name: "DefectCalendar",
  components: {DefectFlag},
  data() {
    return {
      currentDate: new Date(),
      currentYear: new Date().getFullYear(),
      currentMonth: new Date().getMonth(),
      value: new Date(),
      dayList: [],
      defectList: [],
      defectMap: new Map(),
      defectGroup: new Map(),
    }
  },
  computed: {
    pageDefectList: function () {
      return function (day) {
        return day.list.slice((day.pageNum-1)*day.pageSize,day.pageNum*day.pageSize);
      }
    }
  },
  methods: {
    parseTime,
    /** 初始化 */
    init() {
      this.dayList = this.createCalendarDayList();
    },
    /** 查找缺陷列表 */
    search(query) {
      let q = JSON.parse(JSON.stringify(query));
      q.pageSize=9999999;
      q.pageNum=1;
      q.params.calendarStartDate = parseTime(this.getFirstDateOfMonth(this.currentDate));
      q.params.calendarEndDate = parseTime(this.getLastDateOfMonth(this.currentDate));
      listDefect(q).then(response => {
        this.loading = false;
        let defectList = [];
        let row = [];
        // 初始化缺陷列表，按照一行7天格式添加
        for(let i=0;i<this.dayList.length;i++) {
          let day = this.dayList[i];
          let list = [];
          response.rows.forEach(defect=>{
            // 如果缺陷计划或创建日期在当前范围内，且没有关闭，就显示在当天列表内
            if((defect.planStartTime && this.parseDate(defect.planStartTime).getTime()<=day.getTime()) ||
                (defect.planEndTime && this.parseDate(defect.planEndTime).getTime()>=day.getTime()) ||
                this.parseDate(defect.updateTime).getTime()==day.getTime()
            ) {
              list.push(defect);
            }
          });
          row.push({
            day: day,
            list: list,
            pageNum: 1,
            pageSize: 10,
            processingCount: list.filter(d=>d.defectStateName=='PROCESSING').length,
            auditCount: list.filter(d=>d.defectStateName=='AUDIT').length,
            rejectedCount: list.filter(d=>d.defectStateName=='REJECTED').length,
            closedCond: list.filter(d=>d.defectStateName=='CLOSED').length,
          });
          // 如果是周日，就另起一行记录
          if(i>0 && (i+1)%7==0) {
            defectList.push(row);
            row = [];
          }
        };
        this.defectList = defectList;
      });
    },
    /** 字符转日期 */
    parseDate(str) {
      const date = new Date(str);
      return new Date(date.getFullYear(),date.getMonth(),date.getDate());
    },
    /** 处理点击了表格中的某一行 */
    handleDefectClick(event, defect) {
      this.$emit('defect-click',defect);
      event.stopPropagation();
    },
    /** 创建日历天数 */
    createCalendarDayList() {
      let dayList = [];
      // 当前月第一天
      const currentFirstDay = this.getFirstDateOfMonth(new Date(this.currentYear, this.currentMonth));
      // 当前月的天数长度
      const currentMonthLength = this.getLengthOfMonth(this.currentYear, this.currentMonth);
      // 当前月最后一天
      const currentLastDay = new Date(this.currentYear, this.currentMonth, currentMonthLength);
      // 当前月的第一天是周几
      const startWeek = currentFirstDay.getDay();
      // 当前月的最后一天是周几
      const lastWeek = currentLastDay.getDay();
      // 添加上一个月日期
      // 上一个月的年份
      const prevYear = (this.currentMonth - 1) >= 0 ? this.currentYear : this.currentYear-1;
      // 上一个月
      const prevMonth = (this.currentMonth - 1) >= 0 ? this.currentMonth - 1: 11;
      // 上一个月的天数长度
      let prevMonthLength = this.getLengthOfMonth(prevYear, prevMonth);
      for(let i = startWeek;i>1;i--) {
        dayList.unshift(new Date(prevYear,prevMonth,prevMonthLength-i));
      }
      // 添加当月日期
      for(let i=1;i<=currentMonthLength;i++) {
        dayList.push(new Date(this.currentYear,this.currentMonth,i));
      }
      // 添加下一个月日期
      // 下一个月的年份
      const nextYear = (this.currentMonth + 1) > 11 ? this.currentYear + 1 : this.currentYear;
      // 下一个月
      const nextMonth = (this.currentMonth + 1) % 12;
      for(let i=lastWeek;i<=6;i++) {
        dayList.push(new Date(nextYear, nextMonth,i-lastWeek+1));
      }
      return dayList;
    },
    /** 刷新查询 */
    refreshSearch() {
      this.$emit('refresh');
    },
    /** 处理月份改变事件 */
    handleCurrentDateChange(date) {
      let val = new Date(date);
      this.currentYear = val.getFullYear();
      this.currentMonth = val.getMonth();
      this.dayList = this.createCalendarDayList();
      this.refreshSearch();
    },
    /** 选择上一个月 */
    handlePrevMonthChange() {
      // 上一个月的年份
      this.currentYear = (this.currentMonth - 1) >= 0 ? this.currentYear : this.currentYear-1;
      // 上一个月
      this.currentMonth = (this.currentMonth - 1) >= 0 ? this.currentMonth - 1: 11;
      this.currentDate = new Date(this.currentYear,this.currentMonth,1);
      this.dayList = this.createCalendarDayList();
      this.refreshSearch();
    },
    /** 选择下一个月 */
    handleNextMonthChange() {
      // 下一个月的年份
      this.currentYear = (this.currentMonth + 1) > 11 ? this.currentYear + 1 : this.currentYear;
      // 下一个月
      this.currentMonth = (this.currentMonth + 1) % 12;
      this.currentDate = new Date(this.currentYear,this.currentMonth,1);
      this.dayList = this.createCalendarDayList();
      this.refreshSearch();
    },
    /** 指定月的第一天 */
    getFirstDateOfMonth: function (date) {
      return new Date(date.getFullYear(),date.getMonth(),1);
    },
    /** 指定月的最后一天 */
    getLastDateOfMonth: function (date) {
      return new Date(date.getFullYear(),date.getMonth()+1,0);
    },
    /** 判断是否为闰年 */
    isLeapYear (year) {
      //传入为时间格式需要处理
      if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) return true;
      else return false;
    },
    /** 获取每月天数 */
    getLengthOfMonth (year, month) {
      const monthLengthList = [31, this.isLeapYear(year) ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
      return monthLengthList[month];
    },
  },
}
</script>

<style lang="scss" scoped>
.calendar {
  width: 100%;
  display: inline-flex;
  flex-direction: column;
  border-bottom: 1px solid #DCDFE6;
  > div {
    width: 100%;
    display: inline-flex;
    flex-direction: row;
    > * {
      width: 100%;
    }
  }
  .calendar-day {
    display: inline-flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 5px;
    border-top: 1px solid #DCDFE6;
    border-left: 1px solid #DCDFE6;
    min-height: 100px;
    padding: 10px;
    > * {
      float: left;
    }
    .calendar-day-title-header {
      span {
        padding-left: 5px;
      }
      i, span {
        color: #C0C4CC;
      }
      span[now="true"] {
        color: #303133;
      }
    }
    .calendar-day-title {
      display: inline-flex;
      flex-direction: column;
      justify-content: center;
      align-items: flex-start;
      width: 100%;
      > div:first-child {
        font-weight: 500;
      }
      > div:last-child {
        font-size: 0.8rem;
      }
    }
  }
  .calendar-day[week="6"] {
    border-top: 1px solid #DCDFE6;
    border-left: 1px solid #DCDFE6;
    border-right: 1px solid #DCDFE6;
  }
}
.defect-flag:hover {
  z-index: 999;
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.3); /* 鼠标滑过时显示阴影 */
}
.defect-tools {
  display: inline-flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  > * {
    display: inline-flex;
    justify-content: flex-start;
    margin-bottom: 0px;
    ::v-deep .el-form-item {
      margin-bottom: 0px;
    }
  }
}
.row {
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  gap: 3px;
  > * {
    margin: 0px;
  }
}
.right {
  width: 100%;
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  justify-content: flex-end;
}
.block {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}
.red {
  background-color: #ff4949;
  border-color: #ff4949;
}
.blue {
  background-color: #1890ff;
  border-color: #1890ff;
}
.orange {
  background-color: #ffba00;
  border-color: #ffba00;
}
.white {
  background-color: white;
  border: 1px solid #d3d4d6;
}
.defect-calendar-picker {
  width: 180px;
  margin-left: 20px;
  ::v-deep input {
    border-width: 0px;
    font-size: 1.2rem;
  }
}
</style>
