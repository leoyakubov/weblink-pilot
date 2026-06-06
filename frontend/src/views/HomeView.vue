<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import Button from 'primevue/button';
import InputText from 'primevue/inputtext';
import Tag from 'primevue/tag';
import CopyActionButton from '@/components/CopyActionButton.vue';
import { authState } from '@/lib/auth';
import { buildApiBaseUrl, createLink, listLinks } from '@/lib/api';
import { loadSettings, saveSettings } from '@/lib/settings';
import type { ApiSettings, CreateLinkRequest, LinkResponse } from '@/types';

const settings = reactive<ApiSettings>(loadSettings());
const form = reactive<CreateLinkRequest>({
  originalUrl: 'https://github.com/weblinkpilot/weblink-pilot/tree/main/docs',
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
const qrModalUrl = ref('');
const qrModalTitle = ref('');

const userStatus = computed(() =>
  authState.currentUser
    ? `Signed in as ${authState.currentUser.username} (${authState.currentUser.role})`
    : 'Guest mode ready for demo links',
);

const canSeePreview = computed(() => authState.currentUser?.role === 'ADMIN');

const recentTitle = computed(() => (authState.currentUser ? 'Your Recent Links' : 'Recent Links'));

const linkPreviewUrl = computed(() =>
  createdLink.value ? buildApiBaseUrl(`/urls/${createdLink.value.code}/preview`, settings) : '',
);

const dashboardUrl = computed(() =>
  createdLink.value ? { name: 'dashboard', query: { code: createdLink.value.code } } : '/',
);

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
    <div class="hero-split">
      <article class="card hero-card">
        <div class="card-inner hero-copy">
          <p class="eyebrow">Link management</p>
          <h2 class="hero-title">Short links, QR, analytics.</h2>
          <p class="hero-note">
            WebLinkPilot is built for fast sharing. Create a demo link instantly, or sign in to keep
            ownership and private history. Redirects, QR scans, and analytics stay public and fast.
          </p>

          <div class="hero-badges">
            <Tag value="Guest demo links" severity="info" />
            <Tag value="Owned user links" severity="success" />
            <Tag value="QR scans" severity="warn" />
            <Tag value="Analytics insights" severity="contrast" />
          </div>

          <div class="hero-points">
            <div class="hero-point">
              <strong>Simple start</strong>
              <p>Create a link now, no account required.</p>
            </div>
            <div class="hero-point">
              <strong>Signed-in mode</strong>
              <p>Keep your links owned and revisit them later.</p>
            </div>
          </div>
        </div>
      </article>

      <article class="card hero-card">
        <div class="card-inner stack">
          <p class="status warning">
            <span class="status-dot"></span>
            {{ userStatus }}
          </p>

          <div>
            <p class="eyebrow">Quick create</p>
            <h3 class="panel-title">Shorten a URL</h3>
            <p class="help-text">
              Leave alias blank for a random code. Add one if you want a branded short URL.
            </p>
          </div>

          <form class="form-grid" @submit.prevent="submit">
            <label class="form-field">
              <span class="field-label">Original URL</span>
              <InputText
                v-model="form.originalUrl"
                type="url"
                placeholder="https://github.com/weblinkpilot/weblink-pilot/tree/main/docs"
                required
              />
            </label>

            <label class="form-field">
              <span class="field-label">Custom alias (optional)</span>
              <InputText
                v-model="form.customAlias"
                type="text"
                placeholder="github-org"
              />
            </label>

            <label class="form-field">
              <span class="field-label">Expiration</span>
              <InputText v-model="form.expiresAt" type="datetime-local" />
              <p class="help-text">
                Optional. Expired links stop redirecting, and the backend caps the maximum lifetime.
              </p>
            </label>

            <div class="actions">
              <Button
                type="submit"
                :label="submitting ? 'Creating...' : 'Create short link'"
                icon="pi pi-link"
                :disabled="submitting"
              />
              <RouterLink to="/about">
              <Button
                label="About"
                icon="pi pi-info-circle"
                severity="secondary"
                variant="outlined"
              />
              </RouterLink>
            </div>

            <p v-if="errorMessage" class="status error">
              <span class="status-dot"></span>
              {{ errorMessage }}
            </p>
            <p v-else-if="successMessage" class="status">
              <span class="status-dot"></span>
              {{ successMessage }}
            </p>
          </form>
        </div>
      </article>
    </div>

    <article class="card">
      <div class="card-inner stack">
          <div class="section-row">
            <div>
              <p class="eyebrow">Recent links</p>
              <h3 class="panel-title">{{ recentTitle }}</h3>
            </div>
          <Button
            type="button"
            :label="loadingRecent ? 'Refreshing...' : 'Refresh'"
            icon="pi pi-refresh"
            severity="secondary"
            variant="outlined"
            :disabled="loadingRecent"
            @click="refreshRecent"
          />
          </div>

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
              <span class="footnote">{{ item.ownerUsername ?? 'Anonymous demo' }}</span>
            </div>
            <p class="footnote">{{ item.clickCount }} clicks</p>
            <div class="actions">
              <RouterLink
                :to="{ name: 'link', params: { code: item.code } }"
              >
                <Button label="Details" icon="pi pi-external-link" />
              </RouterLink>
              <RouterLink
                :to="{ name: 'dashboard', query: { code: item.code } }"
              >
                <Button
                  label="Analytics"
                  icon="pi pi-chart-line"
                  severity="secondary"
                  variant="outlined"
                />
              </RouterLink>
              <CopyActionButton
                :value="item.shortUrl"
                label="Copy short URL"
                copied-label="Short URL copied"
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
            <div class="list-item-meta">
              <span>Created: {{ formatDate(item.createdAt) }}</span>
              <span>Expires: {{ formatDate(item.expiresAt) }}</span>
            </div>
          </div>
        </div>

        <div v-else class="empty-state">
          <p class="eyebrow">No history yet</p>
          <h4 class="card-title">Create your first short link and it will appear here.</h4>
          <p class="muted">
            Recent links come from the backend, so this section always reflects the latest saved
            data.
          </p>
        </div>
      </div>
    </article>

    <section v-if="createdLink" class="page-grid two-col">
      <article class="card">
        <div class="card-inner stack">
          <div>
            <p class="eyebrow">Created link</p>
            <h3 class="panel-title">Share card</h3>
          </div>

          <div class="list-item">
            <strong>{{ createdLink.shortUrl }}</strong>
            <p>Short URL ready to copy, open, or share.</p>
          </div>
          <div class="list-item">
            <strong>{{ createdLink.ownerUsername ?? 'Anonymous demo' }}</strong>
            <p>Ownership for this link.</p>
          </div>
          <div class="list-item-meta">
            <span>Created: {{ formatDate(createdLink.createdAt) }}</span>
            <span>Expires: {{ formatDate(createdLink.expiresAt) }}</span>
          </div>
          <div class="actions">
            <RouterLink
              :to="{ name: 'link', params: { code: createdLink.code } }"
            >
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
            <CopyActionButton
              :value="createdLink.shortUrl"
              label="Copy short URL"
              copied-label="Short URL copied"
              variant="primary"
            />
            <Button
              type="button"
              label="Open redirect"
              icon="pi pi-arrow-right"
              severity="secondary"
              variant="outlined"
              @click="openExternal(createdLink.shortUrl)"
            />
            <CopyActionButton
              v-if="canSeePreview"
              :value="linkPreviewUrl"
              label="Copy preview URL"
              copied-label="Preview URL copied"
            />
          </div>
        </div>
      </article>

      <article class="card">
        <div class="card-inner stack">
          <div>
            <p class="eyebrow">QR output</p>
            <h3 class="panel-title">Mobile scan ready</h3>
          </div>

          <figure class="compact-figure">
            <img
              class="qr-image"
              :src="createdLink.qrCodeUrl"
              :alt="`QR code for ${createdLink.code}`"
            />
          </figure>

          <div class="grid-2">
            <CopyActionButton
              :value="createdLink.qrCodeUrl"
              label="Copy QR URL"
              copied-label="QR URL copied"
              variant="primary"
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
        </div>
      </article>
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
