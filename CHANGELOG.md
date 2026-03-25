# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added

- `docs/doxygen/refman.pdf`: Doxygen LaTeX output (stable, tracked path in the repo). During `mvn site`, this file is also copied to `target/site/refman-api.pdf`.

## [1.0.0] - 2026-03-25

CEN206 midterm submission; GitHub tag: `midterm-v1.0.0`.

### Added

- Console **Kids Activity Planner**: kid and activity management (CRUD), overlap checks, and validation rules.
- Persistence: **Binary**, **SQLite**, **MySQL** (`IRepository`, `RepositoryFactory`, MySQL via Docker Compose).
- Design artifacts: `design/` — PlantUML (class, ER, use case, Gantt, sequence), C4 (context/container/component), Umple (`model.ump`), draw.io deployment diagram, ProjectLibre, Figma wireframe PDF.
- **JUnit 5** tests (20+), JaCoCo report; API documentation generation via the root `Doxyfile`.
- GitHub Actions: release on `midterm-v*` tag push (JAR, JaCoCo and Surefire report archives).

[1.0.0]: https://github.com/eceiremsas/cen206-2025-2026-16-eceirem-sas-java/releases/tag/midterm-v1.0.0
