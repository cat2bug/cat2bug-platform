import { parsePlanRatePercent } from '@/utils/planStatRate'

/**
 * 比率统计卡：把 plan 字段解析为 --plan-stat-progress，供共享进度条使用。
 * @param {string} field plan 上的比率字段名
 */
export function planStatRateMixin(field) {
  return {
    computed: {
      planStatRateStyle() {
        const raw = this.plan && this.plan[field]
        return { '--plan-stat-progress': String(parsePlanRatePercent(raw)) }
      }
    }
  }
}
