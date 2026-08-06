import { enableAutoUnmount, mount } from '@vue/test-utils';
import { createMemoryHistory, createRouter } from 'vue-router';

import { NavBar as VantNavBar } from 'vant';
import { afterEach, describe, expect, it, vi } from 'vitest';

import Locale from '#/components/locale/index.vue';
import NavBar from '#/components/nav-bar/index.vue';

vi.mock('#/components/locale/index.vue', () => ({
  default: {
    name: 'Locale',
    template: '<span data-testid="locale"></span>',
  },
}));

enableAutoUnmount(afterEach);

describe('mobile nav bar', () => {
  it('forwards Vant props with fixed layout and safe area defaults', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ component: { template: '<main />' }, path: '/' }],
    });
    await router.push('/');
    await router.isReady();

    const wrapper = mount(NavBar, {
      attrs: { 'aria-label': 'Page navigation' },
      global: { plugins: [router] },
      props: {
        border: false,
        leftText: 'Back',
        rightText: 'More',
        zIndex: 100,
      },
    });
    const vantNavBar = wrapper.findComponent(VantNavBar);

    expect(vantNavBar.props()).toMatchObject({
      border: false,
      fixed: true,
      leftText: 'Back',
      placeholder: true,
      rightText: 'More',
      safeAreaInsetTop: true,
      zIndex: 100,
    });
    expect(vantNavBar.attributes('aria-label')).toBe('Page navigation');
    expect(vantNavBar.vm.$slots.left).toBeUndefined();

    const configurable = mount(NavBar, {
      global: { plugins: [router] },
      props: {
        fixed: false,
        placeholder: false,
        safeAreaInsetTop: false,
      },
    }).findComponent(VantNavBar);
    expect(configurable.props()).toMatchObject({
      fixed: false,
      placeholder: false,
      safeAreaInsetTop: false,
    });
  });

  it('forwards the Vant slots and right click event', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ component: { template: '<main />' }, path: '/' }],
    });
    await router.push('/');
    await router.isReady();

    const wrapper = mount(NavBar, {
      global: { plugins: [router] },
      slots: {
        left: '<span class="custom-left">Left</span>',
        right: '<span class="custom-right">Right</span>',
        title: '<strong class="custom-title">Title</strong>',
      },
    });
    const vantNavBar = wrapper.findComponent(VantNavBar);
    const event = new MouseEvent('click');

    expect(wrapper.find('.custom-left').text()).toBe('Left');
    expect(wrapper.find('.custom-title').text()).toBe('Title');
    expect(wrapper.find('.custom-right').text()).toBe('Right');
    vantNavBar.vm.$emit('clickRight', event);
    expect(wrapper.emitted('clickRight')).toEqual([[event]]);
  });

  it('does not navigate back when a custom left slot replaces the arrow', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ component: { template: '<main />' }, path: '/' }],
    });
    await router.push('/');
    await router.isReady();
    const back = vi.spyOn(router, 'back');
    const wrapper = mount(NavBar, {
      global: { plugins: [router] },
      props: { leftArrow: true },
      slots: { left: '<span>Menu</span>' },
    });
    const event = new MouseEvent('click');

    wrapper.findComponent(VantNavBar).vm.$emit('clickLeft', event);

    expect(back).not.toHaveBeenCalled();
    expect(wrapper.emitted('clickLeft')).toEqual([[event]]);
  });

  it('gives the locale flag precedence over the back arrow', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ component: { template: '<main />' }, path: '/' }],
    });
    await router.push('/');
    await router.isReady();
    const back = vi.spyOn(router, 'back');

    const localeBar = mount(NavBar, {
      global: { plugins: [router], stubs: { Locale: true } },
      props: { leftArrow: true, locale: true },
    });
    expect(localeBar.findComponent(Locale).exists()).toBe(true);
    expect(localeBar.findComponent(VantNavBar).props('leftArrow')).toBe(false);
    localeBar
      .findComponent(VantNavBar)
      .vm.$emit('clickLeft', new MouseEvent('click'));
    expect(back).not.toHaveBeenCalled();
    expect(localeBar.emitted('clickLeft')).toBeUndefined();

    const backBar = mount(NavBar, {
      global: { plugins: [router] },
      props: { leftArrow: true, locale: false },
    });
    expect(backBar.findComponent(Locale).exists()).toBe(false);
    expect(backBar.findComponent(VantNavBar).props('leftArrow')).toBe(true);
    const event = new MouseEvent('click');
    backBar.findComponent(VantNavBar).vm.$emit('clickLeft', event);
    expect(back).toHaveBeenCalledOnce();
    expect(backBar.emitted('clickLeft')).toEqual([[event]]);
  });
});
