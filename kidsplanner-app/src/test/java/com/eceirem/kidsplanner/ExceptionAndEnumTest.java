/**
 * @file ExceptionAndEnumTest.java
 * @brief Tests for exception and enum classes.
 * @details Covers RepositoryException, ValidationException, and enum values.
 */
package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * @class ExceptionAndEnumTest
 * @brief Unit tests for small utility classes.
 * @details This class checks exception constructors and enum values.
 */
public class ExceptionAndEnumTest {

  /**
   * @brief Verifies RepositoryException stores message.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws none This method does not throw checked exceptions.
   */
  @Test
  public void testRepositoryExceptionWithMessage() {
    RepositoryException exception = new RepositoryException("repo error");
    assertEquals("repo error", exception.getMessage());
  }

  /**
   * @brief Verifies RepositoryException stores cause.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws none This method does not throw checked exceptions.
   */
  @Test
  public void testRepositoryExceptionWithCause() {
    Throwable cause = new IllegalStateException("x");
    RepositoryException exception = new RepositoryException("repo error", cause);
    assertEquals(cause, exception.getCause());
  }

  /**
   * @brief Verifies ValidationException stores message.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws none This method does not throw checked exceptions.
   */
  @Test
  public void testValidationExceptionWithMessage() {
    ValidationException exception = new ValidationException("validation");
    assertEquals("validation", exception.getMessage());
  }

  /**
   * @brief Verifies ActivityType enum values are accessible.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws none This method does not throw checked exceptions.
   */
  @Test
  public void testActivityTypeEnumContainsStudy() {
    assertNotNull(ActivityType.valueOf("STUDY"));
  }

  /**
   * @brief Verifies StorageType enum values are accessible.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws none This method does not throw checked exceptions.
   */
  @Test
  public void testStorageTypeEnumContainsMysql() {
    assertNotNull(StorageType.valueOf("MYSQL"));
  }
}
