import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import AboutView from '@/app/pages/AboutView.vue';

describe('AboutView', () => {
  it('renders product details without editable operational settings', () => {
    const wrapper = mount(AboutView);

    expect(wrapper.text()).toContain('WebLinkPilot');
    expect(wrapper.text()).toContain('Vue 3');
    expect(wrapper.text()).toContain('Profiles and environments');
    expect(wrapper.text()).toContain('Default users');
    expect(wrapper.text()).toContain('admin123');
    expect(wrapper.text()).toContain('Demo links');
    expect(wrapper.text()).toContain('spring-boot');
    expect(wrapper.text()).toContain('https://spring.io/projects/spring-boot');
    expect(wrapper.text()).toContain('Account access');
    expect(wrapper.text()).toContain('Profiles and environments');
    expect(wrapper.find('[data-testid="save-settings"]').exists()).toBe(false);
  });
});
