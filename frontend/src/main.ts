import { createApp } from 'vue';
import PrimeVue from 'primevue/config';
import Nora from '@primeuix/themes/nora';
import 'primeicons/primeicons.css';
import App from './App.vue';
import router from './app/router';
import { initializeUiMode } from './lib/ui-mode';
import './styles.css';

initializeUiMode();

createApp(App)
  .use(router)
  .use(PrimeVue, {
    theme: {
      preset: Nora,
      options: {
        darkModeSelector: '.app-dark',
      },
    },
  })
  .mount('#app');
