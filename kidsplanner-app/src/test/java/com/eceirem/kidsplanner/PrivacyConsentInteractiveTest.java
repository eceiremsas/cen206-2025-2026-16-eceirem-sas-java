/**
 * @file PrivacyConsentInteractiveTest.java
 * @brief Tests for interactive consent flows (menu vs text mode).
 */
package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class PrivacyConsentInteractiveTest {

  @AfterEach
  public void cleanup() throws Exception {
    // If a directory was created with this name, delete it explicitly.
    File f = new File("kidsplanner-consent.flag");
    if (f.exists() && f.isDirectory()) {
      f.delete();
    }
    Files.deleteIfExists(Paths.get("kidsplanner-consent.flag"));
  }

  @Test
  public void menuModeWritesFlagOnYes() throws Exception {
    assertFalse(PrivacyConsent.hasConsentFlag());
    ByteArrayInputStream in = new ByteArrayInputStream("\n".getBytes(StandardCharsets.UTF_8));
    PrintStream originalOut = System.out;
    try {
      System.setOut(new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8.name()));
      PrivacyConsent.ensureInteractiveConsent(null, in, true);
    } finally {
      System.setOut(originalOut);
    }
    assertTrue(PrivacyConsent.hasConsentFlag());
  }

  @Test
  public void menuModeThrowsOnNoAndDoesNotWriteFlag() throws Exception {
    assertFalse(PrivacyConsent.hasConsentFlag());
    ByteArrayInputStream in = new ByteArrayInputStream("\t\n".getBytes(StandardCharsets.UTF_8));
    PrintStream originalOut = System.out;
    try {
      System.setOut(new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8.name()));
      assertThrows(RepositoryException.class, () -> PrivacyConsent.ensureInteractiveConsent(null, in, true));
    } finally {
      System.setOut(originalOut);
    }
    assertFalse(PrivacyConsent.hasConsentFlag());
  }

  @Test
  public void writeConsentFlagThrowsWhenPathIsDirectory() throws Exception {
    Files.deleteIfExists(Paths.get("kidsplanner-consent.flag"));
    new File("kidsplanner-consent.flag").mkdirs();
    assertThrows(RepositoryException.class, () -> PrivacyConsent.writeConsentFlag());
  }
}

