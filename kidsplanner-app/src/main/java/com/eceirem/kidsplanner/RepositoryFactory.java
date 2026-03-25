package com.eceirem.kidsplanner;

import java.util.Locale;

/**
 * Factory for pluggable persistence (Binary / SQLite / MySQL). Active backend is switched at runtime.
 */
public final class RepositoryFactory {

  private static final Object LOCK = new Object();
  private static volatile StorageType activeStorage = StorageType.BINARY;
  private static volatile String sqliteJdbcUrl = "jdbc:sqlite:kidsplanner.db";
  private static volatile String mysqlJdbcUrl = defaultMysqlUrl();
  private static volatile String mysqlUser = defaultMysqlUser();
  private static volatile String mysqlPassword = defaultMysqlPassword();

  private RepositoryFactory() {
  }

  private static String defaultMysqlUrl() {
    String env = System.getenv("KIDSPLANNER_MYSQL_URL");
    if (env != null && !env.trim().isEmpty()) {
      return env.trim();
    }
    return "jdbc:mysql://localhost:3306/kidsplanner?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
  }

  private static String defaultMysqlUser() {
    String env = System.getenv("KIDSPLANNER_MYSQL_USER");
    return env != null && !env.trim().isEmpty() ? env.trim() : "root";
  }

  private static String defaultMysqlPassword() {
    String env = System.getenv("KIDSPLANNER_MYSQL_PASSWORD");
    return env != null && !env.trim().isEmpty() ? env.trim() : "kidsplanner";
  }

  public static StorageType getActiveStorage() {
    return activeStorage;
  }

  public static void setActiveStorage(StorageType storageType) {
    synchronized (LOCK) {
      activeStorage = storageType;
    }
  }

  public static void configureSqliteUrl(String jdbcUrl) {
    synchronized (LOCK) {
      sqliteJdbcUrl = jdbcUrl;
    }
  }

  public static void configureMysql(String jdbcUrl, String user, String password) {
    synchronized (LOCK) {
      mysqlJdbcUrl = jdbcUrl;
      mysqlUser = user;
      mysqlPassword = password;
    }
  }

  public static IRepository<Kid> createKidRepository() {
    switch (activeStorage) {
      case BINARY:
        return new BinaryRepository<Kid>("kidsplanner-kids.bin");
      case SQLITE:
        return new SqliteRepository<Kid>(sqliteJdbcUrl, SqliteRepository.EntityKind.KID);
      case MYSQL:
        return new MysqlRepository<Kid>(mysqlJdbcUrl, mysqlUser, mysqlPassword, MysqlRepository.EntityKind.KID);
      default:
        throw new RepositoryException("Desteklenmeyen depolama turu: " + activeStorage);
    }
  }

  public static IRepository<Activity> createActivityRepository() {
    switch (activeStorage) {
      case BINARY:
        return new BinaryRepository<Activity>("kidsplanner-activities.bin");
      case SQLITE:
        return new SqliteRepository<Activity>(sqliteJdbcUrl, SqliteRepository.EntityKind.ACTIVITY);
      case MYSQL:
        return new MysqlRepository<Activity>(mysqlJdbcUrl, mysqlUser, mysqlPassword, MysqlRepository.EntityKind.ACTIVITY);
      default:
        throw new RepositoryException("Desteklenmeyen depolama turu: " + activeStorage);
    }
  }

  public static StorageType parseStorageTypeOrNull(String text) {
    if (text == null) {
      return null;
    }
    String normalized = text.trim().toUpperCase(Locale.ROOT);
    if (normalized.isEmpty()) {
      return null;
    }
    try {
      return StorageType.valueOf(normalized);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  public static String describeJdbcTargets() {
    return "SQLite URL: " + sqliteJdbcUrl + System.lineSeparator()
        + "MySQL URL : " + mysqlJdbcUrl + " (user=" + mysqlUser + ")";
  }
}
