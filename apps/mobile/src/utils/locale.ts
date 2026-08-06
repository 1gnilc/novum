import { DEFAULT_LOCALE } from '#/locales/locale';

function getLocale(
  supportedLocales: readonly string[],
  fallbackLocale = DEFAULT_LOCALE,
) {
  if (supportedLocales.length === 0 || typeof navigator === 'undefined') {
    return fallbackLocale;
  }

  const browserLocales = [...navigator.languages];
  if (browserLocales.length === 0 && navigator.language) {
    browserLocales.push(navigator.language);
  }
  const supported = supportedLocales.map((locale) => ({
    locale,
    normalized: normalizeLocale(locale),
  }));

  for (const browserLocale of browserLocales) {
    const normalized = normalizeLocale(browserLocale);
    const exactMatch = supported.find(
      (candidate) => candidate.normalized === normalized,
    );
    if (exactMatch) {
      return exactMatch.locale;
    }
  }

  return fallbackLocale;
}

function normalizeLocale(locale: string) {
  return locale.trim().replaceAll('_', '-').toLowerCase();
}

export { getLocale };
