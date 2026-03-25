/**
 * @file IRepository.java
 * @brief Generic persistence operations for planner entities.
 */
package com.eceirem.kidsplanner;

import java.util.List;

/**
 * Persistence operations for entities with an {@code Integer} id ({@link Kid}, {@link Activity}).
 *
 * @param <T> stored entity type
 */
public interface IRepository<T extends HasId<Integer>> {

  /**
   * Persists a new entity (assigns id) or replaces state per implementation.
   *
   * @param entity entity to store
   */
  void save(T entity);

  /**
   * @param id primary key
   * @return entity or {@code null} if missing
   */
  T findById(int id);

  /** @return all entities (order undefined) */
  List<T> findAll();

  /**
   * Updates an existing row; {@code entity.getId()} must refer to a stored record.
   *
   * @param entity entity with non-null id
   */
  void update(T entity);

  /**
   * Deletes by id if present.
   *
   * @param id primary key
   */
  void delete(int id);
}
