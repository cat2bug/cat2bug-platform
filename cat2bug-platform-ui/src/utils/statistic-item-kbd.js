/**
 * 缺陷页统计模块键盘导航：Enter/空格触发模块主点击区（与鼠标点击等效）。
 * 仅配置在映射表中的模块可激活；无点击交互的模块忽略按键。
 */

/** 统计组件 name → 主点击区域选择器 */
export const STATISTIC_ITEM_KBD_CLICK_SELECTORS = Object.freeze({
  DefectState: '.defect-state-label',
  DefectType: '.defect-type-label',
  DefectModule: '.defect-module-row',
  DefectMemberOnline: '.cat2-bug-avatar.click',
  MyOpenTodoGauge: '.statistic-echarts-body',
  MyLife: '.life-text',
  TeamOpenWorkloadBar: '.team-workload-row',
  MyParticipationHeatmap: '.participation-heatmap-cell:not(.is-pad)',
  PersonalRemindTimer: '.remind-timer-body'
})

export function statisticItemHasKeyboardClick(componentName) {
  return !!(componentName && STATISTIC_ITEM_KBD_CLICK_SELECTORS[componentName])
}

function isVisibleEl(el) {
  if (!el || typeof el.getBoundingClientRect !== 'function') return false
  const rect = el.getBoundingClientRect()
  return rect.width > 0 || rect.height > 0
}

/** 解析模块内第一个可点击主区域 */
export function resolveStatisticItemClickTarget(itemEl, componentName) {
  if (!itemEl || !componentName) return null
  const selector = STATISTIC_ITEM_KBD_CLICK_SELECTORS[componentName]
  if (!selector) return null
  const nodes = itemEl.querySelectorAll(selector)
  for (let i = 0; i < nodes.length; i++) {
    const node = nodes[i]
    if (isVisibleEl(node) && typeof node.click === 'function') {
      return node
    }
  }
  return null
}

/** 触发模块主点击；有配置且找到目标时返回 true */
export function activateStatisticItemClick(itemEl, componentName) {
  const target = resolveStatisticItemClickTarget(itemEl, componentName)
  if (!target) return false
  target.click()
  return true
}
