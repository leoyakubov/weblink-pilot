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
const createdModalMode = ref<'created' | 'existing'>('created');

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
      createdLink.value = existingLink;
      createdModalMode.value = 'existing';
      createdModalOpen.value = true;
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
    createdModalMode.value = 'created';
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
      description="A personal workspace for clean links, QR sharing, and lightweight analytics."
    />

    <div class="page-grid two-col home-top-grid">
      <PanelCard
        eyebrow="How it works"
        title="Create once, share anywhere"
        description="Paste a URL, choose an optional alias, and WebLinkPilot prepares a short redirect with QR access and tracking."
      >
        <div class="feature-grid feature-grid--compact">
          <FeatureCard
            tone="info"
            eyebrow="Anonymous mode"
            title="Start instantly"
            description="Create a quick link without signing in."
          />
          <FeatureCard
            tone="success"
            eyebrow="Saved workspace"
            title="Keep your links"
            description="Sign in to return to your private link list later."
          />
          <FeatureCard
            tone="warn"
            eyebrow="QR included"
            title="Share offline"
            description="Open a QR code for slides, notes, or phone sharing."
          />
          <FeatureCard
            tone="contrast"
            eyebrow="Useful signals"
            title="See what happened"
            description="Review clicks, scans, countries, and recent activity."
          />
        </div>
      </PanelCard>

      <PanelCard
        eyebrow="Create link"
        title="Shorten link"
        description="Use the defaults, or add your own URL, alias, and expiration."
      >
        <form class="form-grid create-link-form" @submit.prevent="submit">
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
                <label class="field-label" for="custom-alias">
                  Custom alias <span class="field-label-optional">(optional)</span>
                </label>
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
                <label class="field-label" for="expiration">
                  Expiration <span class="field-label-optional">(optional)</span>
                </label>
                <HelpTooltip button-label="Toggle expiration help">
                  Leave blank to keep this link active without an expiration date. Choose a date if
                  you want redirects to stop automatically. By default, selected expiration dates
                  cannot be more than 365 days in the future.
                </HelpTooltip>
              </div>
            </div>
            <InputText id="expiration" v-model="form.expiresAt" type="datetime-local" />
          </div>

          <div class="actions">
            <Button
              type="submit"
              class="shorten-submit-button"
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
      description="Recently created links from your current session or account."
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
                  <p class="eyebrow">
                    {{ createdModalMode === 'created' ? 'Created link' : 'Shortened link exists' }}
                  </p>
                  <h3 class="panel-title created-modal-title">
                    <i
                      v-if="createdModalMode === 'existing'"
                      class="pi pi-exclamation-triangle"
                      aria-hidden="true"
                    ></i>
                    <span>{{ createdLink.code }}</span>
                  </h3>
                </div>
                <Button
                  type="button"
                  icon="pi pi-times"
                  class="modal-close-button modal-close-button--icon"
                  severity="secondary"
                  aria-label="Close"
                  @click="closeCreatedModal"
                />
              </div>

              <p v-if="createdModalMode === 'existing'" class="status warning">
                <span class="status-dot"></span>
                This URL already has a short link for
                {{ createdLink.ownerUsername ?? 'anonymous demo' }}. Use the existing link below
                instead of creating a duplicate.
              </p>

              <div class="created-link-result">
                <p class="recent-link-label">Short URL</p>
                <strong>{{ createdLink.shortUrl }}</strong>
              </div>

              <div class="actions recent-link-actions created-link-actions">
                <RouterLink :to="{ name: 'link', params: { code: createdLink.code } }">
                  <Button
                    label="View details"
                    icon="pi pi-external-link"
                    severity="secondary"
                    variant="outlined"
                  />
                </RouterLink>
                <Button
                  type="button"
                  :label="isCopied('created-short') ? 'Copied' : 'Copy'"
                  :icon="isCopied('created-short') ? 'pi pi-check' : 'pi pi-copy'"
                  severity="secondary"
                  variant="outlined"
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
                  :label="isCopied('created-preview') ? 'Copied' : 'Copy preview URL'"
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
