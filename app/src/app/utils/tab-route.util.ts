/**
 * Tabs routing + overlays must agree on the active segment.
 * With `useHash: true`, prefer `location.hash`; some WebViews omit hash — fall back to
 * `pathname` and finally `Router.url`.
 */
export function getTabsRoutePath(routerUrl: string): string {
  const fromRouter = normalizeAngularRouterUrl(routerUrl);

  if (typeof document === 'undefined') {
    return fromRouter;
  }

  const hash = document.location.hash;
  if (hash.startsWith('#/')) {
    const raw = (hash.slice(1).split('?')[0] ?? '').trim();
    if (raw.length > 0) {
      const p = raw.startsWith('/') ? raw : `/${raw}`;
      if (p.includes('/tabs')) return p;
    }
  }

  let pathname = document.location.pathname;
  if (pathname.length > 1 && pathname.endsWith('/')) {
    pathname = pathname.slice(0, -1);
  }
  if (pathname.includes('/tabs')) {
    return pathname.startsWith('/') ? pathname : `/${pathname}`;
  }

  return fromRouter;
}

function normalizeAngularRouterUrl(url: string): string {
  const noQuery = url.split('?')[0] ?? url;
  let path = noQuery.includes('#') ? (noQuery.split('#').pop() ?? '').split('?')[0] ?? '' : noQuery;
  if (!path.startsWith('/')) path = `/${path}`;
  return path;
}
