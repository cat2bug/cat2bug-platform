<template>
  <el-tooltip placement="bottom" popper-class="tooltip-width">
    <!--            缺陷平均时长的说明-->
    <div slot="content" class="column">
      <span class="key-point">{{$t('definition')}}</span>
      <div>
        <span v-for="d in definition">{{d}}<br/></span>
      </div>
      <span class="key-point">{{$t('equation')}}</span>
      <div class="equation">
        <span>
          {{ $t('plan.defect-repair-avg-hour') }}＝
        </span>
        <span class="semicolon">
          <span>∑ {{ $t('plan.defect-repair-avg-hour.use-hour') }}</span>
          <span>∑ {{ $t('plan.defect-repair-rate.defect-total') }}</span>
        </span>
      </div>
    </div>
    <!--            缺陷平均时长的数据-->
    <div class="plan-statistical-block">
      <el-statistic
        group-separator=","
        :title="$t('plan.defect-repair-avg-hour')"
      >
        <template slot="formatter">
          {{`${plan.defectRepairAvgHour}`}}h
        </template>
      </el-statistic>
    </div>
  </el-tooltip>
</template>

<script>
export default {
  name: "DefectRepairAvgHour",
  props: {
    plan: {
      type: Object,
      default: ()=>{}
    }
  },
  computed: {
    definition: function () {
      return this.$i18n.t('plan.defect-repair-avg-hour.definition').split('\n');
    }
  }
}
</script>
<style lang="scss">
.tooltip-width {
  max-width: 60%;
}
</style>
<style lang="scss" scoped>
.column {
  display: inline-flex;
  flex-direction: column;
}
// 重点
   .key-point {
     font-weight: 500;
     font-size: 1.05rem;
     margin: 10px 0;
   }
// 方程式
   .equation {
     display: inline-flex;
     flex-direction: row;
     justify-content: flex-start;
     align-items: center;
     margin-bottom: 10px;
   }
// 分号
   .semicolon {
      display: inline-flex;
      flex-direction: column;
      >* {
        padding-left: 10px;
        padding-right: 10px;
      }
      >*:first-child {
        border-bottom: 1px solid white;
        padding-bottom: 5px;
      }
      >*:last-child {
        padding-top: 5px;
      }
}
</style>
