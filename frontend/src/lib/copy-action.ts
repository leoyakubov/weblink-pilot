import { onBeforeUnmount, ref } from 'vue';
import { copyText } from '@/lib/clipboard';

export function useCopyAction(resetDelayMs = 1400) {
  const copiedKey = ref('');
  let resetTimer: ReturnType<typeof setTimeout> | null = null;

  async function copy(value: string, key: string) {
    await copyText(value);
    copiedKey.value = key;

    if (resetTimer) {
      clearTimeout(resetTimer);
    }

    resetTimer = setTimeout(() => {
      copiedKey.value = '';
      resetTimer = null;
    }, resetDelayMs);
  }

  function isCopied(key: string) {
    return copiedKey.value === key;
  }

  onBeforeUnmount(() => {
    if (resetTimer) {
      clearTimeout(resetTimer);
    }
  });

  return {
    copiedKey,
    copy,
    isCopied,
  };
}
