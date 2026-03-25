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

class KidsplannerMenuModeTest {

  @AfterEach
  void restore() throws Exception {
    System.setProperty(Kidsplanner.PROP_TEXT_MODE, "true");
    System.clearProperty(Kidsplanner.PROP_MENU_MODE_TEST);
    Files.deleteIfExists(Paths.get("kidsplanner-consent.flag"));
    Files.deleteIfExists(Paths.get("kidsplanner-kids.bin"));
    Files.deleteIfExists(Paths.get("kidsplanner-activities.bin"));
    RepositoryFactory.resetConfigurationForTests();
  }

  @Test
  void consentYesThenQuitMainMenu() throws Exception {
    Files.deleteIfExists(Paths.get("kidsplanner-consent.flag"));
    System.clearProperty(Kidsplanner.PROP_TEXT_MODE);
    System.setProperty(Kidsplanner.PROP_MENU_MODE_TEST, "true");

    InputStream oldIn = System.in;
    PrintStream oldOut = System.out;
    ByteArrayOutputStream cap = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(cap, true, "UTF-8");
    // Consent YES; main: Kid (\n), Kid menu: Back (q), main: Exit (q)
    byte[] script = "\n\nqq\n".getBytes(StandardCharsets.UTF_8);
    System.setIn(new ByteArrayInputStream(script));
    System.setOut(ps);
    try {
      new Kidsplanner().run();
    } finally {
      System.setIn(oldIn);
      System.setOut(oldOut);
      ps.close();
    }
    String out = cap.toString("UTF-8");
    assertTrue(out.contains("Application closed"));
  }
}
