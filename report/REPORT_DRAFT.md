# CEN206 — Midterm Report Draft (Kids Activity Planner)

> This file is a **draft** you can copy into the course `.docx` template.
> Replace placeholders like **TODO** and update names/IDs/dates.

## Cover Page (Template Fields)

- **Course**: CEN206 — Object-Oriented Programming (Java)
- **Project**: Kids Activity Planner (Console)
- **Team / Student**: **TODO: Name Surname**
- **Student ID**: **TODO**
- **Repository URL**: **TODO: https://github.com/<user>/cen206-...-java**
- **Release Tag**: `midterm-v1.0.0`
- **Date**: **TODO**

---

## 1. Executive Summary

Kids Activity Planner is a console-based planning tool that allows a parent/caregiver to manage children (Kids) and their scheduled Activities. The system focuses on a clean domain model, strict validation (including time overlap rules), and a pluggable persistence layer that can switch between **Binary**, **SQLite**, and **MySQL** backends at runtime via a repository abstraction.

---

## 2. Problem Statement & Goals

### 2.1 Problem

Planning activities for multiple children becomes error-prone when schedules overlap, data is scattered, or changes are not tracked consistently.

### 2.2 Goals

- Provide fast CRUD operations for kids and activities.
- Prevent invalid schedules (time overlap, invalid time ranges).
- Persist data across restarts with switchable storage backends.
- Achieve high test coverage and produce documentation & design artifacts.

---

## 3. Requirements Mapping (Midterm)

### 3.1 Console UI

- Menu-driven console interaction
- Keyboard-friendly navigation (where supported)
- Clear prompts, validation messages, and error handling

### 3.2 Modular Architecture

- Domain types separated from persistence and UI responsibilities.
- Persistence behind an abstraction (`IRepository<T>`).

### 3.3 Storage Layer (IRepository Pattern)

**Repository Abstraction**

- `IRepository<T>` provides common CRUD operations.
- `RepositoryFactory` selects active backend and can switch at runtime.

**Backends**

- `BinaryRepository<T>`: file-based serialization.
- `SqliteRepository<T>`: JDBC-based local database.
- `MysqlRepository<T>`: JDBC + Docker Compose MySQL.

---

## 4. Domain Model (OOP)

### 4.1 Main Entities

- **Kid**
  - Represents a child profile used as the owner of activities.
- **Activity**
  - Represents a scheduled activity for a kid on a specific date and time range.
- **ActivityType**
  - Enum to categorize activities.

### 4.2 Key Rules

- An activity’s `endTime` must be after `startTime`.
- Two activities for the same kid on the same date cannot overlap in time.

### 4.3 OOP Principles Evidence

- **Encapsulation**: fields are private with controlled access.
- **Abstraction**: persistence via `IRepository<T>`.
- **Polymorphism**: runtime selection of repository implementations.
- **(Optional) Inheritance**: **TODO: if any inheritance exists, describe it**; otherwise justify using composition/interfaces.

---

## 5. Architecture Overview

### 5.1 Layers

- **UI Layer**: Console UI / menu flow.
- **Application / Service Layer**: Coordination logic (validation, overlap checks).
- **Persistence Layer**: repository interface + backend implementations.

### 5.2 Runtime Storage Switching

User can select the active storage backend (Binary / SQLite / MySQL). The app uses the selected repository without changing domain logic.

---

## 6. Testing & Coverage

### 6.1 Test Strategy

- Unit tests focus on domain rules and repository factory selection.
- Negative tests cover invalid inputs and overlap scenarios.

### 6.2 Tools

- **JUnit 5** for unit tests
- **JaCoCo** for test coverage

### 6.3 Evidence

- Include a screenshot or exported report showing coverage is **TODO: 100%** in the required metrics.
- Mention CI status (GitHub Actions release workflow).

---

## 7. Documentation (Doxygen + Coverage)

### 7.1 Doxygen Output

- HTML entry: `kidsplanner-app/target/site/doxygen/html/index.html`
- PDF entry (if generated): `docs/doxygen/refman.pdf`

### 7.2 Documentation Coverage

- Coverxygen is used to measure documentation coverage based on Doxygen XML.
- **TODO**: Include final doc coverage percentage and ensure it meets the required threshold (target: 100%).

---

## 8. Design Artifacts (Design-First)

All design artifacts are stored under `design/`.

### 8.1 PlantUML

- Class diagram: `design/plantuml/class.puml`
- ER diagram: `design/plantuml/er.puml`
- Use-case: `design/plantuml/usecase.puml`
- Sequence diagrams: `design/plantuml/seq-*.puml`
- Gantt: `design/plantuml/gantt.puml`

### 8.2 C4 Model

- Context / Container / Component: `design/c4/*.puml`

### 8.3 UMPLE

- Domain model: `design/umple/model.ump` (**TODO: verify path**)

### 8.4 Figma

- Console wireframes PDF: `design/figma/console-wireframes.pdf`

### 8.5 draw.io

- Deployment diagram: `design/drawio/*.drawio` (**TODO: verify file name**)

### 8.6 ProjectLibre

- Project plan: `design/projectlibre/project.xml`

---

## 9. CI/CD & Release

### 9.1 Workflow

- GitHub Actions release workflow runs build, tests, coverage checks, documentation generation, and packages artifacts.

### 9.2 Release Evidence

- Release tag: `midterm-v1.0.0`
- Assets: runnable JAR + coverage and documentation archives.

---

## 10. Constraints & Standards (MÜDEK 5.5 — PÇ.3)

**Chosen constraint**: **TODO (example: time pressure + accessibility)**  

- **Accessibility**: console UI output clarity and predictable navigation.
- **Time pressure**: short iteration cycles with automated checks via CI.

**Standard reference**: **TODO** (example: ISO/IEC 25010 quality attributes such as reliability, usability, maintainability).

---

## 11. Known Issues / Limitations

- **TODO**: list any known limitations (e.g., MySQL requires Docker running; console ANSI support varies on Windows).

---

## 12. Appendix

- Repository tree snapshot (optional)
- Screenshots: console flow, coverage report, Doxygen main page

