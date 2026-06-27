import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import AboutView from '@/app/pages/AboutView.vue';

describe('AboutView', () => {
  it('renders product details without editable operational settings', () => {
    const wrapper = mount(AboutView);

    expect(wrapper.text()).toContain('WeblinkPilot');
    expect(wrapper.text()).toContain('Vue 3');
    expect(wrapper.text()).toContain('Profiles and environments');
    expect(wrapper.text()).toContain('Default users');
    expect(wrapper.text()).toContain('admin123');
    expect(wrapper.text()).toContain('view all links');
    expect(wrapper.text()).toContain('create owned links');
    expect(wrapper.text()).toContain('Account access');
    expect(wrapper.text()).toContain('Project links');
    expect(wrapper.text()).toContain('Local URLs');
    expect(wrapper.text()).toContain('Backend API');
    expect(wrapper.text()).toContain('http://localhost:8080/api/v1');
    expect(wrapper.text()).toContain('GitHub');
    expect(wrapper.find('a[href="https://github.com/leoyakubov/weblink-pilot"]').exists()).toBe(
      true,
    );
    expect(
      wrapper
        .find('a[href="https://github.com/leoyakubov/weblink-pilot/blob/main/docs/README.md"]')
        .exists(),
    ).toBe(true);
    expect(wrapper.text()).toContain('Profiles and environments');
    expect(wrapper.find('[data-testid="save-settings"]').exists()).toBe(false);
  });
});
