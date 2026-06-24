<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import Button from 'primevue/button';
import InputText from 'primevue/inputtext';
import FeatureCard from '@/shared/components/common/FeatureCard.vue';
import PageIntro from '@/shared/components/common/PageIntro.vue';
import PanelCard from '@/shared/components/common/PanelCard.vue';
import { useCopyAction } from '@/shared/composables/useCopyAction';
import { authState } from '@/features/auth/services/auth.service';
import { buildApiBaseUrl } from '@/shared/services/http';
import { loadSettings, saveSettings } from '@/shared/services/settings';
import { createLink, listLinks } from '@/features/links/repositories/link.repository';
import type { ApiSettings, CreateLinkRequest, LinkResponse } from '@/shared/types/api';

const settings = reactive<ApiSettings>(loadSettings());
const form = reactive<CreateLinkRequest>({
  originalUrl: 'https://github.com/weblinkpilot/',
  customAlias: 'wlpilot',
  expiresAt: '',
});

const createdLink = ref<LinkResponse | null>(null);
const recentLinks = ref<LinkResponse[]>([]);
const loadingRecent = ref(false);
const recentError = ref('');
const errorMessage = ref('');
const successMessage = ref('');
const submitting = ref(false);
const qrModalUrl = ref('');
const qrModalTitle = ref('');
const openHelp = reactive({
  alias: false,
  expiration: false,
});

const userStatus = computed(() =>
  authState.currentUser
    ? `Signed in as ${authState.currentUser.username} (${authState.currentUser.role})`
    : 'Guest mode (ready for anonymous links)',
);

const canSeePreview = computed(() => authState.currentUser?.role === 'ADMIN');

const linkPreviewUrl = computed(() =>
  createdLink.value ? buildApiBaseUrl(`/urls/${createdLink.value.code}/preview`, settings) : '',
);

const dashboardUrl = computed(() =>
  createdLink.value ? { name: 'dashboard', query: { code: createdLink.value.code } } : '/',
);

const { copy, isCopied } = useCopyAction();

function syncSettings() {
  saveSettings(settings);
}

async function refreshRecent() {
  loadingRecent.value = true;
  recentError.value = '';

  try {
    recentLinks.value = await listLinks(5, settings);
  } catch (error) {
    recentLinks.value = [];
    recentError.value = error instanceof Error ? error.message : 'Could not load recent links';
  } finally {
    loadingRecent.value = false;
  }
}

async function submit() {
  errorMessage.value = '';
  successMessage.value = '';
  submitting.value = true;

  try {
    syncSettings();

    const originalUrl = form.originalUrl.trim();
    new URL(originalUrl);

    const payload: CreateLinkRequest = {
      originalUrl,
      customAlias: form.customAlias?.trim() || undefined,
      expiresAt: form.expiresAt ? new Date(form.expiresAt).toISOString() : null,
    };

    createdLink.value = await createLink(payload, settings);
    successMessage.value = `Created ${createdLink.value.code} successfully`;
    await refreshRecent();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Something went wrong';
  } finally {
    submitting.value = false;
  }
}

function openExternal(url: string) {
  window.open(url, '_blank', 'noopener,noreferrer');
}

function openQrModal(url: string, title: string) {
  qrModalUrl.value = url;
  qrModalTitle.value = title;
}

function closeQrModal() {
  qrModalUrl.value = '';
  qrModalTitle.value = '';
}

function toggleHelp(field: keyof typeof openHelp) {
  openHelp[field] = !openHelp[field];
}

function formatDate(value: string | null) {
  if (!value) {
    return 'Never';
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

onMounted(() => {
  refreshRecent();
});

watch(
  () => authState.currentUser?.username,
  () => {
    refreshRecent();
  },
);
</script>

<template>
  <section class="landing-layout stack">
    <PageIntro
      eyebrow="Link management"
      title="Web link shortener"
      description="Fast, branded short links for demos, teams, and shared workflows."
    />

    <div class="page-grid two-col home-top-grid">
      <PanelCard
        eyebrow="Fast start"
        title="Why teams use it"
        description="Create a demo link in seconds, or sign in to keep ownership and revisit your private history later. Redirects stay fast, QR scans are built in, and analytics are ready when you need them."
      >
        <div class="quick-list">
          <div class="quick-chip-row">
            <span class="quick-chip quick-chip--blue">Guest demo links</span>
            <span class="quick-chip quick-chip--green">Owned user links</span>
            <span class="quick-chip quick-chip--amber">QR scans</span>
            <span class="quick-chip quick-chip--light">Analytics</span>
          </div>

          <div class="quick-card-grid">
            <div class="quick-item quick-item--teal">
              <strong>Simple start</strong>
              <p>Create a link now, no account required.</p>
            </div>
            <div class="quick-item quick-item--slate">
              <strong>Signed-in mode</strong>
              <p>Keep links owned and revisit them later.</p>
            </div>
          </div>
        </div>
      </PanelCard>

      <PanelCard
        eyebrow="Create links"
        title="Shorten link"
        description="Use the defaults for a fast demo, or swap in your own URL, alias, and expiration. Leave expiration blank to keep the link active."
      >
        <form class="form-grid" @submit.prevent="submit">
          <label class="form-field">
            <span class="field-label">Original URL</span>
            <InputText
              v-model="form.originalUrl"
              type="url"
              placeholder="https://github.com/weblinkpilot/"
              required
            />
          </label>

          <div class="form-field">
            <div class="field-label-row">
              <label class="field-label" for="custom-alias">Custom alias</label>
              <Button
                type="button"
                class="field-help-button"
                icon="pi pi-question-circle"
                severity="secondary"
                variant="text"
                size="small"
                aria-label="Toggle custom alias help"
                :aria-expanded="openHelp.alias"
                @click="toggleHelp('alias')"
              />
            </div>
            <InputText
              id="custom-alias"
              v-model="form.customAlias"
              type="text"
              placeholder="wlpilot"
            />
            <p v-if="openHelp.alias" class="help-text help-text--large">
              Optional, and the demo starts with <strong>wlpilot</strong> so you can see a clean
              branded short link right away.
            </p>
          </div>

          <div class="form-field">
            <div class="field-label-row">
              <label class="field-label" for="expiration">Expiration</label>
              <Button
                type="button"
                class="field-help-button"
                icon="pi pi-question-circle"
                severity="secondary"
                variant="text"
                size="small"
                aria-label="Toggle expiration help"
                :aria-expanded="openHelp.expiration"
                @click="toggleHelp('expiration')"
              />
            </div>
            <InputText id="expiration" v-model="form.expiresAt" type="datetime-local" />
            <p v-if="openHelp.expiration" class="help-text help-text--large">
              Leave this blank for no expiration. If you choose a date, the backend still caps the
              maximum lifetime so demo links do not run forever by accident.
            </p>
          </div>

          <div class="actions">
            <Button
              type="submit"
              :label="submitting ? 'Creating...' : 'Shorten link'"
              icon="pi pi-link"
              :disabled="submitting"
            />
          </div>

          <div class="create-footer">
            <p v-if="errorMessage" class="status error">
              <span class="status-dot"></span>
              {{ errorMessage }}
            </p>
            <p v-else-if="successMessage" class="status">
              <span class="status-dot"></span>
              {{ successMessage }}
            </p>
            <p class="status warning create-status">
              <span class="status-dot"></span>
              {{ userStatus }}
            </p>
          </div>
        </form>
      </PanelCard>
    </div>

    <PanelCard
      eyebrow="Recent links"
      title="Recent links"
      description="The latest saved links from the backend, ready to open, copy, or inspect."
    >
      <template #actions>
        <Button
          type="button"
          class="refresh-button"
          :label="loadingRecent ? 'Refreshing...' : 'Refresh'"
          icon="pi pi-refresh"
          severity="secondary"
          variant="text"
          :disabled="loadingRecent"
          @click="refreshRecent"
        />
      </template>

      <p v-if="recentError" class="status error">
        <span class="status-dot"></span>
        {{ recentError }}
      </p>

      <div v-if="recentLinks.length" class="list">
        <div v-for="item in recentLinks" :key="item.code" class="list-item">
          <div class="section-row">
            <div>
              <strong>{{ item.code }}</strong>
              <p>{{ item.shortUrl }}</p>
            </div>
            <div class="list-item-meta list-item-meta--stacked list-item-meta--large">
              <span>{{ item.ownerUsername ?? 'Anonymous demo' }}</span>
              <span>Created: {{ formatDate(item.createdAt) }}</span>
              <span>Expires: {{ formatDate(item.expiresAt) }}</span>
            </div>
          </div>
          <p class="footnote">{{ item.clickCount }} clicks</p>
          <div class="actions">
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
              :label="isCopied(`recent-${item.code}`) ? 'Short URL copied' : 'Copy short URL'"
              :icon="isCopied(`recent-${item.code}`) ? 'pi pi-check' : 'pi pi-copy'"
              severity="secondary"
              variant="outlined"
              @click="copy(item.shortUrl, `recent-${item.code}`)"
            />
            <Button
              type="button"
              label="Open QR"
              icon="pi pi-qrcode"
              severity="secondary"
              variant="outlined"
              @click="openQrModal(item.qrCodeUrl, item.code)"
            />
          </div>
        </div>
      </div>

      <div v-else class="empty-state">
        <p class="eyebrow">No history yet</p>
        <h4 class="card-title">Create your first short link and it will appear here.</h4>
        <p class="muted">
          Recent links come from the backend, so this section always reflects the latest saved data.
        </p>
      </div>
    </PanelCard>

    <PanelCard
      eyebrow="Link management features"
      title="Link management features"
      description="A few quick reasons the product works well for demos and everyday link sharing."
    >
      <div class="feature-grid">
        <FeatureCard
          tone="info"
          eyebrow="Guest demo links"
          title="Create without friction"
          description="Make a short URL in seconds when you only need a public demo."
        />
        <FeatureCard
          tone="success"
          eyebrow="Owned user links"
          title="Keep ownership and history"
          description="Sign in when you want private links, saved history, and account access."
        />
        <FeatureCard
          tone="warn"
          eyebrow="QR scans"
          title="Works on phones and print"
          description="Every link is ready for scanning, slides, posters, and quick handoffs."
        />
        <FeatureCard
          tone="contrast"
          eyebrow="Analytics insights"
          title="See clicks in context"
          description="Track performance without leaving the link workflow."
        />
        <FeatureCard
          tone="accent"
          eyebrow="Simple start"
          title="Ready on first load"
          description="The demo opens with sensible defaults so you can try it immediately."
        />
        <FeatureCard
          tone="muted"
          eyebrow="Signed-in mode"
          title="Return later with confidence"
          description="Your account keeps the link list, so revisits stay predictable."
        />
      </div>
    </PanelCard>

    <section v-if="createdLink" class="page-grid two-col">
      <PanelCard eyebrow="Created link" title="Share card">
        <div class="list-item">
          <strong>{{ createdLink.shortUrl }}</strong>
          <p>Short URL ready to copy, open, or share.</p>
        </div>
        <div class="list-item">
          <strong class="list-item--large">
            {{ createdLink.ownerUsername ?? 'Guest mode (ready for anonymous links)' }}
          </strong>
          <p>Ownership for this link.</p>
        </div>
        <div class="list-item-meta list-item-meta--large">
          <span>Created: {{ formatDate(createdLink.createdAt) }}</span>
          <span>Expires: {{ formatDate(createdLink.expiresAt) }}</span>
        </div>
        <div class="actions">
          <RouterLink :to="{ name: 'link', params: { code: createdLink.code } }">
            <Button label="View details page" icon="pi pi-external-link" />
          </RouterLink>
          <RouterLink :to="dashboardUrl">
            <Button
              label="Open analytics"
              icon="pi pi-chart-bar"
              severity="secondary"
              variant="outlined"
            />
          </RouterLink>
          <Button
            type="button"
            :label="isCopied('created-short') ? 'Short URL copied' : 'Copy short URL'"
            :icon="isCopied('created-short') ? 'pi pi-check' : 'pi pi-copy'"
            @click="copy(createdLink.shortUrl, 'created-short')"
          />
          <Button
            type="button"
            label="Open redirect"
            icon="pi pi-arrow-right"
            severity="secondary"
            variant="outlined"
            @click="openExternal(createdLink.shortUrl)"
          />
          <Button
            v-if="canSeePreview"
            type="button"
            :label="isCopied('created-preview') ? 'Preview URL copied' : 'Copy preview URL'"
            :icon="isCopied('created-preview') ? 'pi pi-check' : 'pi pi-copy'"
            severity="secondary"
            variant="outlined"
            @click="copy(linkPreviewUrl, 'created-preview')"
          />
        </div>
      </PanelCard>

      <PanelCard eyebrow="QR output" title="Mobile scan ready">
        <figure class="compact-figure">
          <img
            class="qr-image"
            :src="createdLink.qrCodeUrl"
            :alt="`QR code for ${createdLink.code}`"
          />
        </figure>

        <div class="grid-2">
          <Button
            type="button"
            :label="isCopied('created-qr') ? 'QR URL copied' : 'Copy QR URL'"
            :icon="isCopied('created-qr') ? 'pi pi-check' : 'pi pi-copy'"
            @click="copy(createdLink.qrCodeUrl, 'created-qr')"
          />
          <Button
            type="button"
            label="Open QR"
            icon="pi pi-qrcode"
            severity="secondary"
            variant="outlined"
            @click="openQrModal(createdLink.qrCodeUrl, createdLink.code)"
          />
        </div>

        <p class="help-text">
          QR code endpoint: <span class="inline-code">{{ createdLink.qrCodeUrl }}</span>
        </p>
      </PanelCard>
    </section>

    <teleport to="body">
      <Transition name="session-notice">
        <div v-if="qrModalUrl" class="modal-backdrop" @click.self="closeQrModal">
          <div class="modal-card card">
            <div class="card-inner stack">
              <div class="section-row">
                <div>
                  <p class="eyebrow">QR code</p>
                  <h3 class="panel-title">{{ qrModalTitle }}</h3>
                </div>
                <Button
                  type="button"
                  label="Close"
                  icon="pi pi-times"
                  severity="secondary"
                  variant="text"
                  size="small"
                  @click="closeQrModal"
                />
              </div>

              <img
                class="qr-image qr-image--compact modal-qr"
                :src="qrModalUrl"
                :alt="`QR code for ${qrModalTitle}`"
              />
            </div>
          </div>
        </div>
      </Transition>
    </teleport>
  </section>
</template>
