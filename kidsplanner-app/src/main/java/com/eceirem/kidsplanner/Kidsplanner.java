/**

@file Kidsplanner.java
@brief Kids Activity Planner console application (menu + text mode).
@details Coordinates CRUD for kids and activities, overlap checks, storage switching, and privacy consent.
*/

/**

@package com.eceirem.kidsplanner
@brief The com.eceirem.kidsplanner package contains all the classes and files related to the Kidsplanner App.
*/
package com.eceirem.kidsplanner;

import java.io.IOException;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**

@class Kidsplanner
@brief Console controller for the Kids Activity Planner.
@details Uses arrow/Tab menus when a real console is attached; falls back to text commands for CI/tests.
*/
public class Kidsplanner {

  /**
   * Interactive menus when a real console is expected. {@code java -jar} from Windows {@code .bat}
   * often yields {@code System.console() == null} even though the user has a console; on Windows we
   * still enable menu mode unless {@link #PROP_TEXT_MODE} is {@code true} (set by Surefire for tests).
   */
  public static final String PROP_TEXT_MODE = "kidsplanner.text.mode";

  /**
   * When {@code true}, {@link #Kidsplanner()} builds a dumb JLine terminal over {@link System#in}/{@link System#out}
   * so menu mode can be exercised in automated tests (no real Windows console required).
   */
  public static final String PROP_MENU_MODE_TEST = "kidsplanner.menu.mode.test";

  /** Non-null only in text (CI / piped) mode — do not use with raw {@code System.in} when menus run. */
  private final Scanner scanner;
  /** Shared with {@link ConsoleUi} in menu mode so java.util.Scanner buffering does not steal keys from menus. */
  private final PushbackInputStream menuStdin;
  private final boolean menuMode;
  private final Terminal terminal;
  private final LineReader lineReader;
  private IRepository<Kid> kidRepository;
  private IRepository<Activity> activityRepository;

  /**
   * @return {@code false} when {@link #PROP_TEXT_MODE} is {@code true}; else {@code true} if
   *         {@code System.console()} is non-null, or on Windows (jar/bat console without Console API).
   */
  static boolean detectMenuMode() {
    if ("true".equals(System.getProperty(PROP_TEXT_MODE))) {
      return false;
    }
    if (System.console() != null) {
      return true;
    }
    String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    return os.contains("win");
  }

  public Kidsplanner() {
    if ("true".equalsIgnoreCase(System.getProperty(PROP_MENU_MODE_TEST))) {
      this.menuStdin = new PushbackInputStream(System.in, 64);
      this.scanner = null;
      Terminal t;
      try {
        t = TerminalBuilder.builder()
            .system(false)
            .streams(menuStdin, System.out)
            .dumb(true)
            .build();
      } catch (IOException e) {
        throw new IllegalStateException("menu mode test terminal", e);
      }
      this.terminal = t;
      this.lineReader = LineReaderBuilder.builder().terminal(t).build();
      this.menuMode = true;
      reloadRepositories();
      return;
    }
    boolean mm = detectMenuMode();
    if (mm) {
      this.menuStdin = new PushbackInputStream(System.in, 64);
      this.scanner = null;
      Terminal t = null;
      try {
        t = TerminalBuilder.builder().system(true).jna(true).build();
      } catch (Exception e) {
        t = null;
      }
      this.terminal = t;
      this.lineReader = t == null ? null : LineReaderBuilder.builder().terminal(t).build();
      this.menuMode = true;
    } else {
      this.menuStdin = null;
      this.scanner = new Scanner(System.in, StandardCharsets.UTF_8.name());
      this.menuMode = false;
      this.terminal = null;
      this.lineReader = null;
    }
    reloadRepositories();
  }

  public Kidsplanner(Scanner scanner, boolean menuMode) {
    if (menuMode) {
      throw new IllegalArgumentException("Menu mode uses default constructor only (real console).");
    }
    if (scanner == null) {
      throw new IllegalArgumentException("scanner required in text mode");
    }
    this.scanner = scanner;
    this.menuStdin = null;
    this.menuMode = false;
    this.terminal = null;
    this.lineReader = null;
    reloadRepositories();
  }

  private String nextLine() throws IOException {
    if (menuMode) {
      if (lineReader != null) {
        // menu input uses raw mode temporarily; line input should be normal line editing
        return lineReader.readLine("");
      }
      return ConsoleUi.readLineUtf8(menuStdin);
    }
    return scanner.nextLine();
  }

  private void reloadRepositories() {
    this.kidRepository = RepositoryFactory.createKidRepository();
    this.activityRepository = RepositoryFactory.createActivityRepository();
  }

  public void run() {
    try {
      PrivacyConsent.ensureInteractiveConsent(terminal, menuMode ? menuStdin : System.in, menuMode);
      if (menuMode) {
        runMenuMode();
      } else {
        runTextMode();
      }
    } catch (RepositoryException | ValidationException e) {
      System.out.println("Error: " + e.getMessage());
    } catch (IOException e) {
      System.out.println("Input error: " + e.getMessage());
    }
    System.out.println("Application closed.");
  }

  private String menuStatusLine() {
    int nk = kidRepository.findAll().size();
    int na = activityRepository.findAll().size();
    StorageType st = RepositoryFactory.getActiveStorage();
    return "Storage: " + st.name() + "  |  Kids: " + nk + "  |  Activities: " + na;
  }

  private void runMenuMode() throws IOException {
    List<String> main = Arrays.asList(
        "Kid management",
        "Activity management",
        "Storage (Binary / SQLite / MySQL)",
        "Course requirements (checklist)",
        "Exit");
    while (true) {
      int choice = ConsoleUi.readMenuChoice(terminal, menuStdin, System.out, "Main menu", main, menuStatusLine());
      if (choice == 0) {
        runKidMenu();
      } else if (choice == 1) {
        runActivityMenu();
      } else if (choice == 2) {
        runStorageMenu();
      } else if (choice == 3) {
        showHelpMenu();
        System.out.println("Press Enter to continue...");
        nextLine();
      } else {
        break;
      }
    }
  }

  private void runKidMenu() throws IOException {
    List<String> items = Arrays.asList("Add kid", "List kids", "Delete kid", "Back");
    while (true) {
      int choice = ConsoleUi.readMenuChoice(terminal, menuStdin, System.out, "Kid management", items, menuStatusLine());
      if (choice == 0) {
        addKidInteractive();
      } else if (choice == 1) {
        ConsoleUi.clearScreen(System.out);
        ConsoleUi.printBanner(System.out);
        ConsoleUi.title(System.out, "Registered kids");
        listKidsPlain();
        System.out.println("Press Enter to continue...");
        nextLine();
      } else if (choice == 2) {
        deleteKidInteractive();
      } else {
        break;
      }
    }
  }

  private void runActivityMenu() throws IOException {
    List<String> items = Arrays.asList(
        "Add activity",
        "List activities",
        "Update activity",
        "Delete activity",
        "Today's schedule (sorted)",
        "Back");
    while (true) {
      int choice = ConsoleUi.readMenuChoice(terminal, menuStdin, System.out, "Activity management", items, menuStatusLine());
      if (choice == 0) {
        addActivityInteractiveMenu();
      } else if (choice == 1) {
        ConsoleUi.clearScreen(System.out);
        ConsoleUi.printBanner(System.out);
        ConsoleUi.title(System.out, "All activities");
        listActivitiesPlain();
        System.out.println("Press Enter to continue...");
        nextLine();
      } else if (choice == 2) {
        updateActivityInteractive();
      } else if (choice == 3) {
        deleteActivityInteractive();
      } else if (choice == 4) {
        listTodayPlan();
        System.out.println("Press Enter to continue...");
        nextLine();
      } else {
        break;
      }
    }
  }

  private void runStorageMenu() throws IOException {
    List<String> items = Arrays.asList("Binary file", "SQLite", "MySQL", "Back");
    while (true) {
      String info = "Binary: .bin files | SQLite: kidsplanner.db | MySQL: database via Docker";
      int choice = ConsoleUi.readMenuChoice(terminal, menuStdin, System.out, "Storage engine", items, info);
      if (choice == 0) {
        switchStorageWithUi(StorageType.BINARY);
      } else if (choice == 1) {
        switchStorageWithUi(StorageType.SQLITE);
      } else if (choice == 2) {
        switchStorageWithUi(StorageType.MYSQL);
      } else {
        break;
      }
    }
  }

  private void switchStorageWithUi(StorageType target) throws IOException {
    ConsoleUi.clearScreen(System.out);
    ConsoleUi.printBanner(System.out);
    ConsoleUi.title(System.out, "Change storage");
    try {
      RepositoryFactory.setActiveStorage(target);
      reloadRepositories(); // may create schema
      ConsoleUi.successLine(System.out, "Active storage: " + target.name());
      System.out.println();
      explainStorage(target);
      System.out.println();
      System.out.println(RepositoryFactory.describeJdbcTargets());
      System.out.println("Warning: switching backends does not migrate data (each store is separate).");
    } catch (RepositoryException e) {
      RepositoryFactory.setActiveStorage(StorageType.BINARY);
      reloadRepositories();
      System.out.println("Error: " + e.getMessage());
      System.out.println();
      System.out.println("Suggested fix:");
      if (target == StorageType.MYSQL) {
        System.out.println("- Start MySQL with docker compose (docker-compose.yml in project root).");
        System.out.println("- Then choose Storage > MySQL again.");
      } else if (target == StorageType.SQLITE) {
        System.out.println("- kidsplanner.db is created in the working directory; check write permission.");
      } else {
        System.out.println("- Binary mode needs write access to .bin files in the project folder.");
      }
      System.out.println();
      System.out.println("Note: fell back to BINARY for a safe default.");
    }
    System.out.println();
    System.out.println("Press Enter to continue...");
    nextLine();
  }

  private void explainStorage(StorageType storage) {
    if (storage == StorageType.BINARY) {
      System.out.println("Binary (files): writes to kidsplanner-kids.bin and kidsplanner-activities.bin.");
      System.out.println("Pros: no setup. Cons: file-based, less flexible querying.");
    } else if (storage == StorageType.SQLITE) {
      System.out.println("SQLite (single-file DB): creates kid/activity tables in kidsplanner.db.");
      System.out.println("Pros: no server, full SQL. Cons: limited for large or multi-user workloads.");
    } else if (storage == StorageType.MYSQL) {
      System.out.println("MySQL (server DB): connects to MySQL (e.g. via Docker) and creates tables.");
      System.out.println("Pros: real server database. Cons: requires Docker/MySQL running (port 3306).");
    }
  }

  private void addKidInteractive() throws IOException {
    ConsoleUi.clearScreen(System.out);
    ConsoleUi.printBanner(System.out);
    ConsoleUi.title(System.out, "New kid");
    System.out.print("Display name: ");
    String name = nextLine().trim();
    System.out.print("Birth year (optional): ");
    String yearText = nextLine().trim();
    System.out.print("Notes (optional): ");
    String notes = nextLine();
    Integer year = null;
    if (!yearText.isEmpty()) {
      year = Integer.valueOf(Integer.parseInt(yearText));
    }
    Kid kid = new Kid(name, year, notes);
    if (kid.getDisplayName() == null || kid.getDisplayName().isEmpty()) {
      throw new ValidationException("Kid display name cannot be empty.");
    }
    kidRepository.save(kid);
    System.out.println("Kid saved. ID = " + kid.getId());
    System.out.println("Press Enter to continue...");
    nextLine();
  }

  private void deleteKidInteractive() throws IOException {
    ConsoleUi.clearScreen(System.out);
    ConsoleUi.printBanner(System.out);
    ConsoleUi.title(System.out, "Delete kid");
    listKidsPlain();
    System.out.print("Kid ID to delete: ");
    int id = Integer.parseInt(nextLine().trim());
    kidRepository.delete(id);
    System.out.println("Kid and related activities removed (if any).");
    System.out.println("Press Enter to continue...");
    nextLine();
  }

  private void addActivityInteractiveMenu() throws IOException {
    int kidId = pickKidIdForMenu();
    if (kidId < 0) {
      return;
    }
    ConsoleUi.clearScreen(System.out);
    ConsoleUi.printBanner(System.out);
    ConsoleUi.title(System.out, "New activity");
    Activity draft = readActivityBodyFromScanner(null, kidId);
    validateActivity(draft, -1);
    activityRepository.save(draft);
    System.out.println("Activity added. ID = " + draft.getId());
    System.out.println("Press Enter to continue...");
    nextLine();
  }

  private int pickKidIdForMenu() throws IOException {
    List<Kid> kids = new ArrayList<Kid>(kidRepository.findAll());
    if (kids.isEmpty()) {
      Kid auto = new Kid("Default kid", null, "");
      kidRepository.save(auto);
      kids.add(auto);
    }
    Collections.sort(kids, new Comparator<Kid>() {
      @Override
      public int compare(Kid a, Kid b) {
        return Integer.compare(a.getId(), b.getId());
      }
    });
    List<String> labels = new ArrayList<String>();
    for (Kid kid : kids) {
      labels.add(kid.getId() + " - " + kid.getDisplayName());
    }
    labels.add("Back (return to activity menu)");
    labels.add("+ New kid");
    int idx = ConsoleUi.readMenuChoice(terminal, menuStdin, System.out, "Select kid", labels, menuStatusLine());
    int idxGeri = labels.size() - 2;
    int idxYeni = labels.size() - 1;
    if (idx == idxYeni) {
      addKidInteractive();
      List<Kid> refreshed = kidRepository.findAll();
      Kid newest = Collections.max(refreshed, new Comparator<Kid>() {
        @Override
        public int compare(Kid a, Kid b) {
          return Integer.compare(a.getId(), b.getId());
        }
      });
      return newest.getId();
    }
    if (idx == idxGeri) {
      return -1;
    }
    return kids.get(idx).getId();
  }

  private void updateActivityInteractive() throws IOException {
    ConsoleUi.clearScreen(System.out);
    ConsoleUi.printBanner(System.out);
    ConsoleUi.title(System.out, "Update activity");
    listActivitiesPlain();
    System.out.print("Activity ID to update: ");
    int id = Integer.parseInt(nextLine().trim());
    Activity existing = activityRepository.findById(id);
    if (existing == null) {
      throw new ValidationException("ID not found: " + id);
    }
    Activity updated = readActivityFields(existing);
    updated.setId(id);
    validateActivity(updated, id);
    activityRepository.update(updated);
    System.out.println("Activity updated.");
    System.out.println("Press Enter to continue...");
    nextLine();
  }

  private void deleteActivityInteractive() throws IOException {
    ConsoleUi.clearScreen(System.out);
    ConsoleUi.printBanner(System.out);
    ConsoleUi.title(System.out, "Delete activity");
    listActivitiesPlain();
    System.out.print("Activity ID to delete: ");
    int id = Integer.parseInt(nextLine().trim());
    activityRepository.delete(id);
    System.out.println("Activity deleted (if it existed).");
    System.out.println("Press Enter to continue...");
    nextLine();
  }

  private Activity readActivityBodyFromScanner(Activity base, int kidId) throws IOException {
    String title = ask("Title", base == null ? "" : base.getTitle());
    if (title == null || title.trim().isEmpty()) {
      throw new ValidationException("Title cannot be empty.");
    }
    String dateText = ask("Date (yyyy-MM-dd)", base == null ? "" : base.getDate().toString());
    String startText = ask("Start (HH:mm)", base == null ? "" : base.getStartTime().toString());
    String endText = ask("End (HH:mm)", base == null ? "" : base.getEndTime().toString());
    String typeText = ask("Type (STUDY/PLAY/SPORT/ART/SLEEP/SOCIAL)",
        base == null ? ActivityType.STUDY.name() : base.getType().name());
    String location = ask("Location", base == null ? "" : base.getLocation());
    String notes = ask("Notes", base == null ? "" : base.getNotes());
    try {
      Activity activity = new Activity(
          title,
          LocalDate.parse(dateText),
          LocalTime.parse(startText),
          LocalTime.parse(endText),
          ActivityType.valueOf(typeText.toUpperCase(Locale.ROOT)),
          location,
          notes,
          Integer.valueOf(kidId));
      return activity;
    } catch (DateTimeParseException e) {
      throw new ValidationException("Invalid date/time format.");
    } catch (IllegalArgumentException e) {
      throw new ValidationException("Invalid activity type.");
    }
  }

  private void runTextMode() {
    System.out.println("=== Kids Activity Planner (text mode) ===");
    boolean running = true;
    while (running) {
      printTextMenu();
      String raw;
      try {
        raw = nextLine();
      } catch (IOException e) {
        System.out.println("Input error: " + e.getMessage());
        continue;
      }
      String cmd = raw.trim().toLowerCase(Locale.ROOT);
      try {
        if ("up".equals(cmd) || "tab".equals(cmd)) {
          System.out.println("Choose a command: open, add, update, delete, list, kid-add, kid-list, kid-delete, storage, exit");
          continue;
        }
        if (cmd.startsWith("storage")) {
          handleStorageCommand(cmd);
          continue;
        }
        if ("open".equals(cmd)) {
          showHelp();
        } else if ("kid-add".equals(cmd)) {
          addKidText();
        } else if ("kid-list".equals(cmd)) {
          listKidsPlain();
        } else if ("kid-delete".equals(cmd)) {
          deleteKidText();
        } else if ("add".equals(cmd)) {
          addActivity();
        } else if ("update".equals(cmd)) {
          updateActivity();
        } else if ("delete".equals(cmd)) {
          deleteActivity();
        } else if ("list".equals(cmd)) {
          listActivities();
        } else if ("exit".equals(cmd)) {
          running = false;
        } else if (cmd.isEmpty()) {
          System.out.println("Invalid command. open/add/update/delete/list/kid-add/kid-list/kid-delete/storage/exit");
        } else {
          System.out.println("Invalid command. open/add/update/delete/list/kid-add/kid-list/kid-delete/storage/exit");
        }
      } catch (RepositoryException | ValidationException e) {
        System.out.println("Error: " + e.getMessage());
      } catch (NumberFormatException e) {
        System.out.println("Error: invalid number format.");
      } catch (IOException e) {
        System.out.println("Input error: " + e.getMessage());
      }
    }
  }

  private void handleStorageCommand(String cmd) {
    String[] parts = cmd.split("\\s+");
    if (parts.length < 2) {
      System.out.println("Usage: storage binary|sqlite|mysql");
      return;
    }
    StorageType parsed = RepositoryFactory.parseStorageTypeOrNull(parts[1]);
    if (parsed == null) {
      System.out.println("Invalid storage. Use binary, sqlite, or mysql.");
      return;
    }
    RepositoryFactory.setActiveStorage(parsed);
    reloadRepositories();
    System.out.println("Storage updated: " + parsed);
    System.out.println(RepositoryFactory.describeJdbcTargets());
  }

  private void addKidText() throws IOException {
    System.out.print("Display name: ");
    String name = nextLine().trim();
    System.out.print("Birth year (optional): ");
    String yearText = nextLine().trim();
    System.out.print("Notes (optional): ");
    String notes = nextLine();
    Integer year = null;
    if (!yearText.isEmpty()) {
      year = Integer.valueOf(Integer.parseInt(yearText));
    }
    Kid kid = new Kid(name, year, notes);
    if (kid.getDisplayName() == null || kid.getDisplayName().isEmpty()) {
      throw new ValidationException("Kid display name cannot be empty.");
    }
    kidRepository.save(kid);
    System.out.println("Kid added. ID = " + kid.getId());
  }

  private void deleteKidText() throws IOException {
    System.out.print("Kid ID to delete: ");
    int id = Integer.parseInt(nextLine().trim());
    kidRepository.delete(id);
    System.out.println("Kid deleted (if it existed).");
  }

  private void printTextMenu() {
    System.out.println();
    System.out.println("Commands (no number entry):");
    System.out.println("- open      : Help / reference");
    System.out.println("- add       : Add activity");
    System.out.println("- update    : Update activity");
    System.out.println("- delete    : Delete activity");
    System.out.println("- list      : List activities");
    System.out.println("- kid-add   : Add kid");
    System.out.println("- kid-list  : List kids");
    System.out.println("- kid-delete: Delete kid");
    System.out.println("- storage X : Storage (binary|sqlite|mysql)");
    System.out.println("- exit      : Exit");
    System.out.print(">> ");
  }

  private void showHelp() {
    System.out.println("Date format: yyyy-MM-dd");
    System.out.println("Time format: HH:mm");
    System.out.println("Types      : STUDY, PLAY, SPORT, ART, SLEEP, SOCIAL");
    System.out.println("Overlap rule: same kid + same day — time ranges must not intersect.");
    System.out.println("MySQL: start MySQL with docker compose (docker-compose.yml in project root).");
    System.out.println(RepositoryFactory.describeJdbcTargets());
  }

  private void showHelpMenu() {
    ConsoleUi.clearScreen(System.out);
    ConsoleUi.printBanner(System.out);
    ConsoleUi.title(System.out, "Course requirements (checklist)");
    ConsoleUi.muted(System.out, "This screen lists course requirements and what the project implements.");
    ConsoleUi.rule(System.out);
    System.out.println("  [OK] Keyboard-driven menus (arrows/Tab/Enter) — no numeric selection");
    System.out.println("  [OK] CRUD screens (kids + activities)");
    System.out.println("  [OK] Validation / rules (time overlap prevention)");
    System.out.println("  [OK] Persistent storage: Binary + SQLite + MySQL (switch at runtime)");
    System.out.println("  [OK] MySQL via Docker Compose (docker-compose.yml)");
    System.out.println("  [OK] Unit tests: JUnit 5 (text mode for automation)");
    System.out.println("  [OK] Code coverage: JaCoCo report (target/site/jacoco)");
    ConsoleUi.rule(System.out);
    System.out.println("  Format summary");
    System.out.println("  - Date : yyyy-MM-dd");
    System.out.println("  - Time : HH:mm");
    System.out.println("  - Types: STUDY, PLAY, SPORT, ART, SLEEP, SOCIAL");
    System.out.println();
    System.out.println("  Note: if Storage > MySQL shows schema errors, start MySQL with Docker first.");
  }

  private void addActivity() throws IOException {
    Activity activity = readActivityFields(null);
    validateActivity(activity, -1);
    activityRepository.save(activity);
    System.out.println("Activity added. ID = " + activity.getId());
  }

  private void updateActivity() throws IOException {
    System.out.print("ID to update: ");
    int id = Integer.parseInt(nextLine().trim());
    Activity existing = activityRepository.findById(id);
    if (existing == null) {
      throw new ValidationException("ID not found: " + id);
    }
    Activity updated = readActivityFields(existing);
    updated.setId(id);
    validateActivity(updated, id);
    activityRepository.update(updated);
    System.out.println("Activity updated.");
  }

  private void deleteActivity() throws IOException {
    System.out.print("ID to delete: ");
    int id = Integer.parseInt(nextLine().trim());
    activityRepository.delete(id);
    System.out.println("Activity deleted (if it existed).");
  }

  private void listActivities() {
    List<Activity> activities = new ArrayList<Activity>(activityRepository.findAll());
    sortActivities(activities);
    if (menuMode) {
      ConsoleUi.printActivityTable(System.out, activities);
    } else {
      System.out.println("--- Activity list ---");
      for (Activity activity : activities) {
        System.out.println(activity);
      }
      if (activities.isEmpty()) {
        System.out.println("(no records)");
      }
    }
  }

  private void listActivitiesPlain() {
    listActivities();
  }

  private void listKidsPlain() {
    List<Kid> kids = new ArrayList<Kid>(kidRepository.findAll());
    Collections.sort(kids, new Comparator<Kid>() {
      @Override
      public int compare(Kid a, Kid b) {
        return Integer.compare(a.getId(), b.getId());
      }
    });
    if (menuMode) {
      ConsoleUi.printKidTable(System.out, kids);
    } else {
      System.out.println("--- Kid list ---");
      for (Kid kid : kids) {
        System.out.println(kid);
      }
      if (kids.isEmpty()) {
        System.out.println("(no kids)");
      }
    }
  }

  private void listTodayPlan() {
    LocalDate today = LocalDate.now();
    List<Activity> activities = new ArrayList<Activity>();
    for (Activity activity : activityRepository.findAll()) {
      if (today.equals(activity.getDate())) {
        activities.add(activity);
      }
    }
    sortActivities(activities);
    if (menuMode) {
      ConsoleUi.clearScreen(System.out);
      ConsoleUi.printBanner(System.out);
      ConsoleUi.title(System.out, "Today's schedule - " + today);
      if (activities.isEmpty()) {
        ConsoleUi.muted(System.out, "(no entries for today)");
      } else {
        ConsoleUi.printActivityTable(System.out, activities);
      }
    } else {
      System.out.println("--- Today's schedule (" + today + ") ---");
      for (Activity activity : activities) {
        System.out.println(activity);
      }
      if (activities.isEmpty()) {
        System.out.println("(no entries for today)");
      }
    }
  }

  private void sortActivities(List<Activity> activities) {
    Collections.sort(activities, new Comparator<Activity>() {
      @Override
      public int compare(Activity a1, Activity a2) {
        int dateCompare = a1.getDate().compareTo(a2.getDate());
        if (dateCompare != 0) {
          return dateCompare;
        }
        int startCompare = a1.getStartTime().compareTo(a2.getStartTime());
        if (startCompare != 0) {
          return startCompare;
        }
        return Integer.compare(a1.getId(), a2.getId());
      }
    });
  }

  private Activity readActivityFields(Activity base) throws IOException {
    String kidDefault = "";
    if (base != null && base.getKidId() != null) {
      kidDefault = base.getKidId().toString();
    }
    String kidLine = ask("Kid ID (empty = auto)", kidDefault);
    int kidId = resolveKidIdFromLine(kidLine);
    String title = ask("Title", base == null ? "" : base.getTitle());
    if (title == null || title.trim().isEmpty()) {
      throw new ValidationException("Title cannot be empty.");
    }
    String dateText = ask("Date (yyyy-MM-dd)", base == null ? "" : base.getDate().toString());
    String startText = ask("Start (HH:mm)", base == null ? "" : base.getStartTime().toString());
    String endText = ask("End (HH:mm)", base == null ? "" : base.getEndTime().toString());
    String typeText = ask("Type (STUDY/PLAY/SPORT/ART/SLEEP/SOCIAL)",
        base == null ? ActivityType.STUDY.name() : base.getType().name());
    String location = ask("Location", base == null ? "" : base.getLocation());
    String notes = ask("Notes", base == null ? "" : base.getNotes());
    try {
      return new Activity(
          title,
          LocalDate.parse(dateText),
          LocalTime.parse(startText),
          LocalTime.parse(endText),
          ActivityType.valueOf(typeText.toUpperCase(Locale.ROOT)),
          location,
          notes,
          Integer.valueOf(kidId));
    } catch (DateTimeParseException e) {
      throw new ValidationException("Invalid date/time format.");
    } catch (IllegalArgumentException e) {
      throw new ValidationException("Invalid activity type.");
    }
  }

  private int resolveKidIdFromLine(String line) {
    String trimmed = line == null ? "" : line.trim();
    if (trimmed.isEmpty()) {
      List<Kid> kids = kidRepository.findAll();
      if (kids.isEmpty()) {
        Kid created = new Kid("Default kid", null, "");
        kidRepository.save(created);
        return created.getId().intValue();
      }
      List<Kid> sorted = new ArrayList<Kid>(kids);
      Collections.sort(sorted, new Comparator<Kid>() {
        @Override
        public int compare(Kid a, Kid b) {
          return Integer.compare(a.getId(), b.getId());
        }
      });
      return sorted.get(0).getId().intValue();
    }
    int id = Integer.parseInt(trimmed);
    if (kidRepository.findById(id) == null) {
      throw new ValidationException("Kid not found: " + id);
    }
    return id;
  }

  private String ask(String label, String defaultValue) throws IOException {
    String def = defaultValue == null ? "" : defaultValue;
    System.out.print(label + (def.isEmpty() ? "" : " [" + def + "]") + ": ");
    String value = nextLine().trim();
    return value.isEmpty() ? def : value;
  }

  private void validateActivity(Activity candidate, int currentId) {
    if (candidate.getTitle() == null || candidate.getTitle().trim().isEmpty()) {
      throw new ValidationException("Title cannot be empty.");
    }
    if (candidate.getKidId() == null) {
      throw new ValidationException("A kid must be selected.");
    }
    if (!candidate.getStartTime().isBefore(candidate.getEndTime())) {
      throw new ValidationException("Start time must be before end time.");
    }
    List<Activity> all = activityRepository.findAll();
    for (Activity activity : all) {
      if (activity.getId() != null && activity.getId() == currentId) {
        continue;
      }
      if (!Objects.equals(activity.getKidId(), candidate.getKidId())) {
        continue;
      }
      if (!activity.getDate().equals(candidate.getDate())) {
        continue;
      }
      if (overlaps(activity.getStartTime().toSecondOfDay(), activity.getEndTime().toSecondOfDay(),
          candidate.getStartTime().toSecondOfDay(), candidate.getEndTime().toSecondOfDay())) {
        throw new ValidationException("Time overlap on the same day. Conflicting ID: " + activity.getId());
      }
    }
  }

  private boolean overlaps(int startA, int endA, int startB, int endB) {
    return startA < endB && startB < endA;
  }
}
