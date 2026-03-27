/**
 * @file MySqlRepositoryBehaviorTest.java
 * @brief Extra MySQL repository tests to increase branch coverage.
 */
package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MySqlRepositoryBehaviorTest {
  private static final String SERVER_URL =
      "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
          + "&connectTimeout=2000&socketTimeout=2000";
  private static final String USER = "root";
  private static final String PASS = "kidsplanner";

  @BeforeAll
  public static void startMysql() {
    DockerComposeMysql.ensureRunning();
  }

  @Test
  public void constructorCreatesMissingDatabaseWhenUnknownDatabaseErrorOccurs() throws Exception {
    String dbName = "kidsplanner_cov_" + System.currentTimeMillis();
    String dbUrl = "jdbc:mysql://localhost:3306/" + dbName
        + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
        + "&connectTimeout=2000&socketTimeout=2000";

    try {
      MySqlRepository<Kid> kidRepo = new MySqlRepository<Kid>(dbUrl, USER, PASS, MySqlRepository.EntityKind.KID);
      Kid kid = new Kid("DbCreated", null, null);
      kidRepo.save(kid);
      assertNotNull(kid.getId());

      MySqlRepository<Activity> actRepo = new MySqlRepository<Activity>(dbUrl, USER, PASS, MySqlRepository.EntityKind.ACTIVITY);
      Activity a = new Activity("A", LocalDate.of(2026, 3, 25), LocalTime.of(9, 0), LocalTime.of(10, 0),
          ActivityType.STUDY, null, null, kid.getId());
      actRepo.save(a);
      assertNotNull(a.getId());
    } finally {
      // Cleanup the database to avoid polluting local MySQL.
      try (Connection c = DriverManager.getConnection(SERVER_URL, USER, PASS);
          Statement st = c.createStatement()) {
        st.executeUpdate("DROP DATABASE IF EXISTS " + dbName);
      }
    }
  }

  @Test
  public void constructorThrowsFriendlyMessageOnAuthenticationFailure() {
    String url = "jdbc:mysql://localhost:3306/kidsplanner?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
        + "&connectTimeout=2000&socketTimeout=2000";
    RepositoryException ex = assertThrows(RepositoryException.class,
        () -> new MySqlRepository<Kid>(url, USER, "wrong-password", MySqlRepository.EntityKind.KID));
    assertTrue(ex.getMessage().toLowerCase().contains("authentication failed"));
  }
}

