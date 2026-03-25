/**
 * @file BinaryRepository.java
 * @brief {@link IRepository} backed by Java serialization to one file.
 */
package com.eceirem.kidsplanner;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link IRepository} using Java serialization to a single file.
 *
 * @param <T> {@link Kid} or {@link Activity}
 */
public class BinaryRepository<T extends HasId<Integer>> implements IRepository<T> {
  /** Path to the binary store file. */
  private final String filePath;
  /** In-memory entities keyed by id. */
  private final Map<Integer, T> data;
  /** Monotonic id generator for new saves. */
  private int nextId;

  /**
   * Loads existing map from disk or starts empty if missing/unreadable.
   *
   * @param filePath path to the binary store
   */
  public BinaryRepository(String filePath) {
    this.filePath = filePath;
    this.data = loadData();
    this.nextId = calculateNextId(this.data);
  }

  /**
   * Assigns the next id, stores the entity, and flushes to disk.
   *
   * @param entity entity to store
   */
  @Override public void save(T entity) {
    entity.setId(nextId++);
    data.put(entity.getId(), entity);
    persist();
  }

  /** @param id primary key
   *  @return entity or {@code null} if missing */
  @Override public T findById(int id) {
    return data.get(id);
  }

  /** @return all entities (order undefined) */
  @Override public List<T> findAll() {
    return new ArrayList<T>(data.values());
  }

  /**
   * Replaces state for an existing id.
   *
   * @param entity entity with non-null id
   */
  @Override public void update(T entity) {
    Integer id = entity.getId();
    if (id == null || !data.containsKey(id)) {
      throw new RepositoryException("Record to update not found.");
    }
    data.put(id, entity);
    persist();
  }

  /** @param id primary key to remove */
  @Override public void delete(int id) {
    data.remove(id);
    persist();
  }

  /**
   * Deserializes the map from {@link #filePath}, or returns an empty map if missing or invalid.
   *
   * @return stored entities
   */
  @SuppressWarnings("unchecked")
  private Map<Integer, T> loadData() {
    try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(filePath))) {
      Object read = input.readObject();
      return (Map<Integer, T>) read;
    } catch (IOException | ClassNotFoundException e) {
      // Treat missing/corrupted/unknown binary data as empty store to keep the app usable.
      return new HashMap<Integer, T>();
    }
  }

  /** @return one greater than max existing id */
  private int calculateNextId(Map<Integer, T> currentData) {
    int max = 0;
    for (Integer id : currentData.keySet()) {
      if (id != null && id > max) {
        max = id;
      }
    }
    return max + 1;
  }

  /** Writes {@link #data} to {@link #filePath}. */
  private void persist() {
    try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(filePath))) {
      output.writeObject(data);
    } catch (IOException e) {
      throw new RepositoryException("Binary data could not be written.", e);
    }
  }
}
