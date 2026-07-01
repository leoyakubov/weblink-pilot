<script setup lang="ts">
import './GithubLoginCompleteView.css';
import { useGithubLoginCompleteView } from './GithubLoginCompleteView';

const { busy, errorMessage } = useGithubLoginCompleteView();
</script>

<template>
  <section class="auth-layout">
    <article class="card auth-card">
      <div class="card-inner stack">
        <div class="auth-heading">
          <p class="eyebrow">Account</p>
          <h3 class="panel-title">GitHub sign-in</h3>
          <p class="help-text">Finishing the GitHub handoff and returning you to WeblinkPilot.</p>
        </div>

        <p v-if="busy && !errorMessage" class="footnote">Completing GitHub sign-in...</p>
        <p v-if="errorMessage" class="status error" role="alert" aria-live="polite">
          <span class="status-dot"></span>
          {{ errorMessage }}
        </p>
      </div>
    </article>
  </section>
</template>
