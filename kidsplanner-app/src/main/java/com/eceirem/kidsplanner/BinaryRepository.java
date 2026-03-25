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

public class BinaryRepository<T extends HasId<Integer>> implements IRepository<T> {
  private final String filePath;
  private final Map<Integer, T> data;
  private int nextId;

  public BinaryRepository(String filePath) {
    this.filePath = filePath;
    this.data = loadData();
    this.nextId = calculateNextId(this.data);
  }

  @Override
  public void save(T entity) {
    entity.setId(nextId++);
    data.put(entity.getId(), entity);
    persist();
  }

  @Override
  public T findById(int id) {
    return data.get(id);
  }

  @Override
  public List<T> findAll() {
    return new ArrayList<T>(data.values());
  }

  @Override
  public void update(T entity) {
    Integer id = entity.getId();
    if (id == null || !data.containsKey(id)) {
      throw new RepositoryException("Guncellenecek kayit bulunamadi.");
    }
    data.put(id, entity);
    persist();
  }

  @Override
  public void delete(int id) {
    data.remove(id);
    persist();
  }

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

  private int calculateNextId(Map<Integer, T> currentData) {
    int max = 0;
    for (Integer id : currentData.keySet()) {
      if (id != null && id > max) {
        max = id;
      }
    }
    return max + 1;
  }

  private void persist() {
    try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(filePath))) {
      output.writeObject(data);
    } catch (IOException e) {
      throw new RepositoryException("Binary veri yazilamadi.", e);
    }
  }
}
