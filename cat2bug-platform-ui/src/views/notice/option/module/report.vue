<template>
  <div class="option-body">
    <h3><svg-icon icon-class="chart"/>报告通知</h3>
    <el-form ref="form" :model="form" label-width="120px">
      <el-form-item label="通知事件">
        <el-checkbox label="有新报告时通知我" v-model="form.event.newReport" @change="handleChange"></el-checkbox>
      </el-form-item>
      <el-form-item label="触发方式">
        <div class="col">
          <el-checkbox label="实时触发" v-model="form.option.realtime.switch" @change="handleChange"></el-checkbox>
<!--          <div class="option-item">-->
<!--            <el-checkbox label="间隔时间" v-model="form.option.interval.switch" class="margin-right-30" @change="handleChange"></el-checkbox>-->
<!--            <span>每隔</span>-->
<!--            <el-input-number v-model="form.option.interval.time" size="mini" :min="1" :max="365" style="width: 120px" @input="handleChange"></el-input-number>-->
<!--            <el-select v-model="form.option.interval.unit" size="mini" placeholder="请选择" style="width: 80px"  @change="handleChange">-->
<!--              <el-option-->
<!--                v-for="item in timeUnitOptions"-->
<!--                :key="item.value"-->
<!--                :label="item.label"-->
<!--                :value="item.value">-->
<!--              </el-option>-->
<!--            </el-select>-->
<!--          </div>-->
<!--          <div>-->
<!--            <el-checkbox label="固定时间" v-model="form.option.regular.switch" class="margin-right-30" @change="handleChange"></el-checkbox>-->
<!--            <div class="col">-->
<!--              <div class="row" v-for="(r,index) in form.option.regular.list" :key="index">-->
<!--                <el-time-select-->
<!--                  v-model="form.option.regular.time"-->
<!--                  size="mini"-->
<!--                  style="width: 170px"-->
<!--                  :picker-options="timeSpanOptions"-->
<!--                  @change="handleChange"-->
<!--                  placeholder="选择时间">-->
<!--                </el-time-select>-->
<!--                <el-button v-if="form.option.regular.list.length>1" type="text" size="mini" icon="el-icon-delete" class="red" @click="handleDeleteRegular(index,r)"></el-button>-->
<!--                <el-button v-if="index==form.option.regular.list.length-1" type="text" size="mini" icon="el-icon-plus" @click="handleAddRegular"></el-button>-->
<!--              </div>-->
<!--            </div>-->
<!--          </div>-->
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
export default {
  name: "ReportNoticeOption",
  model: {
    prop: 'report',
    event: 'change'
  },
  data() {
    return {
      form: this.report,
      timeUnitOptions: [{
        value: 'day',
        label: '天'
      },{
        value: 'hour',
        label: '小时'
      },{
        value: 'minute',
        label: '分'
      }],
      timeSpanOptions: { start: '00:00', step: '00:30', end: '24:59' },
    }
  },
  props: {
    report: {
      type: Object,
      default: ()=>{
        return {
          event: {},
          option: {
            realtime: {},
            regular: {
              list: [{
                time: '00:00'
              }]
            },
            interval: {
              time: 1,
              unit: 'hour'
            }
          }
        }
      }
    }
  },
  methods: {
    /** 操作改变 */
    handleChange() {
      this.$emit('change', this.form);
    },
    /** 删除固定时间 */
    handleDeleteRegular(index, row) {
      this.form.option.regular.list.splice(index,1);
    },
    /** 新增固定时间 */
    handleAddRegular() {
      this.form.option.regular.list.push({
        time: '00:00'
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.option-body {
  padding: 0px 10px;
  .svg-icon {
    margin-right: 5px;
  }
  .option-item {
    span, .el-input-number {
      margin-right: 10px;
    }
  }
}
.row {
  display: inline-flex;
  flex-direction: row;
  gap: 10px;
}
.col {
  display: inline-flex;
  flex-direction: column;
}
.margin-right-30 {
  margin-right: 30px;
}
</style>
