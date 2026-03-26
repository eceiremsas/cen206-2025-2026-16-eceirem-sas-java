/**
 * @file KidsplannerMenuModeTest.java
 * @brief Covers Kidsplanner menu-mode flows with deterministic stdin.
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class KidsplannerMenuModeTest {

  @AfterEach
  public void cleanup() throws Exception {
    Files.deleteIfExists(Paths.get("kidsplanner-activities.bin"));
    Files.deleteIfExists(Paths.get("kidsplanner-kids.bin"));
    Files.deleteIfExists(Paths.get("kidsplanner-consent.flag"));
    Files.deleteIfExists(Paths.get("kidsplanner.db"));
    System.clearProperty(Kidsplanner.PROP_TEXT_MODE);
    System.clearProperty(Kidsplanner.PROP_DISABLE_JLINE);
  }

  @Test
  public void menuModeCanOpenStorageSwitchAndExit() throws Exception {
    // Force menu mode and disable JLine so menu reads from the provided stdin stream.
    System.setProperty(Kidsplanner.PROP_TEXT_MODE, "false");
    System.setProperty(Kidsplanner.PROP_DISABLE_JLINE, "true");

    // Consent: Enter on YES
    // Main: Tab Tab Enter => Storage
    // Storage: Tab Enter => SQLite (index 1) then Enter to continue
    // Storage: q => Back
    // Main: q => Exit
    byte[] keys = new byte[] {
        (byte) '\n',
        (byte) '\t', (byte) '\t', (byte) '\n',
        (byte) '\t', (byte) '\n',
        (byte) '\n',
        (byte) 'q',
        (byte) 'q'
    };

    InputStream originalIn = System.in;
    PrintStream originalOut = System.out;
    ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8.name());
    try {
      System.setIn(new ByteArrayInputStream(keys));
      System.setOut(out);
      new Kidsplanner().run();
    } finally {
      System.setIn(originalIn);
      System.setOut(originalOut);
      out.close();
    }

    String printed = outBytes.toString(StandardCharsets.UTF_8.name());
    assertTrue(printed.contains("Privacy / personal data"));
    assertTrue(printed.contains("Storage engine"));
    assertTrue(printed.contains("Active storage: SQLITE"));
    assertTrue(printed.contains("Application closed."));
  }
}

