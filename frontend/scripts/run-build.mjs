import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const frontendRoot = fileURLToPath(new URL('..', import.meta.url));
const viteBin = path.join(frontendRoot, 'node_modules', 'vite', 'bin', 'vite.js');
const vueTscBin = path.join(frontendRoot, 'node_modules', 'vue-tsc', 'bin', 'vue-tsc.js');

function runNodeScript(script, args) {
  const result = spawnSync(process.execPath, [script, ...args], {
    cwd: frontendRoot,
    env: process.env,
    encoding: 'utf8',
  });

  return result.status ?? 1;
}

const typecheckStatus = runNodeScript(vueTscBin, ['--noEmit', '-p', 'tsconfig.json']);
if (typecheckStatus !== 0) {
  process.exit(typecheckStatus);
}

const distBuild = spawnSync(process.execPath, [viteBin, 'build', '--configLoader', 'runner'], {
  cwd: frontendRoot,
  env: process.env,
  encoding: 'utf8',
});

if (distBuild.status === 0) {
  if (distBuild.stdout) {
    process.stdout.write(distBuild.stdout);
  }
  if (distBuild.stderr) {
    process.stderr.write(distBuild.stderr);
  }
  process.exit(0);
}

const fallbackOutDir = fs.mkdtempSync(path.join(os.tmpdir(), 'weblink-pilot-frontend-build-'));
console.log(`Falling back to temporary build output: ${fallbackOutDir}`);
const fallbackBuild = spawnSync(process.execPath, [
  viteBin,
  'build',
  '--configLoader',
  'runner',
  '--outDir',
  fallbackOutDir,
  '--emptyOutDir',
  'false',
], {
  cwd: frontendRoot,
  env: process.env,
  stdio: 'inherit',
});

process.exit(fallbackBuild.status ?? 1);
