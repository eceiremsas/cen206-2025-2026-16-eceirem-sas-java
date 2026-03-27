/**
 * @file KidsplannerMenuFlowCoverageTest.java
 * @brief Covers Kidsplanner menu-mode flows deterministically (no real terminal).
 */
package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class KidsplannerMenuFlowCoverageTest {

  @AfterEach
  public void cleanup() throws Exception {
    Files.deleteIfExists(Paths.get("kidsplanner-activities.bin"));
    Files.deleteIfExists(Paths.get("kidsplanner-kids.bin"));
    Files.deleteIfExists(Paths.get("kidsplanner-consent.flag"));
    Files.deleteIfExists(Paths.get("kidsplanner.db"));
    // Reset global factory config so tests do not leak state.
    RepositoryFactory.setActiveStorage(StorageType.BINARY);
    RepositoryFactory.configureSqliteUrl("jdbc:sqlite:kidsplanner.db");
    RepositoryFactory.configureMysql(
        "jdbc:mysql://localhost:3306/kidsplanner?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
        "root",
        "kidsplanner");
    System.clearProperty(Kidsplanner.PROP_TEXT_MODE);
    System.clearProperty(Kidsplanner.PROP_DISABLE_JLINE);
    System.clearProperty(Kidsplanner.PROP_FORCE_MENU);
  }

  private String runMenuWithInput(byte[] inputBytes) throws Exception {
    InputStream originalIn = System.in;
    PrintStream originalOut = System.out;
    ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8.name());
    try {
      System.setProperty(Kidsplanner.PROP_TEXT_MODE, "false");
      System.setProperty(Kidsplanner.PROP_DISABLE_JLINE, "true");
      System.setProperty(Kidsplanner.PROP_FORCE_MENU, "true");
      System.setIn(new ByteArrayInputStream(inputBytes));
      System.setOut(out);
      new Kidsplanner().run();
    } finally {
      System.setIn(originalIn);
      System.setOut(originalOut);
      out.close();
    }
    return outBytes.toString(StandardCharsets.UTF_8.name());
  }

  @Test
  public void menuModeShowsEmptyTodayScheduleThenExit() throws Exception {
    ByteArrayOutputStream in = new ByteArrayOutputStream();
    // Consent: Enter on YES
    in.write('\n');
    // Main: Tab Enter => Activity management
    in.write('\t');
    in.write('\n');
    // Activity: Tab x4 Enter => Today's schedule (sorted)
    for (int i = 0; i < 4; i++) {
      in.write('\t');
    }
    in.write('\n');
    // Continue
    in.write('\n');
    // Activity: Back (Tab x5 Enter)
    for (int i = 0; i < 5; i++) {
      in.write('\t');
    }
    in.write('\n');
    // Main: Exit (Tab x4 Enter)
    for (int i = 0; i < 4; i++) {
      in.write('\t');
    }
    in.write('\n');

    String printed = runMenuWithInput(in.toByteArray());
    assertTrue(printed.contains("Today's schedule"));
    assertTrue(printed.contains("Application closed."));
  }

  @Test
  public void menuModeCoversKidAndActivityCrudAndHelp() throws Exception {
    String today = LocalDate.now().toString();
    ByteArrayOutputStream in = new ByteArrayOutputStream();

    // Consent: Enter on YES
    in.write('\n');

    // Main: Enter => Kid management
    in.write('\n');

    // Kid menu: Enter => Add kid
    in.write('\n');
    in.write(("Bob\n2017\nnote\n\n").getBytes(StandardCharsets.UTF_8));

    // Kid menu: Tab Enter => List kids, then continue
    in.write('\t');
    in.write('\n');
    in.write('\n');

    // Kid menu: Tab x2 Enter => Delete kid, provide ID, continue
    in.write('\t');
    in.write('\t');
    in.write('\n');
    in.write(("1\n\n").getBytes(StandardCharsets.UTF_8));

    // Kid menu: Tab x3 Enter => Back
    for (int i = 0; i < 3; i++) {
      in.write('\t');
    }
    in.write('\n');

    // Main: Tab Enter => Activity management
    in.write('\t');
    in.write('\n');

    // Activity: Enter => Add activity
    in.write('\n');
    // Kid select: Enter => select default kid (auto-created when no kids exist)
    in.write('\n');
    // Activity fields + continue
    in.write(("Math\n" + today + "\n09:00\n10:00\nSTUDY\nHome\nNote\n\n").getBytes(StandardCharsets.UTF_8));

    // Activity: Tab Enter => List activities, continue
    in.write('\t');
    in.write('\n');
    in.write('\n');

    // Activity: Tab x2 Enter => Update activity; choose id=1; update title; keep other defaults; continue
    in.write('\t');
    in.write('\t');
    in.write('\n');
    in.write(("1\n").getBytes(StandardCharsets.UTF_8));
    // readActivityFields prompts: kidId, title, date, start, end, type, location, notes
    in.write(("\nNew\n\n\n\n\n\n\n").getBytes(StandardCharsets.UTF_8));
    in.write('\n');

    // Activity: Tab x4 Enter => Today's schedule; continue
    for (int i = 0; i < 4; i++) {
      in.write('\t');
    }
    in.write('\n');
    in.write('\n');

    // Activity: Tab x3 Enter => Delete activity; id=1; continue
    for (int i = 0; i < 3; i++) {
      in.write('\t');
    }
    in.write('\n');
    in.write(("1\n\n").getBytes(StandardCharsets.UTF_8));

    // Activity: Back (Tab x5 Enter)
    for (int i = 0; i < 5; i++) {
      in.write('\t');
    }
    in.write('\n');

    // Main: Tab x3 Enter => Course requirements (checklist), continue
    for (int i = 0; i < 3; i++) {
      in.write('\t');
    }
    in.write('\n');
    in.write('\n');

    // Main: Exit (Tab x4 Enter)
    for (int i = 0; i < 4; i++) {
      in.write('\t');
    }
    in.write('\n');

    String printed = runMenuWithInput(in.toByteArray());
    assertTrue(printed.contains("Kid saved"));
    assertTrue(printed.contains("Registered kids"));
    assertTrue(printed.contains("All activities"));
    assertTrue(printed.contains("Activity added"));
    assertTrue(printed.contains("Today's schedule"));
    assertTrue(printed.contains("Course requirements"));
    assertTrue(printed.contains("Application closed."));
  }

  @Test
  public void menuModeAddActivityWithNewKidCoversNewestKidSelectionBranch() throws Exception {
    String today = LocalDate.now().toString();
    ByteArrayOutputStream in = new ByteArrayOutputStream();

    // Consent: Enter on YES
    in.write('\n');

    // Main: Tab Enter => Activity management
    in.write('\t');
    in.write('\n');

    // Activity: Enter => Add activity
    in.write('\n');

    // Kid select menu: should auto-create one kid if none.
    // Labels: [kid0], Back, + New kid. Select "+ New kid" => Tab x2 Enter
    in.write('\t');
    in.write('\t');
    in.write('\n');

    // New kid prompts + continue
    in.write(("Zoe\n2019\nn\n\n").getBytes(StandardCharsets.UTF_8));

    // Activity fields + continue
    in.write(("A\n" + today + "\n08:00\n09:00\nPLAY\nPark\n-\n\n").getBytes(StandardCharsets.UTF_8));

    // Activity: Back (Tab x5 Enter)
    for (int i = 0; i < 5; i++) {
      in.write('\t');
    }
    in.write('\n');

    // Main: Exit (Tab x4 Enter)
    for (int i = 0; i < 4; i++) {
      in.write('\t');
    }
    in.write('\n');

    String printed = runMenuWithInput(in.toByteArray());
    assertTrue(printed.contains("New kid"));
    assertTrue(printed.contains("Kid saved"));
    assertTrue(printed.contains("Activity added"));
    assertTrue(printed.contains("Application closed."));
  }

  @Test
  public void menuModeStorageSwitchToMisconfiguredMysqlHitsErrorFallback() throws Exception {
    // Force a MySQL config that will fail to connect quickly, to cover the UI error/fallback path.
    RepositoryFactory.configureMysql(
        "jdbc:mysql://localhost:3307/kidsplanner?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=500&socketTimeout=500",
        "root",
        "kidsplanner");
    RepositoryFactory.setActiveStorage(StorageType.BINARY);

    ByteArrayOutputStream in = new ByteArrayOutputStream();
    // Consent: Enter on YES
    in.write('\n');
    // Main: Tab x2 Enter => Storage engine
    in.write('\t');
    in.write('\t');
    in.write('\n');
    // Storage: Tab x2 Enter => MySQL (will fail), then press Enter to continue
    in.write('\t');
    in.write('\t');
    in.write('\n');
    in.write('\n');
    // Storage: Back (Tab x3 Enter)
    for (int i = 0; i < 3; i++) {
      in.write('\t');
    }
    in.write('\n');
    // Main: Exit (Tab x4 Enter)
    for (int i = 0; i < 4; i++) {
      in.write('\t');
    }
    in.write('\n');

    String printed = runMenuWithInput(in.toByteArray());
    assertTrue(printed.contains("Suggested fix:"));
    assertTrue(printed.contains("fell back to BINARY"));
    assertTrue(printed.contains("Application closed."));
  }

  @Test
  public void menuModeAddKidEmptyNameTriggersValidationCaughtInRun() throws Exception {
    ByteArrayOutputStream in = new ByteArrayOutputStream();
    // Consent: Enter on YES
    in.write('\n');
    // Main: Enter => Kid management
    in.write('\n');
    // Kid menu: Enter => Add kid
    in.write('\n');
    // Display name empty => ValidationException
    in.write('\n');

    String printed = runMenuWithInput(in.toByteArray());
    assertTrue(printed.contains("Error:"));
    assertTrue(printed.toLowerCase().contains("display name"));
    assertTrue(printed.contains("Application closed."));
  }

  @Test
  public void menuModeStorageSwitchToSqliteWithBadPathPrintsSqliteSuggestion() throws Exception {
    RepositoryFactory.configureSqliteUrl("jdbc:sqlite:Z:\\\\this\\\\path\\\\does\\\\not\\\\exist\\\\kidsplanner.db");
    RepositoryFactory.setActiveStorage(StorageType.BINARY);

    ByteArrayOutputStream in = new ByteArrayOutputStream();
    // Consent: Enter on YES
    in.write('\n');
    // Main: Tab x2 Enter => Storage engine
    in.write('\t');
    in.write('\t');
    in.write('\n');
    // Storage: Tab Enter => SQLite (index 1)
    in.write('\t');
    in.write('\n');
    // Continue
    in.write('\n');
    // Storage: Back (Tab x3 Enter)
    for (int i = 0; i < 3; i++) {
      in.write('\t');
    }
    in.write('\n');
    // Main: Exit (Tab x4 Enter)
    for (int i = 0; i < 4; i++) {
      in.write('\t');
    }
    in.write('\n');

    String printed = runMenuWithInput(in.toByteArray());
    assertTrue(printed.contains("Suggested fix:"));
    assertTrue(printed.toLowerCase().contains("kidsplanner.db"));
    assertTrue(printed.contains("Application closed."));
  }

  @Test
  public void menuModeStorageSwitchToBinaryPrintsBinaryExplanation() throws Exception {
    RepositoryFactory.setActiveStorage(StorageType.SQLITE);

    ByteArrayOutputStream in = new ByteArrayOutputStream();
    // Consent: Enter on YES
    in.write('\n');
    // Main: Tab x2 Enter => Storage engine
    in.write('\t');
    in.write('\t');
    in.write('\n');
    // Storage: Enter => Binary
    in.write('\n');
    // Continue
    in.write('\n');
    // Storage: Back (Tab x3 Enter)
    for (int i = 0; i < 3; i++) {
      in.write('\t');
    }
    in.write('\n');
    // Main: Exit (Tab x4 Enter)
    for (int i = 0; i < 4; i++) {
      in.write('\t');
    }
    in.write('\n');

    String printed = runMenuWithInput(in.toByteArray());
    assertTrue(printed.contains("Active storage: BINARY"));
    assertTrue(printed.toLowerCase().contains("binary (files)"));
    assertTrue(printed.contains("Application closed."));
  }

  @Test
  public void menuModeAddActivityThenSelectBackCoversKidPickBackBranch() throws Exception {
    ByteArrayOutputStream in = new ByteArrayOutputStream();
    // Consent: Enter on YES
    in.write('\n');
    // Main: Tab Enter => Activity management
    in.write('\t');
    in.write('\n');
    // Activity: Enter => Add activity
    in.write('\n');
    // Kid select: Back (Tab Enter) since list has [kid0], Back, + New kid
    in.write('\t');
    in.write('\n');
    // Activity: Back (Tab x5 Enter)
    for (int i = 0; i < 5; i++) {
      in.write('\t');
    }
    in.write('\n');
    // Main: Exit (Tab x4 Enter)
    for (int i = 0; i < 4; i++) {
      in.write('\t');
    }
    in.write('\n');

    String printed = runMenuWithInput(in.toByteArray());
    assertTrue(printed.contains("Select kid"));
    assertTrue(printed.contains("Application closed."));
  }

  @Test
  public void menuModeUpdateActivityWithMissingIdTriggersValidationCaughtInRun() throws Exception {
    ByteArrayOutputStream in = new ByteArrayOutputStream();
    // Consent: Enter on YES
    in.write('\n');
    // Main: Tab Enter => Activity management
    in.write('\t');
    in.write('\n');
    // Activity: Tab x2 Enter => Update activity
    in.write('\t');
    in.write('\t');
    in.write('\n');
    // Provide non-existent ID
    in.write("999\n".getBytes(StandardCharsets.UTF_8));

    String printed = runMenuWithInput(in.toByteArray());
    // Message text is not critical; we only require that validation is handled and app exits cleanly.
    assertTrue(printed.contains("Error:"));
    assertTrue(printed.contains("Application closed."));
  }

  @Test
  public void menuModeAddActivityWithBadDateTriggersValidationCaughtInRun() throws Exception {
    ByteArrayOutputStream in = new ByteArrayOutputStream();
    // Consent: Enter on YES
    in.write('\n');
    // Main: Tab Enter => Activity management
    in.write('\t');
    in.write('\n');
    // Activity: Enter => Add activity
    in.write('\n');
    // Kid select: Enter => select default kid
    in.write('\n');
    // Title ok, Date invalid => DateTimeParseException -> ValidationException
    in.write("T\nbad-date\n09:00\n10:00\nSTUDY\n\n\n".getBytes(StandardCharsets.UTF_8));

    String printed = runMenuWithInput(in.toByteArray());
    assertTrue(printed.contains("Error:"));
    assertTrue(printed.contains("Application closed."));
  }
}

