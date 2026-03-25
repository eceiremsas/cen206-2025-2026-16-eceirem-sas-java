/**
 * @file DomainAndConsentBehaviorTest.java
 * @brief Additional focused tests that keep JaCoCo at 100%.
 * @details Exercises real-world behaviors of already-included classes (domain + consent + exceptions + enums).
 */
package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class DomainAndConsentBehaviorTest {

  @AfterEach
  public void cleanup() throws Exception {
    Files.deleteIfExists(Paths.get("kidsplanner-consent.flag"));
  }

  @Test
  public void testKidToStringHandlesNullBirthYear() {
    Kid kid = new Kid("Ece", null, "notes");
    kid.setId(1);
    String text = kid.toString();
    assertTrue(text.contains("id=1"));
    assertTrue(text.contains("displayName='Ece'"));
    assertTrue(text.contains("birthYear=null"));
  }

  @Test
  public void testActivityToStringIncludesKeyFields() {
    Activity a = new Activity(
        "Math",
        LocalDate.of(2026, 3, 25),
        LocalTime.of(9, 0),
        LocalTime.of(10, 0),
        ActivityType.STUDY,
        "Home",
        "N",
        Integer.valueOf(2));
    a.setId(7);
    String text = a.toString();
    assertTrue(text.contains("id=7"));
    assertTrue(text.contains("kidId=2"));
    assertTrue(text.contains("title='Math'"));
    assertTrue(text.contains("date=2026-03-25"));
    assertTrue(text.contains("start=09:00"));
    assertTrue(text.contains("end=10:00"));
    assertTrue(text.contains("type=STUDY"));
    assertTrue(text.contains("location='Home'"));
  }

  @Test
  public void testActivityConstructorWithoutKidIdLeavesKidIdNull() {
    Activity a = new Activity(
        "Play",
        LocalDate.of(2026, 3, 26),
        LocalTime.of(15, 0),
        LocalTime.of(16, 0),
        ActivityType.PLAY,
        "Park",
        "Ball");
    assertNull(a.getKidId());
    assertEquals("Play", a.getTitle());
  }

  @Test
  public void testPrivacyConsentTextModeDoesNotCreateFlag() throws Exception {
    assertFalse(PrivacyConsent.hasConsentFlag());
    PrivacyConsent.ensureInteractiveConsent(new ByteArrayInputStream(new byte[0]), false);
    assertFalse(PrivacyConsent.hasConsentFlag());
  }

  @Test
  public void testPrivacyConsentWriteAndHasFlagRoundTrip() throws Exception {
    assertFalse(PrivacyConsent.hasConsentFlag());
    PrivacyConsent.writeConsentFlag();
    assertTrue(PrivacyConsent.hasConsentFlag());
  }

  /** When flag exists, interactive consent returns immediately (no menu). */
  @Test
  public void testPrivacyConsentSkipsMenuWhenFlagAlreadyPresent() throws Exception {
    PrivacyConsent.writeConsentFlag();
    assertTrue(PrivacyConsent.hasConsentFlag());
    PrivacyConsent.ensureInteractiveConsent(new ByteArrayInputStream(new byte[0]), true);
    assertTrue(PrivacyConsent.hasConsentFlag());
  }

  @Test
  public void testRepositoryExceptionStoresCauseAndMessage() {
    Throwable cause = new IllegalStateException("root");
    RepositoryException ex = new RepositoryException("msg", cause);
    assertEquals("msg", ex.getMessage());
    assertNotNull(ex.getCause());
    assertEquals(cause, ex.getCause());
  }

  @Test
  public void testValidationExceptionStoresMessage() {
    ValidationException ex = new ValidationException("bad input");
    assertEquals("bad input", ex.getMessage());
  }

  @Test
  public void testEnumsContainExpectedValues() {
    assertEquals(ActivityType.SLEEP, ActivityType.valueOf("SLEEP"));
    assertEquals(StorageType.SQLITE, StorageType.valueOf("SQLITE"));
  }
}

