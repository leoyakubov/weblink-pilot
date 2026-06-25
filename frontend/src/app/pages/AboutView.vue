<script setup lang="ts">
import PageIntro from '@/shared/components/common/PageIntro.vue';
import PanelCard from '@/shared/components/common/PanelCard.vue';

const productItems = [
  ['Guest links', 'Create public demo links without signing in.'],
  ['Owned links', 'Sign in to keep private history and revisit saved links.'],
  ['Public redirects', 'Short URLs, QR URLs, and preview endpoints remain fast and public.'],
  ['Analytics', 'Inspect clicks, QR scans, unique visitors, and country breakdowns.'],
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
      description="A compact short-link workspace with guest links, signed-in ownership, QR codes, analytics, and production-shaped deployment settings."
    />

    <div class="page-grid two-col about-card-row">
      <PanelCard
        eyebrow="Product"
        title="What it does"
        description="WebLinkPilot keeps link creation quick while still supporting account ownership and operational visibility."
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
        description="The same app supports quick anonymous use, signed-in ownership, and administrator monitoring."
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
        description="Local and demo profiles can seed these accounts so the app is immediately explorable."
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
        description="These links help open details, QR codes, and analytics without creating fresh data first."
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
        description="The stack is split by responsibility so UI, API, data, and cache behavior stay easy to reason about."
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
        description="Each mode keeps the same product behavior while swapping infrastructure details. Editable backend and browser settings now live on the Monitoring page."
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
