export type PrimaryNavItem = {
  label: string;
  icon: string;
  to: string;
};

export function getPrimaryNavigation(): PrimaryNavItem[] {
  return [
    { label: 'Home', icon: 'pi pi-home', to: '/' },
    { label: 'Links', icon: 'pi pi-link', to: '/links' },
    { label: 'Analytics', icon: 'pi pi-chart-bar', to: '/analytics' },
    { label: 'About', icon: 'pi pi-info-circle', to: '/about' },
  ];
}

export function getSectionTitle(routeName: string | symbol | undefined) {
  if (routeName === 'link') {
    return 'Link details';
  }

  if (routeName === 'analytics') {
    return 'Analytics';
  }

  if (routeName === 'analytics-detail') {
    return 'Link analytics';
  }

  if (routeName === 'links') {
    return 'Links';
  }

  if (routeName === 'monitoring') {
    return 'Admin monitoring';
  }

  if (routeName === 'account') {
    return 'Account settings';
  }

  if (routeName === 'about') {
    return 'About this product';
  }

  return 'Home';
}
