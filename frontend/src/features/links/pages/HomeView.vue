<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import Button from 'primevue/button';
import InputText from 'primevue/inputtext';
import LinkList from '@/features/links/components/LinkList.vue';
import FeatureCard from '@/shared/components/common/FeatureCard.vue';
import PageIntro from '@/shared/components/common/PageIntro.vue';
import PanelCard from '@/shared/components/common/PanelCard.vue';
import RefreshButton from '@/shared/components/common/RefreshButton.vue';
import HelpTooltip from '@/shared/components/common/HelpTooltip.vue';
import QrCodeModal from '@/shared/components/common/QrCodeModal.vue';
import { useCopyAction } from '@/shared/composables/useCopyAction';
import { authState } from '@/features/auth/services/auth.service';
import { buildApiBaseUrl } from '@/shared/services/http';
import { loadSettings, saveSettings } from '@/shared/services/settings';
import { createLink, listLinks } from '@/features/links/repositories/link.repository';
import type { ApiSettings, CreateLinkRequest, LinkResponse } from '@/shared/types/api';

const CREATE_COOLDOWN_MS = 1500;

const settings = reactive<ApiSettings>(loadSettings());
const form = reactive<CreateLinkRequest>({
  originalUrl: 'https://github.com/leoyakubov/weblink-pilot',
  customAlias: '',
  expiresAt: '',
});

const createdLink = ref<LinkResponse | null>(null);
const recentLinks = ref<LinkResponse[]>([]);
const loadingRecent = ref(false);
const recentError = ref('');
const errorMessage = ref('');
const successMessage = ref('');
const submitting = ref(false);
const createCooldownUntil = ref(0);
const qrModalUrl = ref('');
const qrModalTitle = ref('');
const createdModalOpen = ref(false);

const userStatus = computed(() =>
  authState.currentUser
    ? `Signed in as ${authState.currentUser.username} (${authState.currentUser.role})`
    : 'Guest mode (ready for anonymous links)',
);

const canSeePreview = computed(() => authState.currentUser?.role === 'ADMIN');

const linkPreviewUrl = computed(() =>
  createdLink.value ? buildApiBaseUrl(`/urls/${createdLink.value.code}/preview`, settings) : '',
);

const { copy, isCopied } = useCopyAction();

function normalizeComparableUrl(value: string) {
  try {
    return new URL(value.trim()).href;
  } catch {
    return value.trim();
  }
}

function findExistingLink(originalUrl: string) {
  const currentOwner = authState.currentUser?.username ?? null;
  const normalizedOriginalUrl = normalizeComparableUrl(originalUrl);

  return recentLinks.value.find(
    (link) =>
      normalizeComparableUrl(link.originalUrl) === normalizedOriginalUrl &&
      (link.ownerUsername ?? null) === currentOwner,
  );
}

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

  try {
    syncSettings();

    const originalUrl = form.originalUrl.trim();
    new URL(originalUrl);

    const now = Date.now();
    if (now < createCooldownUntil.value) {
      errorMessage.value = 'Please wait a moment before shortening another link.';
      return;
    }

    const existingLink = findExistingLink(originalUrl);
    if (existingLink) {
      errorMessage.value = `This full URL already has a short link for ${existingLink.ownerUsername ?? 'anonymous demo'}: ${existingLink.shortUrl}`;
      return;
    }

    const payload: CreateLinkRequest = {
      originalUrl,
      customAlias: form.customAlias?.trim() || undefined,
      expiresAt: form.expiresAt ? new Date(form.expiresAt).toISOString() : null,
    };

    submitting.value = true;
    createCooldownUntil.value = Date.now() + CREATE_COOLDOWN_MS;
    createdLink.value = await createLink(payload, settings);
    successMessage.value = `Created ${createdLink.value.code} successfully`;
    createdModalOpen.value = true;
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

function closeCreatedModal() {
  createdModalOpen.value = false;
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
      description="Fast, branded short links for personal demos, saved history, and everyday sharing."
    />

    <div class="page-grid two-col home-top-grid">
      <PanelCard
        eyebrow="Fast start"
        title="Fast, personal links"
        description="Create a demo link in seconds, or sign in to keep ownership and revisit your private history later. Redirects stay fast, QR scans are built in, and analytics are ready when you need them."
      >
        <div class="feature-grid feature-grid--compact">
          <FeatureCard
            tone="info"
            eyebrow="Guest demo links"
            title="Create without friction"
            description="Make a short URL in seconds when you only need a quick personal demo."
          />
          <FeatureCard
            tone="success"
            eyebrow="Owned user links"
            title="Keep ownership and history"
            description="Sign in when you want private links, saved history, and easy revisits later."
          />
          <FeatureCard
            tone="warn"
            eyebrow="QR scans"
            title="Works on phones and print"
            description="Every link is ready for scanning from your phone, printouts, posters, and slides."
          />
          <FeatureCard
            tone="contrast"
            eyebrow="Analytics insights"
            title="See clicks in context"
            description="Track performance without leaving the link workflow or opening extra tools."
          />
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
              placeholder="https://github.com/leoyakubov/weblink-pilot"
              required
            />
          </label>

          <div class="form-field form-field--with-popover">
            <div class="field-label-row">
              <div class="field-label-group">
                <label class="field-label" for="custom-alias">Custom alias (optional)</label>
                <HelpTooltip button-label="Toggle custom alias help">
                  Choose a short, memorable word for the end of your link, like
                  <strong>portfolio</strong> or <strong>launch-notes</strong>. Leave it empty when
                  you want WebLinkPilot to create one automatically.
                </HelpTooltip>
              </div>
            </div>
            <InputText
              id="custom-alias"
              v-model="form.customAlias"
              type="text"
              placeholder="weblinkpilot"
            />
          </div>

          <div class="form-field form-field--with-popover">
            <div class="field-label-row">
              <div class="field-label-group">
                <label class="field-label" for="expiration">Expiration (optional)</label>
                <HelpTooltip button-label="Toggle expiration help">
                  Leave this blank for no expiration. If you choose a date, the backend still caps
                  the maximum lifetime so your links do not run forever by accident.
                </HelpTooltip>
              </div>
            </div>
            <InputText id="expiration" v-model="form.expiresAt" type="datetime-local" />
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
      eyebrow="Saved history"
      title="Latest links"
      description="Fresh links from the backend, ready to open, copy, or inspect."
    >
      <template #actions>
        <RefreshButton :loading="loadingRecent" @refresh="refreshRecent" />
      </template>

      <p v-if="recentError" class="status error">
        <span class="status-dot"></span>
        {{ recentError }}
      </p>

      <LinkList
        v-if="recentLinks.length"
        :links="recentLinks"
        copy-key-prefix="recent"
        @open-qr="(item) => openQrModal(item.qrCodeUrl, item.code)"
      />

      <div v-else class="empty-state">
        <p class="eyebrow">No history yet</p>
        <h4 class="card-title">Create your first short link and it will appear here.</h4>
        <p class="muted">This section reflects the latest saved data from the backend.</p>
      </div>
    </PanelCard>

    <QrCodeModal
      :visible="Boolean(qrModalUrl)"
      :title="qrModalTitle"
      :url="qrModalUrl"
      @close="closeQrModal"
    />

    <teleport to="body">
      <Transition name="session-notice">
        <div
          v-if="createdLink && createdModalOpen"
          class="modal-backdrop"
          @click.self="closeCreatedModal"
        >
          <div class="modal-card modal-card--wide card">
            <div class="card-inner stack">
              <div class="section-row">
                <div>
                  <p class="eyebrow">Created link</p>
                  <h3 class="panel-title">{{ createdLink.code }}</h3>
                </div>
                <Button
                  type="button"
                  label="Close"
                  icon="pi pi-times"
                  class="modal-close-button"
                  severity="secondary"
                  @click="closeCreatedModal"
                />
              </div>

              <div class="created-link-result">
                <p class="recent-link-label">Short URL</p>
                <strong>{{ createdLink.shortUrl }}</strong>
              </div>

              <div class="actions">
                <RouterLink :to="{ name: 'link', params: { code: createdLink.code } }">
                  <Button label="View details page" icon="pi pi-external-link" />
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
            </div>
          </div>
        </div>
      </Transition>
    </teleport>
  </section>
</template>
