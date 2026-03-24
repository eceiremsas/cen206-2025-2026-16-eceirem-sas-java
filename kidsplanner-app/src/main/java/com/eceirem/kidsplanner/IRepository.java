package com.eceirem.kidsplanner;

import java.util.List;

public interface IRepository<T extends HasId<Integer>> {
  void save(T entity);

  T findById(int id);

  List<T> findAll();

  void update(T entity);

  void delete(int id);
}
