import type { RouteRecordRaw } from 'vue-router';

import { BasicLayout } from '#/layouts';

export const routes: RouteRecordRaw[] = [
  {
    children: [
      {
        component: () => import('#/views/home.vue'),
        meta: { tabbar: true, title: 'home.title' },
        name: 'home',
        path: '',
      },
      {
        component: () => import('#/views/market.vue'),
        meta: { tabbar: true, title: 'market.title' },
        name: 'market',
        path: 'market',
      },
      {
        component: () => import('#/views/team.vue'),
        meta: { requiresAuth: true, tabbar: true, title: 'team.title' },
        name: 'team',
        path: 'team',
      },
      {
        component: () => import('#/views/fund.vue'),
        meta: { requiresAuth: true, tabbar: true, title: 'fund.title' },
        name: 'fund',
        path: 'fund',
      },
      {
        component: () => import('#/views/my.vue'),
        meta: { requiresAuth: true, tabbar: true, title: 'my.title' },
        name: 'my',
        path: 'my',
      },
      {
        component: () => import('#/views/login.vue'),
        meta: { title: 'login.title' },
        name: 'login',
        path: 'login',
      },
      {
        component: () => import('#/views/not-found.vue'),
        meta: { title: 'notFound.title' },
        name: 'not-found',
        path: ':pathMatch(.*)*',
      },
    ],
    component: BasicLayout,
    path: '/',
  },
];
