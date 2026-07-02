const THEME_KEY = 'theme'
const DARK = 'dark'
const LIGHT = 'light'

export function getStoredTheme() {
  try {
    return localStorage.getItem(THEME_KEY) === DARK ? DARK : LIGHT
  } catch (error) {
    return LIGHT
  }
}

export function applyTheme(theme, persist = false) {
  const nextTheme = theme === DARK ? DARK : LIGHT
  document.documentElement.setAttribute('data-bs-theme', nextTheme)
  document.documentElement.style.colorScheme = nextTheme
  document.body?.classList.toggle('theme-dark', nextTheme === DARK)

  if (persist) {
    try {
      localStorage.setItem(THEME_KEY, nextTheme)
    } catch (error) {
      console.warn('Failed to persist theme:', error)
    }
  }

  return nextTheme
}

export function initializeTheme() {
  return applyTheme(getStoredTheme())
}

export function toggleTheme(currentTheme) {
  return currentTheme === DARK ? LIGHT : DARK
}
