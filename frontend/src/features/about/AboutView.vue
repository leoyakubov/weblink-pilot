<script setup lang="ts">
import './AboutView.css';
import PageIntro from '@/shared/components/PageIntro.vue';
import PanelCard from '@/shared/components/PanelCard.vue';
import { useAboutView } from './AboutView';

const { accountItems, demoUsers, localUrls, productItems, projectLinks, runtimeItems, stackItems } =
  useAboutView();
</script>

<template>
  <section class="page-grid">
    <PageIntro
      eyebrow="About"
      title="WeblinkPilot"
      description="A small short-link workspace for personal sharing, saved history, QR codes, and analytics."
    />

    <div class="page-grid two-col about-card-row">
      <PanelCard
        eyebrow="Product"
        title="What it does"
        description="WeblinkPilot keeps link creation quick, with optional ownership and insight when you need it."
      >
        <dl class="detail-list detail-list--about-horizontal">
          <div v-for="[label, value] in productItems" :key="label">
            <dt>{{ label }}</dt>
            <dd>{{ value }}</dd>
          </div>
        </dl>
      </PanelCard>

      <PanelCard
        eyebrow="Account access"
        title="Guest, user, and admin flows"
        description="Use it anonymously, sign in for your own workspace, or inspect the demo as an admin."
      >
        <dl class="detail-list detail-list--about-horizontal">
          <div v-for="[label, value] in accountItems" :key="label">
            <dt>{{ label }}</dt>
            <dd>{{ value }}</dd>
          </div>
        </dl>
      </PanelCard>
    </div>

    <div class="page-grid two-col about-card-row">
      <PanelCard
        eyebrow="Runtime"
        title="Profiles and environments"
        description="Each mode keeps the same app experience while changing local or hosted infrastructure."
      >
        <dl class="detail-list">
          <div v-for="[label, value] in runtimeItems" :key="label">
            <dt>{{ label }}</dt>
            <dd>{{ value }}</dd>
          </div>
        </dl>
      </PanelCard>

      <PanelCard
        eyebrow="Seeded data"
        title="Default users"
        description="Demo accounts are available so you can try ownership and admin-only views right away."
      >
        <dl class="detail-list">
          <div v-for="[username, description, credentials] in demoUsers" :key="username">
            <dt>{{ username }}</dt>
            <dd>
              <span>{{ description }}</span>
              <strong class="footnote">{{ credentials }}</strong>
            </dd>
          </div>
        </dl>
      </PanelCard>
    </div>

    <div class="page-grid two-col about-card-row">
      <PanelCard
        eyebrow="Tech stack"
        title="Implementation"
        description="The project is split into clear frontend, backend, data, testing, and delivery layers."
      >
        <dl class="detail-list">
          <div v-for="[label, value] in stackItems" :key="label">
            <dt>{{ label }}</dt>
            <dd>{{ value }}</dd>
          </div>
        </dl>
      </PanelCard>

      <PanelCard
        eyebrow="Local URLs"
        title="API endpoints"
        description="Useful development URLs when the frontend and backend run on your machine."
      >
        <dl class="detail-list detail-list--about-links">
          <div v-for="[label, href, value] in localUrls" :key="label">
            <dt>
              <a :href="href" target="_blank" rel="noreferrer">{{ label }}</a>
            </dt>
            <dd>
              <strong>{{ href }}</strong>
              <span class="footnote">{{ value }}</span>
            </dd>
          </div>
        </dl>
      </PanelCard>
    </div>

    <div class="page-grid two-col about-card-row">
      <PanelCard
        eyebrow="Project links"
        title="Source and docs"
        description="Useful references for the codebase, API contract, roadmap, and deployment notes."
      >
        <dl class="detail-list detail-list--about-links">
          <div v-for="[label, value, href] in projectLinks" :key="label">
            <dt>
              <a :href="href" target="_blank" rel="noreferrer">{{ label }}</a>
            </dt>
            <dd>{{ value }}</dd>
          </div>
        </dl>
      </PanelCard>
    </div>
  </section>
</template>
