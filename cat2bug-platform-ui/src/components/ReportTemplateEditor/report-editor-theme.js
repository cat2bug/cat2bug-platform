import { readStoredThemeMode } from '@/utils/theme-mode'

/** 与平台主题开关保持一致（html.dark + localStorage 双保险） */
export function isReportEditorDarkMode() {
  if (typeof document === 'undefined') {
    return readStoredThemeMode() === 'dark'
  }
  if (document.documentElement.classList.contains('dark')) {
    return true
  }
  return readStoredThemeMode() === 'dark'
}
