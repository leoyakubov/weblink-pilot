import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { clearSettings } from '@/shared/services/settings';

export function useBrowserSettingsResetView() {
  const router = useRouter();

  onMounted(async () => {
    clearSettings();
    await router.replace({ name: 'home' });
  });
}
