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

export function useAboutView() {
  return {
    accountItems,
    demoUsers,
    localUrls,
    productItems,
    projectLinks,
    runtimeItems,
    stackItems,
  };
}
