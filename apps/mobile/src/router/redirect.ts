export function resolveRedirect(
  value: null | readonly (null | string)[] | string | undefined,
) {
  const source =
    typeof value === 'string' || value === null || value === undefined
      ? value
      : value.find((item): item is string => typeof item === 'string');
  if (!source) {
    return '/';
  }

  let path: string;
  try {
    path = decodeURIComponent(source);
  } catch {
    return '/';
  }

  if (!path.startsWith('/') || path.startsWith('//') || path.includes('\\')) {
    return '/';
  }

  const pathname = path.split(/[?#]/u, 1)[0];
  return pathname === '/login' || pathname?.startsWith('/login/') ? '/' : path;
}
