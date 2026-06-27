# SonarQube Stack

This stack runs a local SonarQube server and its PostgreSQL database for backend code-quality analysis.

## Start the stack

From the repo root:

```powershell
docker compose -p weblink-pilot-sonar -f infra/sonar/docker-compose.yml up -d
```

On macOS/Linux:

```bash
docker compose -p weblink-pilot-sonar -f infra/sonar/docker-compose.yml up -d
```

The UI is available at:

- `http://localhost:9001`

Default local credentials:

- username: `admin`
- password: `admin`

SonarQube asks you to change the password after the first login.

## Run the analysis

From `backend/`, after the SonarQube stack is up:

```powershell
$env:SONAR_TOKEN = "<your-token>"
$env:SONAR_HOST_URL = "http://localhost:9001"
.\mvnw.cmd -Pci clean install sonar:sonar -Dsonar.token=$env:SONAR_TOKEN -Dsonar.host.url=$env:SONAR_HOST_URL
```

On macOS/Linux:

```bash
export SONAR_TOKEN="<your-token>"
export SONAR_HOST_URL="http://localhost:9001"
./mvnw -Pci clean install sonar:sonar -Dsonar.token="$SONAR_TOKEN" -Dsonar.host.url="$SONAR_HOST_URL"
```

The Maven build already knows where the aggregate JaCoCo XML report lives:

- `coverage/target/site/jacoco-aggregate/jacoco.xml`

If you do not want to type the Maven command manually, use the helper script from the repo root:

- WSL/Linux/macOS: `bash ./scripts/quality/sonar-analysis.sh`
- Windows PowerShell: `wsl bash ./scripts/quality/sonar-analysis.sh`

You can also put the token in a local-only `infra/sonar/.env` file:

```bash
SONAR_TOKEN=your-token-here
SONAR_HOST_URL=http://localhost:9001
```

## Notes

- This is a local-dev Sonar setup, not a hosted SonarCloud configuration.
- If you run into Docker memory or Elasticsearch bootstrap warnings, restart Docker Desktop first.
- GitHub Actions Sonar is currently disabled; run the local analysis script instead.
