/**
@file doxygen-main.md
@brief Doxygen main page source (Markdown).
*/

/**
@page mainpage Kids Activity Planner

Console application for planning children’s activities: domain model (kid, activity,
privacy consent), three interchangeable storage backends (binary file, SQLite, MySQL via
Docker), and keyboard-driven console UI.
*/

# Kids Activity Planner

Console application for planning children’s activities: domain model (kid, activity,
privacy consent), three interchangeable storage backends (binary file, SQLite, MySQL via
Docker), and keyboard-driven console UI.

## Browse the API

Use the **Packages**, **Classes**, and **Files** tabs in the header to open the generated
reference. Notable entry points:

- Application: `com.eceirem.kidsplanner.KidsplannerApp`
- Domain and repositories: types in package `com.eceirem.kidsplanner`

## Build and docs

Build with Maven from the `kidsplanner-app` module. Regenerate this HTML with Doxygen from the
repository root (`doxygen Doxyfile`).
