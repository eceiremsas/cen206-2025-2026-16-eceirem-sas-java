package com.eceirem.kidsplanner;

public final class RepositoryFactory {

  private RepositoryFactory() {
  }

  public static IRepository<Activity> createActivityRepository(StorageType storageType) {
    if (storageType == StorageType.BINARY) {
      return new BinaryRepository<Activity>("kidsplanner-activities.bin");
    }
    throw new RepositoryException("Bu adimda sadece Binary backend aktif. Sonraki adimda SQLite/MySQL eklenecek.");
  }
}
