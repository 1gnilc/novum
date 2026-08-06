import type { RouterHistory } from 'vue-router';

import { createWebHashHistory, createWebHistory } from 'vue-router';

export function createAppRouterHistory(
  mode = import.meta.env.VITE_ROUTER_HISTORY,
  base = import.meta.env.BASE_URL,
): RouterHistory {
  return mode === 'hash' ? createWebHashHistory(base) : createWebHistory(base);
}
