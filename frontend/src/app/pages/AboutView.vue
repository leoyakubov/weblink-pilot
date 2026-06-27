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
    'Administrator account for monitoring and full demo access. It can view all links, filter by creator, open monitoring, and inspect admin-only data.',
    'Credentials: admin / admin123',
  ],
  [
    'user',
    'Standard user account for owned links and private history. It can create owned links, revisit saved history, and view analytics for owned links.',
    'Credentials: user / user123',
  ],
];

const accountItems = [
  ['Guest mode', 'Create anonymous demo links without an account.'],
  ['Signed-in mode', 'Keep link ownership, private history, and owned analytics.'],
  ['Admin mode', 'Open monitoring, backend endpoints, and creator-filtered dashboard flows.'],
];

const projectLinks = [
  [
    'GitHub',
    'Source code, issues, and pull requests.',
    'https://github.com/leoyakubov/weblink-pilot',
  ],
  [
    'Docs',
    'Documentation index for setup, design, testing, and operations.',
    'https://github.com/leoyakubov/weblink-pilot/blob/main/docs/README.md',
  ],
  [
    'Roadmap',
    'Current product phases and remaining delivery scope.',
    'https://github.com/leoyakubov/weblink-pilot/blob/main/docs/planning/roadmap.md',
  ],
  [
    'API contract',
    'REST API shape used by the frontend and backend.',
    'https://github.com/leoyakubov/weblink-pilot/blob/main/docs/implementation/api-contract-v1.md',
  ],
  [
    'Deployment',
    'Runtime environment notes for Netlify, Render, Postgres, Redis, and SMTP.',
    'https://github.com/leoyakubov/weblink-pilot/blob/main/docs/operations/deployment.md',
  ],
];

const localUrls = [
  ['Frontend', 'http://localhost:5173', 'Vite dev server for the Vue app.'],
  ['Backend API', 'http://localhost:8080/api/v1', 'Base URL used by frontend requests.'],
  ['OpenAPI JSON', 'http://localhost:8080/v3/api-docs', 'Machine-readable REST contract.'],
  ['Health check', 'http://localhost:8080/actuator/health', 'Backend readiness endpoint.'],
  ['Prometheus', 'http://localhost:8080/actuator/prometheus', 'Metrics endpoint for monitoring.'],
];
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
