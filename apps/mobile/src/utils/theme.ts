import type { ConfigProviderTheme } from 'vant';

const SYSTEM_THEME_QUERIES: Record<ConfigProviderTheme, string> = {
  dark: '(prefers-color-scheme: dark)',
  light: '(prefers-color-scheme: light)',
};

function getSystemTheme(): ConfigProviderTheme | null {
  if (
    typeof window === 'undefined' ||
    typeof window.matchMedia !== 'function'
  ) {
    return null;
  }

  if (window.matchMedia(SYSTEM_THEME_QUERIES.dark).matches) {
    return 'dark';
  }

  if (window.matchMedia(SYSTEM_THEME_QUERIES.light).matches) {
    return 'light';
  }

  return null;
}

export { getSystemTheme, SYSTEM_THEME_QUERIES };
