/**
 * @file KidsplannerTest.java
 * @brief This file contains tests for Kidsplanner class.
 * @details It validates command flows through public run method.
 */
package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
    assertTrue(output.contains("Gecersiz komut"));
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
    assertTrue(output.contains("Tarih formati"));
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
    assertTrue(output.contains("Komut seciniz"));
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
    assertTrue(output.contains("Komut seciniz"));
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
    assertTrue(output.contains("Aktivite eklendi"));
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
    assertTrue(output.contains("--- Aktivite Listesi ---"));
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
    assertTrue(output.contains("--- Aktivite Listesi ---"));
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
    assertTrue(output.contains("Tarih/saat formati gecersiz"));
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
    assertTrue(output.contains("Ayni gunde saat cakismasi var"));
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
    assertTrue(output.contains("ID bulunamadi"));
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
    assertTrue(output.contains("Aktivite guncellendi."));
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
    assertTrue(output.contains("Aktivite silindi (varsa)."));
    assertFalse(output.contains("Hata:"));
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
    assertTrue(output.contains("(kayit yok)"));
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
    assertTrue(output.contains("(kayit yok)"));
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
    assertTrue(output.contains("Aktivite tipi gecersiz"));
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
    assertTrue(output.contains("Baslik bos olamaz"));
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
    assertTrue(output.contains("Baslangic saati, bitis saatinden once olmali"));
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
    assertTrue(output.contains("--- Aktivite Listesi ---"));
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
    assertTrue(output.contains("Gecersiz komut"));
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
    assertTrue(output.contains("Uygulama kapatildi."));
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
      new Kidsplanner().run();
    } finally {
      System.setIn(originalIn);
      System.setOut(originalOut);
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
