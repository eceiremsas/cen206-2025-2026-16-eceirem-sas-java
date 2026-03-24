/**
 * @file RepositoryFactoryTest.java
 * @brief Unit tests for RepositoryFactory class.
 * @details Verifies factory behavior for supported and unsupported backends.
 */
package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * @class RepositoryFactoryTest
 * @brief Unit tests for RepositoryFactory.
 * @details This class tests only RepositoryFactory behavior.
 */
public class RepositoryFactoryTest {

  /**
   * @brief Verifies binary storage creates a repository instance.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws none This method does not throw checked exceptions.
   */
  @Test
  public void testCreateActivityRepositoryWithBinaryReturnsRepository() {
    IRepository<Activity> repository = RepositoryFactory.createActivityRepository(StorageType.BINARY);
    assertNotNull(repository);
  }

  /**
   * @brief Verifies unsupported storage throws repository exception.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws none This method does not throw checked exceptions.
   */
  @Test
  public void testCreateActivityRepositoryWithSqliteThrowsException() {
    RepositoryException exception = assertThrows(
        RepositoryException.class,
        () -> RepositoryFactory.createActivityRepository(StorageType.SQLITE));
    assertTrue(exception.getMessage().contains("sadece Binary backend aktif"));
  }

  /**
   * @brief Verifies mysql storage also throws repository exception.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws none This method does not throw checked exceptions.
   */
  @Test
  public void testCreateActivityRepositoryWithMysqlThrowsException() {
    RepositoryException exception = assertThrows(
        RepositoryException.class,
        () -> RepositoryFactory.createActivityRepository(StorageType.MYSQL));
    assertTrue(exception.getMessage().contains("sadece Binary backend aktif"));
  }
}
