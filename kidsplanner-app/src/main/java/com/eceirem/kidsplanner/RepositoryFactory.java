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
    return "jdbc:mysql://localhost:3306/kidsplanner?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
  }

  private static String defaultMysqlUser() {
    return "root";
  }

  private static String defaultMysqlPassword() {
    return "kidsplanner";
  }

  /** @return active {@link StorageType} for new repositories */
  public static StorageType getActiveStorage() {
    return activeStorage;
  }

  /** @param storageType backend to use until changed */
  public static void setActiveStorage(StorageType storageType) {
    synchronized (LOCK) {
      activeStorage = storageType;
    }
  }

  /** @param jdbcUrl SQLite JDBC URL when {@link StorageType#SQLITE} is active */
  public static void configureSqliteUrl(String jdbcUrl) {
    synchronized (LOCK) {
      sqliteJdbcUrl = jdbcUrl;
    }
  }

  /**
   * @param jdbcUrl MySQL URL including database
   * @param user DB user
   * @param password DB password
   */
  public static void configureMysql(String jdbcUrl, String user, String password) {
    synchronized (LOCK) {
      mysqlJdbcUrl = jdbcUrl;
      mysqlUser = user;
      mysqlPassword = password;
    }
  }

  /** @return {@link Kid} repository for current {@link StorageType} */
  public static IRepository<Kid> createKidRepository() {
    switch (activeStorage) {
      case BINARY:
        return new BinaryRepository<Kid>("kidsplanner-kids.bin");
      case SQLITE:
        return new SqliteRepository<Kid>(sqliteJdbcUrl, SqliteRepository.EntityKind.KID);
      case MYSQL:
        return new MysqlRepository<Kid>(mysqlJdbcUrl, mysqlUser, mysqlPassword, MysqlRepository.EntityKind.KID);
    }
    throw new RepositoryException("Unsupported storage type: " + activeStorage);
  }

  /** @return {@link Activity} repository for current {@link StorageType} */
  public static IRepository<Activity> createActivityRepository() {
    switch (activeStorage) {
      case BINARY:
        return new BinaryRepository<Activity>("kidsplanner-activities.bin");
      case SQLITE:
        return new SqliteRepository<Activity>(sqliteJdbcUrl, SqliteRepository.EntityKind.ACTIVITY);
      case MYSQL:
        return new MysqlRepository<Activity>(mysqlJdbcUrl, mysqlUser, mysqlPassword, MysqlRepository.EntityKind.ACTIVITY);
    }
    throw new RepositoryException("Unsupported storage type: " + activeStorage);
  }

  /**
   * @param text e.g. {@code BINARY}, {@code SQLITE}, {@code MYSQL}
   * @return enum or {@code null}
   */
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

  /** @return JDBC URLs for diagnostics */
  public static String describeJdbcTargets() {
    return "SQLite URL: " + sqliteJdbcUrl + System.lineSeparator()
        + "MySQL URL : " + mysqlJdbcUrl + " (user=" + mysqlUser + ")";
  }
}
