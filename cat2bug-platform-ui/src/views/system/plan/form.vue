<template>
  <add-plan-dialog ref="planForm" />
</template>

<script>
import AddPlanDialog from '@/components/Plan/AddPlanDialog'

/** 测试计划新建/编辑整页：复用原 AddPlanDialog 内容，路由 plan/new 与 plan/:planId/edit */
export default {
  name: 'PlanForm',
  components: { AddPlanDialog },
  beforeRouteLeave(to, from, next) {
    const form = this.$refs.planForm
    if (form && typeof form.guardLeavePlanForm === 'function') {
      form.guardLeavePlanForm(next)
      return
    }
    next()
  }
}
</script>
