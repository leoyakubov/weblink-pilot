import { config } from '@vue/test-utils';
import PrimeVue from 'primevue/config';
import Aura from '@primeuix/themes/aura';

config.global.plugins = [
  ...(config.global.plugins ?? []),
  [
    PrimeVue,
    {
      theme: {
        preset: Aura,
        options: {
          darkModeSelector: '.app-dark',
        },
      },
    },
  ],
];
