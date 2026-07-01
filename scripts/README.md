# Project Scripts

WeblinkPilot uses one Bash-only automation set on every platform.

## Layout

- `dev/` - local runtime, Docker, demo, monitoring, and deployment helpers
- `git/` - Git hooks and secret scanning
- `lib/` - shared Bash functions
- `quality/` - formatting, tests, coverage, smoke, and Sonar helpers
- `security/` - backend and frontend dependency checks
- `run-before-push.sh` - the main local verification entrypoint

## Windows

Install WSL 2 and a Linux distribution, then enable that distribution in Docker Desktop WSL integration.

Run a script from PowerShell:

```powershell
wsl bash ./scripts/run-before-push.sh
```

Or enter WSL first:

```powershell
wsl
```

```bash
cd ~/projects/weblink-pilot
bash ./scripts/run-before-push.sh
```

For better filesystem performance, keep the repository under the WSL filesystem, such as `~/projects/weblink-pilot`.
If `frontend/node_modules` was created on Windows first, the frontend scripts will refresh it automatically the next time you run them from WSL.
For browser-driven frontend tests in WSL, the scripts can drive a Windows Chrome/Edge executable through Playwright CDP if it is installed under the standard Windows paths. If you prefer a Linux browser inside WSL, you can still set `PLAYWRIGHT_BROWSER_PATH` to a Linux executable.

## Verification Modes

```bash
bash ./scripts/run-before-push.sh
bash ./scripts/run-before-push.sh be
bash ./scripts/run-before-push.sh fe
```

- `all` (default) - backend style, backend unit/integration tests, backend coverage, backend static analysis, secret scan, frontend style, frontend unit/component tests, frontend coverage, frontend E2E, and frontend build
- `be` / `backend` - backend style, unit/integration tests, coverage, and static analysis only
- `fe` / `frontend` - frontend style, unit/component tests, coverage, E2E, and build checks only

## Requirements

- Bash
- Java 21
- Node.js 24.16.0 LTS and npm 11.13.0
- Docker with Compose
- Git
- `curl`, `jq`, and `lsof`

See [`docs/implementation/development-environment.md`](../docs/implementation/development-environment.md) for the complete setup.
