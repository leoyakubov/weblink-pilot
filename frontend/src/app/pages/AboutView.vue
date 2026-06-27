<script setup lang="ts">
import PageIntro from '@/shared/components/common/PageIntro.vue';
import PanelCard from '@/shared/components/common/PanelCard.vue';

const productItems = [
  ['Guest links', 'Create a shareable link without an account.'],
  ['Owned links', 'Sign in to save private history and return later.'],
  ['Public sharing', 'Short URLs and QR codes stay easy to open.'],
  ['Analytics', 'See clicks, scans, visitors, and country signals.'],
];

const stackItems = [
  ['Frontend', 'Vue 3, Vue Router, PrimeVue controls, TypeScript, and Vite.'],
  ['Backend', 'Spring Boot modular API with auth, ownership, redirects, QR codes, and analytics.'],
  ['Data', 'PostgreSQL persistence, Redis cache, H2 local profile, and Flyway migrations.'],
  ['Testing', 'Vitest, Vue Test Utils, Playwright, JUnit, Mockito, and JaCoCo.'],
  ['Quality', 'ESLint, Prettier, Maven verification gates, and Docker Compose smoke flows.'],
  ['Delivery', 'GitHub Actions CI/CD, Netlify frontend hosting, and Render backend services.'],
  ['Email', 'Mailpit for local testing and Brevo SMTP for demo email delivery.'],
];

const runtimeItems = [
  ['local', 'H2 and in-memory cache for quick isolated development.'],
  ['dev', 'Docker Compose with Postgres, Redis, and Mailpit.'],
  ['demo-local', 'Local demo flow with external SMTP through Brevo.'],
  ['demo', 'Netlify frontend with Render backend, Postgres, Redis, and Brevo.'],
];

const demoUsers = [
  [
    'admin',
    'Administrator account for monitoring and full demo access.',
    'Credentials: admin / admin123',
  ],
  [
    'user',
    'Standard user account for owned links and private history.',
    'Credentials: user / user123',
  ],
];

const seededLinks = [
  ['/spring-boot', 'https://spring.io/projects/spring-boot', 'anonymous'],
  ['/vue-js', 'https://vuejs.org/guide/introduction.html', 'anonymous'],
  ['/postgres', 'https://www.postgresql.org/about/', 'user'],
  ['/redis', 'https://redis.io/docs/latest/develop/', 'user'],
];

const accountItems = [
  ['Guest mode', 'Create anonymous demo links without an account.'],
  ['Signed-in mode', 'Keep link ownership, private history, and owned analytics.'],
  ['Admin mode', 'Open monitoring, backend endpoints, and creator-filtered dashboard flows.'],
];
</script>

<template>
  <section class="page-grid">
    <PageIntro
      eyebrow="About"
      title="WebLinkPilot"
      description="A small short-link workspace for personal sharing, saved history, QR codes, and analytics."
    />

    <div class="page-grid two-col about-card-row">
      <PanelCard
        eyebrow="Product"
        title="What it does"
        description="WebLinkPilot keeps link creation quick, with optional ownership and insight when you need it."
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

      <PanelCard
        eyebrow="Seeded data"
        title="Demo links"
        description="Starter links make details, QR codes, and analytics visible immediately."
      >
        <dl class="detail-list">
          <div v-for="[path, url, owner] in seededLinks" :key="path">
            <dt>{{ path }}</dt>
            <dd>
              <strong>{{ url }}</strong>
              <span class="footnote">by {{ owner }}</span>
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
    </div>
  </section>
</template>
