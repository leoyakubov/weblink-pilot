import { ensureFrontendDependencies } from './ensure-frontend-deps.mjs';
import { spawnSync } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const frontendRoot = fileURLToPath(new URL('..', import.meta.url));
ensureFrontendDependencies(frontendRoot);
const viteBin = path.join(frontendRoot, 'node_modules', 'vite', 'bin', 'vite.js');
const vueTscBin = path.join(frontendRoot, 'node_modules', 'vue-tsc', 'bin', 'vue-tsc.js');

function runNodeScript(script, args) {
  const result = spawnSync(process.execPath, [script, ...args], {
    cwd: frontendRoot,
    env: process.env,
    encoding: 'utf8',
  });

  if (result.stdout) {
    process.stdout.write(result.stdout);
  }
  if (result.stderr) {
    process.stderr.write(result.stderr);
  }

  return result.status ?? 1;
}

const typecheckStatus = runNodeScript(vueTscBin, ['--noEmit', '-p', 'tsconfig.json']);
if (typecheckStatus !== 0) {
  process.exit(typecheckStatus);
}

const distBuild = spawnSync(process.execPath, [viteBin, 'build', '--configLoader', 'runner'], {
  cwd: frontendRoot,
  env: process.env,
  stdio: 'inherit',
});

if (distBuild.status === 0) {
  process.exit(0);
}

console.warn(
  'Could not clean the existing dist directory. Rebuilding in place without removing old hashed assets.',
);
const inPlaceBuild = spawnSync(
  process.execPath,
  [viteBin, 'build', '--configLoader', 'runner', '--emptyOutDir', 'false'],
  {
    cwd: frontendRoot,
    env: process.env,
    stdio: 'inherit',
  },
);

process.exit(inPlaceBuild.status ?? 1);
