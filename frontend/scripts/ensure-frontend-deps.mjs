import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';

function npmCommand() {
  return process.platform === 'win32' ? 'npm.cmd' : 'npm';
}

function probeVite(frontendRoot) {
  return spawnSync(
    process.execPath,
    ['--input-type=module', '-e', 'import("vite").then(() => process.exit(0)).catch(() => process.exit(1))'],
    {
      cwd: frontendRoot,
      env: process.env,
      stdio: 'ignore',
      shell: false,
    },
  );
}

function installDependencies(frontendRoot) {
  return spawnSync(npmCommand(), ['install', '--package-lock=false'], {
    cwd: frontendRoot,
    env: process.env,
    stdio: 'inherit',
    shell: false,
  });
}

export function ensureFrontendDependencies(frontendRoot) {
  const nodeModulesDir = path.join(frontendRoot, 'node_modules');
  let needsRefresh = !fs.existsSync(nodeModulesDir);

  if (!needsRefresh) {
    const initialProbe = probeVite(frontendRoot);
    needsRefresh = (initialProbe.status ?? 1) !== 0;
  }

  if (!needsRefresh) {
    return;
  }

  if (!fs.existsSync(nodeModulesDir)) {
    console.log('Frontend dependencies are missing. Installing them for the current platform...');
  } else {
    console.log('Frontend dependencies do not match the current platform. Refreshing them with npm install...');
  }

  const installResult = installDependencies(frontendRoot);
  if ((installResult.status ?? 1) !== 0) {
    throw new Error('npm install failed while refreshing frontend dependencies.');
  }

  const finalProbe = probeVite(frontendRoot);
  if ((finalProbe.status ?? 1) !== 0) {
    throw new Error(
      'Frontend dependencies still cannot load Vite. Remove frontend/node_modules and reinstall inside WSL.',
    );
  }
}
