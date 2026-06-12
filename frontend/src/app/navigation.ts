export type PrimaryNavItem = {
  label: string;
  icon: string;
  to: string;
};

export function getPrimaryNavigation(isAdmin = false): PrimaryNavItem[] {
  const items: PrimaryNavItem[] = [
    { label: 'Home', icon: 'pi pi-home', to: '/' },
    { label: 'Dashboard', icon: 'pi pi-chart-bar', to: '/dashboard' },
    { label: 'History', icon: 'pi pi-history', to: '/history' },
    { label: 'About', icon: 'pi pi-info-circle', to: '/about' },
  ];

  if (isAdmin) {
    items.push({ label: 'Monitoring', icon: 'pi pi-shield', to: '/monitoring' });
  }

  return items;
}

export function getSectionTitle(routeName: string | symbol | undefined) {
  if (routeName === 'link') {
    return 'Link details';
  }

  if (routeName === 'dashboard') {
    return 'Analytics shell';
  }

  if (routeName === 'history') {
    return 'Link history';
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
