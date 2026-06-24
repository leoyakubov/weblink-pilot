<script setup lang="ts">
import Button from 'primevue/button';
import { RouterLink } from 'vue-router';
import { useCopyAction } from '@/shared/composables/useCopyAction';
import type { LinkResponse } from '@/shared/types/api';

const props = defineProps<{
  links: LinkResponse[];
  copyKeyPrefix: string;
}>();

const emit = defineEmits<{
  openQr: [link: LinkResponse];
}>();

const { copy, isCopied } = useCopyAction();

function formatDate(value: string | null) {
  if (!value) {
    return 'Never';
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

function formatCreatedHeading(value: string) {
  const parts = new Intl.DateTimeFormat('en-US', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  }).formatToParts(new Date(value));

  const part = (type: string) => parts.find((item) => item.type === type)?.value ?? '';

  return `${part('hour')}:${part('minute')}, ${part('month')} ${part('day')}, ${part('year')}`;
}

async function shareLink(item: LinkResponse) {
  try {
    const shareApi = window.navigator as {
      share?: (data: { title: string; text: string; url: string }) => Promise<void>;
    };

    if (shareApi.share) {
      await shareApi.share({
        title: `WebLinkPilot link: ${item.code}`,
        text: `Open this short link: ${item.shortUrl}`,
        url: item.shortUrl,
      });
      return;
    }

    await copy(item.shortUrl, `share-${props.copyKeyPrefix}-${item.code}`);
  } catch {
    // Native share can be cancelled by the user; keep the page state unchanged.
  }
}
</script>

<template>
  <div class="list recent-links-list">
    <div v-for="item in props.links" :key="item.code" class="list-item">
      <div class="recent-link-row">
        <div class="recent-link-heading">
          {{ formatCreatedHeading(item.createdAt) }} by
          {{ item.ownerUsername ?? 'Anonymous' }}
        </div>
        <div class="recent-link-main">
          <div class="recent-link-pair">
            <p class="recent-link-label">Alias</p>
            <strong class="recent-link-code">{{ item.code }}</strong>
          </div>
          <div class="recent-link-pair">
            <p class="recent-link-label">Short URL</p>
            <p class="recent-link-url">{{ item.shortUrl }}</p>
          </div>
          <div class="recent-link-pair">
            <p class="recent-link-label">Full URL</p>
            <p class="recent-link-original">{{ item.originalUrl }}</p>
          </div>
        </div>
        <div class="recent-link-meta">
          <span>Expires: {{ formatDate(item.expiresAt) }}</span>
          <span class="recent-link-clicks">{{ item.clickCount }} clicks</span>
        </div>
      </div>
      <div class="actions recent-link-actions">
        <RouterLink :to="{ name: 'link', params: { code: item.code } }">
          <Button label="Details" icon="pi pi-external-link" />
        </RouterLink>
        <RouterLink :to="{ name: 'dashboard', query: { code: item.code } }">
          <Button
            label="Analytics"
            icon="pi pi-chart-line"
            severity="secondary"
            variant="outlined"
          />
        </RouterLink>
        <Button
          type="button"
          :label="
            isCopied(`${props.copyKeyPrefix}-${item.code}`) ? 'Short URL copied' : 'Copy short URL'
          "
          :icon="isCopied(`${props.copyKeyPrefix}-${item.code}`) ? 'pi pi-check' : 'pi pi-copy'"
          severity="secondary"
          variant="outlined"
          @click="copy(item.shortUrl, `${props.copyKeyPrefix}-${item.code}`)"
        />
        <Button
          type="button"
          label="Open QR"
          icon="pi pi-qrcode"
          severity="secondary"
          variant="outlined"
          @click="emit('openQr', item)"
        />
        <Button
          type="button"
          :label="
            isCopied(`share-${props.copyKeyPrefix}-${item.code}`) ? 'Share URL copied' : 'Share'
          "
          :icon="
            isCopied(`share-${props.copyKeyPrefix}-${item.code}`)
              ? 'pi pi-check'
              : 'pi pi-share-alt'
          "
          severity="secondary"
          variant="outlined"
          @click="shareLink(item)"
        />
      </div>
    </div>
  </div>
</template>
