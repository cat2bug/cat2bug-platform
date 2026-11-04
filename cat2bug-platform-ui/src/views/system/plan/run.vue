<template>
  <handle-plan-dialog />
</template>

<script>
import HandlePlanDialog from '@/components/Plan/HandlePlanDialog'

/** 测试计划执行整页：复用原执行工作台内容，路由 plan/:planId/run */
export default {
  name: 'PlanRun',
  components: { HandlePlanDialog }
}
</script>
