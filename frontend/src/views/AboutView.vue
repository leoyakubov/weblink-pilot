<script setup lang="ts">
import { reactive, ref } from 'vue';
import { loadSettings, saveSettings } from '@/lib/settings';
import type { ApiSettings } from '@/types';

const settings = reactive<ApiSettings>(loadSettings());
const saved = ref(false);

function saveBaseUrl() {
  saveSettings(settings);
  saved.value = true;
  window.setTimeout(() => {
    saved.value = false;
  }, 1500);
}
</script>

<template>
  <section class="page-grid two-col">
    <article class="card">
      <div class="card-inner stack">
        <div>
          <p class="eyebrow">About</p>
          <h3 class="panel-title">Built like a small SaaS, not a classroom demo.</h3>
        </div>

        <p class="hero-note">
          WebLinkPilot combines guest demo links, signed-in ownership, QR codes, redirect analytics,
          Redis caching, and a production-shaped deployment flow on Netlify and Render.
        </p>

        <div class="grid-2">
          <div class="metric">
            <span class="value">Vue 3</span>
            <span class="label">Frontend shell and app flow</span>
          </div>
          <div class="metric">
            <span class="value">Spring Boot</span>
            <span class="label">Backend API, security, and ownership</span>
          </div>
          <div class="metric">
            <span class="value">Postgres</span>
            <span class="label">Links, users, and analytics persistence</span>
          </div>
          <div class="metric">
            <span class="value">Redis</span>
            <span class="label">Hot lookups and cache invalidation</span>
          </div>
        </div>

        <div class="list-item">
          <strong>Runtime modes</strong>
          <p>
            local: H2 and in-memory cache
            <br />
            dev: Docker Compose with Postgres and Redis
            <br />
            demo: Netlify frontend with Render backend, Postgres, and Redis
          </p>
        </div>
      </div>
    </article>

    <article class="card">
      <div class="card-inner stack">
        <div>
          <p class="eyebrow">Developer settings</p>
          <h3 class="panel-title">Backend base URL</h3>
          <p class="help-text">
            Update this if you want the frontend to point at a different local, dev, or demo
            backend.
          </p>
        </div>

        <label class="form-field">
          <span class="field-label">API base URL</span>
          <input
            v-model="settings.apiBaseUrl"
            class="input"
            type="url"
            placeholder="http://localhost:8080/api/v1"
          />
        </label>

        <div class="actions">
          <button class="button button-primary" type="button" @click="saveBaseUrl">
            Save settings
          </button>
        </div>

        <p v-if="saved" class="status">
          <span class="status-dot"></span>
          Saved for this browser
        </p>

        <div class="list-item">
          <strong>Current backend</strong>
          <p>{{ settings.apiBaseUrl }}</p>
        </div>

        <div class="list-item">
          <strong>Demo accounts</strong>
          <p>
            Local and dev seed the starter accounts:
            <br />
            admin / admin123
            <br />
            user / user123
          </p>
        </div>

        <div class="list-item">
          <strong>What stays public</strong>
          <p>
            Short URL redirects, QR URLs, and preview endpoints remain public. Sign-in only controls
            ownership and admin views.
          </p>
        </div>
      </div>
    </article>
  </section>
</template>
