package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Real MySQL integration tests.
 *
 * Uses Testcontainers to run a real MySQL instance (no fakes).
 */
@Testcontainers
public class MySqlRepositoryIntegrationTest {

  @Container
  private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
      .withDatabaseName("kidsplanner")
      .withUsername("root")
      .withPassword("kidsplanner");

  @Test
  public void kidCrudRoundTrip() {
    MySqlRepository<Kid> kidRepo =
        new MySqlRepository<Kid>(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword(), MySqlRepository.EntityKind.KID);

    Kid kid = new Kid("MySQL Kid", 2018, "n");
    kidRepo.save(kid);
    assertNotNull(kid.getId());

    Kid loaded = kidRepo.findById(kid.getId());
    assertNotNull(loaded);
    assertEquals("MySQL Kid", loaded.getDisplayName());

    loaded.setNotes("updated");
    kidRepo.update(loaded);
    Kid loaded2 = kidRepo.findById(loaded.getId());
    assertEquals("updated", loaded2.getNotes());

    List<Kid> all = kidRepo.findAll();
    assertTrue(all.size() >= 1);

    kidRepo.delete(loaded.getId());
    assertNull(kidRepo.findById(loaded.getId()));
  }

  @Test
  public void activityCrudRoundTripAndCascade() {
    MySqlRepository<Kid> kidRepo =
        new MySqlRepository<Kid>(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword(), MySqlRepository.EntityKind.KID);
    MySqlRepository<Activity> actRepo =
        new MySqlRepository<Activity>(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword(), MySqlRepository.EntityKind.ACTIVITY);

    Kid kid = new Kid("MySQL K", null, null);
    kidRepo.save(kid);

    Activity a = new Activity("Math", LocalDate.of(2026, 3, 25), LocalTime.of(9, 0), LocalTime.of(10, 0),
        ActivityType.STUDY, "Home", "N", kid.getId());
    actRepo.save(a);
    assertNotNull(a.getId());

    Activity loaded = actRepo.findById(a.getId());
    assertNotNull(loaded);
    assertEquals("Math", loaded.getTitle());

    loaded.setTitle("New");
    actRepo.update(loaded);
    Activity loaded2 = actRepo.findById(loaded.getId());
    assertEquals("New", loaded2.getTitle());

    // Cascade via kid delete
    kidRepo.delete(kid.getId());
    assertNull(actRepo.findById(a.getId()));
  }

  @Test
  public void updateThrowsWhenMissingIdOrNotFound() {
    MySqlRepository<Kid> kidRepo =
        new MySqlRepository<Kid>(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword(), MySqlRepository.EntityKind.KID);

    Kid missingId = new Kid("X", null, null);
    assertThrows(RepositoryException.class, () -> kidRepo.update(missingId));

    Kid notFound = new Kid("Y", null, null);
    notFound.setId(999999);
    assertThrows(RepositoryException.class, () -> kidRepo.update(notFound));
  }
}

