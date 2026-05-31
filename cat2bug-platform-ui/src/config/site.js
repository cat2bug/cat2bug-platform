/** 版权起始年份（全站统一维护） */
export const COPYRIGHT_START_YEAR = 2024

/** 版权年份区间，如 2024–2026；起止相同时仅返回单年 */
export function getCopyrightYearRange() {
  const endYear = new Date().getFullYear()
  if (endYear <= COPYRIGHT_START_YEAR) {
    return String(COPYRIGHT_START_YEAR)
  }
  return `${COPYRIGHT_START_YEAR}–${endYear}`
}
