/**
 * 解析计划统计比率字段（如 "12.5%" / 12.5 / "0"）为 0–100 数值，供进度条 CSS 使用。
 */
export function parsePlanRatePercent(val) {
  if (val == null || val === '') return 0
  if (typeof val === 'number' && Number.isFinite(val)) {
    return Math.min(100, Math.max(0, val))
  }
  const m = String(val).match(/-?[\d.]+/)
  if (!m) return 0
  const n = parseFloat(m[0])
  if (!Number.isFinite(n)) return 0
  return Math.min(100, Math.max(0, n))
}
