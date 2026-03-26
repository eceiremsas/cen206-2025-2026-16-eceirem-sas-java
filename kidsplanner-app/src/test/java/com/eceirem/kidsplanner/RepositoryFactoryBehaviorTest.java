/**
 * @file RepositoryFactoryBehaviorTest.java
 * @brief Tests covering RepositoryFactory parsing and description branches.
 */
package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class RepositoryFactoryBehaviorTest {

  @AfterEach
  public void reset() {
    RepositoryFactory.setActiveStorage(StorageType.BINARY);
  }

  @Test
  public void parseStorageTypeOrNullHandlesNullBlankAndInvalid() {
    assertNull(RepositoryFactory.parseStorageTypeOrNull(null));
    assertNull(RepositoryFactory.parseStorageTypeOrNull(""));
    assertNull(RepositoryFactory.parseStorageTypeOrNull("   "));
    assertNull(RepositoryFactory.parseStorageTypeOrNull("nope"));
    assertEquals(StorageType.BINARY, RepositoryFactory.parseStorageTypeOrNull(" binary "));
    assertEquals(StorageType.SQLITE, RepositoryFactory.parseStorageTypeOrNull("sqlite"));
    assertEquals(StorageType.MYSQL, RepositoryFactory.parseStorageTypeOrNull("MYSQL"));
  }

  @Test
  public void describeJdbcTargetsIncludesConfiguredUrls() {
    RepositoryFactory.configureSqliteUrl("jdbc:sqlite:target/x.db");
    RepositoryFactory.configureMysql("jdbc:mysql://example.invalid:3306/kidsplanner", "u", "p");
    String desc = RepositoryFactory.describeJdbcTargets();
    assertTrue(desc.contains("SQLite URL: jdbc:sqlite:target/x.db"));
    assertTrue(desc.contains("MySQL URL"));
    assertTrue(desc.contains("user=u"));
  }

  @Test
  public void createRepositoriesThrowWhenStorageIsNull() {
    RepositoryFactory.setActiveStorage(null);
    assertThrows(RepositoryException.class, () -> RepositoryFactory.createKidRepository());
    assertThrows(RepositoryException.class, () -> RepositoryFactory.createActivityRepository());
  }
}

