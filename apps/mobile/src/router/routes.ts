import type { RouteRecordRaw } from 'vue-router';

export const routes: RouteRecordRaw[] = [
  {
    component: () => import('#/views/home.vue'),
    meta: { title: 'home.title' },
    path: '/',
  },
  {
    component: () => import('#/views/login.vue'),
    meta: { title: 'login.title' },
    path: '/login',
  },
  {
    component: () => import('#/views/account.vue'),
    meta: { requiresAuth: true, title: 'account.title' },
    path: '/account',
  },
  {
    component: () => import('#/views/not-found.vue'),
    meta: { title: 'notFound.title' },
    path: '/:pathMatch(.*)*',
  },
];
