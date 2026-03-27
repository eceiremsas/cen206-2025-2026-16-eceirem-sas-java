/**
 * @file MySqlRepositoryErrorPathTest.java
 * @brief Forces real MySQL SQL errors (no fakes) to cover catch branches.
 */
package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;

public class MySqlRepositoryErrorPathTest {
  private static final String SERVER_URL =
      "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
          + "&connectTimeout=2000&socketTimeout=2000";
  private static final String USER = "root";
  private static final String PASS = "kidsplanner";

  private static String tempDbUrl(String dbName) {
    return "jdbc:mysql://localhost:3306/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
        + "&connectTimeout=2000&socketTimeout=2000";
  }

  private static Object callPrivate(Object target, String name, Class<?>[] paramTypes, Object... args) throws Exception {
    Method m = target.getClass().getDeclaredMethod(name, paramTypes);
    m.setAccessible(true);
    return m.invoke(target, args);
  }

  private static Object callPrivateStatic(Class<?> clazz, String name, Class<?>[] paramTypes, Object... args) throws Exception {
    Method m = clazz.getDeclaredMethod(name, paramTypes);
    m.setAccessible(true);
    return m.invoke(null, args);
  }

  @Test
  public void findAllThrowsWhenActivityTableIsDropped() throws Exception {
    DockerComposeMysql.ensureRunning();
    String dbName = "kidsplanner_err_" + System.currentTimeMillis();
    String url = tempDbUrl(dbName);
    try {
      try (Connection c = DriverManager.getConnection(SERVER_URL, USER, PASS); Statement st = c.createStatement()) {
        st.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName + " DEFAULT CHARACTER SET utf8mb4");
      }
      MySqlRepository<Activity> repo = new MySqlRepository<Activity>(url, USER, PASS, MySqlRepository.EntityKind.ACTIVITY);
      try (Connection c = DriverManager.getConnection(url, USER, PASS); Statement st = c.createStatement()) {
        st.executeUpdate("DROP TABLE IF EXISTS activity");
      }
      RepositoryException ex = assertThrows(RepositoryException.class, () -> repo.findAll());
      assertTrue(ex.getMessage().toLowerCase().contains("list"));
    } finally {
      try (Connection c = DriverManager.getConnection(SERVER_URL, USER, PASS); Statement st = c.createStatement()) {
        st.executeUpdate("DROP DATABASE IF EXISTS " + dbName);
      }
    }
  }

  @Test
  public void findByIdThrowsWhenKidTableIsDropped() throws Exception {
    DockerComposeMysql.ensureRunning();
    String dbName = "kidsplanner_err_" + System.currentTimeMillis();
    String url = tempDbUrl(dbName);
    try {
      try (Connection c = DriverManager.getConnection(SERVER_URL, USER, PASS); Statement st = c.createStatement()) {
        st.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName + " DEFAULT CHARACTER SET utf8mb4");
      }
      MySqlRepository<Kid> repo = new MySqlRepository<Kid>(url, USER, PASS, MySqlRepository.EntityKind.KID);
      try (Connection c = DriverManager.getConnection(url, USER, PASS); Statement st = c.createStatement()) {
        st.executeUpdate("SET FOREIGN_KEY_CHECKS=0");
        st.executeUpdate("DROP TABLE IF EXISTS activity");
        st.executeUpdate("DROP TABLE IF EXISTS kid");
        st.executeUpdate("SET FOREIGN_KEY_CHECKS=1");
      }
      RepositoryException ex = assertThrows(RepositoryException.class, () -> repo.findById(1));
      assertTrue(ex.getMessage().toLowerCase().contains("read"));
    } finally {
      try (Connection c = DriverManager.getConnection(SERVER_URL, USER, PASS); Statement st = c.createStatement()) {
        st.executeUpdate("DROP DATABASE IF EXISTS " + dbName);
      }
    }
  }

  @Test
  public void findAllKidsThrowsWhenKidTableIsDropped() throws Exception {
    DockerComposeMysql.ensureRunning();
    String dbName = "kidsplanner_err_" + System.currentTimeMillis();
    String url = tempDbUrl(dbName);
    try {
      try (Connection c = DriverManager.getConnection(SERVER_URL, USER, PASS); Statement st = c.createStatement()) {
        st.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName + " DEFAULT CHARACTER SET utf8mb4");
      }
      MySqlRepository<Kid> repo = new MySqlRepository<Kid>(url, USER, PASS, MySqlRepository.EntityKind.KID);
      try (Connection c = DriverManager.getConnection(url, USER, PASS); Statement st = c.createStatement()) {
        st.executeUpdate("SET FOREIGN_KEY_CHECKS=0");
        st.executeUpdate("DROP TABLE IF EXISTS activity");
        st.executeUpdate("DROP TABLE IF EXISTS kid");
        st.executeUpdate("SET FOREIGN_KEY_CHECKS=1");
      }
      RepositoryException ex = assertThrows(RepositoryException.class, () -> repo.findAll());
      assertTrue(ex.getMessage().toLowerCase().contains("list kids"));
    } finally {
      try (Connection c = DriverManager.getConnection(SERVER_URL, USER, PASS); Statement st = c.createStatement()) {
        st.executeUpdate("DROP DATABASE IF EXISTS " + dbName);
      }
    }
  }

  @Test
  public void findByIdThrowsWhenActivityTableIsDropped() throws Exception {
    DockerComposeMysql.ensureRunning();
    String dbName = "kidsplanner_err_" + System.currentTimeMillis();
    String url = tempDbUrl(dbName);
    try {
      try (Connection c = DriverManager.getConnection(SERVER_URL, USER, PASS); Statement st = c.createStatement()) {
        st.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName + " DEFAULT CHARACTER SET utf8mb4");
      }
      MySqlRepository<Activity> repo = new MySqlRepository<Activity>(url, USER, PASS, MySqlRepository.EntityKind.ACTIVITY);
      try (Connection c = DriverManager.getConnection(url, USER, PASS); Statement st = c.createStatement()) {
        st.executeUpdate("DROP TABLE IF EXISTS activity");
      }
      RepositoryException ex = assertThrows(RepositoryException.class, () -> repo.findById(1));
      assertTrue(ex.getMessage().toLowerCase().contains("read activity"));
    } finally {
      try (Connection c = DriverManager.getConnection(SERVER_URL, USER, PASS); Statement st = c.createStatement()) {
        st.executeUpdate("DROP DATABASE IF EXISTS " + dbName);
      }
    }
  }

  @Test
  public void parseAndStripJdbcHelpersCoverFallbackBranches() throws Exception {
    assertNull((String) callPrivateStatic(MySqlRepository.class, "parseDatabaseName",
        new Class<?>[] {String.class}, "jdbc:mysql:invalid"));
    assertNull((String) callPrivateStatic(MySqlRepository.class, "parseDatabaseName",
        new Class<?>[] {String.class}, "jdbc:mysql://localhost:3306"));
    assertEquals("jdbc:mysql:invalid",
        (String) callPrivateStatic(MySqlRepository.class, "stripDatabaseFromJdbcUrl",
            new Class<?>[] {String.class}, "jdbc:mysql:invalid"));
    assertEquals("jdbc:mysql://localhost:3306",
        (String) callPrivateStatic(MySqlRepository.class, "stripDatabaseFromJdbcUrl",
            new Class<?>[] {String.class}, "jdbc:mysql://localhost:3306"));
  }

  @Test
  public void parseAndStripJdbcHelpersCoverNominalBranches() throws Exception {
    assertEquals("kidsplanner",
        (String) callPrivateStatic(MySqlRepository.class, "parseDatabaseName",
            new Class<?>[] {String.class}, "jdbc:mysql://localhost:3306/kidsplanner?useSSL=false"));
    assertEquals("kidsplanner",
        (String) callPrivateStatic(MySqlRepository.class, "parseDatabaseName",
            new Class<?>[] {String.class}, "jdbc:mysql://localhost:3306/kidsplanner"));
    assertEquals("jdbc:mysql://localhost:3306/?useSSL=false",
        (String) callPrivateStatic(MySqlRepository.class, "stripDatabaseFromJdbcUrl",
            new Class<?>[] {String.class}, "jdbc:mysql://localhost:3306/kidsplanner?useSSL=false"));
    assertEquals("jdbc:mysql://localhost:3306/",
        (String) callPrivateStatic(MySqlRepository.class, "stripDatabaseFromJdbcUrl",
            new Class<?>[] {String.class}, "jdbc:mysql://localhost:3306/kidsplanner"));
  }

  @Test
  public void buildFriendlySchemaErrorCovers1049AndFallback() throws Exception {
    DockerComposeMysql.ensureRunning();
    String dbName = "kidsplanner_err_" + System.currentTimeMillis();
    String url = tempDbUrl(dbName);
    try {
      try (Connection c = DriverManager.getConnection(SERVER_URL, USER, PASS); Statement st = c.createStatement()) {
        st.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName + " DEFAULT CHARACTER SET utf8mb4");
      }
      MySqlRepository<Kid> repo = new MySqlRepository<Kid>(url, USER, PASS, MySqlRepository.EntityKind.KID);
      SQLException e1049 = new SQLException("unknown db", "42000", 1049);
      String msg1049 = (String) callPrivate(repo, "buildFriendlySchemaError", new Class<?>[] {SQLException.class}, e1049);
      assertTrue(msg1049.toLowerCase().contains("database not found"));

      SQLException eOther = new SQLException("other", "HY000", 2000);
      String msgOther = (String) callPrivate(repo, "buildFriendlySchemaError", new Class<?>[] {SQLException.class}, eOther);
      assertTrue(msgOther.toLowerCase().contains("could not create mysql schema"));
    } finally {
      try (Connection c = DriverManager.getConnection(SERVER_URL, USER, PASS); Statement st = c.createStatement()) {
        st.executeUpdate("DROP DATABASE IF EXISTS " + dbName);
      }
    }
  }

  @Test
  public void buildFriendlySchemaErrorCoversConnectionAndAuth() throws Exception {
    DockerComposeMysql.ensureRunning();
    String dbName = "kidsplanner_err_" + System.currentTimeMillis();
    String url = tempDbUrl(dbName);
    try {
      try (Connection c = DriverManager.getConnection(SERVER_URL, USER, PASS); Statement st = c.createStatement()) {
        st.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName + " DEFAULT CHARACTER SET utf8mb4");
      }
      MySqlRepository<Kid> repo = new MySqlRepository<Kid>(url, USER, PASS, MySqlRepository.EntityKind.KID);

      SQLException conn = new SQLException("connect", "08S01", 0);
      String msgConn = (String) callPrivate(repo, "buildFriendlySchemaError", new Class<?>[] {SQLException.class}, conn);
      assertTrue(msgConn.toLowerCase().contains("could not connect"));

      SQLException auth = new SQLException("auth", "28000", 1045);
      String msgAuth = (String) callPrivate(repo, "buildFriendlySchemaError", new Class<?>[] {SQLException.class}, auth);
      assertTrue(msgAuth.toLowerCase().contains("authentication failed"));
    } finally {
      try (Connection c = DriverManager.getConnection(SERVER_URL, USER, PASS); Statement st = c.createStatement()) {
        st.executeUpdate("DROP DATABASE IF EXISTS " + dbName);
      }
    }
  }

  @Test
  public void constructorCreatesMissingDatabaseViaEnsurePath() throws Exception {
    DockerComposeMysql.ensureRunning();
    String dbName = "kidsplanner_missing_" + System.currentTimeMillis();
    String url = tempDbUrl(dbName);
    try {
      try (Connection c = DriverManager.getConnection(SERVER_URL, USER, PASS); Statement st = c.createStatement()) {
        st.executeUpdate("DROP DATABASE IF EXISTS " + dbName);
      }
      MySqlRepository<Kid> repo = new MySqlRepository<Kid>(url, USER, PASS, MySqlRepository.EntityKind.KID);
      assertTrue(repo != null);
      try (Connection c = DriverManager.getConnection(SERVER_URL, USER, PASS); Statement st = c.createStatement()) {
        java.sql.ResultSet rs = st.executeQuery("SHOW DATABASES LIKE '" + dbName + "'");
        assertTrue(rs.next());
      }
    } finally {
      try (Connection c = DriverManager.getConnection(SERVER_URL, USER, PASS); Statement st = c.createStatement()) {
        st.executeUpdate("DROP DATABASE IF EXISTS " + dbName);
      }
    }
  }

  @Test
  public void deleteAndSaveAndUpdateCatchBranchesAreCovered() throws Exception {
    DockerComposeMysql.ensureRunning();
    String dbName = "kidsplanner_err_" + System.currentTimeMillis();
    String url = tempDbUrl(dbName);
    try {
      try (Connection c = DriverManager.getConnection(SERVER_URL, USER, PASS); Statement st = c.createStatement()) {
        st.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName + " DEFAULT CHARACTER SET utf8mb4");
      }

      MySqlRepository<Kid> kidRepo = new MySqlRepository<Kid>(url, USER, PASS, MySqlRepository.EntityKind.KID);
      MySqlRepository<Activity> actRepo = new MySqlRepository<Activity>(url, USER, PASS, MySqlRepository.EntityKind.ACTIVITY);

      try (Connection c = DriverManager.getConnection(url, USER, PASS); Statement st = c.createStatement()) {
        st.executeUpdate("SET FOREIGN_KEY_CHECKS=0");
        st.executeUpdate("DROP TABLE IF EXISTS activity");
        st.executeUpdate("DROP TABLE IF EXISTS kid");
        st.executeUpdate("SET FOREIGN_KEY_CHECKS=1");
      }

      RepositoryException exDelete = assertThrows(RepositoryException.class, () -> actRepo.delete(1));
      assertTrue(exDelete.getMessage().toLowerCase().contains("delete"));

      Kid k = new Kid("K", null, null);
      RepositoryException exSaveKid = assertThrows(RepositoryException.class, () -> kidRepo.save(k));
      assertTrue(exSaveKid.getMessage().toLowerCase().contains("insert kid"));

      Activity a = new Activity("A", java.time.LocalDate.of(2026, 3, 25), java.time.LocalTime.of(9, 0),
          java.time.LocalTime.of(10, 0), ActivityType.STUDY, null, null, Integer.valueOf(1));
      RepositoryException exSaveAct = assertThrows(RepositoryException.class, () -> actRepo.save(a));
      assertTrue(exSaveAct.getMessage().toLowerCase().contains("insert activity"));

      Kid ku = new Kid("U", null, null);
      ku.setId(1);
      RepositoryException exUpdKid = assertThrows(RepositoryException.class, () -> kidRepo.update(ku));
      assertTrue(exUpdKid.getMessage().toLowerCase().contains("update kid"));

      Activity au = new Activity("U2", java.time.LocalDate.of(2026, 3, 25), java.time.LocalTime.of(9, 0),
          java.time.LocalTime.of(10, 0), ActivityType.STUDY, null, null, Integer.valueOf(1));
      au.setId(1);
      RepositoryException exUpdAct = assertThrows(RepositoryException.class, () -> actRepo.update(au));
      assertTrue(exUpdAct.getMessage().toLowerCase().contains("update activity"));
    } finally {
      try (Connection c = DriverManager.getConnection(SERVER_URL, USER, PASS); Statement st = c.createStatement()) {
        st.executeUpdate("DROP DATABASE IF EXISTS " + dbName);
      }
    }
  }
}

