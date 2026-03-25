package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SqliteRepositoryIntegrationTest {

  private static final String DB = "target/sqlite-it.db";

  @AfterEach
  void cleanup() throws Exception {
    Files.deleteIfExists(Paths.get(DB));
    RepositoryFactory.resetConfigurationForTests();
  }

  @Test
  void kidCrudAndDeleteCascadesActivities() {
    IRepository<Kid> kids = new SqliteRepository<Kid>("jdbc:sqlite:" + DB, SqliteRepository.EntityKind.KID);
    IRepository<Activity> acts =
        new SqliteRepository<Activity>("jdbc:sqlite:" + DB, SqliteRepository.EntityKind.ACTIVITY);

    Kid k = new Kid("Ada", 2015, "note");
    kids.save(k);
    assertNotNull(k.getId());

    Activity a = new Activity("Read", LocalDate.of(2026, 3, 26), LocalTime.of(9, 0), LocalTime.of(10, 0),
        ActivityType.STUDY, "Home", "n", k.getId());
    acts.save(a);
    assertNotNull(a.getId());

    assertEquals(1, kids.findAll().size());
    assertEquals(1, acts.findAll().size());

    Kid loaded = kids.findById(k.getId());
    assertEquals("Ada", loaded.getDisplayName());
    assertEquals(Integer.valueOf(2015), loaded.getBirthYear());

    kids.delete(k.getId());
    assertTrue(kids.findAll().isEmpty());
    assertTrue(acts.findAll().isEmpty());
  }

  @Test
  void activityUpdateAndFindById() {
    IRepository<Kid> kids = new SqliteRepository<Kid>("jdbc:sqlite:" + DB, SqliteRepository.EntityKind.KID);
    IRepository<Activity> acts =
        new SqliteRepository<Activity>("jdbc:sqlite:" + DB, SqliteRepository.EntityKind.ACTIVITY);

    Kid k = new Kid("B", null, "");
    kids.save(k);
    Activity a = new Activity("T", LocalDate.of(2026, 1, 1), LocalTime.of(8, 0), LocalTime.of(9, 0),
        ActivityType.PLAY, "L", "", k.getId());
    acts.save(a);

    Activity patch = new Activity("T2", LocalDate.of(2026, 1, 2), LocalTime.of(10, 0), LocalTime.of(11, 0),
        ActivityType.ART, "X", "y", k.getId());
    patch.setId(a.getId());
    acts.update(patch);

    Activity got = acts.findById(a.getId());
    assertEquals("T2", got.getTitle());
    assertEquals(ActivityType.ART, got.getType());

    assertNull(acts.findById(99999));
    assertNull(kids.findById(99999));
  }

  @Test
  void kidUpdateNotFoundThrows() {
    IRepository<Kid> kids = new SqliteRepository<Kid>("jdbc:sqlite:" + DB, SqliteRepository.EntityKind.KID);
    Kid k = new Kid("x", null, "");
    k.setId(404);
    RepositoryException ex = assertThrows(RepositoryException.class, () -> kids.update(k));
    assertTrue(ex.getMessage().contains("not found"));
  }

  @Test
  void kidUpdateMissingIdThrows() {
    IRepository<Kid> kids = new SqliteRepository<Kid>("jdbc:sqlite:" + DB, SqliteRepository.EntityKind.KID);
    Kid k = new Kid("x", null, "");
    RepositoryException ex = assertThrows(RepositoryException.class, () -> kids.update(k));
    assertTrue(ex.getMessage().contains("id missing"));
  }

  @Test
  void activityUpdateNotFoundThrows() {
    IRepository<Activity> acts =
        new SqliteRepository<Activity>("jdbc:sqlite:" + DB, SqliteRepository.EntityKind.ACTIVITY);
    Activity a = new Activity("x", LocalDate.now(), LocalTime.NOON, LocalTime.of(13, 0),
        ActivityType.STUDY, "", "", 1);
    a.setId(999);
    RepositoryException ex = assertThrows(RepositoryException.class, () -> acts.update(a));
    assertTrue(ex.getMessage().length() > 0);
  }

  @Test
  void deleteActivityOnlyRemovesActivityRow() {
    IRepository<Kid> kids = new SqliteRepository<Kid>("jdbc:sqlite:" + DB, SqliteRepository.EntityKind.KID);
    IRepository<Activity> acts =
        new SqliteRepository<Activity>("jdbc:sqlite:" + DB, SqliteRepository.EntityKind.ACTIVITY);
    Kid k = new Kid("c", null, "");
    kids.save(k);
    Activity a = new Activity("z", LocalDate.now(), LocalTime.of(1, 0), LocalTime.of(2, 0),
        ActivityType.SLEEP, "", "", k.getId());
    acts.save(a);
    acts.delete(a.getId());
    assertEquals(1, kids.findAll().size());
    assertTrue(acts.findAll().isEmpty());
  }

  @Test
  void schemaFailureThrowsRepositoryException() throws Exception {
    java.io.File dir = new java.io.File("target/sqlite-path-is-directory");
    Files.createDirectories(dir.toPath());
    String url = "jdbc:sqlite:" + dir.getAbsolutePath().replace('\\', '/');
    assertThrows(RepositoryException.class, () -> new SqliteRepository<Kid>(url, SqliteRepository.EntityKind.KID));
  }

  @Test
  void listActivitiesOrdered() {
    IRepository<Kid> kids = new SqliteRepository<Kid>("jdbc:sqlite:" + DB, SqliteRepository.EntityKind.KID);
    IRepository<Activity> acts =
        new SqliteRepository<Activity>("jdbc:sqlite:" + DB, SqliteRepository.EntityKind.ACTIVITY);
    Kid k = new Kid("k", null, "");
    kids.save(k);
    acts.save(new Activity("b", LocalDate.of(2026, 2, 2), LocalTime.of(9, 0), LocalTime.of(10, 0),
        ActivityType.STUDY, "", "", k.getId()));
    acts.save(new Activity("a", LocalDate.of(2026, 2, 1), LocalTime.of(9, 0), LocalTime.of(10, 0),
        ActivityType.STUDY, "", "", k.getId()));
    List<Activity> list = acts.findAll();
    assertEquals(2, list.size());
    assertTrue(list.get(0).getDate().isBefore(list.get(1).getDate()));
  }
}
