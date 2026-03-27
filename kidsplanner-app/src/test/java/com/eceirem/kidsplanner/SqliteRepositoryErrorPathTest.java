/**
 * @file SqliteRepositoryErrorPathTest.java
 * @brief Forces real SQL errors (no fakes) to cover catch branches.
 */
package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class SqliteRepositoryErrorPathTest {
  private static final String URL = "jdbc:sqlite:target/sqlite-error-path.db";

  @AfterEach
  public void cleanup() throws Exception {
    Files.deleteIfExists(Paths.get("target/sqlite-error-path.db"));
  }

  private static void dropTable(String table) throws Exception {
    try (Connection c = DriverManager.getConnection(URL); Statement st = c.createStatement()) {
      st.executeUpdate("DROP TABLE IF EXISTS " + table);
    }
  }

  @Test
  public void findAllThrowsWhenActivityTableMissing() throws Exception {
    SqliteRepository<Activity> repo = new SqliteRepository<Activity>(URL, SqliteRepository.EntityKind.ACTIVITY);
    dropTable("activity");
    RepositoryException ex = assertThrows(RepositoryException.class, () -> repo.findAll());
    assertTrue(ex.getMessage().toLowerCase().contains("could not list"));
  }

  @Test
  public void findByIdThrowsWhenKidTableMissing() throws Exception {
    SqliteRepository<Kid> repo = new SqliteRepository<Kid>(URL, SqliteRepository.EntityKind.KID);
    dropTable("kid");
    RepositoryException ex = assertThrows(RepositoryException.class, () -> repo.findById(1));
    assertTrue(ex.getMessage().toLowerCase().contains("could not read"));
  }

  @Test
  public void saveThrowsWhenKidTableMissing() throws Exception {
    SqliteRepository<Kid> repo = new SqliteRepository<Kid>(URL, SqliteRepository.EntityKind.KID);
    dropTable("kid");
    Kid k = new Kid("X", null, null);
    RepositoryException ex = assertThrows(RepositoryException.class, () -> repo.save(k));
    assertTrue(ex.getMessage().toLowerCase().contains("could not insert kid"));
  }

  @Test
  public void deleteThrowsWhenActivityTableMissing() throws Exception {
    SqliteRepository<Activity> repo = new SqliteRepository<Activity>(URL, SqliteRepository.EntityKind.ACTIVITY);
    dropTable("activity");
    RepositoryException ex = assertThrows(RepositoryException.class, () -> repo.delete(1));
    assertTrue(ex.getMessage().toLowerCase().contains("delete"));
  }

  @Test
  public void updateThrowsWhenActivityTableMissing() throws Exception {
    SqliteRepository<Kid> kidRepo = new SqliteRepository<Kid>(URL, SqliteRepository.EntityKind.KID);
    Kid kid = new Kid("K", null, null);
    kidRepo.save(kid);

    SqliteRepository<Activity> actRepo = new SqliteRepository<Activity>(URL, SqliteRepository.EntityKind.ACTIVITY);
    Activity a = new Activity("A", LocalDate.of(2026, 3, 25), LocalTime.of(9, 0), LocalTime.of(10, 0),
        ActivityType.STUDY, null, null, kid.getId());
    actRepo.save(a);

    dropTable("activity");
    RepositoryException ex = assertThrows(RepositoryException.class, () -> actRepo.update(a));
    assertTrue(ex.getMessage().toLowerCase().contains("update"));
  }

  @Test
  public void updateActivityThrowsWhenIdMissingAndWhenNotFound() throws Exception {
    SqliteRepository<Activity> actRepo = new SqliteRepository<Activity>(URL, SqliteRepository.EntityKind.ACTIVITY);
    Activity missingId = new Activity("A", LocalDate.of(2026, 3, 25), LocalTime.of(9, 0), LocalTime.of(10, 0),
        ActivityType.STUDY, null, null, Integer.valueOf(1));
    RepositoryException ex1 = assertThrows(RepositoryException.class, () -> actRepo.update(missingId));
    assertTrue(ex1.getMessage().toLowerCase().contains("id"));

    Activity notFound = new Activity("B", LocalDate.of(2026, 3, 25), LocalTime.of(9, 0), LocalTime.of(10, 0),
        ActivityType.STUDY, null, null, Integer.valueOf(1));
    notFound.setId(999999);
    RepositoryException ex2 = assertThrows(RepositoryException.class, () -> actRepo.update(notFound));
    assertTrue(ex2.getMessage().toLowerCase().contains("not found"));
  }

  @Test
  public void kidListAndUpdateThrowWhenKidTableMissing() throws Exception {
    SqliteRepository<Kid> repo = new SqliteRepository<Kid>(URL, SqliteRepository.EntityKind.KID);
    Kid k = new Kid("K", null, null);
    repo.save(k);
    dropTable("kid");

    RepositoryException exList = assertThrows(RepositoryException.class, () -> repo.findAll());
    assertTrue(exList.getMessage().toLowerCase().contains("list"));

    k.setDisplayName("K2");
    RepositoryException exUpdate = assertThrows(RepositoryException.class, () -> repo.update(k));
    assertTrue(exUpdate.getMessage().toLowerCase().contains("update"));
  }

  @Test
  public void activitySaveAndFindByIdThrowWhenActivityTableMissing() throws Exception {
    SqliteRepository<Kid> kidRepo = new SqliteRepository<Kid>(URL, SqliteRepository.EntityKind.KID);
    Kid k = new Kid("K", null, null);
    kidRepo.save(k);

    SqliteRepository<Activity> actRepo = new SqliteRepository<Activity>(URL, SqliteRepository.EntityKind.ACTIVITY);
    dropTable("activity");

    Activity a = new Activity("A", LocalDate.of(2026, 3, 25), LocalTime.of(9, 0), LocalTime.of(10, 0),
        ActivityType.STUDY, null, null, k.getId());
    RepositoryException exSave = assertThrows(RepositoryException.class, () -> actRepo.save(a));
    assertTrue(exSave.getMessage().toLowerCase().contains("insert activity"));

    RepositoryException exRead = assertThrows(RepositoryException.class, () -> actRepo.findById(1));
    assertTrue(exRead.getMessage().toLowerCase().contains("read activity"));
  }
}

