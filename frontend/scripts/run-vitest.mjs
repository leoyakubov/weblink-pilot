import { ensureFrontendDependencies } from './ensure-frontend-deps.mjs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const frontendRoot = fileURLToPath(new URL('..', import.meta.url));
const coverageEnabled = process.argv.includes('--coverage');

ensureFrontendDependencies(frontendRoot);

const [{ default: vue }, { startVitest }] = await Promise.all([
  import('@vitejs/plugin-vue'),
  import('vitest/node'),
]);

const testConfig = {
  environment: 'jsdom',
  globals: true,
  include: ['src/**/*.test.ts'],
  setupFiles: ['src/test/setup.ts'],
  cache: false,
};

if (coverageEnabled) {
  testConfig.coverage = {
    provider: 'v8',
    reporter: ['text', 'html', 'lcov', 'json-summary'],
    all: true,
    include: ['src/**/*.{ts,vue}'],
    exclude: [
      'src/main.ts',
      'src/env.d.ts',
      'src/**/*.d.ts',
      'src/**/*.test.ts',
      'src/**/*.spec.ts',
    ],
    thresholds: {
      statements: 70,
      branches: 65,
      functions: 60,
      lines: 70,
    },
  };
}

await startVitest(
  'test',
  [],
  {
    root: frontendRoot,
    config: false,
    run: true,
  },
  {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': path.resolve(frontendRoot, 'src'),
      },
    },
    cacheDir: path.resolve(frontendRoot, '.vite'),
    test: testConfig,
  },
);

if (typeof process.exitCode === 'number' && process.exitCode !== 0) {
  process.exit(process.exitCode);
}
