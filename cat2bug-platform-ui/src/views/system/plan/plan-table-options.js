/** 测试计划列表动态列（key 与旧版 tableAllFieldList / i18n 一致） */
export const PlanTableColumnDefaults = [
  { key: 'id', prop: 'planNumber', fixed: true, visible: true, align: 'center', width: 100 },
  { key: 'plan.name', prop: 'planName', fixed: true, visible: true, align: 'left', minWidth: 150 },
  { key: 'plan.version', prop: 'planVersion', fixed: false, visible: true, align: 'center', width: 120 },
  { key: 'plan.time', prop: 'planStartTime', fixed: false, visible: true, align: 'center', width: 260 },
  { key: 'updateBy', prop: 'updateById', fixed: false, visible: true, align: 'center', width: 120 },
  { key: 'updateTime', prop: 'updateTime', fixed: false, visible: true, align: 'center', width: 180 },
  { key: 'plan.process', prop: 'itemTotal', fixed: false, visible: true, align: 'center', width: 150, sortable: false },
  { key: 'remark', prop: 'remark', fixed: false, visible: true, align: 'center', minWidth: 120 }
]
