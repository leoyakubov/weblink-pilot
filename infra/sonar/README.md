# SonarQube Stack

This stack runs a local SonarQube server and its PostgreSQL database for backend code-quality analysis.

## Start the stack

From the repo root:

```powershell
docker compose -f infra/sonar/docker-compose.yml up -d
```

On macOS/Linux:

```bash
docker compose -f infra/sonar/docker-compose.yml up -d
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
.\mvnw.cmd -Pci clean install sonar:sonar -Dsonar.token=$env:SONAR_TOKEN
```

On macOS/Linux:

```bash
export SONAR_TOKEN="<your-token>"
./mvnw -Pci clean install sonar:sonar -Dsonar.token="$SONAR_TOKEN"
```

The Maven build already knows where the aggregate JaCoCo XML report lives:

- `coverage/target/site/jacoco-aggregate/jacoco.xml`

If you do not want to type the Maven command manually, use the helper script from the repo root:

- Windows: `.\scripts\sonar\run-sonar-analysis.ps1`
- Unix: `./scripts/sonar/run-sonar-analysis.sh`

You can also put the token in a local-only `.env.local` file at the repo root:

```bash
SONAR_TOKEN=your-token-here
```

## Notes

- This is a local-dev Sonar setup, not a hosted SonarCloud configuration.
- If you run into Docker memory or Elasticsearch bootstrap warnings, restart Docker Desktop first.
