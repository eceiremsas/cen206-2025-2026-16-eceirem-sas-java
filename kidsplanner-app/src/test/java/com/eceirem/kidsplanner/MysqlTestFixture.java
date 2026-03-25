package com.eceirem.kidsplanner;

/**
 * JDBC settings for MySQL integration tests. Uses environment variables when set (GitHub Actions);
 * otherwise defaults match {@code docker-compose.yml} at repo root ({@code docker compose up -d}).
 */
public final class MysqlTestFixture {

  private static final String ENV_JDBC = "KIDSPLANNER_TEST_MYSQL_JDBC";
  private static final String ENV_USER = "KIDSPLANNER_TEST_MYSQL_USER";
  private static final String ENV_PW = "KIDSPLANNER_TEST_MYSQL_PASSWORD";

  private static final String DEFAULT_JDBC =
      "jdbc:mysql://127.0.0.1:3306/kidsplanner?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

  private MysqlTestFixture() {
  }

  public static String jdbcUrl() {
    String u = System.getenv(ENV_JDBC);
    return (u != null && !u.trim().isEmpty()) ? u.trim() : DEFAULT_JDBC;
  }

  public static String user() {
    String u = System.getenv(ENV_USER);
    return (u != null && !u.trim().isEmpty()) ? u.trim() : "root";
  }

  public static String password() {
    String p = System.getenv(ENV_PW);
    return p != null ? p : "kidsplanner";
  }
}
