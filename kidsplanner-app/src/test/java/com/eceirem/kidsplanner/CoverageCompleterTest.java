/**
 * @file CoverageCompleterTest.java
 * @brief Completes unit coverage for small utility classes.
 * @details Targets branches in RepositoryFactory, BinaryRepository, PrivacyConsent, Kid, Activity.
 */
package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class CoverageCompleterTest {

  @AfterEach
  public void cleanup() throws Exception {
    Files.deleteIfExists(Paths.get("kidsplanner-consent.flag"));
    Files.deleteIfExists(Paths.get("target/coverage-sqlite.db"));
    Files.deleteIfExists(Paths.get("target/coverage-binary.bin"));
    RepositoryFactory.resetConfigurationForTests();
  }

  @Test
  public void testRepositoryFactoryParseStorageTypeNullEmptyInvalid() {
    assertNull(RepositoryFactory.parseStorageTypeOrNull(null));
    assertNull(RepositoryFactory.parseStorageTypeOrNull(""));
    assertNull(RepositoryFactory.parseStorageTypeOrNull("   "));
    assertNull(RepositoryFactory.parseStorageTypeOrNull("nope"));
    assertEquals(StorageType.BINARY, RepositoryFactory.parseStorageTypeOrNull(" binary "));
  }

  @Test
  public void testRepositoryFactoryDescribeJdbcTargetsAndConfigure() {
    RepositoryFactory.configureSqliteUrl("jdbc:sqlite:target/coverage-sqlite.db");
    RepositoryFactory.configureMysql("jdbc:mysql://example.invalid:3306/kidsplanner", "u", "p");
    String desc = RepositoryFactory.describeJdbcTargets();
    assertTrue(desc.contains("SQLite URL: jdbc:sqlite:target/coverage-sqlite.db"));
    assertTrue(desc.contains("MySQL URL"));
    assertTrue(desc.contains("user=u"));
  }

  @Test
  public void testRepositoryFactoryCreateRepositoriesForAllStorageTypes() {
    // BINARY
    RepositoryFactory.setActiveStorage(StorageType.BINARY);
    assertNotNull(RepositoryFactory.createKidRepository());
    assertNotNull(RepositoryFactory.createActivityRepository());

    // SQLITE
    RepositoryFactory.configureSqliteUrl("jdbc:sqlite:target/coverage-sqlite.db");
    RepositoryFactory.setActiveStorage(StorageType.SQLITE);
    assertNotNull(RepositoryFactory.createKidRepository());
    assertNotNull(RepositoryFactory.createActivityRepository());

    RepositoryFactory.configureMysql(MysqlTestFixture.jdbcUrl(), MysqlTestFixture.user(), MysqlTestFixture.password());
    RepositoryFactory.setActiveStorage(StorageType.MYSQL);
    assertNotNull(RepositoryFactory.createKidRepository());
    assertNotNull(RepositoryFactory.createActivityRepository());
  }

  @Test
  public void testPrivacyConsentTextModeDoesNotCreateFlag() throws Exception {
    assertTrue(!PrivacyConsent.hasConsentFlag());
    PrivacyConsent.ensureInteractiveConsent(new ByteArrayInputStream(new byte[0]), false);
    assertTrue(!PrivacyConsent.hasConsentFlag());
  }

  @Test
  public void testPrivacyConsentMenuModeWritesFlagOnYes() throws Exception {
    Files.deleteIfExists(Paths.get("kidsplanner-consent.flag"));
    ByteArrayInputStream in = new ByteArrayInputStream("\n".getBytes("UTF-8"));
    PrivacyConsent.ensureInteractiveConsent(null, in, true);
    assertTrue(PrivacyConsent.hasConsentFlag());
  }

  @Test
  public void testPrivacyConsentMenuModeThrowsOnNo() throws Exception {
    Files.deleteIfExists(Paths.get("kidsplanner-consent.flag"));
    // Tab to second option, then Enter.
    ByteArrayInputStream in = new ByteArrayInputStream("\t\n".getBytes("UTF-8"));
    assertThrows(RepositoryException.class, () -> PrivacyConsent.ensureInteractiveConsent(null, in, true));
    assertTrue(!PrivacyConsent.hasConsentFlag());
  }

  @Test
  public void testPrivacyConsentReturnsWhenFlagAlreadyExists() throws Exception {
    Files.deleteIfExists(Paths.get("kidsplanner-consent.flag"));
    PrivacyConsent.writeConsentFlag();
    // If flag exists, should return without prompting or reading input.
    PrivacyConsent.ensureInteractiveConsent(null, new ByteArrayInputStream(new byte[0]), true);
    assertTrue(PrivacyConsent.hasConsentFlag());
  }

  @Test
  public void testPrivacyConsentWriteConsentFlagCreatesFile() throws Exception {
    Files.deleteIfExists(Paths.get("kidsplanner-consent.flag"));
    PrivacyConsent.writeConsentFlag();
    assertTrue(Files.exists(Paths.get("kidsplanner-consent.flag")));
  }

  @Test
  public void testPrivacyConsentWriteConsentFlagThrowsWhenPathIsDirectory() throws Exception {
    Files.deleteIfExists(Paths.get("kidsplanner-consent.flag"));
    new File("kidsplanner-consent.flag").mkdirs();
    assertThrows(RepositoryException.class, () -> PrivacyConsent.writeConsentFlag());
    // cleanup handled by @AfterEach (deleteIfExists won't delete directory on Windows, remove explicitly)
    new File("kidsplanner-consent.flag").delete();
  }

  @Test
  public void testBinaryRepositoryUpdateBranches() throws Exception {
    String file = "target/coverage-binary.bin";
    new File("target").mkdirs();
    new File(file).delete();

    BinaryRepository<Activity> repo = new BinaryRepository<Activity>(file);

    Activity a = new Activity("A", LocalDate.of(2026, 3, 25), LocalTime.of(9, 0), LocalTime.of(10, 0), ActivityType.STUDY, "H", "N",
        Integer.valueOf(1));
    repo.save(a);
    assertNotNull(a.getId());

    Activity missingId = new Activity();
    assertThrows(RepositoryException.class, () -> repo.update(missingId));

    Activity notInRepo = new Activity("B", LocalDate.of(2026, 3, 25), LocalTime.of(10, 0), LocalTime.of(11, 0), ActivityType.PLAY, "P", "N",
        Integer.valueOf(1));
    notInRepo.setId(999);
    assertThrows(RepositoryException.class, () -> repo.update(notInRepo));
  }

  @Test
  public void testBinaryRepositoryCalculateNextIdSkipsNullKey() throws Exception {
    String file = "target/coverage-binary.bin";
    new File("target").mkdirs();
    new File(file).delete();

    Map<Integer, Activity> seeded = new HashMap<Integer, Activity>();
    seeded.put(null, new Activity("X", LocalDate.of(2026, 3, 25), LocalTime.of(8, 0), LocalTime.of(9, 0), ActivityType.STUDY, "H", "N",
        Integer.valueOf(1)));
    Activity a = new Activity("A", LocalDate.of(2026, 3, 25), LocalTime.of(9, 0), LocalTime.of(10, 0), ActivityType.STUDY, "H", "N",
        Integer.valueOf(1));
    a.setId(10);
    seeded.put(10, a);
    Activity a2 = new Activity("A2", LocalDate.of(2026, 3, 25), LocalTime.of(7, 0), LocalTime.of(8, 0), ActivityType.STUDY, "H", "N",
        Integer.valueOf(1));
    a2.setId(5);
    seeded.put(5, a2);
    try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(file))) {
      output.writeObject(seeded);
    }

    BinaryRepository<Activity> repo = new BinaryRepository<Activity>(file);
    Activity b = new Activity("B", LocalDate.of(2026, 3, 25), LocalTime.of(10, 0), LocalTime.of(11, 0), ActivityType.PLAY, "P", "N",
        Integer.valueOf(1));
    repo.save(b);
    assertEquals(Integer.valueOf(11), b.getId());
  }

  @Test
  public void testKidAndActivitySettersAndToString() {
    Kid kid = new Kid();
    kid.setId(7);
    kid.setDisplayName("Ada");
    kid.setBirthYear(Integer.valueOf(2018));
    kid.setNotes("x");
    assertEquals(Integer.valueOf(7), kid.getId());
    assertEquals("Ada", kid.getDisplayName());
    assertEquals(Integer.valueOf(2018), kid.getBirthYear());
    assertEquals("x", kid.getNotes());
    assertTrue(kid.toString().contains("displayName='Ada'"));

    Activity activity = new Activity();
    activity.setId(5);
    activity.setKidId(7);
    activity.setTitle("T");
    activity.setDate(LocalDate.of(2026, 3, 25));
    activity.setStartTime(LocalTime.of(9, 0));
    activity.setEndTime(LocalTime.of(10, 0));
    activity.setType(ActivityType.ART);
    activity.setLocation("L");
    activity.setNotes("N");
    assertTrue(activity.toString().contains("kidId=7"));
  }

  @Test
  public void testActivityConstructorWithoutKidIdDelegates() {
    Activity a = new Activity(
        "T",
        LocalDate.of(2026, 3, 25),
        LocalTime.of(9, 0),
        LocalTime.of(10, 0),
        ActivityType.SOCIAL,
        "L",
        "N");
    assertNull(a.getKidId());
    assertEquals("T", a.getTitle());
  }
}

