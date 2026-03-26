/**
 * @file StorageType.java
 * @brief Persistence backend selector enum.
 */
package com.eceirem.kidsplanner;

/**
 * Backend selector for {@link RepositoryFactory}.
 */
public enum StorageType {
  /** {@link BinaryRepository} */
  BINARY,
  /** {@link SqliteRepository} */
  SQLITE,
  /** {@link MySqlRepository} */
  MYSQL
}
