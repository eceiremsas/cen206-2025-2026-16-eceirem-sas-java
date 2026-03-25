/**
 * @file KidsplannerAppTest.java
 * @brief This file contains tests for KidsplannerApp class.
 * @details It verifies application startup flow through main method.
 */
package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertNotNull;
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
 * @class KidsplannerAppTest
 * @brief Unit tests for KidsplannerApp.
 * @details This class checks only KidsplannerApp behavior.
 */
public class KidsplannerAppTest {

  /**
   * @brief Cleans binary file that might be produced by tests.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws java.io.IOException If file deletion fails.
   */
  @AfterEach
  public void cleanupFile() throws java.io.IOException {
    Files.deleteIfExists(Paths.get("kidsplanner-activities.bin"));
    Files.deleteIfExists(Paths.get("kidsplanner-kids.bin"));
    Files.deleteIfExists(Paths.get("kidsplanner-consent.flag"));
    Files.deleteIfExists(Paths.get("kidsplanner.db"));
  }

  /**
   * @brief Verifies main starts planner and prints close message.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws java.io.IOException If output conversion fails.
   */
  @Test
  public void testMainRunsPlannerAndExits() throws java.io.IOException {
    InputStream originalIn = System.in;
    PrintStream originalOut = System.out;
    ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();
    PrintStream redirectedOut = new PrintStream(capturedOut);
    ByteArrayInputStream redirectedIn = new ByteArrayInputStream("exit\n".getBytes(StandardCharsets.UTF_8));

    try {
      System.setIn(redirectedIn);
      System.setOut(redirectedOut);
      KidsplannerApp.main(new String[0]);
    } finally {
      System.setIn(originalIn);
      System.setOut(originalOut);
      redirectedOut.close();
    }

    String output = capturedOut.toString(StandardCharsets.UTF_8.name());
    assertTrue(output.contains("Uygulama kapatildi."));
  }

  /**
   * @brief Verifies default constructor creates app instance.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws none This method does not throw checked exceptions.
   */
  @Test
  public void testConstructorCreatesInstance() {
    KidsplannerApp app = new KidsplannerApp();
    assertNotNull(app);
  }

}
