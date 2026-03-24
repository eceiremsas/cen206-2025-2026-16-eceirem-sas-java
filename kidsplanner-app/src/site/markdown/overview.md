# Kids Activity Planner (CEN206 Midterm)

Kids Activity Planner is a Java **console** application for managing child profiles and daily activities.

## Features

- **Child management**: Minimal-profile child records (KVKK / GDPR-style data minimization narrative).
- **Activity scheduling**: Date and time range, type (STUDY, PLAY, SPORT, ART, SLEEP, SOCIAL), location, and notes.
- **Conflict detection**: For the same child on the same day, time ranges must not overlap (interval overlap check).
- **Pluggable storage (runtime)**: Binary serialization, SQLite (JDBC), MySQL (via Docker Compose).
- **Console UI**: Arrow-key / Tab menus in a real terminal; plain text command mode for CI and automated tests.

## Run

```bash
cd kidsplanner-app
mvn -q -DskipTests package
java -jar target/kidsplanner-app-1.0-SNAPSHOT.jar
```

For MySQL, start the database with `docker-compose.yml` at the repository root. Optional environment variables: `KIDSPLANNER_MYSQL_URL`, `KIDSPLANNER_MYSQL_USER`, `KIDSPLANNER_MYSQL_PASSWORD` (defaults: `root` / `kidsplanner`).

Default SQLite file: `kidsplanner.db` (created in the working directory).

## Documentation

Maven site: `mvn site` (content under `src/site`).
