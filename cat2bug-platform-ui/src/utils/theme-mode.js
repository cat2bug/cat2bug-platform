export const THEME_MODE_STORAGE_KEY = 'theme-mode'

/** 同步应用主题到 documentElement（切换时禁用过渡，避免闪烁） */
export function applyThemeMode(mode) {
  const root = document.documentElement
  root.classList.add('theme-switching')
  if (mode === 'dark') {
    root.classList.add('dark')
    root.style.colorScheme = 'dark'
  } else {
    root.classList.remove('dark')
    root.style.colorScheme = 'light'
  }
  requestAnimationFrame(() => {
    root.classList.remove('theme-switching')
  })
}

export function readStoredThemeMode() {
  try {
    const stored = localStorage.getItem(THEME_MODE_STORAGE_KEY)
    if (stored === 'dark' || stored === 'light') {
      return stored
    }
  } catch (e) {
    /* ignore */
  }
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}
