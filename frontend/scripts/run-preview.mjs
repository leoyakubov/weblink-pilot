import { ensureFrontendDependencies } from './ensure-frontend-deps.mjs';
import { spawnSync } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const frontendRoot = fileURLToPath(new URL('..', import.meta.url));
ensureFrontendDependencies(frontendRoot);

const viteBin = path.join(frontendRoot, 'node_modules', 'vite', 'bin', 'vite.js');
const result = spawnSync(process.execPath, [viteBin, 'preview', ...process.argv.slice(2)], {
  cwd: frontendRoot,
  env: process.env,
  stdio: 'inherit',
});

process.exit(result.status ?? 1);
