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
import java.util.Scanner;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class KidsplannerExtendedCoverageTest {

  @AfterEach
  void cleanup() throws Exception {
    Files.deleteIfExists(Paths.get("kidsplanner-consent.flag"));
    Files.deleteIfExists(Paths.get("kidsplanner-kids.bin"));
    Files.deleteIfExists(Paths.get("kidsplanner-activities.bin"));
    Files.deleteIfExists(Paths.get("kidsplanner.db"));
    Files.deleteIfExists(Paths.get("target/ext-sqlite.db"));
    RepositoryFactory.resetConfigurationForTests();
  }

  private String runWithInput(String in) throws Exception {
    InputStream oldIn = System.in;
    PrintStream oldOut = System.out;
    ByteArrayOutputStream cap = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(cap, true, "UTF-8");
    System.setIn(new ByteArrayInputStream(in.getBytes(StandardCharsets.UTF_8)));
    System.setOut(ps);
    try {
      new Kidsplanner().run();
    } finally {
      System.setIn(oldIn);
      System.setOut(oldOut);
      ps.close();
    }
    return cap.toString("UTF-8");
  }

  @Test
  void textModeStorageBinarySqliteMysql() throws Exception {
    RepositoryFactory.configureMysql(MysqlTestFixture.jdbcUrl(), MysqlTestFixture.user(), MysqlTestFixture.password());
    String in = "storage binary\nstorage sqlite\nstorage mysql\nlist\nexit\n";
    String out = runWithInput(in);
    assertTrue(out.contains("Storage updated"));
    assertTrue(out.contains("MYSQL") || out.contains("mysql"));
  }

  @Test
  void textModeStorageUsageAndInvalid() throws Exception {
    String out = runWithInput("storage\nstorage whee\nexit\n");
    assertTrue(out.contains("Usage: storage"));
    assertTrue(out.contains("Invalid storage"));
  }

  @Test
  void kidAddListDeleteFlow() throws Exception {
    String in = "kid-add\nAnn\n2010\nn1\nkid-list\nkid-delete\n1\nlist\nexit\n";
    String out = runWithInput(in);
    assertTrue(out.contains("Kid added"));
    assertTrue(out.contains("Kid list") || out.contains("Ann"));
  }

  @Test
  void kidAddRejectsEmptyName() throws Exception {
    // name, birth year, notes lines must be provided before validation runs
    String out = runWithInput("kid-add\n\n\n\nexit\n");
    assertTrue(out.contains("cannot be empty"));
  }

  @Test
  void deleteActivityInvalidNumberFormat() throws Exception {
    String out = runWithInput("delete\noops\nexit\n");
    assertTrue(out.contains("invalid number") || out.contains("invalid"));
  }

  @Test
  void addActivityWithEmptyKidIdPicksFirstKid() throws Exception {
    String in = "kid-add\nBob\n\n\nadd\n\nDraw\n2026-03-26\n09:00\n10:00\nSTUDY\nH\nN\nlist\nexit\n";
    String out = runWithInput(in);
    assertTrue(out.contains("Activity added"));
  }

  @Test
  void updateActivityKidNotFound() throws Exception {
    String in = "kid-add\nC\n\n\nadd\n\nZ\n2026-03-26\n08:00\n09:00\nPLAY\np\nn\nupdate\n1\n999\nexit\n";
    String out = runWithInput(in);
    assertTrue(out.contains("Kid not found"));
  }

  @Test
  void detectMenuModeTextProperty() {
    String old = System.getProperty(Kidsplanner.PROP_TEXT_MODE);
    System.setProperty(Kidsplanner.PROP_TEXT_MODE, "true");
    try {
      assertFalse(Kidsplanner.detectMenuMode());
    } finally {
      if (old != null) {
        System.setProperty(Kidsplanner.PROP_TEXT_MODE, old);
      } else {
        System.setProperty(Kidsplanner.PROP_TEXT_MODE, "true");
      }
    }
  }

  @Test
  void detectMenuModeLinuxHeadless() {
    String oldOs = System.getProperty("os.name");
    String oldTxt = System.getProperty(Kidsplanner.PROP_TEXT_MODE);
    System.setProperty("os.name", "Linux");
    System.clearProperty(Kidsplanner.PROP_TEXT_MODE);
    try {
      assertFalse(Kidsplanner.detectMenuMode());
    } finally {
      System.setProperty("os.name", oldOs);
      if (oldTxt != null) {
        System.setProperty(Kidsplanner.PROP_TEXT_MODE, oldTxt);
      } else {
        System.setProperty(Kidsplanner.PROP_TEXT_MODE, "true");
      }
    }
  }

  @Test
  void detectMenuModeWindowsJarConsole() {
    String oldOs = System.getProperty("os.name");
    String oldTxt = System.getProperty(Kidsplanner.PROP_TEXT_MODE);
    System.setProperty("os.name", "Windows 11");
    System.clearProperty(Kidsplanner.PROP_TEXT_MODE);
    try {
      assertTrue(Kidsplanner.detectMenuMode());
    } finally {
      System.setProperty("os.name", oldOs);
      if (oldTxt != null) {
        System.setProperty(Kidsplanner.PROP_TEXT_MODE, oldTxt);
      } else {
        System.setProperty(Kidsplanner.PROP_TEXT_MODE, "true");
      }
    }
  }

  @Test
  void ctorRejectsNullScanner() {
    assertThrows(IllegalArgumentException.class, () -> new Kidsplanner(null, false));
  }

  @Test
  void ctorRejectsMenuFlagWithScanner() {
    assertThrows(IllegalArgumentException.class, () -> new Kidsplanner(new Scanner(System.in), true));
  }
}
