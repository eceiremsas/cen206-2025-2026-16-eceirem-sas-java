/**
 * @file KidsplannerTest.java
 * @brief This file contains tests for Kidsplanner class.
 * @details It validates command flows through public run method.
 */
package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @class KidsplannerTest
 * @brief Unit tests for Kidsplanner interactive behavior.
 * @details This class tests only Kidsplanner class.
 */
public class KidsplannerTest {

  /**
   * @brief Deletes created binary store between tests.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws java.io.IOException If file operation fails.
   */
  @AfterEach
  public void cleanupStore() throws java.io.IOException {
    Files.deleteIfExists(Paths.get("kidsplanner-activities.bin"));
    Files.deleteIfExists(Paths.get("kidsplanner-kids.bin"));
    Files.deleteIfExists(Paths.get("kidsplanner-consent.flag"));
    Files.deleteIfExists(Paths.get("kidsplanner.db"));
    // Reset factory globals to avoid leaking state between tests.
    RepositoryFactory.setActiveStorage(StorageType.BINARY);
    RepositoryFactory.configureSqliteUrl("jdbc:sqlite:kidsplanner.db");
    RepositoryFactory.configureMysql(
        "jdbc:mysql://localhost:3306/kidsplanner?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
        "root",
        "kidsplanner");
  }

  /**
   * @brief Verifies invalid command path prints error then exits.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws java.io.IOException If output conversion fails.
   */
  @Test
  public void testRunWithInvalidCommandShowsMessage() throws java.io.IOException {
    String output = runWithInput("x\nexit\n");
    assertTrue(output.contains("Invalid command"));
  }

  /**
   * @brief Verifies open command prints help lines.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws java.io.IOException If output conversion fails.
   */
  @Test
  public void testRunWithOpenCommandShowsHelp() throws java.io.IOException {
    String output = runWithInput("open\nexit\n");
    assertTrue(output.contains("Date format"));
  }

  /**
   * @brief Verifies up alias prints command selection hint.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws java.io.IOException If output conversion fails.
   */
  @Test
  public void testRunWithUpCommandShowsHint() throws java.io.IOException {
    String output = runWithInput("up\nexit\n");
    assertTrue(output.contains("Choose a command"));
  }

  /**
   * @brief Verifies tab alias prints command selection hint.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws java.io.IOException If output conversion fails.
   */
  @Test
  public void testRunWithTabCommandShowsHint() throws java.io.IOException {
    String output = runWithInput("tab\nexit\n");
    assertTrue(output.contains("Choose a command"));
  }

  /**
   * @brief Verifies add command creates activity and list prints it.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws java.io.IOException If output conversion fails.
   */
  @Test
  public void testRunAddThenListShowsCreatedActivity() throws java.io.IOException {
    String input = "add\n\nMath\n2026-03-25\n09:00\n10:00\nSTUDY\nHome\nNote\nlist\nexit\n";
    String output = runWithInput(input);
    assertTrue(output.contains("Activity added"));
    assertTrue(output.contains("title='Math'"));
    assertTrue(output.contains("date=2026-03-25"));
    assertTrue(output.contains("start=09:00"));
  }

  /**
   * @brief Verifies list sorting compares multiple activities.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws java.io.IOException If output conversion fails.
   */
  @Test
  public void testRunListWithMultipleActivitiesTriggersSortingComparator() throws java.io.IOException {
    String input = "add\n\nSecond\n2026-03-25\n11:00\n12:00\nSTUDY\nHome\nNote\n"
        + "add\n\nFirst\n2026-03-25\n09:00\n10:00\nPLAY\nPark\nNote\nlist\nexit\n";
    String output = runWithInput(input);
    assertTrue(output.contains("--- Activity list ---"));
    assertInOrder(output, "title='First'", "title='Second'");
  }

  /**
   * @brief Verifies list sorting compares activities on different dates.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws java.io.IOException If output conversion fails.
   */
  @Test
  public void testRunListWithDifferentDatesTriggersDateComparison() throws java.io.IOException {
    String input = "add\n\nA\n2026-03-26\n09:00\n10:00\nSTUDY\nHome\nNote\n"
        + "add\n\nB\n2026-03-25\n09:00\n10:00\nPLAY\nPark\nNote\nlist\nexit\n";
    String output = runWithInput(input);
    assertTrue(output.contains("--- Activity list ---"));
    assertInOrder(output, "title='B'", "title='A'");
  }

  /**
   * @brief Verifies add command rejects invalid date.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws java.io.IOException If output conversion fails.
   */
  @Test
  public void testRunAddWithInvalidDateShowsValidationError() throws java.io.IOException {
    String input = "add\n\nMath\nbad-date\n09:00\n10:00\nSTUDY\nHome\nNote\nexit\n";
    String output = runWithInput(input);
    assertTrue(output.contains("Invalid date/time format"));
  }

  /**
   * @brief Verifies add command rejects overlapping activities.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws java.io.IOException If output conversion fails.
   */
  @Test
  public void testRunAddWithOverlapShowsValidationError() throws java.io.IOException {
    String input = "add\n\nFirst\n2026-03-25\n09:00\n10:00\nSTUDY\nHome\nn1\n"
        + "add\n\nSecond\n2026-03-25\n09:30\n10:30\nPLAY\nPark\nn2\nexit\n";
    String output = runWithInput(input);
    assertTrue(output.contains("Time overlap on the same day"));
  }

  /**
   * @brief Verifies update command rejects missing id.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws java.io.IOException If output conversion fails.
   */
  @Test
  public void testRunUpdateWithMissingIdShowsValidationError() throws java.io.IOException {
    String output = runWithInput("update\n9\nexit\n");
    assertTrue(output.contains("ID not found"));
  }

  /**
   * @brief Verifies update command updates existing activity.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws java.io.IOException If output conversion fails.
   */
  @Test
  public void testRunUpdateWithExistingIdUpdatesActivity() throws java.io.IOException {
    String input = "add\n\nOld\n2026-03-25\n09:00\n10:00\nSTUDY\nHome\nn1\n"
        + "update\n1\n\nNew\n2026-03-25\n10:30\n11:30\nPLAY\nPark\nn2\nlist\nexit\n";
    String output = runWithInput(input);
    assertTrue(output.contains("Activity updated."));
    assertTrue(output.contains("title='New'"));
    assertTrue(output.contains("start=10:30"));
    assertTrue(output.contains("end=11:30"));
    assertTrue(output.contains("type=PLAY"));
  }

  /**
   * @brief Verifies update keeps previous values when blank defaults are used.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws java.io.IOException If output conversion fails.
   */
  @Test
  public void testRunUpdateWithBlankInputsKeepsExistingValues() throws java.io.IOException {
    String input = "add\n\nOriginal\n2026-03-25\n09:00\n10:00\nSTUDY\nHome\nn1\n"
        + "update\n1\n\n\n\n\n\n\n\n\nlist\nexit\n";
    String output = runWithInput(input);
    assertTrue(output.contains("title='Original'"));
    assertTrue(output.contains("date=2026-03-25"));
    assertTrue(output.contains("start=09:00"));
    assertTrue(output.contains("end=10:00"));
    assertTrue(output.contains("type=STUDY"));
  }

  /**
   * @brief Verifies delete command for missing id keeps flow healthy.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws java.io.IOException If output conversion fails.
   */
  @Test
  public void testRunDeleteForMissingIdPrintsDeleteMessage() throws java.io.IOException {
    String output = runWithInput("delete\n42\nexit\n");
    assertTrue(output.contains("Activity deleted (if it existed)."));
    assertFalse(output.contains("Error:"));
  }

  /**
   * @brief Verifies delete removes added activity from later list output.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws java.io.IOException If output conversion fails.
   */
  @Test
  public void testRunDeleteExistingIdRemovesActivityFromList() throws java.io.IOException {
    String input = "add\n\nMath\n2026-03-25\n09:00\n10:00\nSTUDY\nHome\nNote\n"
        + "delete\n1\nlist\nexit\n";
    String output = runWithInput(input);
    assertTrue(output.contains("(no records)"));
  }

  /**
   * @brief Verifies list command on empty data prints empty marker.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws java.io.IOException If output conversion fails.
   */
  @Test
  public void testRunListWithNoDataShowsEmptyMarker() throws java.io.IOException {
    String output = runWithInput("list\nexit\n");
    assertTrue(output.contains("(no records)"));
  }

  /**
   * @brief Verifies add command rejects invalid activity type.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws java.io.IOException If output conversion fails.
   */
  @Test
  public void testRunAddWithInvalidTypeShowsValidationError() throws java.io.IOException {
    String input = "add\n\nMath\n2026-03-25\n09:00\n10:00\nUNKNOWN\nHome\nNote\nexit\n";
    String output = runWithInput(input);
    assertTrue(output.contains("Invalid activity type"));
  }

  /**
   * @brief Verifies add command rejects empty title.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws java.io.IOException If output conversion fails.
   */
  @Test
  public void testRunAddWithEmptyTitleShowsValidationError() throws java.io.IOException {
    String input = "add\n\n\nexit\n";
    String output = runWithInput(input);
    assertTrue(output.contains("Title cannot be empty"));
  }

  /**
   * @brief Verifies add command rejects start time not before end time.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws java.io.IOException If output conversion fails.
   */
  @Test
  public void testRunAddWithInvalidTimeOrderShowsValidationError() throws java.io.IOException {
    String input = "add\n\nMath\n2026-03-25\n10:00\n10:00\nSTUDY\nHome\nNote\nexit\n";
    String output = runWithInput(input);
    assertTrue(output.contains("Start time must be before end time"));
  }

  /**
   * @brief Verifies overlap edge where one activity starts at previous end is allowed.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws java.io.IOException If output conversion fails.
   */
  @Test
  public void testRunAddWithBoundaryTimesDoesNotTriggerOverlap() throws java.io.IOException {
    String input = "add\n\nFirst\n2026-03-25\n09:00\n10:00\nSTUDY\nHome\nn1\n"
        + "add\n\nSecond\n2026-03-25\n10:00\n11:00\nPLAY\nPark\nn2\nlist\nexit\n";
    String output = runWithInput(input);
    assertTrue(output.contains("title='Second'"));
  }

  /**
   * @brief Verifies command parsing tolerates mixed case and surrounding spaces.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws java.io.IOException If output conversion fails.
   */
  @Test
  public void testRunWithMixedCaseAndSpacesCommandParsesCorrectly() throws java.io.IOException {
    String output = runWithInput("  LiSt  \nexit\n");
    assertTrue(output.contains("--- Activity list ---"));
  }

  /**
   * @brief Verifies empty command line is handled as invalid command.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws java.io.IOException If output conversion fails.
   */
  @Test
  public void testRunWithEmptyCommandShowsInvalidMessage() throws java.io.IOException {
    String output = runWithInput("\nexit\n");
    assertTrue(output.contains("Invalid command"));
  }

  /**
   * @brief Verifies exit command ends flow and prints shutdown message.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws java.io.IOException If output conversion fails.
   */
  @Test
  public void testRunWithExitPrintsShutdownMessage() throws java.io.IOException {
    String output = runWithInput("exit\n");
    assertTrue(output.contains("Application closed."));
  }

  @Test
  public void testStorageCommandWithoutArgPrintsUsage() throws java.io.IOException {
    String output = runWithInput("storage\nexit\n");
    assertTrue(output.contains("Usage: storage binary|sqlite|mysql"));
  }

  @Test
  public void testStorageCommandWithInvalidBackendPrintsError() throws java.io.IOException {
    String output = runWithInput("storage nope\nexit\n");
    assertTrue(output.contains("Invalid storage. Use binary, sqlite, or mysql."));
  }

  @Test
  public void testStorageCommandSwitchesSqliteAndPrintsTargets() throws java.io.IOException {
    String output = runWithInput("storage sqlite\nexit\n");
    assertTrue(output.contains("Storage updated: SQLITE"));
    assertTrue(output.toLowerCase().contains("jdbc"));
  }

  @Test
  public void testKidAddListAndDeleteCommandsWork() throws java.io.IOException {
    String input = ""
        + "kid-add\n"
        + "Alice\n"
        + "2018\n"
        + "note\n"
        + "kid-list\n"
        + "kid-delete\n"
        + "1\n"
        + "kid-list\n"
        + "exit\n";
    String output = runWithInput(input);
    assertTrue(output.contains("Kid added. ID ="));
    assertTrue(output.contains("--- Kid list ---"));
    assertTrue(output.contains("Kid deleted (if it existed)."));
  }

  @Test
  public void testTextModeDeleteWithNonNumberPrintsNumberFormatError() throws java.io.IOException {
    String output = runWithInput("delete\nabc\nexit\n");
    assertTrue(output.contains("Error: invalid number format."));
  }

  @Test
  public void testTextModeAddWithUnknownKidIdPrintsKidNotFoundError() throws java.io.IOException {
    // add activity -> kid id 999 not found => ValidationException caught in text loop
    String input = ""
        + "add\n"
        + "999\n"
        + "T\n"
        + "2026-03-25\n"
        + "09:00\n"
        + "10:00\n"
        + "STUDY\n"
        + "\n"
        + "\n"
        + "exit\n";
    String output = runWithInput(input);
    assertTrue(output.contains("Error: Kid not found"));
  }

  @Test
  public void testScannerConstructorRunsTextMode() throws Exception {
    java.util.Scanner sc = new java.util.Scanner(new java.io.ByteArrayInputStream("exit\n".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
        java.nio.charset.StandardCharsets.UTF_8.name());
    java.io.PrintStream originalOut = System.out;
    java.io.ByteArrayOutputStream outBytes = new java.io.ByteArrayOutputStream();
    java.io.PrintStream out = new java.io.PrintStream(outBytes, true, java.nio.charset.StandardCharsets.UTF_8.name());
    try {
      System.setOut(out);
      Kidsplanner kp = new Kidsplanner(sc, false);
      kp.run();
    } finally {
      System.setOut(originalOut);
      out.close();
      sc.close();
    }
    String printed = outBytes.toString(java.nio.charset.StandardCharsets.UTF_8.name());
    assertTrue(printed.contains("Application closed."));
  }

  @Test
  public void testScannerConstructorRejectsNullScannerAndMenuModeTrue() {
    assertThrows(IllegalArgumentException.class, () -> new Kidsplanner(null, false));
    assertThrows(IllegalArgumentException.class,
        () -> new Kidsplanner(new java.util.Scanner(new java.io.ByteArrayInputStream(new byte[0])), true));
  }

  @Test
  public void testDetectMenuModeCanBeForcedForDeterministicCoverage() {
    System.clearProperty(Kidsplanner.PROP_TEXT_MODE);
    System.setProperty(Kidsplanner.PROP_FORCE_MENU, "true");
    try {
      assertTrue(Kidsplanner.detectMenuMode());
    } finally {
      System.clearProperty(Kidsplanner.PROP_FORCE_MENU);
    }
  }

  /**
   * @brief Runs planner with provided input and returns captured output.
   *
   * @param scriptedInput Console commands to feed into System.in.
   * @return String Captured System.out content.
   * @throws java.io.IOException If output conversion fails.
   */
  private String runWithInput(String scriptedInput) throws java.io.IOException {
    InputStream originalIn = System.in;
    PrintStream originalOut = System.out;
    ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();
    PrintStream redirectedOut = new PrintStream(capturedOut);
    ByteArrayInputStream redirectedIn = new ByteArrayInputStream(scriptedInput.getBytes(StandardCharsets.UTF_8));

    try {
      System.setIn(redirectedIn);
      System.setOut(redirectedOut);
      // Force non-interactive deterministic behavior in tests.
      System.setProperty(Kidsplanner.PROP_TEXT_MODE, "true");
      System.setProperty(Kidsplanner.PROP_DISABLE_JLINE, "true");
      new Kidsplanner().run();
    } finally {
      System.setIn(originalIn);
      System.setOut(originalOut);
      System.clearProperty(Kidsplanner.PROP_TEXT_MODE);
      System.clearProperty(Kidsplanner.PROP_DISABLE_JLINE);
      redirectedOut.close();
    }
    return capturedOut.toString(StandardCharsets.UTF_8.name());
  }

  /**
   * @brief Asserts first marker appears before second marker in output text.
   *
   * @param outputText Full captured output text.
   * @param firstMarker Marker expected to appear first.
   * @param secondMarker Marker expected to appear second.
   * @return void This method returns no value.
   */
  private void assertInOrder(String outputText, String firstMarker, String secondMarker) {
    int firstIndex = outputText.indexOf(firstMarker);
    int secondIndex = outputText.indexOf(secondMarker);
    assertTrue(firstIndex >= 0, "Output should contain first marker: " + firstMarker);
    assertTrue(secondIndex >= 0, "Output should contain second marker: " + secondMarker);
    assertTrue(firstIndex < secondIndex, "Expected '" + firstMarker + "' before '" + secondMarker + "'");
  }
}
