<template>
  <el-tooltip placement="bottom">
    <div slot="content" class="column">
      <span class="key-point">{{$t('definition')}}</span>
      <div>{{$t('plan.defect-count.definition')}}</div>
    </div>
    <div class="plan-statistical-block">
      <el-statistic
        :title="$t('plan.defect-count')"
      >
        <template slot="formatter">
          {{`${plan.defectCount}`}}{{ $t('a') }}
        </template>
      </el-statistic>
    </div>
  </el-tooltip>
</template>

<script>
export default {
  name: "DefectTotal",
  props: {
    plan: {
      type: Object,
      default: ()=>{}
    }
  },
}
</script>
<style>
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
