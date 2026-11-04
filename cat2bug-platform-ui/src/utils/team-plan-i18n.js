/** 种子「普通团队」显示名国际化；自定义套餐名原样返回。 */
export function localizeTeamPlanName(vm, planOrName) {
  const name = typeof planOrName === 'string'
    ? planOrName
    : (planOrName && planOrName.planName)
  if (name == null || name === '') {
    return '—'
  }
  if (name === '普通团队') {
    return vm.$t('admin.team-plan.ordinary')
  }
  return name
}

export function localizeTeamPlanRemark(vm, remark) {
  if (remark == null || remark === '') {
    return ''
  }
  if (remark === '系统默认套餐') {
    return vm.$t('admin.team-plan.ordinary-remark')
  }
  return remark
}
