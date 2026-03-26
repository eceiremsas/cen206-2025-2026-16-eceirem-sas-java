/**
 * @file SqliteRepositorySchemaErrorTest.java
 * @brief Covers SqliteRepository schema error path.
 */
package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class SqliteRepositorySchemaErrorTest {

  @Test
  public void constructorThrowsRepositoryExceptionWhenDatabaseFileCannotBeOpened() {
    // Use an obviously invalid path on most Windows machines to trigger "unable to open database file".
    String badUrl = "jdbc:sqlite:Z:\\\\this\\\\path\\\\does\\\\not\\\\exist\\\\kidsplanner.db";
    RepositoryException ex = assertThrows(RepositoryException.class,
        () -> new SqliteRepository<Kid>(badUrl, SqliteRepository.EntityKind.KID));
    assertTrue(ex.getMessage().contains("Could not create SQLite schema."));
  }
}

