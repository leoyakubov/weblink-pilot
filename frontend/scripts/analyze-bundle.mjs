import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { gzipSync } from 'node:zlib';
import { fileURLToPath } from 'node:url';

const frontendRoot = fileURLToPath(new URL('..', import.meta.url));
const viteBin = path.join(frontendRoot, 'node_modules', 'vite', 'bin', 'vite.js');
const tempOutDir = fs.mkdtempSync(path.join(os.tmpdir(), 'weblink-pilot-bundle-'));

const build = spawnSync(process.execPath, [viteBin, 'build', '--configLoader', 'runner', '--outDir', tempOutDir, '--emptyOutDir', 'false'], {
  cwd: frontendRoot,
  env: process.env,
  encoding: 'utf8',
});

if (build.stdout) {
  process.stdout.write(build.stdout);
}
if (build.stderr) {
  process.stderr.write(build.stderr);
}

if ((build.status ?? 1) !== 0) {
  process.exit(build.status ?? 1);
}

const assetsDir = path.join(tempOutDir, 'assets');
const assets = fs.existsSync(assetsDir)
  ? fs.readdirSync(assetsDir)
      .map((file) => {
        const fullPath = path.join(assetsDir, file);
        const contents = fs.readFileSync(fullPath);
        return {
          file,
          bytes: contents.length,
          gzipBytes: gzipSync(contents).length,
        };
      })
      .sort((left, right) => right.bytes - left.bytes)
  : [];

console.log('\nTop frontend assets by size:');
for (const asset of assets.slice(0, 10)) {
  const kilobytes = (asset.bytes / 1024).toFixed(1);
  const gzipKilobytes = (asset.gzipBytes / 1024).toFixed(1);
  console.log(`- ${asset.file}: ${kilobytes} kB (${gzipKilobytes} kB gzip)`);
}

console.log(`\nBundle analysis output directory: ${tempOutDir}`);
