/**
 * @file SqliteRepositoryTest.java
 * @brief Integration-style unit tests for SqliteRepository using a temp DB file.
 */
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

public class SqliteRepositoryTest {

  private static final String DB = "target/sqlite-repo-test.db";

  @AfterEach
  public void cleanup() throws Exception {
    Files.deleteIfExists(Paths.get(DB));
  }

  @Test
  public void kidCrudRoundTrip() {
    SqliteRepository<Kid> kidRepo = new SqliteRepository<Kid>("jdbc:sqlite:" + DB, SqliteRepository.EntityKind.KID);

    Kid kid = new Kid("Ece", 2017, "n");
    kidRepo.save(kid);
    assertNotNull(kid.getId());

    Kid loaded = kidRepo.findById(kid.getId().intValue());
    assertNotNull(loaded);
    assertEquals("Ece", loaded.getDisplayName());

    loaded.setNotes("updated");
    kidRepo.update(loaded);
    Kid loaded2 = kidRepo.findById(loaded.getId().intValue());
    assertEquals("updated", loaded2.getNotes());

    List<Kid> all = kidRepo.findAll();
    assertEquals(1, all.size());

    kidRepo.delete(loaded.getId().intValue());
    assertNull(kidRepo.findById(loaded.getId().intValue()));
  }

  @Test
  public void activityCrudRoundTrip() {
    SqliteRepository<Kid> kidRepo = new SqliteRepository<Kid>("jdbc:sqlite:" + DB, SqliteRepository.EntityKind.KID);
    SqliteRepository<Activity> actRepo = new SqliteRepository<Activity>("jdbc:sqlite:" + DB, SqliteRepository.EntityKind.ACTIVITY);

    Kid kid = new Kid("K", null, null);
    kidRepo.save(kid);

    Activity a = new Activity("Math", LocalDate.of(2026, 3, 25), LocalTime.of(9, 0), LocalTime.of(10, 0),
        ActivityType.STUDY, "Home", "N", kid.getId());
    actRepo.save(a);
    assertNotNull(a.getId());

    Activity loaded = actRepo.findById(a.getId().intValue());
    assertEquals("Math", loaded.getTitle());
    assertEquals(ActivityType.STUDY, loaded.getType());

    loaded.setTitle("New");
    actRepo.update(loaded);
    Activity loaded2 = actRepo.findById(loaded.getId().intValue());
    assertEquals("New", loaded2.getTitle());

    assertEquals(1, actRepo.findAll().size());
    actRepo.delete(loaded.getId().intValue());
    assertNull(actRepo.findById(loaded.getId().intValue()));
  }

  @Test
  public void kidDeleteCascadesActivities() {
    SqliteRepository<Kid> kidRepo = new SqliteRepository<Kid>("jdbc:sqlite:" + DB, SqliteRepository.EntityKind.KID);
    SqliteRepository<Activity> actRepo = new SqliteRepository<Activity>("jdbc:sqlite:" + DB, SqliteRepository.EntityKind.ACTIVITY);

    Kid kid = new Kid("K", null, null);
    kidRepo.save(kid);

    Activity a = new Activity("A", LocalDate.of(2026, 3, 25), LocalTime.of(9, 0), LocalTime.of(10, 0),
        ActivityType.PLAY, null, null, kid.getId());
    actRepo.save(a);

    kidRepo.delete(kid.getId().intValue());
    assertTrue(actRepo.findAll().isEmpty());
  }

  @Test
  public void findByIdReturnsNullWhenMissing() {
    SqliteRepository<Kid> kidRepo = new SqliteRepository<Kid>("jdbc:sqlite:" + DB, SqliteRepository.EntityKind.KID);
    assertNull(kidRepo.findById(12345));
  }

  @Test
  public void mapsNullBirthYearAndNullNotes() {
    SqliteRepository<Kid> kidRepo = new SqliteRepository<Kid>("jdbc:sqlite:" + DB, SqliteRepository.EntityKind.KID);
    Kid kid = new Kid("Nulls", null, null);
    kidRepo.save(kid);
    Kid loaded = kidRepo.findById(kid.getId().intValue());
    assertNotNull(loaded);
    assertNull(loaded.getBirthYear());
    assertNull(loaded.getNotes());
  }

  @Test
  public void updateThrowsWhenIdMissingOrNotFound() {
    SqliteRepository<Kid> kidRepo = new SqliteRepository<Kid>("jdbc:sqlite:" + DB, SqliteRepository.EntityKind.KID);
    Kid missingId = new Kid("X", null, null);
    assertThrows(RepositoryException.class, () -> kidRepo.update(missingId));

    Kid notFound = new Kid("Y", null, null);
    notFound.setId(999);
    assertThrows(RepositoryException.class, () -> kidRepo.update(notFound));
  }
}

