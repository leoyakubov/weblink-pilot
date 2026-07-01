<script setup lang="ts">
import { useBrowserSettingsResetView } from './BrowserSettingsResetView';
import './BrowserSettingsResetView.css';

useBrowserSettingsResetView();
</script>

<template>
  <section class="page-grid browser-settings-reset-page">
    <article class="card">
      <div class="card-inner stack">
        <p class="eyebrow">Reset</p>
        <h3 class="panel-title">Browser settings cleared.</h3>
        <p class="hero-note">
          Saved API settings were removed from this browser. Taking you back to the app now.
        </p>
      </div>
    </article>
  </section>
</template>
