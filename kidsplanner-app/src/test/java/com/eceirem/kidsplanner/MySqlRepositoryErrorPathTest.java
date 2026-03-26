/**
 * @file MySqlRepositoryErrorPathTest.java
 * @brief Forces real MySQL SQL errors (no fakes) to cover catch branches.
 */
package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
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
}

