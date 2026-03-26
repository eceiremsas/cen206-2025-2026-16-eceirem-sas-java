/**
 * @file RepositoryFactoryTest.java
 * @brief Unit tests for RepositoryFactory class.
 * @details Verifies factory behavior for supported backends.
 */
package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @class RepositoryFactoryTest
 * @brief Unit tests for RepositoryFactory.
 * @details This class tests only RepositoryFactory behavior.
 */
public class RepositoryFactoryTest {
  private static final String JDBC_URL =
      "jdbc:mysql://localhost:3306/kidsplanner?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
          + "&connectTimeout=2000&socketTimeout=2000";
  private static final String USER = "root";
  private static final String PASS = "kidsplanner";

  @BeforeAll
  public static void startMysql() {
    DockerComposeMysql.ensureRunning();
  }

  /**
   * @brief Restores binary storage and removes sqlite test file.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws java.io.IOException If delete fails.
   */
  @AfterEach
  public void resetFactory() throws java.io.IOException {
    RepositoryFactory.setActiveStorage(StorageType.BINARY);
    Files.deleteIfExists(Paths.get("target/repo-factory-sqlite.db"));
  }

  /**
   * @brief Verifies binary storage creates a repository instance.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws none This method does not throw checked exceptions.
   */
  @Test
  public void testCreateActivityRepositoryWithBinaryReturnsRepository() {
    RepositoryFactory.setActiveStorage(StorageType.BINARY);
    IRepository<Activity> repository = RepositoryFactory.createActivityRepository();
    assertNotNull(repository);
  }

  /**
   * @brief Verifies sqlite storage creates a repository instance.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws none This method does not throw checked exceptions.
   */
  @Test
  public void testCreateActivityRepositoryWithSqliteReturnsRepository() {
    RepositoryFactory.configureSqliteUrl("jdbc:sqlite:target/repo-factory-sqlite.db");
    RepositoryFactory.setActiveStorage(StorageType.SQLITE);
    IRepository<Activity> repository = RepositoryFactory.createActivityRepository();
    assertNotNull(repository);
  }

  /**
   * @brief Verifies mysql factory when server is reachable; skips otherwise.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws none This method does not throw checked exceptions.
   */
  @Test
  public void testCreateActivityRepositoryWithMysqlReturnsRepositoryWhenServerUp() {
    RepositoryFactory.configureMysql(JDBC_URL, USER, PASS);
    RepositoryFactory.setActiveStorage(StorageType.MYSQL);
    IRepository<Activity> repository = RepositoryFactory.createActivityRepository();
    assertNotNull(repository);
  }

  @Test
  public void testCreateKidRepositoryWithMysqlReturnsRepositoryWhenServerUp() {
    RepositoryFactory.configureMysql(JDBC_URL, USER, PASS);
    RepositoryFactory.setActiveStorage(StorageType.MYSQL);
    IRepository<Kid> repository = RepositoryFactory.createKidRepository();
    assertNotNull(repository);
  }

  /**
   * @brief Verifies kid repository is created for sqlite backend.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws none This method does not throw checked exceptions.
   */
  @Test
  public void testCreateKidRepositoryWithSqliteReturnsRepository() {
    RepositoryFactory.configureSqliteUrl("jdbc:sqlite:target/repo-factory-sqlite.db");
    RepositoryFactory.setActiveStorage(StorageType.SQLITE);
    IRepository<Kid> repository = RepositoryFactory.createKidRepository();
    assertNotNull(repository);
  }

  /**
   * @brief Verifies parse helper accepts mysql keyword.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws none This method does not throw checked exceptions.
   */
  @Test
  public void testParseStorageTypeHandlesMysqlKeyword() {
    StorageType parsed = RepositoryFactory.parseStorageTypeOrNull("mysql");
    assertEquals(StorageType.MYSQL, parsed);
  }
}
