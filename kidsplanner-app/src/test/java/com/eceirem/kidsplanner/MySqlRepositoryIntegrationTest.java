package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
/**
 * Real MySQL integration tests.
 *
 * Uses the project's real MySQL container from docker-compose.yml (no fakes).
 */
public class MySqlRepositoryIntegrationTest {

  private static final String JDBC_URL =
      "jdbc:mysql://localhost:3306/kidsplanner?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
          + "&connectTimeout=2000&socketTimeout=2000";
  private static final String USER = "root";
  private static final String PASS = "kidsplanner";

  @BeforeAll
  public static void startMysql() {
    DockerComposeMysql.ensureRunning();
  }

  @Test
  public void kidCrudRoundTrip() {
    MySqlRepository<Kid> kidRepo =
        new MySqlRepository<Kid>(JDBC_URL, USER, PASS, MySqlRepository.EntityKind.KID);

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
        new MySqlRepository<Kid>(JDBC_URL, USER, PASS, MySqlRepository.EntityKind.KID);
    MySqlRepository<Activity> actRepo =
        new MySqlRepository<Activity>(JDBC_URL, USER, PASS, MySqlRepository.EntityKind.ACTIVITY);

    Kid kid = new Kid("MySQL K", null, null);
    kidRepo.save(kid);
    // Reload to exercise mapKid birth_year NULL -> wasNull() branch.
    Kid loadedKid = kidRepo.findById(kid.getId().intValue());
    assertNotNull(loadedKid);
    assertNull(loadedKid.getBirthYear());

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

    // Exercise findAll on the Activity repository kind.
    List<Activity> allActs = actRepo.findAll();
    assertTrue(allActs.size() >= 1);

    // Cascade via kid delete
    kidRepo.delete(kid.getId());
    assertNull(actRepo.findById(a.getId()));
  }

  @Test
  public void updateThrowsWhenMissingIdOrNotFound() {
    MySqlRepository<Kid> kidRepo =
        new MySqlRepository<Kid>(JDBC_URL, USER, PASS, MySqlRepository.EntityKind.KID);

    Kid missingId = new Kid("X", null, null);
    assertThrows(RepositoryException.class, () -> kidRepo.update(missingId));

    Kid notFound = new Kid("Y", null, null);
    notFound.setId(999999);
    assertThrows(RepositoryException.class, () -> kidRepo.update(notFound));

    MySqlRepository<Activity> actRepo =
        new MySqlRepository<Activity>(JDBC_URL, USER, PASS, MySqlRepository.EntityKind.ACTIVITY);
    Activity missingActId = new Activity("T", LocalDate.of(2026, 3, 25), LocalTime.of(9, 0), LocalTime.of(10, 0),
        ActivityType.STUDY, null, null, Integer.valueOf(1));
    assertThrows(RepositoryException.class, () -> actRepo.update(missingActId));

    Activity actNotFound = new Activity("T2", LocalDate.of(2026, 3, 25), LocalTime.of(9, 0), LocalTime.of(10, 0),
        ActivityType.STUDY, null, null, Integer.valueOf(1));
    actNotFound.setId(999999);
    assertThrows(RepositoryException.class, () -> actRepo.update(actNotFound));
  }
}

